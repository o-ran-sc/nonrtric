// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2022: Nordix Foundation
//   %%
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//   ========================LICENSE_END===================================
//

package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"flag"
	"fmt"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v2"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/getter"
	"helm.sh/helm/v3/pkg/kube"
	"helm.sh/helm/v3/pkg/repo"
	"io/ioutil"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/cli-runtime/pkg/genericclioptions"
	kubernetes "k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"net/http"
	"os"
	"path/filepath"
)

var settings *cli.EnvSettings
var chartRequested *chart.Chart

//var url string
var repoName string
var chartName string
var releaseName string
var namespace string

type ChartInfo struct {
	Name       string                 `json:",omitempty"`
	Namespace  string                 `json:",omitempty"`
	Revision   int                    `json:",omitempty"`
	Updated    string                 `json:",omitempty"`
	Status     string                 `json:",omitempty"`
	Chart      string                 `json:",omitempty"`
	AppVersion string                 `json:",omitempty"`
	Values     map[string]interface{} `json:"-"`
}

type Rapp struct {
	Type            string
	SecurityEnabled bool
	Realm           string
	Client          string
	Roles           []struct {
		Role   string
		Grants []string
	}
	Apps []struct {
		Prefix  string
		Methods []string
	}
}

var rapp Rapp

const (
	host     = "postgres.default"
	port     = 5432
	user     = "capif"
	password = "capif"
	dbname   = "capif"
)

func runInstall(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	chartName = query.Get("chart")
	releaseName = chartName
	fmt.Println("Installing ", chartName)

	var msg string
	var chart string
	var install *action.Install
	chartMuseumService, chartMuseumPort := findService("chartmuseum", "default")
	fmt.Printf("Chart Museum service:%s, Port:%d\n", chartMuseumService, chartMuseumPort)
	url := "http://" + chartMuseumService + ":" + fmt.Sprint(chartMuseumPort)
	if !chartInstalled(chartName) {
		// Add repo
		fmt.Printf("Adding %s to Helm Repo\n", url)
		_, err := addToRepo(url)
		if err != nil {
			msg = err.Error()
		} else {
			install, err = dryRun()
			if err != nil {
				msg = err.Error()
			} else {
				if rapp.SecurityEnabled && rapp.Type == "provider" {
					// keycloak client setup
					fmt.Println("Setting up keycloak")
					_, err = http.Get("http://rapps-keycloak-mgr.default/create?realm=" + rapp.Realm + "&name=" + rapp.Client + "&role=" + rapp.Roles[0].Role)
					if err != nil {
						msg = err.Error()
					} else {
						fmt.Println("Setting up istio")
						_, err := http.Get("http://rapps-istio-mgr.default/create?name=" + chartName + "&realm=" + rapp.Realm + "&role=" + rapp.Roles[0].Role + "&method=" + rapp.Roles[0].Grants[0])
						if err != nil {
							msg = err.Error()
						} else {
							// Install chart
							fmt.Printf("Installing chart %s to %s namespace\n", chartName, namespace)
							chart, err = installHelmChart(install)
							if err != nil {
								msg = "Error occurred during installation " + err.Error()
							} else {
								msg = "Successfully installed release: " + chart
							}
						}
					}
				} else {
					// Install chart
					fmt.Printf("Installing chart %s to %s namespace\n", chartName, namespace)
					chart, err = installHelmChart(install)
					if err != nil {
						msg = "Error occurred during installation " + err.Error()
					} else {
						msg = "Successfully installed release: " + chart
					}
				}

			}
		}
		registrerRapp(chartName, rapp.Type)
	} else {
		msg = chartName + " has already been installed"
	}

	// create response binary data
	data := []byte(msg) // slice of bytes
	// write `data` to response
	res.Write(data)
}

func runUninstall(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	chartName = query.Get("chart")
	releaseName = chartName
	fmt.Println("Uninstalling ", chartName)

	var msg string
	var chart string
	if chartInstalled(chartName) {
		err := getChartValues(chartName)
		if err != nil {
			msg = err.Error()
		} else {
			chart, err = uninstallHelmChart(chartName)
			if err != nil {
				msg = "Error occurred during uninstall " + err.Error()
			} else {
				msg = "Successfully uninstalled release: " + chart
			}
			if rapp.SecurityEnabled && rapp.Type == "provider" {
				// Remove istio objects for rapp
				fmt.Println("Removing istio services")
				_, err := http.Get("http://rapps-istio-mgr.default/remove?name=" + chartName)
				if err != nil {
					msg = err.Error()
				}
				// remove keycloak client
				fmt.Println("Removing keycloak client")
				_, err = http.Get("http://rapps-keycloak-mgr.default/remove?realm=" + rapp.Realm + "&name=" + rapp.Client + "&role=" + rapp.Roles[0].Role)
				if err != nil {
					msg = err.Error()
				}
			}
		}
		unregistrerRapp(chartName, rapp.Type)
	} else {
		msg = chartName + " is not installed"
	}

	// create response binary data
	data := []byte(msg) // slice of bytes
	// write `data` to response
	res.Write(data)
}

func runList(res http.ResponseWriter, req *http.Request) {
	chartInfo := list()
	// create response binary data
	data, err := json.Marshal(chartInfo)
	if err != nil {
		fmt.Printf("Error happened in JSON marshal. Err: %s\n", err)
	}
	// write `data` to response
	res.Write(data)
}

func main() {
	//flag.StringVar(&url, "url", "http://chartmuseum:8080", "ChartMuseum url")
	flag.StringVar(&repoName, "repoName", "local-dev", "Repository name")
	flag.StringVar(&namespace, "namespace", "istio-nonrtric", "namespace for install")
	flag.Parse()
	settings = cli.New()

	runInstallHandler := http.HandlerFunc(runInstall)
	http.Handle("/install", runInstallHandler)
	runUninstallHandler := http.HandlerFunc(runUninstall)
	http.Handle("/uninstall", runUninstallHandler)
	runListHandler := http.HandlerFunc(runList)
	http.Handle("/list", runListHandler)
	http.ListenAndServe(":9000", nil)
}

func addToRepo(url string) (string, error) {
	repoFile := settings.RepositoryConfig

	//Ensure the file directory exists as it is required for file locking
	err := os.MkdirAll(filepath.Dir(repoFile), os.ModePerm)
	if err != nil && !os.IsExist(err) {
		return "", err
	}

	b, err := ioutil.ReadFile(repoFile)
	if err != nil && !os.IsNotExist(err) {
		return "", err
	}

	var f repo.File
	if err := yaml.Unmarshal(b, &f); err != nil {
		return "", err
	}

	if f.Has(repoName) {
		fmt.Printf("repository name (%s) already exists\n", repoName)
		return "", nil
	}

	c := repo.Entry{
		Name: repoName,
		URL:  url,
	}

	r, err := repo.NewChartRepository(&c, getter.All(settings))
	if err != nil {
		return "", err
	}

	if _, err := r.DownloadIndexFile(); err != nil {
		err := errors.Wrapf(err, "looks like %q is not a valid chart repository or cannot be reached", url)
		return "", err
	}

	f.Update(&c)

	if err := f.WriteFile(repoFile, 0644); err != nil {
		return "", err
	}
	fmt.Printf("%q has been added to your repositories\n", repoName)
	return "", nil
}

func dryRun() (*action.Install, error) {
	actionConfig, err := getActionConfig(namespace)

	install := action.NewInstall(actionConfig)

	cp, err := install.ChartPathOptions.LocateChart(fmt.Sprintf("%s/%s", repoName, chartName), settings)

	chartRequested, err = loader.Load(cp)

	install.Namespace = namespace
	install.ReleaseName = releaseName
	install.DryRun = true
	rel, err := install.Run(chartRequested, nil)
	if err != nil {
		fmt.Println(err)
		return install, err
	}

	rappMap := rel.Chart.Values["rapp"]
	// Convert map to json string
	jsonStr, err := json.Marshal(rappMap)
	if err != nil {
		fmt.Println(err)
		return install, err
	}

	if err := json.Unmarshal(jsonStr, &rapp); err != nil {
		fmt.Println(err)
		return install, err
	}
	fmt.Printf("Keycloak key/value pairs in values.yaml - Realm: %s Client: %s Client Role: %s\n", rapp.Realm, rapp.Client, rapp.Roles)
	return install, nil
}

func installHelmChart(install *action.Install) (string, error) {

	install.DryRun = false
	rel, err := install.Run(chartRequested, nil)
	if err != nil {
		fmt.Println(err)
	}
	fmt.Println("Successfully installed release: ", rel.Name)

	return rel.Name, err
}

func getActionConfig(namespace string) (*action.Configuration, error) {
	actionConfig := new(action.Configuration)
	// Create the rest config instance with ServiceAccount values loaded in them
	config, err := rest.InClusterConfig()
	if err != nil {
		// fallback to kubeconfig
		home, exists := os.LookupEnv("HOME")
		if !exists {
			home = "/root"
		}
		kubeconfigPath := filepath.Join(home, ".kube", "config")
		if envvar := os.Getenv("KUBECONFIG"); len(envvar) > 0 {
			kubeconfigPath = envvar
		}
		if err := actionConfig.Init(kube.GetConfig(kubeconfigPath, "", namespace), namespace, os.Getenv("HELM_DRIVER"),
			func(format string, v ...interface{}) {
				fmt.Sprintf(format, v)
			}); err != nil {
			fmt.Println(err)
		}
	} else {
		// Create the ConfigFlags struct instance with initialized values from ServiceAccount
		var kubeConfig *genericclioptions.ConfigFlags
		kubeConfig = genericclioptions.NewConfigFlags(false)
		kubeConfig.APIServer = &config.Host
		kubeConfig.BearerToken = &config.BearerToken
		kubeConfig.CAFile = &config.CAFile
		kubeConfig.Namespace = &namespace
		if err := actionConfig.Init(kubeConfig, namespace, os.Getenv("HELM_DRIVER"), func(format string, v ...interface{}) {
			fmt.Sprintf(format, v)
		}); err != nil {
			fmt.Println(err)
		}
	}
	return actionConfig, err
}

func uninstallHelmChart(name string) (string, error) {
	actionConfig, err := getActionConfig(namespace)
	if err != nil {
		fmt.Println(err)
	}

	iCli := action.NewUninstall(actionConfig)

	resp, err := iCli.Run(name)
	if err != nil {
		fmt.Println(err)
	}
	fmt.Println("Successfully uninstalled release: ", resp.Release.Name)
	return resp.Release.Name, err
}

func connectToK8s() *kubernetes.Clientset {
	config, err := rest.InClusterConfig()
	if err != nil {
		fmt.Println("failed to create K8s config")
	}

	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		fmt.Println("Failed to create K8s clientset")
	}

	return clientset
}

func findService(serviceName, namespace string) (string, int32) {
	clientset := connectToK8s()
	svc, err := clientset.CoreV1().Services(namespace).Get(context.TODO(), serviceName, metav1.GetOptions{})
	if err != nil {
		fmt.Println(err.Error())
	}
	return svc.Name, svc.Spec.Ports[0].Port
}

func list() []ChartInfo {
	var charts = []ChartInfo{}
	var chart ChartInfo
	actionConfig, err := getActionConfig(namespace)
	if err != nil {
		panic(err)
	}

	listAction := action.NewList(actionConfig)
	releases, err := listAction.Run()
	if err != nil {
		fmt.Println(err)
	}
	for _, release := range releases {
		//fmt.Println("Release: " + release.Name + " Status: " + release.Info.Status.String())
		chart.Name = release.Name
		chart.Namespace = release.Namespace
		chart.Revision = release.Version
		chart.Updated = release.Info.LastDeployed.String()
		chart.Status = release.Info.Status.String()
		chart.Chart = release.Chart.Metadata.Name + "-" + release.Chart.Metadata.Version
		chart.AppVersion = release.Chart.Metadata.AppVersion
		chart.Values = release.Chart.Values
		charts = append(charts, chart)
	}
	return charts
}

func chartInstalled(chartName string) bool {
	charts := list()
	for _, chart := range charts {
		if chart.Name == chartName {
			return true
		}
	}
	return false
}

func getChartValues(chartName string) error {
	charts := list()
	for _, chart := range charts {
		if chart.Name == chartName {
			rappMap := chart.Values["rapp"]
			fmt.Println("rappMap:", rappMap)
			// Convert map to json string
			jsonStr, err := json.Marshal(rappMap)
			if err != nil {
				fmt.Println("Error:", err)
				return err
			}

			if err := json.Unmarshal(jsonStr, &rapp); err != nil {
				fmt.Println("Error:", err)
				return err
			}
			return nil
		}
	}
	return errors.New("Chart: cannot retrieve values")
}

func registrerRapp(chartName, chartType string) {
	psqlconn := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable", host, port, user, password, dbname)

	db, err := sql.Open("postgres", psqlconn)
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Connected!")
	}

	defer db.Close()

	// create
	// hardcoded
	createStmt := `CREATE TABLE IF NOT EXISTS services (
	id serial PRIMARY KEY,
	name VARCHAR ( 50 ) UNIQUE NOT NULL,
	type VARCHAR ( 50 ) NOT NULL,
	created_on TIMESTAMP DEFAULT NOW() 
        );`
	_, err = db.Exec(createStmt)
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Created table for service registry")
	}

	// dynamic
	insertDynStmt := `insert into "services"("name", "type") values($1, $2)`
	_, err = db.Exec(insertDynStmt, chartName, chartType)
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Inserted " + chartName + " into service registry")
	}
}

func unregistrerRapp(chartName, chartType string) {
	psqlconn := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable", host, port, user, password, dbname)

	db, err := sql.Open("postgres", psqlconn)
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Connected!")
	}

	defer db.Close()

	// dynamic
	deleteDynStmt := `delete from services where name=$1 and type=$2`
	_, err = db.Exec(deleteDynStmt, chartName, chartType)
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted " + chartName + " from service registry")
	}
}
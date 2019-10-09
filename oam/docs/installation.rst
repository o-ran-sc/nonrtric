.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Introduction
============
The purpose of this document is to explain how to build an ONAP SDNC Instance on vanilla Openstack deployment.
The document begins with creation of a network, and a VM.
Then, the document explains how to run the installation scripts on the VM.
Finally, the document shows how to check that the SDNC installation was completed successfully.
This document and logs were created on 22 November, 2018.

Infrastructure setup on OpenStack
---------------------------------
Create the network, we call it “ONAP-net”:

::

 cloud@olc-ubuntu2:~$ neutron net-create onap-net
 neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
 Created a new network:
 +-----------------+--------------------------------------+
 | Field           | Value                                |
 +-----------------+--------------------------------------+
 | admin_state_up  | True                                 |
 | id              | 662650f0-d178-4745-b4fe-dd2cb735160c |
 | name            | onap-net                             |
 | router:external | False                                |
 | shared          | False                                |
 | status          | ACTIVE                               |
 | subnets         |                                      |
 | tenant_id       | 324b90de6e9a4ad88e93a100c2cedd5d     |
 +-----------------+--------------------------------------+
 cloud@olc-ubuntu2:~$

Create the subnet, “ONAP-subnet”:

::

 cloud@olc-ubuntu2:~$ neutron subnet-create --name onap-subnet onap-net 10.0.0.0/16
 neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
 Created a new subnet:
 +-------------------+----------------------------------------------+
 | Field             | Value                                        |
 +-------------------+----------------------------------------------+
 | allocation_pools  | {"start": "10.0.0.2", "end": "10.0.255.254"} |
 | cidr              | 10.0.0.0/16                                  |
 | dns_nameservers   |                                              |
 | enable_dhcp       | True                                         |
 | gateway_ip        | 10.0.0.1                                     |
 | host_routes       |                                              |
 | id                | 574df42f-15e9-4761-a4c5-e48d64f04b99         |
 | ip_version        | 4                                            |
 | ipv6_address_mode |                                              |
 | ipv6_ra_mode      |                                              |
 | name              | onap-subnet                                  |
 | network_id        | 662650f0-d178-4745-b4fe-dd2cb735160c         |
 | tenant_id         | 324b90de6e9a4ad88e93a100c2cedd5d             |
 +-------------------+----------------------------------------------+
 cloud@olc-ubuntu2:~$

Boot an Ubuntu 14.04 instance, using the correct flavor name according to your Openstack :

::

 cloud@olc-ubuntu2:~$ nova boot --flavor n2.cw.standard-4 --image "Ubuntu 14.04" --key-name olc-
 key2 --nic net-name=onap-net,v4-fixed-ip=10.0.7.1 vm1-sdnc
 +--------------------------------------+-----------------------------------------------------+
 | Property                             | Value                                               |
 +--------------------------------------+-----------------------------------------------------+
 | OS-DCF:diskConfig                    | MANUAL                                              |
 | OS-EXT-AZ:availability_zone          |                                                     |
 | OS-EXT-STS:power_state               | 0                                                   |
 | OS-EXT-STS:task_state                | scheduling                                          |
 | OS-EXT-STS:vm_state                  | building                                            |
 | OS-SRV-USG:launched_at               | -                                                   |
 | OS-SRV-USG:terminated_at             | -                                                   |
 | accessIPv4                           |                                                     |
 | accessIPv6                           |                                                     |
 | config_drive                         |                                                     |
 | created                              | 2017-11-14T15:48:37Z                                |
 | flavor                               | n2.cw.standard-4 (44)                               |
 | hostId                               |                                                     |
 | id                                   | 596e2b1f-ff09-4c8e-b3e8-fc06566306cf                |
 | image                                | Ubuntu 14.04 (ac9d6735-7c2b-4ff1-90e9-b45225fd80a9) |
 | key_name                             | olc-key2                                            |
 | metadata                             | {}                                                  |
 | name                                 | vm1-sdnc                                            |
 | os-extended-volumes:volumes_attached | []                                                  |
 | progress                             | 0                                                   |
 | security_groups                      | default                                             |
 | status                               | BUILD                                               |
 | tenant_id                            | 324b90de6e9a4ad88e93a100c2cedd5d                    |
 | updated                              | 2017-11-14T15:48:38Z                                |
 | user_id                              | 24c673ecc97f4b42887a195654d6a0b9                    |
 +--------------------------------------+-----------------------------------------------------+
 cloud@olc-ubuntu2:~$

Create a floating IP and associate to the SDNC VM so that it can access internet to download needed files:

::

 cloud@olc-ubuntu2:~$ neutron floatingip-create public
 neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
 Created a new floatingip:
 +---------------------+--------------------------------------+
 | Field               | Value                                |
 +---------------------+--------------------------------------+
 | fixed_ip_address    |                                      |
 | floating_ip_address | 84.39.47.153                         |
 | floating_network_id | b5dd7532-1533-4b9c-8bf9-e66631a9be1d |
 | id                  | eac0124f-9c92-47e5-a694-53355c06c6b2 |
 | port_id             |                                      |
 | router_id           |                                      |
 | status              | ACTIVE                               |
 | tenant_id           | 324b90de6e9a4ad88e93a100c2cedd5d     |
 +---------------------+--------------------------------------+
 cloud@olc-ubuntu2:~$
 cloud@olc-ubuntu2:~$ neutron port-list
 neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
 +--------------------------------------+--------------------------------------+-------------------+-------------------------------------------------------------------------------------+
 | id                                   | name                                 | mac_address       | fixed_ips                                                                           |
 +--------------------------------------+--------------------------------------+-------------------+-------------------------------------------------------------------------------------+
 | 5d8e8f30-a13a-417d-b5b4-f4038224364b | 5d8e8f30-a13a-417d-b5b4-f4038224364b | 02:5d:8e:8f:30:a1 | {"subnet_id": "574df42f-15e9-4761-a4c5-e48d64f04b99", "ip_address": "10.0.7.1"}     |
 +--------------------------------------+--------------------------------------+-------------------+-------------------------------------------------------------------------------------+
 cloud@olc-ubuntu2:~$
 cloud@olc-ubuntu2:~$ neutron floatingip-associate eac0124f-9c92-47e5-a694-53355c06c6b25d8e8f30-a13a-417d-b5b4-f4038224364b
 neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
 Associated floating IP eac0124f-9c92-47e5-a694-53355c06c6b2
 cloud@olc-ubuntu2:~$

Add the security group to the VM in order to open needed ports for SDNC like port 22, 3000, 8282 etc ...:

::

 cloud@olc-ubuntu2:~$ nova add-secgroup vm1-sdnc olc-onap
 cloud@olc-ubuntu2:~$

Installing SDNC
---------------

Connect to the new VM and change to user "root", and run the following commands to start the installation:

::

 # Login as root
 sudo -i
 # Clone Casablanca branch for demo Repo
 root@sdnc-test:~# git clone https://gerrit.onap.org/r/demo -b casablanca
 Cloning into 'demo'...
 remote: Counting objects: 10, done
 remote: Finding sources: 100% (10/10)
 remote: Total 9562 (delta 0), reused 9562 (delta 0)
 Receiving objects: 100% (9562/9562), 43.00 MiB | 13.84 MiB/s, done.
 Resolving deltas: 100% (5922/5922), done.
 Checking connectivity... done.
 root@sdnc-test:~#

Use below commands to update installation environment

::

 # Create Configuration directory
 mkdir -p /opt/config
 # Update configuration folder with variables used during the installation
 awk '$1 == "artifacts_version:" {print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/artifacts_version.txt
 awk '$1 == "sdnc_repo:" {print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/remote_repo.txt
 awk '$1 == "sdnc_branch:" {print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/gerrit_branch.txt
 echo "no_proxy" > /opt/config/http_proxy.txt
 echo "no_proxy" > /opt/config/https_proxy.txt
 echo "https://nexus.onap.org" > /opt/config/nexus_artifact_repo.txt
 echo "8.8.8.8" > /opt/config/external_dns.txt
 awk '$1 == "dns_ip_addr:" {print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/dns_ip_addr.txt
 awk '$1 == "nexus_username:" {print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/nexus_username.txt
 awk '$1 == "nexus_password:" {print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/nexus_password.txt
 awk '$1 == "nexus_docker_repo:" {print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/nexus_docker_repo.txt
 awk '$1 == "sdnc_docker:" {gsub("\"","",$2);print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/docker_version.txt
 awk '$1 == "dgbuilder_docker:" {gsub("\"","",$2);print $2}' /root/demo/heat/ONAP/onap_openstack.env > /opt/config/dgbuilder_version.txt
 # Add host name to /etc/host to avoid warnings in openstack images
 echo 127.0.0.1 $(hostname) >> /etc/hosts
 # Install additional components
 apt update
 apt-get install -y linux-image-extra-$(uname -r) linux-image-extra-virtual apt-transport-https ca-certificates wget git ntp ntpdate make jq unzip
 # Enable autorestart when VM reboots
 chmod +x /root/demo/heat/ONAP/cloud-config/serv.sh
 cp /root/demo/heat/ONAP/cloud-config/serv.sh /etc/init.d
 update-rc.d serv.sh defaults

Install docker engine

::

 echo "deb https://apt.dockerproject.org/repo ubuntu-$(lsb_release -cs) main" | tee /etc/apt/sources.list.d/docker.list
 apt-get update
 apt-get install -y --allow-unauthenticated docker-engine

Install docker-compose & complete docker configuration

::

 root@sdnc-test:~# mkdir -p /opt/docker
 root@sdnc-test:~# curl -L "https://github.com/docker/compose/releases/download/1.16.1/docker-compose-$(uname -s)-$(uname -m)" > /opt/docker/docker-compose
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                  Dload  Upload   Total   Spent    Left  Speed
	100 8648k  100 8648k    0     0  3925k      0  0:00:02  0:00:02 --:--:-- 10.3M
 root@sdnc-test:~# chmod +x /opt/docker/docker-compose
 # Set the MTU size of docker containers to the minimum MTU size supported by vNICs
 root@sdnc-test:~# MTU=$(/sbin/ifconfig | grep MTU | sed 's/.*MTU://' | sed 's/ .*//' | sort -n | head -1)
 root@sdnc-test:~# echo "DOCKER_OPTS=\"$DNS_FLAG--mtu=$MTU\"" >> /etc/default/docker
 root@sdnc-test:~# cp /lib/systemd/system/docker.service /etc/systemd/system
 root@sdnc-test:~# sed -i "/ExecStart/s/$/ --mtu=$MTU/g" /etc/systemd/system/docker.service
 root@sdnc-test:~# systemctl daemon-reload
 root@sdnc-test:~# service docker restart
 # DNS IP address configuration
 root@sdnc-test:~# echo "nameserver $(cat /opt/config/external_dns.txt)" >> /etc/resolvconf/resolv.conf.d/head
 root@sdnc-test:~# resolvconf -u

Copy & run installation scripts

::

 # Copy installation scripts to opt directory
 root@sdnc-test:~# cp /root/demo/heat/ONAP/cloud-config/sdnc_install.sh /opt/sdnc_install.sh
 root@sdnc-test:~# cp /root/demo/heat/ONAP/cloud-config/sdnc_vm_init.sh /opt/sdnc_vm_init.sh
 # Run installation script
 root@sdnc-test:~# cd /opt
 root@sdnc-test:~# chmod +x sdnc_install.sh
 root@sdnc-test:~# chmod +x sdnc_vm_init.sh
 root@sdnc-test:~# ./sdnc_install.sh
 Cloning into 'sdnc'...
 remote: Finding sources: 100% (8962/8962)
 remote: Total 8962 (delta 3999), reused 8956 (delta 3999)
 Receiving objects: 100% (8962/8962), 702.76 MiB | 19.20 MiB/s, done.
 Resolving deltas: 100% (3999/3999), done.
 Checking connectivity... done.
 Already up-to-date.
 Login Succeeded
 1.4-STAGING-latest: Pulling from onap/sdnc-image
 18d680d61657: Pull complete
 … output truncated …

The following install logs shows the containers are coming up, meaning a successful deployment of the SDNC:

::

 ... truncated output ...
 d3565df0a804: Pull complete
 Digest: sha256:0ba03586c705ca8f79030586a579001c4fab3d6fa8c388b6c1c37c695645b78e
 Status: Downloaded newer image for mysql/mysql-server:5.6
 Creating sdnc_db_container ...
 Creating sdnc_db_container ... done
 Creating sdnc_ansible_container ...
 Creating sdnc_ansible_container ... done
 Creating sdnc_controller_container ...
 Creating sdnc_controller_container ... done
 Creating sdnc_ueblistener_container ...
 Creating sdnc_portal_container ...
 Creating sdnc_dgbuilder_container ...
 Creating sdnc_dmaaplistener_container ...
 Creating sdnc_ueblistener_container
 Creating sdnc_portal_container
 Creating sdnc_dmaaplistener_container
 Creating sdnc_dgbuilder_container ... done

Check that the containers are up and running:

::

 root@sdnc-test:/opt# docker container list
 CONTAINER ID        IMAGE                                   COMMAND                  CREATED             STATUS                    PORTS                     NAMES
 9de71aea163a        onap/ccsdk-dgbuilder-image:latest       "/bin/bash -c 'cd ..."   11 minutes ago      Up 11 minutes             0.0.0.0:3000->3100/tcp    sdnc_dgbuilder_container
 adffc0e70758        onap/sdnc-dmaap-listener-image:latest   "/opt/onap/sdnc/dm..."   11 minutes ago      Up 11 minutes                                       sdnc_dmaaplistener_container
 53bfa2e31c44        onap/admportal-sdnc-image:latest        "/bin/bash -c 'cd ..."   11 minutes ago      Up 11 minutes             0.0.0.0:8843->8843/tcp    sdnc_portal_container
 2fd18ceb09de        onap/sdnc-image:latest                  "/opt/onap/sdnc/bi..."   11 minutes ago      Up 11 minutes             0.0.0.0:8282->8181/tcp    sdnc_controller_container
 3ddb85174acb        onap/sdnc-ansible-server-image:latest   "/opt/onap/ccsdk/s..."   11 minutes ago      Up 11 minutes             0.0.0.0:32769->8000/tcp   sdnc_ansible_container
 4a11c393ffa3        mysql/mysql-server:5.6                  "/entrypoint.sh my..."   11 minutes ago      Up 11 minutes (healthy)   0.0.0.0:32768->3306/tcp   sdnc_db_container
 root@sdnc-test:/opt#

Login into APIDOC Explorer and check that you can see Swagger UI interface with all the APIs:

::

 APIDOC Explorer URL: http://{SDNC-IP}:8282/apidoc/explorer/index.html
 Username: admin
 Password: Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U

Login into DG Builder and check that you can see the GUI:

::

 DG Builder URL: http://dguser:test123@{SDNC-IP}:3000

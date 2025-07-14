#  ============LICENSE_START===============================================
#  Copyright (C) 2019-2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================

from docs_conf.conf import *

## Update here to configure which branch is used. 
#branch configurations
controlpanelbranch = 'latest'
simulatorbranch = 'latest'
dmaapmediatorproducerbranch = 'latest'
dmaapadapterbranch = 'latest'
informationcoordinatorservicebranch = 'latest'
rappcataloguebranch = 'latest'
rappmanagerbranch = 'latest'
helmmanagerbranch = 'latest'
ransliceassurancebranch = 'latest'
orufhrecoverybranch = 'latest'
authtokenfetchbranch = 'latest'
a1policymanagementservicebranch = 'latest'
smebranch = 'latest'
ranpmbranch = 'latest'
teivbranch = 'latest'
onapbranch = 'latest'
# Add some useful links available in every page
rst_epilog = """
.. _nonrtricwiki: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/overview
.. _nonrtricwikidevguide: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/446758914/Release+L
.. _nonrtricwikik8s: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/446759372/Release+L+-+Run+in+Kubernetes
.. _nonrtricwikidocker: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/446759174/Release+L+-+Run+in+Docker
"""
## 


linkcheck_ignore = [
    'http://localhost.*',
    'http://127.0.0.1.*',
    'https://gerrit.o-ran-sc.org.*',
]

extensions = [
    'sphinx.ext.intersphinx',
    'sphinx.ext.autosectionlabel',
]

#intershpinx mapping with other projects
intersphinx_mapping = {}

intersphinx_mapping['controlpanel'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-portal-nonrtric-controlpanel/en/%s' % controlpanelbranch, None)
intersphinx_mapping['simulator'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-sim-a1-interface/en/%s' % simulatorbranch, None)
intersphinx_mapping['dmaapmediatorproducer'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-dmaapmediatorproducer/en/%s' % dmaapmediatorproducerbranch, None)
intersphinx_mapping['dmaapadapter'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-dmaapadapter/en/%s' % dmaapadapterbranch, None)
intersphinx_mapping['informationcoordinatorservice'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-informationcoordinatorservice/en/%s' % informationcoordinatorservicebranch, None)
intersphinx_mapping['rappcatalogue'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-rappcatalogue/en/%s' % rappcataloguebranch, None)
intersphinx_mapping['rappmanager'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-rappmanager/en/%s' % rappmanagerbranch, None)
intersphinx_mapping['helmmanager'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-helmmanager/en/%s' % helmmanagerbranch, None)
intersphinx_mapping['ransliceassurance'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-rapp-ransliceassurance/en/%s' % ransliceassurancebranch, None)
intersphinx_mapping['orufhrecovery'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-rapp-orufhrecovery/en/%s' % orufhrecoverybranch, None)
intersphinx_mapping['authtokenfetch'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric/en/%s' % authtokenfetchbranch, None)
intersphinx_mapping['a1policymanagementservice'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-a1policymanagementservice/en/%s' % a1policymanagementservicebranch, None)
intersphinx_mapping['sme'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-sme/en/%s' % smebranch, None)
intersphinx_mapping['ranpm'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-ranpm/en/%s' % ranpmbranch, None)
intersphinx_mapping['teiv'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-smo-teiv/en/%s' % teivbranch, None)
intersphinx_mapping['onapa1policymanagementservice'] = ('https://docs.onap.org/projects/onap-ccsdk-oran/en/%s' % onapbranch, None)

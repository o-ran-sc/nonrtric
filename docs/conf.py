from docs_conf.conf import *

#branch configuration

branch = 'f-release'
onapbranch = 'jakarta'

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

intersphinx_mapping['controlpanel'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-portal-nonrtric-controlpanel/en/%s' % branch, None)
intersphinx_mapping['simulator'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-sim-a1-interface/en/%s' % branch, None)
intersphinx_mapping['dmaapmediatorproducer'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-dmaapmediatorproducer/en/%s' % branch, None)
intersphinx_mapping['dmaapadapter'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-dmaapadapter/en/%s' % branch, None)
intersphinx_mapping['informationcoordinatorservice'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-informationcoordinatorservice/en/%s' % branch, None)
intersphinx_mapping['rappcatalogue'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-rappcatalogue/en/%s' % branch, None)
intersphinx_mapping['helmmanager'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-helmmanager/en/%s' % branch, None)
intersphinx_mapping['ransliceassurance'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-rapp-ransliceassurance/en/%s' % branch, None)
intersphinx_mapping['orufhrecovery'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-rapp-orufhrecovery/en/%s' % branch, None)
intersphinx_mapping['a1policymanagementservice'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-a1policymanagementservice/en/%s' % branch, None)
intersphinx_mapping['onapa1policymanagementservice'] = ('https://docs.onap.org/projects/onap-ccsdk-oran/en/%s' % onapbranch, None)

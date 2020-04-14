from docs_conf.conf import *

#branch configuration

branch = 'latest'

linkcheck_ignore = [
    'http://localhost.*',
    'http://127.0.0.1.*',
    'https://gerrit.o-ran-sc.org.*'
]

#intershpinx mapping with other projects
intersphinx_mapping = {}

intersphinx_mapping['nonrtric-controlpanel'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-portal-nonrtric-controlpanel/en/%s' % branch, None)
intersphinx_mapping['sim-a1-interface'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-sim-a1-interface/en/%s' % branch, None)

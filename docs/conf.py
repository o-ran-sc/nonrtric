from docs_conf.conf import *

#branch configuration

branch = 'latest'

linkcheck_ignore = [
    'http://localhost.*',
    'http://127.0.0.1.*',
    'https://gerrit.o-ran-sc.org.*',
    './rac-api.html', #Generated file that doesn't exist at link check.
    './ics-api.html', #Generated file that doesn't exist at link check.
    './dmaap-adaptor-api.html' #Generated file that doesn't exist at link check.
]

extensions = ['sphinxcontrib.redoc', 'sphinx.ext.intersphinx',]

redoc = [
            {
                'name': 'RAC API',
                'page': 'rac-api',
                'spec': '../r-app-catalogue/api/rac-api.json',
                'embed': True,
            },
            {
                'name': 'ICS API',
                'page': 'ics-api',
                'spec': '../information-coordinator-service/api/ics-api.json',
                'embed': True,
            },
            {
                'name': 'DMaaP Adaptor API',
                'page': 'dmaap-adaptor-api',
                'spec': '../dmaap-adaptor-java/api/api.json',
                'embed': True,
            }
        ]

redoc_uri = 'https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js'

#intershpinx mapping with other projects
intersphinx_mapping = {}

intersphinx_mapping['nonrtric-controlpanel'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-portal-nonrtric-controlpanel/en/%s' % branch, None)
intersphinx_mapping['sim-a1-interface'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-sim-a1-interface/en/%s' % branch, None)

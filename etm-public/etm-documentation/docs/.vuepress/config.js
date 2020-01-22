module.exports = {
    base: '/documentation/enterprise-telemetry-monitor/4.0.x/',
    title: 'Enterprise Telemetry Monitor - 4.0.x',
    description: 'Finding without searching',
    themeConfig: {
        logo: '/jecstar-logo-512x512.png',
        smoothScroll: true,
        nav: [
            {text: 'Home', link: 'https://www.jecstar.com/'},
            {text: 'Downloads', link: 'https://www.jecstar.com/downloads/'}
        ],
        sidebarDepth: 2,
        sidebar: [
            '/',
            {
                title: 'Getting started',
                children: [
                    '/getting-started/basic-concepts',
                    '/getting-started/installation',
                ]
            },
            {
                title: 'Setup',
                path: '/setup/',
                children: [
                    '/setup/system-requirements',
                    '/setup/installation-with-tgz',
                    '/setup/installation-with-zip',
                    '/setup/installation-on-windows',
                    '/setup/installation-with-docker',
                    '/setup/installation-with-kubernetes',
                    '/setup/integration-with-ibm',
                    '/setup/integration-with-jms',
                    '/setup/node-configuration',
                ]
            },
            {
                title: 'Administrating',
                path: '/administrating/',
                children: [
                    '/administrating/license-registration',
                    '/administrating/users',
                    '/administrating/groups',
                    '/administrating/cluster',
                    '/administrating/nodes',
                    '/administrating/parsers',
                    '/administrating/import-profiles',
                    '/administrating/notifiers',
                    '/administrating/audit-logs',
                    '/administrating/index-statistics',
                    '/administrating/iib-nodes',
                    '/administrating/iib-events',
                ]
            },
            {
                title: 'Event layout',
                path: '/event-layout/',
            },
            {
                title: 'Processing events',
                path: '/processing-events/',
                children: [
                    '/processing-events/rest-processor',
                    '/processing-events/ibm-mq-processor',
                ]
            },
            {
                title: 'Searching events',
                path: '/searching/',
                children: [
                    '/searching/search-widget',
                    '/searching/search-template-widget',
                    '/searching/search-history-widget',
                    '/searching/search-result-widget',
                    '/searching/query-syntax',
                ]
            },
            {
                title: 'User preferences',
                path: '/user-preferences/',
            },
            {
                title: 'Visualizations',
                path: '/visualizations/',
                children: [
                    '/visualizations/graphs',
                    '/visualizations/dashboards',
                ]
            },
            {
                title: 'Signals',
                path: '/signals/',
            },
            {
                title: 'Error codes',
                path: '/error-codes/',
            },
            {
                title: 'Changelog',
                path: '/changelog/',
            },
            {
                title: 'Support matrix',
                path: '/support-matrix/',
            },
        ]
    }
};

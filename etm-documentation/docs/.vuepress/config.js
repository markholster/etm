module.exports = {
    title: 'Enterprise Telemetry Monitor - 4.0.x',
    description: 'Finding without searching',
    themeConfig: {
        smoothScroll: true,
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
        ]
    }
};

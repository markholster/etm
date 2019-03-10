package com.jecstar.etm.cloud;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressRuleBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.HashMap;
import java.util.Map;

public class KubernetesCloudManager implements CloudManager {

    private final KubernetesClient client;

    public KubernetesCloudManager(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public Namespace createNamespace(String name) {
        return this.client.namespaces().createNew()
                .withNewMetadata()
                .withName(name)
                .withClusterName("standard-cluster-1")
                .endMetadata()
                .done();

    }

    @Override
    public NamespaceList getNamespaces() {
        return this.client.namespaces().list();
    }

    private void newCustomer(String namespace) {
        Map<String, String> esMasterLabels = new HashMap<>();
        esMasterLabels.put("app", "elasticsearch");
        esMasterLabels.put("component", "master");
        esMasterLabels.put("release", "6.6.0");

        Map<String, String> esClientLabels = new HashMap<>();
        esClientLabels.put("app", "elasticsearch");
        esClientLabels.put("component", "client");
        esClientLabels.put("release", "6.6.0");

        Map<String, String> esDataLabels = new HashMap<>();
        esDataLabels.put("app", "elasticsearch");
        esDataLabels.put("component", "data");
        esDataLabels.put("release", "6.6.0");

        Map<String, String> etmLabels = new HashMap<>();
        esDataLabels.put("app", "etm");
        esDataLabels.put("component", "client");
        esDataLabels.put("release", "3.4.1");

        this.client.services().createNew()
                .withApiVersion("v1")
                .withKind("Service")
                .withNewMetadata()
                .withName("elasticsearch-discovery")
                .withNamespace(namespace)
                .withLabels(esMasterLabels)
                .endMetadata()
                .withNewSpec()
                .withClusterIP("None")
                .withPorts(
                        new ServicePortBuilder().withName("http").withPort(9200).withTargetPort(new IntOrString(9200)).build(),
                        new ServicePortBuilder().withName("transport").withPort(9300).withTargetPort(new IntOrString(9300)).build()
                )
                .withSelector(esMasterLabels)
                .endSpec()
                .done();

        this.client.services().createNew()
                .withApiVersion("v1")
                .withKind("Service")
                .withNewMetadata()
                .withName("elasticsearch-client")
                .withNamespace(namespace)
                .withLabels(esClientLabels)
                .endMetadata()
                .withNewSpec()
                .withClusterIP("None")
                .withPorts(
                        new ServicePortBuilder().withName("http").withPort(9200).withTargetPort(new IntOrString(9200)).build(),
                        new ServicePortBuilder().withName("transport").withPort(9300).withTargetPort(new IntOrString(9300)).build()
                )
                .withSelector(esClientLabels)
                .endSpec()
                .done();

        this.client.services().createNew()
                .withApiVersion("v1")
                .withKind("Service")
                .withNewMetadata()
                .withName("etm-client")
                .withNamespace(namespace)
                .withLabels(etmLabels)
                .endMetadata()
                .withNewSpec()
                .withPorts(
                        new ServicePortBuilder().withPort(80).withTargetPort(new IntOrString(8080)).build()
                )
                .withSelector(etmLabels)
                .endSpec()
                .done();

        Map<String, String> annotations = new HashMap<>();
        annotations.put("kubernetes.io/ingress.class", "nginx");
        annotations.put("nginx.ingress.kubernetes.io/rewrite-target", "/$1");
        this.client.extensions().ingresses().createNew()
                .withApiVersion("extensions/v1beta1")
                .withKind("Ingress")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName("etm-ingress")
                .withAnnotations(annotations)
                .endMetadata()
                .withNewSpec()
                .withRules(new IngressRuleBuilder()
                        .withNewHttp()
                        .withPaths(new HTTPIngressPathBuilder()
                                .withNewBackend()
                                .withServiceName("etm-client")
                                .withServicePort(new IntOrString(80))
                                .endBackend()
                                .withPath("/" + namespace + "/?(.*)")
                                .build()
                        )
                        .endHttp()
                        .build()
                )
                .endSpec()
                .done();
    }
}

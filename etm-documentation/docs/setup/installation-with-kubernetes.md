# Installation with Kubernetes

Both Enterprise Telemetry Monitor and Elasticsearch are capable of running on Kubernetes. Templates of Kubernetes configuration files can be found at the [Jecstar Github Repository](https://github.com/jecstarinnovations/etm/tree/v4.1.1/etm-kubernetes/src/main/kubernetes) of Enterprise Telemetry Monitor.

These templates give you a good starting point with great flexibility on setting up your cluster. To create an Enterprise Telemetry Monitor cluster with 3 Elasticsearch master nodes, 2 Elasticsearch data nodes, 2 Elasticsearch client nodes and 2 Enterprise Telemetry Monitor nodes run the following commands on a Google Cloud Platform connected Kubernetes cluster:

```bash
kubectl create -f storage.yaml
kubectl create -f elasticearch-master-svc.yml
kubectl create -f elasticearch-client-svc.yml
kubectl create -f etm-svc.yml
kubectl create -f elasticearch-master.yml
kubectl create -f elasticearch-data.yml
kubectl create -f elasticearch-client.yml
kubectl create -f etm.yml
```
Of course this 9 node cluster created with the templates might be overkill for your situation, but nothing should stop you from creating a single node Elasticsearch cluster with the use of these templates.
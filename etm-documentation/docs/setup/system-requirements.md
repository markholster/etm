# System requirements
Enterprise Telemetry Monitor works best on machines that are equipped with 8 to 64 GiB of RAM. More than 64 GiB makes the Java garbage collections run so long it has an overall negative impact on the performance. In most situations 8 GiB is more than sufficient, but when you need to process a lot of big events you might need some more memory.

If your system is running on spinning disk you might consider upgrade to SSD or even NVME disks. This is in particular important for your Elasticsearch nodes! Elasticsearch will store and retrieve all the event data, so make sure your disks at these nodes are as fast as possible. They will be most likely the bottleneck of your Enterprise Telemetry Monitor cluster setup.

Processing events is the most CPU intensive task in Enterprise Telemetry Monitor. Because events are processed in parallel Enterprise Telemetry Monitor takes a huge benefit from multi-core CPU's. If you have to choose between a faster CPU or a multi-core CPU always go for the multi-core CPU. 

When you are planning to create a multi node cluster, make sure the nodes are as close to each other as possible. Network latency can have a negative impact on the processing performance. Optical network interfaces are the preferred way to go, but if they are not available to you make sure your servers are equipped with at least Gigabit network interfaces. 
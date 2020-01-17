# Installation with .tgz
The .tgz archive for Enterprise Telemetry Monitor can be downloaded and installed as follow:

```bash
wget https://www.jecstar.com/assets/downloads/etm/etm-4.0.0.tgz
wget https://www.jecstar.com/assets/downloads/etm/etm-4.0.0.tgz.sha512
cat etm-{project-version}.tgz.sha512 | sha512sum -c ## Checks the sha512 hash 
# of the downloaded file. If not ok, this command will fail.
tar -xvf etm-4.0.0.tgz
cd etm-4.0.0/bin
./etm ## This command does not actually start Enterprise Telemetry Monitor
#but shows the options that #are available with the etm script.
```
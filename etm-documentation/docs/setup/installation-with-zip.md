# Installation with .zip
The .zip archive for Enterprise Telemetry Monitor can be downloaded and installed as follow:

```bash
wget https://www.jecstar.com/assets/downloads/etm-4.1.0.zip
wget https://www.jecstar.com/assets/downloads/etm-4.1.0.zip.sha512
cat etm-{project-version}.zip.sha512 | sha512sum -c ## Checks the sha512 hash 
# of the downloaded file. If not ok, this command will fail.
unzip etm-4.1.0.zip
cd etm-4.1.0/bin
./etm ## This command does not actually start Enterprise Telemetry Monitor
#but shows the options that #are available with the etm script.
```
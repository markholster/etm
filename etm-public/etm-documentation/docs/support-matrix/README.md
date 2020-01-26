# Support matrix

Our goal is to maintain the most recent minor release from the current major release stream and the most recent patch release from the prior major release stream. We have observed that some users upgrade frequently and stay up to date with our release stream. These users can stay with the latest minor release stream and obtain fixes with the maintenance releases they choose to deploy. For example, these users would follow our Enterprise Telemetry Monitor releases with 3.1.0, 3.1.1, 3.2.0, 3.2.1, etc. The prior minor of the current major release stream will be maintained for a maximum of three months after the release of the next minor.

For those users that cannot upgrade as quickly as we release we will provide bug fixes for the last minor of the prior major release. This last minor will be maintained until the release of the second subsequent major version.

The End of Life policy defines how long a given release is considered supported, as well as how long a release is considered still in active development or maintenance. As part of our support activities we may request you to upgrade to the latest minor release stream of your current (supported) major version. Enterprise Telemetry Monitor will never support or maintain a release that is based on an unsupported version of the Java JRE or Elasticsearch installation.

Version | Release date | Oracle/OpenJDK 1.8.0u111+ | Oracle/OpenJDK 11 | Elasticsearch | EOL | Maintained until
--- | --- | --- | --- | --- | --- | ---
ETM 4.0.x | 2020-01-25 | :x:                | :white_check_mark: | 7.x   | 2022-01-25 | 4.1.0
ETM 3.5.x | 2019-03-19 | :white_check_mark: | :white_check_mark: | 6.6.x | 2021-03-19 | 5.0.0
ETM 3.4.x | 2019-02-18 | :white_check_mark: | :white_check_mark: | 6.6.x | 2021-02-18 | 3.5.0
ETM 3.3.x | 2018-11-09 | :white_check_mark: | :white_check_mark: | 6.4.x | 2020-11-09 | 3.4.0
ETM 3.2.x | 2018-06-03 | :white_check_mark: | :white_check_mark: | 6.2.x | 2020-06-03 | 3.3.0
ETM 3.1.x | 2018-03-27 | :white_check_mark: | :x:                | 6.2.x | 2020-03-27 | 3.2.0
ETM 3.0.x | 2018-02-11 | :white_check_mark: | :x:                | 6.2.x | 2020-02-11 | 3.1.0
ETM 2.4.x | 2017-10-21 | :white_check_mark: | :x:                | 5.6.x | 2019-10-21 | 4.0.0
ETM 2.3.x | 2017-07-31 | :white_check_mark: | :x:                | 5.5.x | 2019-07-31 | 2.4.0
ETM 2.2.x | 2017-07-02 | :white_check_mark: | :x:                | 5.4.x | 2019-07-02 | 2.3.0
ETM 2.1.x | 2017-02-19 | :white_check_mark: | :x:                | 5.2.x | 2019-02-19 | 2.2.0
ETM 2.0.x | 2016-12-18 | :white_check_mark: | :x:                | 5.1.x | 2018-12-18 | 2.1.0
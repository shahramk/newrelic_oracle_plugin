Oracle plugin for New Relic (EARLY EVALUATION)
==============================================
- - -
## Prerequisites
The Oracle plugin for New Relic requires the following:

*    A New Relic account. If you are not already a New Relic user, you can signup for a free account at http://newrelic.com
*    Find and download the New Relic Oracle plugin at https://rpm.newrelic.com/plugins/
*    A server running Oracle 10g database or more recent version.
*    A configured Java Runtime (JRE) environment Version 1.6 or higher.
*    Network access to New Relic (authenticated proxies are not currently supported, but see workaround below)

Linux example:

*    $ mkdir /path/to/newrelic-plugin
*    $ cd /path/to/newrelic-plugin
*    $ tar xfz newrelic_oracle_plugin*.tar.gz
   

## Create Oracle user account or use an existing account
Oracle plugin requires an Oracle user with proper privileges to access "V$_table views", and run queries. 


## Configure the agent environment
New Relic plugin for Oracle runs an agent process to collect and report Oracle metrics to New Relic. In order for that you need to configure your New Relic license and Oracle databases. Additionally you can set the logging properties.

### Configure your New Relic license
Specify your license key in a file by the name 'newrelic.properties' in the 'config' directory.
Your license key can be found under "Account Settings" at https://rpm.newrelic.com. See https://newrelic.com/docs/subscriptions/license-key for more help.

Linux example:

*    $ cp config/template_newrelic.properties config/newrelic.properties
*    $ Edit config/newrelic.properties and paste in your license key

### Configure Oracle properties
Each running Oracle plugin agent requires a JSON configuration file defining the access to the monitored Oracle instance(s). An example file is provided in the config directory.

Linux example:

*    $ cp config/template_oracle.instance.json config/oracle.instance.json
*    $ Edit config/oracle.instance.json and specify the necessary property values

Edit the file and change the values for "name", "host", "port", "user", "password", and "sid". The value of the name field will appear in the New Relic user interface for the Oracle instance (i.e. "Production DB"). 

    [
      {
      "name" : "Production DB",
      "host" : "oradb.example.com",
      "port" : "1521",
      "user" : "newrelic",
      "passwd" : "newrelic",
      "sid" : "newrelic"
      }
    ]

  * name       - A meaningful name for the Oracle server that will show up in the New Relic Dashboard.
  * host       - Hostname of the Oracle database server.
  * port       - Oracle DB main listening port (default: 1521).
  * user       - Oracle user account.
  * passwd     - Password for the Oracle user account.
  * sid        - Oracle instance ID.

**Note:** Specify the above set of properties for each Oracle instance. You will have to follow the syntax (embed the properties for each DB instance in a pair of curley braces separated by a comma).

**NoteL** If you would like to monitor multiple instances of oracle DB, copy the block of JSON properties (separated by comma), and change the values accordingly. Example:

    [
      {
      "name" : "Production DB",
      "host" : "oradb.example.com",
      "port" : "1521",
      "user" : "newrelic",
      "passwd" : "newrelic",
      "sid" : "newrelic"
      },
      {
      "name" : "My Local DB",
      "host" : "localhost",
      "port" : "1521",
      "user" : "newrelicuser",
      "passwd" : "assword",
      "sid" : "dummy"
      }
    ]


### Configure logging properties
The plugin checks for the logging properties in config/logging.properties file. You can copy example_logging.properties and edit it if needed

Linux example:

*    $ cp config/example_loging.properties config/logging.properties


## Running the agent
To run the plugin in from the command line: 

*    `$ java -jar newrelic_oracle_plugin*.jar`

If your host needs a proxy server to access the Internet, you can specify a proxy server & port: 

*    `$ java -Dhttps.proxyHost="PROXY_HOST" -Dhttps.proxyPort="PROXY_PORT" -jar newrelic_oracle_plugin*.jar`

To run the plugin from the command line and detach the process so it will run in the background:

*    `$ nohup java -jar newrelic_oracle_plugin*.jar &`

**Note:** At present there are no [init.d](http://en.wikipedia.org/wiki/Init) scripts to start the New Relic Oracle plugin at system startup. You can create your own script, or use one of the services below to manage the process and keep it running:

*    [Upstart](http://upstart.ubuntu.com/)
*    [Systemd](http://www.freedesktop.org/wiki/Software/systemd/)
*    [Runit](http://smarden.org/runit/)
*    [Monit](http://mmonit.com/monit/)

## For support
Plugin support for troubleshooting assistance can be obtained by visiting New Relic support web site: (https://support.newrelic.com)

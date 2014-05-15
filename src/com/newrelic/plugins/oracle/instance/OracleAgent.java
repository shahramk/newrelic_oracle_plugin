package com.newrelic.plugins.oracle.instance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.plugins.oracle.WaitMetricData;

/**
 * This class obtains Oracle wait metrics from an oracle database, and passes
 * these metrics to New Relic controller component using New Relic java SDK
 * 
 */
public class OracleAgent extends Agent {
    private static final String GUID = "com.newrelic.plugins.oracle.instance";
    private static final String version = "0.0.2-beta";

    public static final String AGENT_DEFAULT_HOST = "localhost"; // Default values for Oracle Agent
    public static final String AGENT_DEFAULT_PORT = "1521";
    public static final String AGENT_DEFAULT_USER = "newrelic";
    public static final String AGENT_DEFAULT_PASSWD = "securepassword";
    public static final String AGENT_DEFAULT_INSTANCE = "newrelic";

    public static final String AGENT_CONFIG_FILE = "oracle.instance.json";

    public static final String SQL_WAIT_METRICS_DATA = "SELECT EVENT, TOTAL_WAITS, TIME_WAITED, WAIT_CLASS FROM V$SYSTEM_EVENT";

    public static String[][] SQL_STRINGS = { // each SQL_STRINGS[] bucket has 4
                                             // elements: category, unit, name,
                                             // SQL
            { "Misc/Events", "TotalWaits/Sec", "TotalWaits",
                    "SELECT sum(total_waits) / 100 FROM v$system_event" },

            { "Misc/Events", "TotalWaitTime/Sec", "TimeWaited",
                    "SELECT sum(time_waited) / 100 FROM v$system_event" },

            { "Misc/Deadlocks", "Ops/Sec", "Enqueue Deadlocks",
                    "SELECT value FROM V$SYSSTAT WHERE name='enqueue deadlocks'" },

            { "Misc/FullTableScans", "Ops/Sec", "Large Full Table Scans",
                    "SELECT value FROM V$SYSSTAT WHERE name='table scans (long tables)'" },

            { "Misc/RollbackSegments", "Ops/Sec", "GetCount",
                    "SELECT sum(gets) FROM V$ROLLSTAT" },

            { "Misc/RollbackSegments", "Ops/Sec", "WaitCount",
                    "SELECT sum(waits) FROM V$ROLLSTAT" },

            { "Misc/SystemGlobalArea", "Ops/Sec",
                    "Library Cache:Sharable Objects",
                    "SELECT SUM(sharable_mem) FROM v$db_object_cache" },

            { "Misc/Sorts", "Ops/Sec", "Disk",
                    "SELECT value FROM v$sysstat WHERE name = 'sorts (disk)'" },

            { "Misc/Sorts", "Ops/Sec", "Memory",
                    "SELECT value FROM v$sysstat WHERE name = 'sorts (memory)'" }, };

    public static final String COMMA = ",";
    public static final String SEPARATOR = "/";

    private String name; // Agent Name

    private String host; // Oracle Connection parameters
    private String port;
    private String user;
    private String passwd;
    private String sid;

    private Map<String, WaitMetricData> waitMetricsData = new HashMap<String, WaitMetricData>();

    final Logger logger; // Local convenience variable

    private boolean firstHarvest = true;

    Connection conn = null;

    long harvestCount = 0;

    /**
     * Default constructor to create a new Oracle Agent
     * 
     * @param map
     * @param String Human name for Agent
     * @param String Oracle Instance host:port
     * @param String Oracle user
     * @param String Oracle user password
     * @param String CSVm List of metrics to be monitored
     */
    public OracleAgent(String name, String host, String port, String user, String passwd, String sid) {
        super(GUID, version);

        this.name = name; // Set local attributes for new class object
        this.host = host;
        this.port = port;
        this.user = user;
        this.passwd = passwd;
        this.sid = sid;

        logger = Context.getLogger(); // Set logging to current Context
        conn = getConnection(this.host, this.port, this.user, this.passwd, this.sid); // Get a database connection (which should be cached)
    }

    /**
     * This method is run for every poll cycle of the Agent. Get an Oracle
     * Database connection and gather metrics.
     */
    public void pollCycle() {
        logger.fine("Gathering Oracle metrics. " + getAgentInfo());
        List<String> results = gatherMetrics(); // Gather Wait metrics
        reportMetrics(results); // Report Wait metrics to New Relic
        logger.info("Reporting Oracle metrics: harvest cycle " + (++harvestCount) + ".");
        firstHarvest = false;
    }

    /**
     * This method will return a Oracle database connection for use, either a
     * new connection or a cached connection
     * 
     * @param host String Hostname
     * @param user String Database username
     * @param passwd String database password
     * @return An Oracle Database connection for use
     */
    public Connection getConnection(String host, String port, String user, String passwd, String dbInstance) {
        if (conn == null) {
            String dbURL = "jdbc:oracle:thin:@//" + host + ":" + port + "/" + dbInstance;

            logger.fine("Getting new Oracle Connection " + dbURL + " " + user + "/" + passwd.replaceAll(".", "*"));
            try {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                conn = DriverManager.getConnection(dbURL, user, passwd);
            } catch (Exception e) {
                logger.severe("Unable to obtain a new database connection, check your Oracle configuration settings. " + e.getMessage());
            }
        }
        return conn;
    }

    public List<String> gatherMetrics() {
        Statement stmt = null;
        ResultSet rs = null;
        List<String> results = new ArrayList<String>();

        try {
            logger.fine("Running SQL Statement " + SQL_WAIT_METRICS_DATA);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(SQL_WAIT_METRICS_DATA); // Execute the given SQL statement

            String waitClass = null;
            String eventName = null;
            Number count = null;
            Number waitTime = null;

            while (rs.next()) { // for wait metrics coming from v$system_event capture both number of waits and time waited
                waitClass = rs.getString("WAIT_CLASS").toLowerCase().replaceAll("/", "").replaceAll(" ", "_");
                eventName = rs.getString("EVENT").toLowerCase().replaceAll("/", "").replaceAll(" ", "_");
                count = rs.getLong("TOTAL_WAITS") / 100.0; // convert 100th of seconds to seconds
                waitTime = rs.getLong("TIME_WAITED") / 100.0; // convert 100th of seconds to seconds

                addWaitMetricData(eventName, waitClass, count, waitTime);

                results.add(eventName);
            }
        } catch (SQLException e) {
            logger.severe("An SQL error occured running '" + SQL_WAIT_METRICS_DATA + "' " + e.getMessage());
        } finally {
            try {
                if (rs != null)
                    rs.close(); // Release objects
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
            }
            rs = null;
            stmt = null;
        }

        getMiscellaneousMetrics(results);

        return results;
    }

    public void getMiscellaneousMetrics(List<String> results) {
        // run individual queries and add result of each query to results array
        Statement stmt = null;
        ResultSet rs = null;
        Number value = null;

        for (int idx = 0; idx < SQL_STRINGS.length; idx++) {
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery(SQL_STRINGS[idx][3]);

                if (rs.next()) {
                    value = rs.getLong(1);
                } else {
                    value = 0.0;
                }

                addWaitMetricData(SQL_STRINGS[idx][2], SQL_STRINGS[idx][0], value, null);
                results.add(SQL_STRINGS[idx][2]);

            } catch (SQLException e) {
                logger.severe("An SQL error occured running '" + SQL_STRINGS[idx][3] + "' " + e.getMessage());
            } finally {
                try {
                    if (rs != null)
                        rs.close(); // Release objects
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException e) {
                }
                rs = null;
                stmt = null;
            }

        }
    }

    /**
     * This method does the reporting of metrics to New Relic
     * 
     * @param Map results
     */
    public void reportMetrics(List<String> results) {
        int count = 0;
        logger.fine("Collected " + results.size() + " Oracle metrics. " + getAgentInfo());
        logger.finest(results.toString());

        Iterator<String> iter = results.iterator();

        try {
            while (iter.hasNext()) { // Iterate over current metrics
                String key = (String) iter.next(); // .toLowerCase();
                WaitMetricData wmd = getWaitMetricData(key); // if it doesn't exist, add it to the collection

                if (wmd == null) {
                    if (firstHarvest) // Provide some feedback of available metrics for future reporting
                        logger.fine("Not reporting identified metric " + key);
                    else
                        logger.fine("Metric: " + key + " does not have value!");
                } else {
                    count++;
                    if (wmd.metricClass.contains("Misc/")) {
                        // handle misc metrics -- no waitTime values
                        reportMetric(wmd.metricClass + SEPARATOR + key, "Operations/Second", wmd.metricCount);
                        logger.fine("Metric " + " " + wmd.metricClass + SEPARATOR + key + "(Operations/Second)=" + wmd.metricCount + " counter");
                    } else {

                        reportMetric("Waits" + SEPARATOR + wmd.metricClass
                                + SEPARATOR + key, "Waits/Second",
                                wmd.metricCount);
                        logger.fine("Metric " + " " + wmd.metricClass
                                + SEPARATOR + key + "(Waits/Second)="
                                + wmd.metricCount + " counter");

                        reportMetric("WaitTime" + SEPARATOR + wmd.metricClass
                                + SEPARATOR + key, "WaitTime/Second",
                                wmd.waitTime);
                        logger.fine("Metric " + " " + wmd.metricClass
                                + SEPARATOR + key + "(Time Waited [sec])="
                                + wmd.waitTime + " counter");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.fine("Reported to New Relic " + count + " metrics. " + getAgentInfo());
    }

    private String getAgentInfo() {
        return "Agent Name: " + name + ". Agent Version: " + version;
    }

    public void addWaitMetricData(String eventName, String waitClass,
            Number count, Number waitTime) {
        WaitMetricData wmd = waitMetricsData.get(eventName); // check if the key has already been added to the collection
        if (wmd == null) {
            if (waitTime == null) {
                wmd = new WaitMetricData(eventName, waitClass, count, null); // if it doesn't exist create it
            } else {
                wmd = new WaitMetricData(eventName, waitClass, count, waitTime); // if it doesn't exist create it
            }
            waitMetricsData.put(eventName, wmd); // and add it to the collection
        }

        wmd.metricCount = wmd.counter.process(count);
        if (waitTime != null)
            wmd.waitTime = wmd.waitedTime.process(waitTime);

    }

    public WaitMetricData getWaitMetricData(String key) {
        return waitMetricsData.get(key);
    }

    /**
     * Return the human readable name for this agent.
     * 
     * @return String
     */
    @Override
    public String getComponentHumanLabel() {
        return name;
    }
}

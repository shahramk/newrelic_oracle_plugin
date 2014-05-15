package com.newrelic.plugins.oracle.instance;

import java.util.Map;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

/**
 * This class produces the necessary Agents to perform gathering and reporting
 * metrics for the Oracle DB plugin
 * 
 */
public class OracleAgentFactory extends AgentFactory {
    /**
     * Construct an Agent Factory based on the default properties file
     */
    public OracleAgentFactory() {
        super(OracleAgent.AGENT_CONFIG_FILE);
    }

    /**
     * Configure an agent based on an entry in the oracle json file. There may
     * be multiple agents per Plugin - one per oracle instance
     * 
     */
    @Override
    public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
        String name = (String) properties.get("name");
        String host = (String) properties.get("host");
        String port = (String) properties.get("port");
        String user = (String) properties.get("user");
        String passwd = (String) properties.get("passwd");
        String sid = (String) properties.get("sid");

        /**
         * Use pre-defined defaults to simplify configuration
         */
        if (host == null || "".equals(host))
            host = OracleAgent.AGENT_DEFAULT_HOST;
        if (port == null || "".equals(port))
            port = OracleAgent.AGENT_DEFAULT_PORT;
        if (user == null || "".equals(user))
            user = OracleAgent.AGENT_DEFAULT_USER;
        if (passwd == null)
            passwd = OracleAgent.AGENT_DEFAULT_PASSWD;
        if (sid == null || "".equals(sid))
            sid = OracleAgent.AGENT_DEFAULT_INSTANCE;

        return new OracleAgent(name, host, port, user, passwd, sid);
    }
}

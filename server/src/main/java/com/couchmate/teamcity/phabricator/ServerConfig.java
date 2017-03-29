package com.couchmate.teamcity.phabricator;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mbenua on 3/29/2017.
 */
public final class ServerConfig {

    private static Logger Log  = Loggers.SERVER;

    private final String SERVER_URL = "serverUrl";

    private final String PHAB_URL = "tcphab.phabricatorUrl";
    private final String PHAB_PROTOCOL = "tcphab.phabricatorProtocol";
    private final String CONDUIT_TOKEN = "tcphab.conduitToken";
    private final String REPORT_BEGIN = "tcphab.reportBegin";

    private final String DIFF_ID = "env.diffId";
    private final String REVISION_ID = "env.revisionId";
    private final String HARBORMASTER_PHID = "env.harbormasterTargetPHID";

    private Map<String, String> serverConfig = new HashMap<>();

    private Boolean canRun = false;
    private String workingDir;

    public ServerConfig(Map<String, String> params)
    {
        this.canRun = true;
        if (params == null)
        {
            this.canRun = false;
            Log.warn("Missing all required parameters, Phabricator build step cannot run.");
            return;
        }

        this.serverConfig.put(SERVER_URL, params.get(SERVER_URL));
        this.serverConfig.put(REPORT_BEGIN, params.get(REPORT_BEGIN));
        this.serverConfig.put(CONDUIT_TOKEN, params.get(CONDUIT_TOKEN));

        this.serverConfig.put(DIFF_ID, params.get(DIFF_ID));
        this.serverConfig.put(REVISION_ID, params.get(REVISION_ID));
        this.serverConfig.put(HARBORMASTER_PHID, params.get(HARBORMASTER_PHID));

        try {
            URL aURL = new URL(params.get(PHAB_URL));
            this.serverConfig.put(PHAB_URL, aURL.getHost());
            this.serverConfig.put(PHAB_PROTOCOL, aURL.getProtocol());
        } catch (IOException e) {
            this.serverConfig.put(PHAB_URL, null);
            this.serverConfig.put(PHAB_PROTOCOL, null);
            Log.info(String.format("phabricator url could not be parsed: %s", e.getStackTrace()[0].toString()));
        }

        for (String key : serverConfig.keySet())
        {
            Log.info(String.format("Found %s as %s", key, serverConfig.get(key)));
            if (serverConfig.get(key) == null)
            {
                canRun = false;
                Log.warn("Missing a required server parameter, will not be able to run Phabricator build step.");
            }
        }
    }

    public String getServerUrl() {
        return this.serverConfig.get(SERVER_URL);
    }

    public String getHarbormasterTargetPHID() {
        return this.serverConfig.get(HARBORMASTER_PHID);
    }

    public String getPhabricatorProtocol() {
        return this.serverConfig.get(PHAB_PROTOCOL);
    }

    public String getPhabricatorUrl() {
        return this.serverConfig.get(PHAB_URL);
    }

    public String getConduitToken() {
        return this.serverConfig.get(CONDUIT_TOKEN);
    }

    public String getDiffId() {
        return this.serverConfig.get(DIFF_ID);
    }

    /**
     * This is what shows up in a Phabricator URL prefixed by D, for example in
     * http://my.phabricator.com/D123
     *
     * We never really use it except when constructing URLs. Patches work off the
     * DiffId.
     * @return
     */
    public String getRevisionId() {
        return this.serverConfig.get(REVISION_ID);
    }

    public Boolean isEnabled() {
        return this.canRun;
    }


    public Boolean reportBegin() {
        return "true".equals(this.serverConfig.get(REPORT_BEGIN));
    }
}

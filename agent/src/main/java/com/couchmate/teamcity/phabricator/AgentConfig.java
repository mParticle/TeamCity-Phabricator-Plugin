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
public final class AgentConfig {

    private static Logger Log  = Loggers.AGENT;

    private final String SERVER_URL = "teamcity.serverUrl";

    private final String ARC_PATH = "tcphab.pathToArc";
    private final String PHAB_URL = "tcphab.phabricatorUrl";
    private final String PHAB_PROTOCOL = "tcphab.phabricatorProtocol";
    private final String PATCH = "tcphab.patch";
    private final String CONDUIT_TOKEN = "tcphab.conduitToken";

    private final String DIFF_ID = "diffId";
    private final String REVISION_ID = "revisionId";
    private final String HARBORMASTER_PHID = "harbormasterTargetPHID";

    private Map<String, String> agentConfig = new HashMap<>();

    private Boolean canRun = false;
    private String workingDir;

    public AgentConfig(Map<String, String> params)
    {
        this.canRun = true;
        if (params == null)
        {
            this.canRun = false;
            Log.warn("Missing all required parameters, Phabricator build step cannot run.");
            return;
        }

        this.agentConfig.put(SERVER_URL, params.get(SERVER_URL));
        this.agentConfig.put(ARC_PATH, params.get(ARC_PATH));
        this.agentConfig.put(PATCH, params.get(PATCH));
        this.agentConfig.put(CONDUIT_TOKEN, params.get(CONDUIT_TOKEN));
        this.agentConfig.put(DIFF_ID, params.get(DIFF_ID));
        this.agentConfig.put(REVISION_ID, params.get(REVISION_ID));
        this.agentConfig.put(HARBORMASTER_PHID, params.get(HARBORMASTER_PHID));

        try {
            URL aURL = new URL(params.get(PHAB_URL));
            this.agentConfig.put(PHAB_URL, aURL.getHost());
            this.agentConfig.put(PHAB_PROTOCOL, aURL.getProtocol());
        } catch (IOException e) {
            this.agentConfig.put(PHAB_URL, null);
            this.agentConfig.put(PHAB_PROTOCOL, null);
            Log.info(String.format("phabricator url could not be parsed: %s", e.getStackTrace()[0].toString()));
        }

        for (String key : agentConfig.keySet())
        {
            Log.info(String.format("Found %s as %s", key, agentConfig.get(key)));
            if (agentConfig.get(key) == null)
            {
                canRun = false;
                Log.warn("Missing a required agent parameter, will not be able to run Phabricator build step.");
            }
        }
    }

    public String getServerUrl() {
        return this.agentConfig.get(SERVER_URL);
    }

    public String getArcPath() {
        return this.agentConfig.get(ARC_PATH);
    }

    public String getHarbormasterTargetPHID() {
        return this.agentConfig.get(HARBORMASTER_PHID);
    }

    public String getPhabricatorProtocol() {
        return this.agentConfig.get(PHAB_PROTOCOL);
    }

    public String getPhabricatorUrl() {
        return this.agentConfig.get(PHAB_URL);
    }

    public String getConduitToken() {
        return this.agentConfig.get(CONDUIT_TOKEN);
    }

    public String getDiffId() {
        return this.agentConfig.get(DIFF_ID);
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
        return this.agentConfig.get(REVISION_ID);
    }

    public Boolean shouldPatch() {
        return "true".equals(this.agentConfig.get(PATCH));
    }

    public Boolean isEnabled() {
        return this.canRun;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public String getWorkingDir() {
        return this.workingDir;
    }
}

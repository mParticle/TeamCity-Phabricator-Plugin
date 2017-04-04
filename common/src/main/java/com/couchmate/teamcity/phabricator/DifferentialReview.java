package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.clients.ConduitClient;
import com.couchmate.teamcity.phabricator.conduit.Result;
import com.google.gson.JsonObject;
import jetbrains.buildServer.log.Loggers;

/**
 * Created by mbenua on 3/24/2017.
 */
public class DifferentialReview {

    private String diffId;

    private ConduitClient conduitClient;

    private String authorName = "unknown";
    private String authorEmail = "unknown";
    private String branch = "master";
    private String baseCommit = "";
    private String revisionId = "";

    public DifferentialReview(ConduitClient conduit){
        this.conduitClient = conduit;
    }

    public String getAuthorName() { return this.authorName; }
    public String getAuthorEmail() { return this.authorEmail; }
    public String getBranch() { return this.branch; }
    public String getBaseCommit() { return this.baseCommit; }
    public String getRevisionId() { return this.revisionId; }
    public String getDiffId() { return this.diffId; }

    public boolean fetchReviewData(String diffId) {
        if (diffId == null || diffId.isEmpty())
        {
            return false;
        }

        this.diffId = getNumericDiffId(diffId);

        Result result = this.conduitClient.getDiffDetails(this.diffId);
        if (result == null)
        {
            Loggers.AGENT.warn("Failed to get back an OK response from Phabricator server.");
            return false;
        }

        JsonObject diff = result.getJsonResult().getAsJsonObject(this.diffId);
        this.authorName = getOrElse(diff, "authorName", this.authorName);
        this.authorEmail = getOrElse(diff, "authorEmail", this.authorEmail);
        this.baseCommit = getOrElse(diff, "sourceControlBaseRevision", this.baseCommit);
        this.branch = getOrElse(diff, "branch", this.branch);
        this.revisionId = getOrElse(diff, "revisionID", this.revisionId);

        return true;
    }

    private static String getNumericDiffId(String longDiffId)
    {
        if (longDiffId.startsWith("D") || longDiffId.startsWith("d")) {
            return longDiffId.substring(1);
        }

        return longDiffId;
    }

    private String getOrElse(JsonObject json, String key, String orElse) {
        if (json.has(key)) {
            return json.getAsJsonPrimitive(key).getAsString();
        }
        return orElse;
    }
}

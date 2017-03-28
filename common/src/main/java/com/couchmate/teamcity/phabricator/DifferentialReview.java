package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.clients.ConduitClient;
import com.couchmate.teamcity.phabricator.conduit.Result;
import net.sf.json.JSONObject;
import org.json.JSONException;

/**
 * Created by mbenua on 3/24/2017.
 */
public class DifferentialReview {

    private String diffId;

    private PhabLogger logger = new PhabLogger();
    private ConduitClient conduitClient;

    private String authorName = "unknown";
    private String authorEmail = "unknown";
    private String branch = "master";
    private String baseCommit = "";

    public DifferentialReview(ConduitClient conduit){
        this.conduitClient = conduit;
    }

    public String getAuthorName() { return this.authorName; }
    public String getAuthorEmail() { return this.authorEmail; }
    public String getBranch() { return this.branch; }
    public String getBaseCommit() { return this.baseCommit; }
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
            return false;
        }

        try
        {
            JSONObject diff = result.getJsonResult().getJSONObject(this.diffId);
            this.authorName = getOrElse(diff, "authorName", this.authorName);
            this.authorEmail = getOrElse(diff, "authorEmail", this.authorEmail);
            this.baseCommit = getOrElse(diff, "sourceControlBaseRevision", this.baseCommit);
            this.branch = getOrElse(diff, "branch", this.branch);

        } catch (JSONException e) {
            logger.warn(String.format("Could not parse off diff ID %s from the returned JSON", this.diffId));
            return false;
        }

        return true;
    }

    private static String getNumericDiffId(String longDiffId)
    {
        if (longDiffId.startsWith("D") || longDiffId.startsWith("d")) {
            return longDiffId.substring(1);
        }

        return longDiffId;
    }

    private String getOrElse(JSONObject json, String key, String orElse) {
        if (json.has(key)) {
            return json.getString(key);
        }
        return orElse;
    }
}

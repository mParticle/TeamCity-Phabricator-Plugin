package com.couchmate.teamcity.phabricator.conduit;

/**
 * Created by mbenua on 3/24/2017.
 */
public final class HarbormasterUriArtifactMessage extends MessageBase {

    private String url;
    private String buildTargetPhid;

    public HarbormasterUriArtifactMessage(String url, String buildTargetPhid, String apiKey) {
        super(apiKey);
        this.url = url;
        this.buildTargetPhid = buildTargetPhid;
    }

    public String getUrl() { return url; }
    public String getBuildTargetPhid() { return buildTargetPhid; }

}

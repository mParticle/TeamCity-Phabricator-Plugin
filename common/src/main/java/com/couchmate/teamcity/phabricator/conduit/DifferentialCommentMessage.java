package com.couchmate.teamcity.phabricator.conduit;

import com.couchmate.teamcity.phabricator.MessageType;

public final class DifferentialCommentMessage extends MessageBase {

    private final String revisionId;
    private final String comment;

    public DifferentialCommentMessage(
            final String apiKey,
            final String revisionId,
            final String comment
    ){
        super(apiKey);
        this.revisionId = revisionId;
        this.comment = comment;
    }

    public String getRevisionId() { return this.revisionId; }
    public String getComment() { return this.comment; }


    public static DifferentialCommentMessage generateBuildMessage(MessageType messageType, String revisionId, String serverUrl)
    {
        String comment;
        switch(messageType)
        {
            case FAIL:
                comment = "Build failed: " + serverUrl;
                break;
            case PASS:
                comment = "Build succeeded: " + serverUrl;
                break;
            case WORK:
                comment = "Build in progress: " + serverUrl;
                break;
            default:
                comment = "Build error: " + serverUrl;
        }
        return new DifferentialCommentMessage("", revisionId, comment);
    }
}

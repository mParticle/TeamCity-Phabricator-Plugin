package com.couchmate.teamcity.phabricator.conduit;

import com.couchmate.teamcity.phabricator.MessageType;
import com.couchmate.teamcity.phabricator.UnitTestResults;

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


    public static DifferentialCommentMessage generateBuildMessage(MessageType messageType, String revisionId, String serverUrl, UnitTestResults testResults)
    {
        String comment;
        switch(messageType)
        {
            case FAIL:
                switch(testResults.didRunPass())
                {
                    case FAIL:
                        comment = String.format("Build failed. Failing unit tests: %s / %s. See: %s",
                                testResults.getFailedTestsCount(),
                                testResults.getTotalTestsCount(),
                                serverUrl);
                        break;

                    case PASS:
                        comment = "Build failed, but all unit tests reported passing. See: " + serverUrl;
                        break;

                    case NOTRUN:
                    default:
                        comment = "Build failed to compile. See: " + serverUrl;
                        break;
                }
                break;

            case PASS:
                if (testResults.didRunPass() == UnitTestResults.TestRunStatus.PASS)
                {
                    comment = String.format("Build succeeded. Tests passed: %s. See: %s", testResults.getTotalTestsCount(), serverUrl);
                }
                else
                {
                    comment = "Build succeeded: " + serverUrl;
                }
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

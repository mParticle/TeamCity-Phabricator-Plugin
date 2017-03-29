package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.clients.ConduitClient;
import com.couchmate.teamcity.phabricator.conduit.DifferentialCommentMessage;
import com.couchmate.teamcity.phabricator.conduit.HarbormasterBuildStatusMessage;
import com.couchmate.teamcity.phabricator.conduit.HarbormasterUriArtifactMessage;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.STestRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildTracker {
    private static Logger Log = Loggers.SERVER;
    private SRunningBuild build;
    private ConduitClient conduitClient = null;

    public BuildTracker(SRunningBuild build) {
        this.build = build;
        Log.info("Phabricator reporting build results for build #" + build.getBuildNumber());
    }

    public void run() {
        ServerConfig appConfig = null;

        try {
            Map<String, String> params = new HashMap<>();
            params.putAll(this.build.getBuildOwnParameters());
            params.putAll(this.build.getValueResolver().resolve(this.build.getBuildPromotion().getParameters()));
            params.putAll(this.build.getBuildFeaturesOfType("phabricator").iterator().next().getParameters());

            appConfig = new ServerConfig(params);
            this.conduitClient = new ConduitClient(appConfig.getPhabricatorUrl(), appConfig.getPhabricatorProtocol(), appConfig.getConduitToken(), Log);
        } catch (Exception e) { Log.error("Failed to parse required appconfig params for reporting final status to Phabricator.", e); }


        if (appConfig != null && appConfig.isEnabled()) {
             ArrayList<UnitTestResult> testResults = createResults(build.getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS).getAllTests());

             Status status = this.build.getBuildStatus();
             HarbormasterBuildStatusMessage buildMessage = new HarbormasterBuildStatusMessage(
                     appConfig.getConduitToken(),
                     appConfig.getHarbormasterTargetPHID(),
                     MessageType.fromStatus(status),
                     testResults);
             this.conduitClient.submitHarbormasterMessage(buildMessage);

             String buildInfo = appConfig.getServerUrl() + "/viewLog.html?buildId=" + build.getBuildId();
             DifferentialCommentMessage comment = DifferentialCommentMessage.generateBuildMessage(MessageType.fromStatus(status), appConfig.getRevisionId(), buildInfo);
             this.conduitClient.submitDifferentialComment(comment);

             HarbormasterUriArtifactMessage artifactMessage = new HarbormasterUriArtifactMessage(
                     buildInfo,
                     appConfig.getHarbormasterTargetPHID(),
                     appConfig.getConduitToken()
             );
             this.conduitClient.submitHarbormasterArtifact(artifactMessage);

            Log.info("Phabricator build #" + this.build.getBuildNumber() + " finished.");
        }
    }

    private static ArrayList<UnitTestResult> createResults(List<STestRun> tests) {
        ArrayList<UnitTestResult> results = new ArrayList<>();

        if (tests != null) {
            for (STestRun test : tests) {
                results.add(new UnitTestResult(
                        test.getTest().getName().getAsString(),
                        MessageType.fromStatus(test.getStatus()),
                        test.getTest().getName().getClassName(),
                        test.getDuration()));
            }
        }

        return results;
    }
}

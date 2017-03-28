package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.clients.ConduitClient;
import com.couchmate.teamcity.phabricator.conduit.DifferentialCommentMessage;
import com.couchmate.teamcity.phabricator.conduit.HarbormasterBuildStatusMessage;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.STestRun;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildTracker {
    private static Logger Log = Logger.getInstance(BuildTracker.class.getName());

    private SRunningBuild build;
    private AppConfig appConfig;
    private ConduitClient conduitClient = null;
    private PhabLogger logger;

    public BuildTracker(SRunningBuild build, PhabLogger logger) {
        this.build = build;
        this.appConfig = new AppConfig();
        this.logger = logger;
        Log.info("Tracking build" + build.getBuildNumber());
    }

    public void run() {
        if (!appConfig.isEnabled()) {
            try {
                Map<String, String> params = new HashMap<>();
                //params.putAll(this.build.getBuildOwnParameters());
                params.putAll(this.build.getValueResolver().resolve(this.build.getBuildPromotion().getParameters()));
                params.putAll(this.build.getBuildFeaturesOfType("phabricator").iterator().next().getParameters());
                for (String param : params.keySet()) {
                    if (param != null) {
                        Loggers.AGENT.info(String.format("Found %s", param));
                    }
                }
                this.appConfig.setParams(params);
                this.appConfig.parse();
                this.conduitClient = new ConduitClient(this.appConfig.getPhabricatorUrl(), this.appConfig.getPhabricatorProtocol(), this.appConfig.getConduitToken(), this.logger);
            } catch (Exception e) { Log.error("BuildTracker Param Parse", e); }
        }

        if (appConfig.isEnabled()) {
             ArrayList<UnitTestResult> testResults = createResults(build.getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS).getAllTests());

             Loggers.SERVER.info("is this build successful " + this.build.getBuildStatus().isSuccessful());
             Loggers.SERVER.info("this.appConfig.getRevisionId() = " + this.appConfig.getRevisionId());
             Status status = this.build.getBuildStatus();
             if (this.appConfig.isEnabled()) {
                 HarbormasterBuildStatusMessage buildMessage = new HarbormasterBuildStatusMessage(
                         this.appConfig.getConduitToken(),
                         this.appConfig.getHarbormasterTargetPHID(),
                         MessageType.fromStatus(status),
                         testResults);
                 this.conduitClient.submitHarbormasterMessage(buildMessage);

                 String buildInfo = this.appConfig.getServerUrl() + "/viewLog.html?buildId=" + build.getBuildId();
                 DifferentialCommentMessage comment = DifferentialCommentMessage.generateBuildMessage(MessageType.fromStatus(status), this.appConfig.getRevisionId(), buildInfo);
                 this.conduitClient.submitDifferentialComment(comment);
             }
             Loggers.SERVER.info(this.build.getBuildNumber() + " finished");
        }
    }

    private CloseableHttpClient createHttpClient() {
        HttpClient client = new HttpClient(true);
        return client.getCloseableHttpClient();
    }

    private static ArrayList<UnitTestResult> createResults(List<STestRun> tests)
    {
        ArrayList<UnitTestResult> results = new ArrayList<>();
        for (STestRun test : tests) {
            results.add(new UnitTestResult(
                    test.getTest().getName().getAsString(),
                    MessageType.fromStatus(test.getStatus()),
                    test.getTest().getName().getClassName(),
                    test.getDuration()));
        }

        return results;
    }

    private void sendTestReport(String testName, STestRun test) {
        HttpRequestBuilder httpPost = new HttpRequestBuilder()
                .post()
                .setHost(this.appConfig.getPhabricatorUrl())
                .setScheme(this.appConfig.getPhabricatorProtocol())
                .setPath("/api/harbormaster.sendmessage")
                .addFormParam(new StringKeyValue("api.token", this.appConfig.getConduitToken()))
                .addFormParam(new StringKeyValue("buildTargetPHID", this.appConfig.getHarbormasterTargetPHID()))
                .addFormParam(new StringKeyValue("type", "work"))
                .addFormParam(new StringKeyValue("unit[0][name]", test.getTest().getName().getTestMethodName()))
                .addFormParam(new StringKeyValue("unit[0][namespace]", test.getTest().getName().getClassName()));

        if (test.getStatus().isSuccessful()) {
            httpPost.addFormParam(new StringKeyValue("unit[0][result]", "pass"));
        } else if (test.getStatus().isFailed()) {
            httpPost.addFormParam(new StringKeyValue("unit[0][result]", "fail"));
        }
        try (CloseableHttpResponse response = createHttpClient().execute(httpPost.build())) {
            Loggers.SERVER.warn(String.format("Test Response: %s\nTest Body: %s\n",
                    response.getStatusLine().getStatusCode(),
                    IOUtils.toString(response.getEntity().getContent())));
        } catch (Exception e) { Loggers.SERVER.error("Send error", e); }
    }
}

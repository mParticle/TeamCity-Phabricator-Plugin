package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.clients.ConduitClient;
import com.couchmate.teamcity.phabricator.conduit.DifferentialCommentMessage;
import com.couchmate.teamcity.phabricator.tasks.ApplyPatch;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Agent extends AgentLifeCycleAdapter {

    private static Logger Log = Logger.getInstance(ConduitClient.class.getName());
    private BuildProgressLogger buildLogger = null;
    private PhabLogger logger = null;
    private AppConfig appConfig = null;
    private Collection<AgentBuildFeature> buildFeatures = null;
    private ConduitClient conduitClient = null;
    private boolean first = true;
    private Map<String, Integer> unique;
    private AgentRunningBuild runningBuild = null;
    public Agent(
            @NotNull final EventDispatcher<AgentLifeCycleListener> eventDispatcher,
            @NotNull final PhabLogger phabLogger,
            @NotNull final AppConfig appConfig
    ) {
        eventDispatcher.addListener(this);
        this.logger = phabLogger;
        this.appConfig = appConfig;
        this.logger.info("Instantiated");
    }

    @Override
    public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
        super.buildStarted(runningBuild);
        this.logger.setBuildLogger(runningBuild.getBuildLogger());
        this.runningBuild = runningBuild;
        this.unique = new HashMap<String, Integer>();
    }

    private void refreshConfig(AgentRunningBuild build) {
        this.buildFeatures = build.getBuildFeaturesOfType("phabricator");
        this.appConfig.setParams(null);
        this.appConfig.setEnabled(false);
        if (!this.buildFeatures.isEmpty()) {
            try {
                Map<String, String> configs = new HashMap<>();
                configs.putAll(build.getSharedBuildParameters().getEnvironmentVariables());
                configs.putAll(build.getSharedConfigParameters());
                configs.putAll(this.buildFeatures.iterator().next().getParameters());
                this.appConfig.setParams(configs);
                this.appConfig.setLogger(this.logger);
                this.appConfig.parse();
                int count = this.unique.containsKey(this.appConfig.getHarbormasterTargetPHID()) ? this.unique.get(this.appConfig.getHarbormasterTargetPHID()) : 0;
                this.unique.put(this.appConfig.getHarbormasterTargetPHID(), count + 1);
              } catch (Exception e) { this.logger.warn("Build Started Error: ", e); }
         }
    }

    @Override
    public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
        super.beforeRunnerStart(runner);
        this.refreshConfig(runner.getBuild());
        if (this.appConfig.isEnabled() && this.unique.get(this.appConfig.getHarbormasterTargetPHID()) == 1) {
            Log.info("Getting Build Id " + runner.getBuild().getBuildId());
            Log.info("Plugin is enabled, starting patch process");
            this.appConfig.setWorkingDir(runner.getWorkingDirectory().getPath());
            Log.info("working dir = " + this.appConfig.getWorkingDir());
            this.conduitClient = new ConduitClient(this.appConfig.getPhabricatorUrl(), this.appConfig.getPhabricatorProtocol(), this.appConfig.getConduitToken(), this.logger);
            if(this.appConfig.shouldPatch()) {
                DifferentialReview review = new DifferentialReview(this.conduitClient);
                if (!review.fetchReviewData(this.appConfig.getDiffId()))
                {
                    Log.warn("Failed to fetch the full diff information from Phabricator.");
                }
                else
                {
                    new ApplyPatch(runner, this.appConfig, review).run();

                    // Notify Phabricator that the build has started.
                    DifferentialCommentMessage message = new DifferentialCommentMessage(
                            this.appConfig.getConduitToken(),
                            this.appConfig.getRevisionId(),
                            "Build started: " + this.appConfig.getServerUrl() + "/viewLog.html?buildId=" + runner.getBuild().getBuildId());
                    this.conduitClient.submitDifferentialComment(message);
                }
            }
        }
    }

    @Override
    public void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus status) {
         super.runnerFinished(runner, status);
    }

    @Override
    public void buildFinished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus status) {
        super.buildFinished(build, status);
        this.refreshConfig(build);
    }
}

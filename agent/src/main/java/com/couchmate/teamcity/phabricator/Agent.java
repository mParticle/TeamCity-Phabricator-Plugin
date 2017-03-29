package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.clients.ConduitClient;
import com.couchmate.teamcity.phabricator.conduit.DifferentialCommentMessage;
import com.couchmate.teamcity.phabricator.tasks.ApplyPatch;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Agent extends AgentLifeCycleAdapter {
    private static Logger Log = Loggers.AGENT;

    private AgentConfig appConfig = null;
    private Collection<AgentBuildFeature> buildFeatures = null;
    private ConduitClient conduitClient = null;

    public Agent(@NotNull final EventDispatcher<AgentLifeCycleListener> eventDispatcher) {
        eventDispatcher.addListener(this);
    }

    @Override
    public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
        super.buildStarted(runningBuild);
    }

    private void refreshConfig(AgentRunningBuild build) {
        this.buildFeatures = build.getBuildFeaturesOfType("phabricator");
        if (!this.buildFeatures.isEmpty()) {
            try {
                Map<String, String> configs = new HashMap<>();
                configs.putAll(build.getSharedBuildParameters().getEnvironmentVariables());
                configs.putAll(build.getSharedConfigParameters());
                configs.putAll(this.buildFeatures.iterator().next().getParameters());
                this.appConfig = new AgentConfig(configs);
              }
              catch (Exception e) { Log.warn("Phabricator Build Started Error: ", e); }
         }
    }

    @Override
    public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
        BuildProgressLogger buildLogger = runner.getBuild().getBuildLogger();
        this.refreshConfig(runner.getBuild());

        if (this.appConfig.isEnabled()) {
            buildLogger.activityStarted("Phabricator Plugin", "Applying Differential Diff");

            this.appConfig.setWorkingDir(runner.getWorkingDirectory().getPath());
            this.conduitClient = new ConduitClient(this.appConfig.getPhabricatorUrl(), this.appConfig.getPhabricatorProtocol(), this.appConfig.getConduitToken(), Log);

            if(this.appConfig.shouldPatch()) {
                DifferentialReview review = new DifferentialReview(this.conduitClient);

                buildLogger.progressStarted("Fetching details for diff " + this.appConfig.getDiffId() + " from Phabricator server.");
                boolean result = review.fetchReviewData(this.appConfig.getDiffId());
                buildLogger.progressFinished();

                if (!result)
                {
                    BuildProblemData problem = BuildProblemData.createBuildProblem("PHAB_DIFF_FAILURE",
                            "PHAB_DIFF_FAILURE",
                            "Failed to fetch the full diff information from Phabricator.");
                    buildLogger.buildFailureDescription(problem.getDescription());
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

            buildLogger.activityFinished("Phabricator Plugin", "Applying Differential Diff");
        }

        super.beforeRunnerStart(runner);
    }

    @Override
    public void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus status) {
         super.runnerFinished(runner, status);
    }

    @Override
    public void buildFinished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus status) {
        super.buildFinished(build, status);
        this.refreshConfig(build);
        build.getBuildLogger().progressStarted("Phabricator is going to post status.");
        build.getBuildLogger().progressFinished();
        build.addSharedConfigParameter("teamcity.serverUrl", this.appConfig.getServerUrl());
    }

}

package com.couchmate.teamcity.phabricator.tasks;

import com.couchmate.teamcity.phabricator.AppConfig;
import com.couchmate.teamcity.phabricator.DifferentialReview;
import com.couchmate.teamcity.phabricator.clients.ArcanistClient;
import com.couchmate.teamcity.phabricator.clients.GitClient;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;

/**
 * Created by mjo20 on 10/15/2015.
 */
public class ApplyPatch extends Task {

    private BuildProgressLogger logger;
    private AppConfig appConfig;
    private GitClient gitClient = null;
    private ArcanistClient arcanistClient = null;
    private BuildRunnerContext runner;
    private BuildProblemData buildProblem;
    private DifferentialReview review;

    private static final String GIT_PATCH_FAILURE = "GIT_PATCH_FAILURE";
    private static final String ARC_PATCH_FAILURE = "ARC_PATCH_FAILURE";

    public ApplyPatch(BuildRunnerContext runner, AppConfig appConfig, DifferentialReview review){
        this.appConfig = appConfig;
        this.logger = runner.getBuild().getBuildLogger();
        this.runner = runner;
        this.review = review;
    }

    @Override
    protected void setup() {
        this.gitClient = new GitClient(this.review.getBaseCommit(), this.appConfig.getWorkingDir());
        this.arcanistClient = new ArcanistClient(
                this.appConfig.getConduitToken(), this.appConfig.getWorkingDir(), this.appConfig.getArcPath());
    }

    @Override
    protected void execute() {
        this.logger.activityStarted("Phabricator Plugin", "Applying Differential Patch " + this.review.getDiffId());
        int cleanCode, patchCode, resetCode;

        if (!gitClient.refreshGit())
        {
            buildProblem = BuildProblemData.createBuildProblem(GIT_PATCH_FAILURE,
                    GIT_PATCH_FAILURE,
                    String.format("Unable to git reset or git clean for arc diff #%s and base commit %s.",
                            this.appConfig.getDiffId(),
                            this.review.getBaseCommit()));
            this.logger.logBuildProblem(buildProblem);
        }

        if (!arcanistClient.patch(this.review))
        {
            buildProblem = BuildProblemData.createBuildProblem(ARC_PATCH_FAILURE,
                    ARC_PATCH_FAILURE,
                    "Unable to patch master with this arc diff " + this.review.getDiffId());
            this.logger.logBuildProblem(buildProblem);
        }

        this.logger.activityFinished("Phabricator Plugin", "Finished Applying Differential Patch " + this.review.getDiffId());
    }

    @Override
    protected void teardown() {

    }
}

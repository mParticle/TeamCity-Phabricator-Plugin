package com.couchmate.teamcity.phabricator.tasks;

import com.couchmate.teamcity.phabricator.AgentConfig;
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
    private AgentConfig appConfig;
    private GitClient gitClient = null;
    private ArcanistClient arcanistClient = null;
    private DifferentialReview review;

    public ApplyPatch(BuildRunnerContext runner, AgentConfig appConfig, DifferentialReview review){
        this.appConfig = appConfig;
        this.logger = runner.getBuild().getBuildLogger();
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
        this.logger.progressStarted("Resetting git to commit ID: " + this.review.getBaseCommit() + " as base for branch: " + this.review.getBranch());
        BuildProblemData gitProblem = gitClient.refreshGit();
        if (gitProblem != null)
        {
            this.logger.buildFailureDescription(gitProblem.getDescription());
        }
        this.logger.progressFinished();

        this.logger.progressStarted("Attempting an arc patch for diff ID: " + this.review.getDiffId());
        BuildProblemData patchProblem = arcanistClient.patch(this.review);
        if (patchProblem != null)
        {
            this.logger.buildFailureDescription(patchProblem.getDescription());
        }
        this.logger.progressFinished();
    }

    @Override
    protected void teardown() {

    }
}

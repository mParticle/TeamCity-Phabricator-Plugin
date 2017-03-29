package com.couchmate.teamcity.phabricator.clients;

import com.couchmate.teamcity.phabricator.CommandBuilder;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.BuildProblemData;

public final class GitClient {

    private final String GIT_COMMAND = "git";
    private final String workingDir;
    private String baseCommit;

    private static Logger Log = Logger.getInstance(GitClient.class.getName());
    /**
     * @param baseCommit The base commit. This has the side-effect of telling us which branch to pull down as the base.
     * @param workingDir Sets the directory for the command to be executed in. This should be the source code root.
     */
    public GitClient(String baseCommit, final String workingDir){
        this.workingDir = workingDir;
        this.baseCommit = baseCommit;
    }

    public BuildProblemData refreshGit()
    {
        CommandBuilder.Command fetch = new CommandBuilder()
                .setWorkingDir(this.workingDir)
                .setCommand(this.GIT_COMMAND)
                .setAction("fetch")
                .build();

        int fetchCode = fetch.exec().join();
        if (fetchCode > 0)
        {
            return BuildProblemData.createBuildProblem("PHABRICATOR_PATCH", "GIT FETCH",
                    String.format("Unable to git fetch from origin: %s",
                            fetch.toString()));
        }

        CommandBuilder.Command reset = new CommandBuilder()
                .setWorkingDir(this.workingDir)
                .setCommand(this.GIT_COMMAND)
                .setAction("reset")
                .setFlag("--hard")
                .setArg(this.baseCommit)
                .build();

        int resetCode = reset.exec().join();
        if (resetCode > 0)
        {
            return BuildProblemData.createBuildProblem("PHABRICATOR_PATCH", "GIT RESET",
                    String.format("Unable to git reset to base commit %s: %s.",
                            this.baseCommit,
                            reset.toString()));
        }

        CommandBuilder.Command clean = new CommandBuilder()
                .setWorkingDir(this.workingDir)
                .setCommand(this.GIT_COMMAND)
                .setAction("clean")
                .setArg("-fd")
                .setArg("-f")
                .build();

        int cleanCode = clean.exec().join();
        if (cleanCode > 0)
        {
            return BuildProblemData.createBuildProblem("PHABRICATOR_PATCH", "GIT CLEAN",
                    "Unable to git clean: " + clean.toString());
        }

        return null;
    }
}

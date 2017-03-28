package com.couchmate.teamcity.phabricator.clients;

import com.couchmate.teamcity.phabricator.CommandBuilder;
import com.intellij.openapi.diagnostic.Logger;

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

    public boolean refreshGit()
    {
        CommandBuilder.Command reset = new CommandBuilder()
                .setWorkingDir(this.workingDir)
                .setCommand(this.GIT_COMMAND)
                .setAction("reset")
                .setFlag("--hard")
                .setFlag(this.baseCommit)
                .build();

        int resetCode = reset.exec().join();
        if (resetCode > 0)
        {
            Log.warn("Unable to git reset to base commit " + this.baseCommit);
            return false;
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
            Log.warn("Unable to git clean.");
            return false;
        }

        return true;
    }
}

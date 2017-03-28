package com.couchmate.teamcity.phabricator.clients;

import com.couchmate.teamcity.phabricator.CommandBuilder;
import com.couchmate.teamcity.phabricator.DifferentialReview;
import com.couchmate.teamcity.phabricator.StringKeyValue;
import com.intellij.openapi.diagnostic.Logger;

public final class ArcanistClient {
    private static Logger Log = Logger.getInstance(GitClient.class.getName());

    private final String conduitToken;
    private final String workingDir;
    private final String arcPath;

    public ArcanistClient(final String conduitToken, final String workingDir, final String arcPath){
        this.conduitToken = conduitToken;
        this.workingDir = workingDir;
        this.arcPath = arcPath;
    }

    /**
     * @param review The differential review details that need to be patched.
     * @return
     */
    public boolean patch(DifferentialReview review){
        try {
            CommandBuilder.Command patch = new CommandBuilder()
                    .setCommand(arcPath)
                    .setAction("patch")
                    .setArg(review.getDiffId().startsWith("D") ? review.getDiffId() : "D" + review.getDiffId())
                    .setWorkingDir(this.workingDir)
                    .setFlag("--nobranch")
                    .setFlag("--nocommit")
                    .setFlagWithValueEquals(new StringKeyValue("--conduit-token", this.conduitToken))
                    .build();

            int patchCode = patch.exec().join();
            if (patchCode > 0)
            {
                Log.warn(String.format("Arcanist returned an error code of %s, assuming patch failed.", patchCode));
                return false;
            }
        } catch (Exception e) {
            Log.warn("Failed to do arc patch.", e);
            return false;
        }

        return true;
    }
}

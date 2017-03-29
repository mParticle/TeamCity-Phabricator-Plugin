package com.couchmate.teamcity.phabricator.clients;

import com.couchmate.teamcity.phabricator.CommandBuilder;
import com.couchmate.teamcity.phabricator.DifferentialReview;
import com.couchmate.teamcity.phabricator.StringKeyValue;
import jetbrains.buildServer.BuildProblemData;

public final class ArcanistClient {
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
    public BuildProblemData patch(DifferentialReview review){
        try {
            CommandBuilder.Command patch = new CommandBuilder()
                    .setCommand(arcPath)
                    .setAction("patch")
                    .setArg(review.getRevisionId().startsWith("D") ? review.getRevisionId() : "D" + review.getRevisionId())
                    .setWorkingDir(this.workingDir)
                    .setFlag("--nobranch")
                    .setFlag("--nocommit")
                    .setFlag("--force")
                    .setFlagWithValueEquals(new StringKeyValue("--conduit-token", this.conduitToken))
                    .build();

            int patchCode = patch.exec().join();
            if (patchCode > 0)
            {
                return BuildProblemData.createBuildProblem("PHABRICATOR_PATCH", "ARC PATCH",
                        String.format("Arcanist returned an error code of %s, patch failed using command %s.",
                                patchCode,
                                patch.toString()));
            }
        } catch (Exception e) {
            return BuildProblemData.createBuildProblem("PHABRICATOR_PATCH", "ARC PATCH",
                    String.format("Failed to do arc patch with exception: ", e.getMessage()));

        }

        return null;
    }
}

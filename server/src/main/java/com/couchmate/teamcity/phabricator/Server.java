package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.clients.ConduitClient;
import com.couchmate.teamcity.phabricator.conduit.HarbormasterBuildStatusMessage;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Server extends BuildServerAdapter {
    private Logger Log = Loggers.SERVER;

    public Server(@NotNull final EventDispatcher<BuildServerListener> buildServerListener) {
        buildServerListener.addListener(this);
    }

    @Override
    public void buildTypeAddedToQueue(@NotNull SQueuedBuild queuedBuild) {
        super.buildTypeAddedToQueue(queuedBuild);

        Collection<SBuildFeatureDescriptor> phabBuildFeature = queuedBuild.getBuildType().getBuildFeaturesOfType("phabricator");
        if (!phabBuildFeature.isEmpty()) {

            // Get all the config params we can find and parse off the ones we need.
            Map<String, String> params = new HashMap<>();
            params.putAll(queuedBuild.getBuildPromotion().getParameters());
            params.putAll(phabBuildFeature.iterator().next().getParameters());
            ServerConfig appConfig = new ServerConfig(params);

            queuedBuild.getBuildPromotion().setBuildComment(queuedBuild.getTriggeredBy().getUser(),
                appConfig.getPhabricatorProtocol() + "://" + appConfig.getPhabricatorUrl() + "/D" + appConfig.getRevisionId());

            if (appConfig.reportBegin()) {
                ConduitClient conduitClient = new ConduitClient(appConfig.getPhabricatorUrl(), appConfig.getPhabricatorProtocol(), appConfig.getConduitToken(), Log);

                conduitClient.submitHarbormasterMessage(new HarbormasterBuildStatusMessage(
                        appConfig.getConduitToken(),
                        appConfig.getHarbormasterTargetPHID(),
                        MessageType.WORK,
                        null));
            }
        }
    }

    @Override
    public void buildFinished(@NotNull SRunningBuild runningBuild) {
        super.buildFinished(runningBuild);
        Collection<SBuildFeatureDescriptor> buildFeatures = runningBuild.getBuildFeaturesOfType("phabricator");
        if (!buildFeatures.isEmpty()) {
            BuildTracker buildTracker = new BuildTracker(runningBuild);
            buildTracker.run();
        }
    }
}

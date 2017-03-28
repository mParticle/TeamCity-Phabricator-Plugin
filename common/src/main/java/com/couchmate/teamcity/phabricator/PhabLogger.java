package com.couchmate.teamcity.phabricator;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

public final class PhabLogger {

    @NonNls
    private static final String AGENT_BLOCK = "agent";
    private static final String ACTIVITY_NAME = "Phabricator";

    @Nullable
    BuildProgressLogger buildProgressLogger;

    public void setBuildLogger(@Nullable BuildProgressLogger buildLogger){
        this.buildProgressLogger = buildLogger;
    }

    public void info(Map map){
        Loggers.SERVER.info(Arrays.toString(map.entrySet().toArray()));
    }

    public void info(String message){
        Loggers.AGENT.info(String.format("Phabricator Plugin: %s", message));
    }

    public void warn(String message, Exception e){
        Loggers.AGENT.warn(String.format("Phabricator Plugin: %s", message, e));
    }

    public void warn(String message){
        Loggers.AGENT.warn(String.format("Phabricator Plugin: %s", message));
    }
    public void serverInfo(String message){
        Loggers.AGENT.info(String.format("Phabricator Plugin: %s", message));
    }

}

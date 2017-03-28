package com.couchmate.teamcity.phabricator;

import jetbrains.buildServer.messages.Status;

/**
 * Created by mbenua on 3/24/2017.
 */
public enum MessageType {
    PASS,
    FAIL,
    WORK,
    UNKNOWN;

    public static MessageType fromStatus(Status status)
    {
        if (status.isSuccessful())
            return MessageType.PASS;

        if (status.isFailed())
            return MessageType.FAIL;

        return MessageType.UNKNOWN;
    }

    public String intoString()
    {
        switch (this){
            case PASS:
                return "pass";
            case FAIL:
                return "fail";
            case WORK:
                return "work";
            default:
                return "unknown";
        }
    }
}
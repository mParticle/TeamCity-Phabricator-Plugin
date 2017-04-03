package com.couchmate.teamcity.phabricator.conduit;

import com.couchmate.teamcity.phabricator.MessageType;
import com.couchmate.teamcity.phabricator.UnitTestResult;
import com.couchmate.teamcity.phabricator.UnitTestResults;

import java.util.ArrayList;
import java.util.List;

public final class HarbormasterBuildStatusMessage extends MessageBase {

    public enum HarbormasterUnitResultType{
        PASS,
        FAIL,
        SKIP,
        BROKEN,
        UNSOUND
    }

    private final String buildPHID;
    private final String messageType;
    private UnitTestResults unitReports;
    private List<HarbormasterLintReport> lintReports;

    private HarbormasterBuildStatusMessage(){
        super(null);
        this.buildPHID = null;
        this.messageType = null;
    }

    public HarbormasterBuildStatusMessage(
            final String apiKey,
            final String buildPHID,
            final MessageType messageType,
            final UnitTestResults unitReports){
        super(apiKey);
        this.buildPHID = buildPHID;
        this.messageType = messageType.intoString();
        this.unitReports = unitReports;
    }

    public String getBuildPhid() { return this.buildPHID; }
    public String getMessageType() { return this.messageType; }

    public List<UnitTestResult> getUnitReports() {
        return this.unitReports != null ?  this.unitReports.getTestResults() : new ArrayList<>();
    }


    public class HarbormasterLintReport {
        //TODO
        private HarbormasterLintReport(){}
    }

}

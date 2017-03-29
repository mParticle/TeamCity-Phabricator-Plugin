package com.couchmate.teamcity.phabricator.conduit;

import com.couchmate.teamcity.phabricator.MessageType;
import com.couchmate.teamcity.phabricator.UnitTestResult;
import com.google.gson.annotations.SerializedName;

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

    private HarbormasterBuildStatusMessage(){
        super(null);
        this.buildPHID = null;
        this.messageType = null;
    }

    public HarbormasterBuildStatusMessage(
            final String apiKey,
            final String buildPHID,
            final MessageType messageType,
            final List<UnitTestResult> unitReports
    ){
        super(apiKey);
        this.buildPHID = buildPHID;
        this.messageType = messageType.intoString();
        this.unitReports = unitReports;
    }

    @SerializedName("buildTargetPHID")
    private final String buildPHID;
    @SerializedName("type")
    private final String messageType;
    @SerializedName("unit")
    private List<UnitTestResult> unitReports;
    @SerializedName("lint")
    private List<HarbormasterLintReport> lintReports;

    public String getBuildPhid() { return this.buildPHID; }
    public String getMessageType() { return this.messageType; }
    public List<UnitTestResult> getUnitReports() { return this.unitReports != null ?  this.unitReports : new ArrayList<>(); }


    public class HarbormasterLintReport {
        //TODO
        private HarbormasterLintReport(){}
    }

}

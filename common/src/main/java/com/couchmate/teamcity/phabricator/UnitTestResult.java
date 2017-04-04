package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.conduit.HarbormasterBuildStatusMessage;

/**
 * Created by mbenua on 3/24/2017.
 */
public class UnitTestResult {

    private final String testName;
    private final String resultType;
    private final String testNamespace;
    private final int testDuration;

    public UnitTestResult(
            final String testName,
            final MessageType resultType,
            final String testNamespace,
            final int testDuration
    ){
        this.testName = testName;
        this.resultType = resultType.intoString();
        this.testNamespace = testNamespace;
        this.testDuration = testDuration;
    }

    private String convertUnitType(HarbormasterBuildStatusMessage.HarbormasterUnitResultType resultType){
        switch (resultType){
            case PASS:
                return "pass";
            case FAIL:
                return "fail";
            case SKIP:
                return "skip";
            case BROKEN:
                return "broken";
            case UNSOUND:
                return "unsound";
            default:
                return null;
        }
    }

    public String getTestName() { return this.testName; }
    public String getTestResult() { return this.resultType; }
    public int getTestDuration() { return this.testDuration; }
}

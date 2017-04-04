package com.couchmate.teamcity.phabricator;

/**
 * Created by mbenua on 4/3/2017.
 */
import java.util.ArrayList;
import java.util.List;

public class UnitTestResults
{
    public enum TestRunStatus
    {
        PASS,
        FAIL,
        NOTRUN
    }

    private int testsFailed = 0;
    private List<UnitTestResult> results;

    public UnitTestResults()
    {
        this.results = new ArrayList<>();
    }

    public void addTest(String testName, String testClassName, int duration, MessageType status)
    {
        this.results.add(new UnitTestResult(testName, status, testClassName, duration));
        if (status == MessageType.FAIL)
        {
            testsFailed++;
        }
    }

    public int getTotalTestsCount()
    {
        return this.results.size();
    }

    public int getFailedTestsCount()
    {
        return this.testsFailed;
    }

    public TestRunStatus didRunPass()
    {
        if (this.results.size() > 0)
        {
            if (testsFailed == 0)
            {
                return TestRunStatus.PASS;
            }

            return TestRunStatus.FAIL;
        }

        return TestRunStatus.NOTRUN;
    }

    public List<UnitTestResult> getTestResults()
    {
        return this.results;
    }
}
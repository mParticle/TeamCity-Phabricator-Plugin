package com.couchmate.teamcity.phabricator.clients;

import com.couchmate.teamcity.phabricator.DifferentialReview;
import jetbrains.buildServer.log.Loggers;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;

/**
 * Created by mbenua on 3/28/2017.
 */
public final class ConduitClientTest extends TestCase {

    public ConduitClientTest(String name) {
        super(name);
    }

    @Test
    public void testPassing() {
        assertTrue(true);
    }

    @Test
    public void testGetDiffDetails() {

        ConduitClient client = new ConduitClient(
                "phabricator.corp.mparticle.com",
                "https",
                "123456", Loggers.AGENT);

        DifferentialReview review = new DifferentialReview(client);
        boolean result = review.fetchReviewData("15");
        assertTrue(result);
    }


}
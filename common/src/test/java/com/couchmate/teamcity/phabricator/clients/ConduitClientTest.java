package com.couchmate.teamcity.phabricator.clients;

import com.couchmate.teamcity.phabricator.DifferentialReview;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by mbenua on 3/28/2017.
 */
class ConduitClientTest {

    @Test
    void getDiffDetails() {

        ConduitClient client = new ConduitClient(
                "phabricator.corp.mparticle.com",
                "https",
                "123456", null);

        DifferentialReview review = new DifferentialReview(client);
        boolean result = review.fetchReviewData("15");
        assertTrue(result);
    }

}
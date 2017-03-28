package com.couchmate.teamcity.phabricator.clients;

import com.couchmate.teamcity.phabricator.*;
import com.couchmate.teamcity.phabricator.conduit.*;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;

public final class ConduitClient {
    private static Logger Log = Logger.getInstance(ConduitClient.class.getName());

    private final String conduitURL;
    private final String conduitScheme;
    private final String apiKey;
    private final PhabLogger logger;
    private final String CONDUIT_PATH = "/api";
    private Gson gson;

    private ConduitClient(){
        this.conduitURL = null;
        this.apiKey = null;
        this.logger = null;
        this.conduitScheme = null;
    }

    public ConduitClient(
            final String conduitURL,
            final String conduitScheme,
            final String apiKey,
            final PhabLogger logger
    ){
        this.conduitURL = conduitURL;
        this.conduitScheme = conduitScheme;
        this.apiKey = apiKey;
        this.logger = logger;
        this.gson = new Gson();
    }

    public Result ping() {
        final String PING_PATH = "/conduit.ping";

        try(CloseableHttpClient httpClient = this.createHttpClient()) {
            try(CloseableHttpResponse response =
                httpClient.execute(
                        new HttpRequestBuilder()
                                .get()
                                .setHost(conduitURL)
                                .setScheme(conduitScheme)
                                .setPath(CONDUIT_PATH + PING_PATH)
                                .setBody(gson.toJson(
                                        new MessageBase(this.apiKey)
                                ))
                                .build()
                )
            ){
                return handleResponse(response);
            }
        } catch ( TCPhabException | URISyntaxException | IOException e) {
            this.logger.warn("Ping error", e);
            return null;
        }
    }

    private CloseableHttpClient createHttpClient() {
        HttpClient client = new HttpClient(true);
        return client.getCloseableHttpClient();
    }

    public Result submitDifferentialComment(DifferentialCommentMessage message){
        final String DIFF_COMMENT_PATH = "/api/differential.createcomment";
        try(CloseableHttpClient httpClient = this.createHttpClient()){
            try(CloseableHttpResponse response =
                httpClient.execute(
                        new HttpRequestBuilder()
                            .post()
                            .setHost(this.conduitURL)
                            .setScheme(this.conduitScheme)
                            .setPath(DIFF_COMMENT_PATH)
                            .addFormParam(new StringKeyValue("api.token", this.apiKey))
                            .addFormParam(new StringKeyValue("message", message.getComment()))
                            .addFormParam(new StringKeyValue("revision_id", message.getRevisionId()))
                            .addFormParam(new StringKeyValue("silent", "true"))
                            .addFormParam(new StringKeyValue("action", "none"))
                            .build()
                        )
                ){
                return handleResponse(response);
            }
        } catch ( TCPhabException | URISyntaxException | IOException e) {
            this.logger.warn("createcomment error", e);
            return null;
        }

    }
    public Result getDiffDetails(String diffId){
        final String DIFF_QUERY_PATH = "/api/differential.querydiffs";
        try(CloseableHttpClient httpClient = this.createHttpClient()){
            try(CloseableHttpResponse response =
                        httpClient.execute(
                                new HttpRequestBuilder()
                                        .post()
                                        .setHost(this.conduitURL)
                                        .setScheme(this.conduitScheme)
                                        .setPath(DIFF_QUERY_PATH)
                                        .addFormParam(new StringKeyValue("api.token", this.apiKey))
                                        .addFormParam(new StringKeyValue("ids[0]", diffId))
                                        .build()
                        )
            ){
                return handleResponse(response);
            }
        } catch ( TCPhabException | URISyntaxException | IOException e) {
            this.logger.warn("getDiffDetails error", e);
            return null;
        }
    }

    public Result submitHarbormasterMessage(HarbormasterBuildStatusMessage message){
        final String HARBORMASTER_MESSAGE = "/api/harbormaster.sendmessage";

        try(CloseableHttpClient httpClient = this.createHttpClient()){
            HttpRequestBuilder builder = new HttpRequestBuilder()
                    .post()
                    .setHost(this.conduitURL)
                    .setScheme(this.conduitScheme)
                    .setPath(HARBORMASTER_MESSAGE)
                    .addFormParam(new StringKeyValue("api.token", this.apiKey))
                    .addFormParam(new StringKeyValue("type", message.getMessageType()))
                    .addFormParam(new StringKeyValue("buildTargetPHID", message.getBuildPhid()));

            // Append all the unit test results.
            List<UnitTestResult> reports = message.getUnitReports();
            for (int x = 0; x < reports.size(); x++)
            {
                builder.addFormParam(new StringKeyValue(String.format("unit[%s][name]", x), reports.get(x).getTestName()));
                builder.addFormParam(new StringKeyValue(String.format("unit[%s][result]", x), reports.get(x).getTestResult()));
            }

            try (CloseableHttpResponse response = httpClient.execute(builder.build()))
            {
                return handleResponse(response);
            }
        } catch ( TCPhabException | URISyntaxException | IOException e){
            this.logger.warn("harbormaster sendmessage", e);
            return null;
        }
    }

    public Result submitHarbormasterArtifact(HarbormasterUriArtifactMessage message)
    {
        final String HARBORMASTER_MESSAGE = "/api/harbormaster.createartifact";

        String encodedUrl;
        try {
            encodedUrl = URLEncoder.encode(message.getUrl(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedUrl = message.getUrl();
        }
        try(CloseableHttpClient httpClient = this.createHttpClient()){
            try(CloseableHttpResponse response =
                        httpClient.execute(
                                new HttpRequestBuilder()
                                        .post()
                                        .setHost(this.conduitURL)
                                        .setScheme(this.conduitScheme)
                                        .setPath(HARBORMASTER_MESSAGE)
                                        .addFormParam(new StringKeyValue("api.token", this.apiKey))
                                        .addFormParam(new StringKeyValue("buildTargetPHID", message.getBuildTargetPhid()))
                                        .addFormParam(new StringKeyValue("artifactType", "uri"))
                                        .addFormParam(new StringKeyValue("artifactData[uri]", encodedUrl))
                                        .addFormParam(new StringKeyValue("artifactData[name]", "TeamCity%20Results"))
                                        .addFormParam(new StringKeyValue("artifactData[ui.external]", "1"))
                                        .build()
                        )
            ){
                return handleResponse(response);
            }
        } catch ( TCPhabException | URISyntaxException | IOException e){
            Log.warn("Call to harbormaster.createartifact failed.", e);
            return null;
        }
    }

    private Result handleResponse(CloseableHttpResponse response) throws IOException{
        switch (response.getStatusLine().getStatusCode()){
            case 200:
            case 201:
                if(response.getEntity().getContentLength() > 0)
                    return gson.fromJson(IOUtils.toString(response.getEntity().getContent()), Result.class);
            default:
                return null;

        }
    }
}

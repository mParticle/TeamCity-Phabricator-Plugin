package com.couchmate.teamcity.phabricator.conduit;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public final class Result {

    private JsonElement result;

    @SerializedName("error_code")
    private String errorCode;

    @SerializedName("error_info")
    private String errorInfo;

    private Result(){}
    public Result(
            final JsonElement result,
            final String errorCode,
            final String errorInfo){
        this.errorCode = errorCode;
        this.errorInfo = errorInfo;
        this.result = result;
    }

    public String getErrorCode(){ return this.errorCode; }
    public String getErrorInfo(){ return this.errorInfo; }

    public JsonObject getJsonResult() {
        if (this.result == null)
        {
            return null;
        }

        return this.result.getAsJsonObject();
    }
}

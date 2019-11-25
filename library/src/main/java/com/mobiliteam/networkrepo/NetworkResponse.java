package com.mobiliteam.networkrepo;

/**
 * Created by admin on 13/04/18.
 */

public class NetworkResponse {

    public static final int DEFAULT_REQUEST_ID = -1;

    private int statusCode;
    private String errorMsg;
    private Object response;
    private boolean success;
    private int requestID = DEFAULT_REQUEST_ID;

    public NetworkResponse() {

    }

    public NetworkResponse(int requestID) {
        this.requestID = requestID;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRequestID() {
        return requestID;
    }

    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }
}

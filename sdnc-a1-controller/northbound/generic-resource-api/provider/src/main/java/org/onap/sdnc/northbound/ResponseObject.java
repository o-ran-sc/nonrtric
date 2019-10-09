package org.onap.sdnc.northbound;

public class ResponseObject {

    private static final String EMPTY_STRING = "";

    private String statusCode;
    private String message;

    public ResponseObject(String statusCode, String message) {
        this.statusCode = statusCode == null ? EMPTY_STRING : statusCode;
        this.message = message == null ? EMPTY_STRING : message;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

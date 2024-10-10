package org.example.dao.runTestCases.response;

public class TestResult {

    private String tcId;
    private String statusCode;
    private String tcResponse;

    public void setTcId(String tcId) {
        this.tcId = tcId;
    }

    public String getTcId() {
        return tcId;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setTcResponse(String tcResponse) {
        this.tcResponse = tcResponse;
    }

    public String getTcResponse() {
        return tcResponse;
    }
}

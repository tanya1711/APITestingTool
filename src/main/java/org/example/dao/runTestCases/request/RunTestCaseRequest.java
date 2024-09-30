package org.example.dao.runTestCases.request;

import java.util.List;

public class RunTestCaseRequest {

    private String curl;
    private List<TestCase> requestBodyList;

    public void setCurl(String curl) {
        this.curl = curl;
    }

    public String getCurl() {
        return curl;
    }

    public void setRequestBodyList(List<TestCase> requestBodyList) {
        this.requestBodyList = requestBodyList;
    }

    public List<TestCase> getRequestBodyList() {
        return requestBodyList;
    }
}

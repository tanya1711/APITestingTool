package org.example.dao.runTestCases.response;


import java.util.List;

public class RunTestCaseResponse {
    private List<TestResult> resultList;

    public void setResultList(List<TestResult> resultList) {
        this.resultList = resultList;
    }

    public List<TestResult> getResultList() {
        return resultList;
    }
}

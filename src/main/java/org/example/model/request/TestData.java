package org.example.model.request;

import java.util.List;

public class TestData {
    private String curl;
    private List<Object> testcases;

    // Default constructor
    public TestData() {}

    // Parameterized constructor
    public TestData(String curl, List<Object> testcases) {
        this.curl = curl;
        this.testcases = testcases;
    }

    // Getter and Setter for curl
    public String getCurl() {
        return curl;
    }

    public void setCurl(String curl) {
        this.curl = curl;
    }

    // Getter and Setter for testcases
    public List<Object> getTestcases() {
        return testcases;
    }

    public void setTestcases(List<Object> testcases) {
        this.testcases = testcases;
    }
}

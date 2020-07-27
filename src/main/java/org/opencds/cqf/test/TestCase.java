package org.opencds.cqf.test;

import org.hl7.fhir.MeasureReport;

public class TestCase {
    protected String id;
    public String getId() {
        return id;
    }
    public void setId(String value) {
        this.id = value;
    }

    protected MeasureReport expectedResult;
    public MeasureReport getExpectedResult() { return expectedResult; }
    public void setExpectedResult(MeasureReport value) {
        this.expectedResult = value;
    }

    protected String testResultMessage;
    public String getTestResultMessage() { return testResultMessage; }
    public void setTestResultMessage(String value) {
        this.testResultMessage = value;
    }
}
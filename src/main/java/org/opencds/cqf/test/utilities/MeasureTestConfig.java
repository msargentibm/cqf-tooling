package org.opencds.cqf.test.utilities;

import org.opencds.cqf.testcase.MeasureTestScript.Test.ExpectedResponse;

public class MeasureTestConfig {
	private String fhirVersion;
	private String pathToIG;
	private String periodStart;
	private String periodEnd;
	private String measurePath;
	private String patientPath;
	private ExpectedResponse expectedResponse;

	public String getPeriodStart() {
		return periodStart;
	}

	public void setPeriodStart(String periodStart) {
		this.periodStart = periodStart;
	}

	public String getPeriodEnd() {
		return periodEnd;
	}

	public void setPeriodEnd(String periodEnd) {
		this.periodEnd = periodEnd;
	}

	public String getPathToIG() {
		return pathToIG;
	}

	public void setPathToIG(String pathToIG) {
		this.pathToIG = pathToIG;
	}

	public String getFhirVersion() {
		return fhirVersion;
	}

	public void setFhirVersion(String fhirVersion) {
		this.fhirVersion = fhirVersion;
	}

	public String getMeasurePath() {
		return measurePath;
	}

	public void setMeasurePath(String measureId) {
		this.measurePath = measureId;
	}

	public String getPatientPath() {
		return patientPath;
	}

	public void setPatientPath(String patientId) {
		this.patientPath = patientId;
	}

	public void setExpectedResponse(ExpectedResponse expectedResponse) {
		this.expectedResponse = expectedResponse;
	}

	public ExpectedResponse getExpectedResponse() {
		return expectedResponse;
	}
}
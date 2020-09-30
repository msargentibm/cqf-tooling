package org.opencds.cqf.tooling.modelinfo.cdm;

import org.opencds.cqf.tooling.modelinfo.ModelInfoSettings;

public class CDMModelInfoSettings extends ModelInfoSettings {

    public CDMModelInfoSettings(String version) {
        super("CDM", version, "http://ibm.com/fhir/cdm", "PatientProfile", "birthDate", "cdm");
    }
}
package org.opencds.cqf.tooling.processor;

import org.fhir.ucum.UcumService;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.opencds.cqf.tooling.npm.NpmPackageManager;

public interface IProcessorContext {
    String getRootDir();

    ImplementationGuide getSourceIg();

    String getFhirVersion();

    String getPackageId();

    String getCanonicalBase();

    UcumService getUcumService();

    NpmPackageManager getPackageManager();
}

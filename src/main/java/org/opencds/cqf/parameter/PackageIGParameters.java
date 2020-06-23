package org.opencds.cqf.parameter;

import org.opencds.cqf.common.CqfmSoftwareSystem;
import org.opencds.cqf.processor.IGProcessor;

import java.util.ArrayList;
import java.util.List;

public class PackageIGParameters {
    public String igResourcePath;
    public String igPath;
    public IGProcessor.IGVersion igVersion;
    public org.opencds.cqf.utilities.IOUtils.Encoding outputEncoding;
    public Boolean includeELM;
    public Boolean includeDependencies;
    public Boolean includeTerminology;
    public Boolean includePatientScenarios;
    public Boolean versioned;
    public Boolean cdsHooksIg;
    public String fhirUri;
    public ArrayList<String> resourceDirs;
    public Boolean conformant;
    public List<CqfmSoftwareSystem> softwareSystems;
}

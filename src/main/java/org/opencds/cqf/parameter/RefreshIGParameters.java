package org.opencds.cqf.parameter;

import java.util.ArrayList;

import org.opencds.cqf.utilities.IGUtils;
import org.opencds.cqf.utilities.IOUtils.Encoding;

public class RefreshIGParameters {
    public String igResourcePath;
    public String igPath;
    public IGUtils.FHIRVersion FHIRVersion;
    public Encoding outputEncoding;
    public Boolean includeELM;
    public Boolean includeDependencies;
    public Boolean includeTerminology;
    public Boolean includePatientScenarios;
    public Boolean versioned;
    public Boolean cdsHooksIg;
    public String fhirUri;
    public ArrayList<String> resourceDirs;
    public Boolean conformant;
    public String measureToRefreshPath;
}
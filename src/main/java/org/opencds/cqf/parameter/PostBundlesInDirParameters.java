package org.opencds.cqf.parameter;

import org.opencds.cqf.utilities.IGUtils;
import org.opencds.cqf.utilities.IOUtils;

public class PostBundlesInDirParameters {  
    public String directoryPath;
    public String fhirUri;
    public IGUtils.FHIRVersion fhirVersion;
    public IOUtils.Encoding encoding;
}
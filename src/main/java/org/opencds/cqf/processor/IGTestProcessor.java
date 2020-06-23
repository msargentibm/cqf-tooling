package org.opencds.cqf.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.opencds.cqf.TestIGParameters;
import org.opencds.cqf.TestIGProcessor;
import org.opencds.cqf.parameter.RefreshIGParameters;
import org.opencds.cqf.utilities.IGUtils;
import org.opencds.cqf.utilities.IOUtils;
import org.opencds.cqf.utilities.LogUtils;
import org.opencds.cqf.utilities.ResourceUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class IGTestProcessor {
    public static void testIG(TestIGParameters params) {
        String igPath = params.igPath;
        String fhirUri = params.fhirUri;
        org.opencds.cqf.processor.IGProcessor.IGVersion igVersion = IGProcessor.IGVersion.parse(params.igVersion.toString());
        ArrayList<String> resourceDirs = params.resourceDirs;
        String scriptsPath = params.scriptsPath;

        IOUtils.resourceDirectories.addAll(resourceDirs);

        TestIGProcessor.testIg(params);
    }
}
package org.opencds.cqf.processor;

import java.util.List;

import org.hl7.fhir.Bundle;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.parameter.PostBundlesInDirParameters;
import org.opencds.cqf.utilities.*;

import ca.uhn.fhir.context.FhirContext;


public class PostBundlesInDirProcessor {

    public static void PostBundlesInDir(PostBundlesInDirParameters params, Boolean recursive) {
        FhirContext fhirContext = IGUtils.getFhirContext(params.fhirVersion);

        List<Bundle> bundles = BundleUtils.GetBundlesInDirectory(params.directoryPath, recursive, fhirContext);
        bundles.forEach(entry -> BundleUtils.postBundle(params.fhirUri, entry, params.encoding, fhirContext));
    }
}
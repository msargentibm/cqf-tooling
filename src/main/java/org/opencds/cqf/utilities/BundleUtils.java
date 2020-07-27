package org.opencds.cqf.utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.RuntimeResourceDefinition;
import org.hl7.fhir.Bundle;
import org.hl7.fhir.BundleEntry;
import org.hl7.fhir.MeasureReport;
import org.hl7.fhir.instance.model.api.IAnyResource;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.test.exception.InvalidTestCaseException;

public class BundleUtils {
    
    public static Object bundleArtifacts(String id, List<IAnyResource> resources, FhirContext fhirContext) {
        for (IAnyResource resource : resources) {
            if (resource.getId() == null || resource.getId().equals("")) {
                ResourceUtils.setIgId(id.replace("-bundle", "-" + UUID.randomUUID()), resource, false);
                resource.setId(resource.getClass().getSimpleName() + "/" + resource.getId());
            }
        }
        
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                return bundleStu3Artifacts(id, resources);
            case R4:
                return bundleR4Artifacts(id, resources);
            default:
                throw new IllegalArgumentException("Unknown fhir version: " + fhirContext.getVersion().getVersion().getFhirVersionString());
        }
    }

    public static org.hl7.fhir.dstu3.model.Bundle bundleStu3Artifacts(String id, List<IAnyResource> resources)
    {
        org.hl7.fhir.dstu3.model.Bundle bundle = new org.hl7.fhir.dstu3.model.Bundle();
        ResourceUtils.setIgId(id, bundle, false);
        bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.TRANSACTION);
        for (IAnyResource resource : resources)
        {
            bundle.addEntry(
            new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent()
                    .setResource((org.hl7.fhir.dstu3.model.Resource) resource)
                    .setRequest(
                            new org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent()
                                    .setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.PUT)
                                    .setUrl(((org.hl7.fhir.dstu3.model.Resource) resource).getId())
                    )
            );
        }
        return bundle;
    }

    public static org.hl7.fhir.r4.model.Bundle bundleR4Artifacts(String id, List<IAnyResource> resources)
    {
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        ResourceUtils.setIgId(id, bundle, false);
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION);
        for (IAnyResource resource : resources)
        {            
            String resourceRef = (resource.getIdElement().getResourceType() == null) ? resource.fhirType() + "/" + resource.getId() : resource.getId();
            bundle.addEntry(
                new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent()
                    .setResource((org.hl7.fhir.r4.model.Resource) resource)
                    .setRequest(
                        new org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent()
                            .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.PUT)
                            .setUrl(resourceRef)//shouldn't this be canonicalUrl?
                    )
            );
        }
        return bundle;
    }

    public static void postBundle(String fhirServerURL, Bundle bundle, IOUtils.Encoding encoding, FhirContext fhirContext) {
        if (fhirServerURL != null && !fhirServerURL.isEmpty()) {
            try {
                HttpClientUtils.post(fhirServerURL, (IAnyResource)bundle, encoding, fhirContext);
            } catch (IOException e) {
                LogUtils.putException(((IAnyResource)bundle).getId(), "Error posting to FHIR Server: " + fhirServerURL + ".  Bundle not posted.");
            }
        }
    }

    public static List<Bundle> GetBundlesInDirectory(String directoryPath, Boolean recursive, FhirContext fhirContext) {
        File dir = new File(directoryPath);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("path to directory must be an existing directory.");
        }

        List<String> filePaths = IOUtils.getFilePaths(directoryPath, recursive == null ? true : recursive).stream().filter(x -> !x.endsWith(".cql")).collect(Collectors.toList());
        List<IAnyResource> resources = IOUtils.readResources(filePaths, fhirContext);

        RuntimeResourceDefinition bundleDefinition = (RuntimeResourceDefinition)ResourceUtils.getResourceDefinition(fhirContext, "Bundle");
        String bundleClassName = bundleDefinition.getImplementingClass().getName();
        resources.removeIf(entry -> entry == null || !bundleClassName.equals(entry.getClass().getName()));
        List<Bundle> bundles = new ArrayList<Bundle>();
        resources.forEach(resource -> bundles.add((Bundle)resource));

        return bundles;
    }

    public static List<MeasureReport> GetMeasureReportsFromBundle(Bundle bundle) {
        List<MeasureReport> measureReports = new ArrayList<MeasureReport>();

        List<BundleEntry> entries = bundle.getEntry();
        entries.stream()
                .filter(entry -> entry != null && entry.getResource() != null && entry.getResource().getMeasureReport() != null)
                .forEach(entry -> measureReports.add(entry.getResource().getMeasureReport()));

        return measureReports;
    }
}

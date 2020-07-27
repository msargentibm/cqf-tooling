package org.opencds.cqf.test.evaluate;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntyped;
import org.opencds.cqf.test.Context;
import org.opencds.cqf.testcase.GitHubResourceItems;
import org.opencds.cqf.testcase.GroupItems;
import org.opencds.cqf.testcase.MeasureTestScript;
import org.opencds.cqf.testcase.ResourceItems;
import org.opencds.cqf.utilities.IOUtils;
import org.opencds.cqf.test.utilities.ResourceLoadingUtils;

import java.util.HashSet;

public class MeasureTestScriptEvaluator extends MeasureTestScript {

    @Override
    public Object run(Context context) {

            if (isEnabled()) {
                if (!IOUtils.resourceDirectories.contains(getPathToIG())) {
                    IOUtils.clearPathsCache();
                    IOUtils.setIgPath(getPathToIG());
                    IOUtils.resourceDirectories.add(getPathToIG());
                }
                try {
                    loadArtifacts(context);
                }
                catch (Exception e) {
                  System.out.println(e.getMessage());
                }
                for (Test test : getTest()) {
                    try {
                        loadPatientData(context, test);
                    }
                    catch (Exception e) {
                  System.out.println(e.getMessage());
                }
                    try {
                        IOperationUntyped operation =
                            context.getClient(getFhirVersion())
                                    .operation()
                                    .onInstance(new IdDt("Measure", test.getMeasureId()))
                                    .named("$evaluate-measure");

                    if (context.isStu3(getFhirVersion())) {
                        org.hl7.fhir.dstu3.model.Parameters result =
                                operation
                                        .withParameters(getStu3MeasureEvaluationParameters(
                                                test.getPatientId(),
                                                test.getPeriodStart(),
                                                test.getPeriodEnd(),
                                                test.getReportType(),
                                                test.getPractitioner())
                                        )
                                        .useHttpGet()
                                        .execute();
                        testExpectedStu3Response(result, test);
                    }
                    else if (context.isR4(getFhirVersion())) {
                        org.hl7.fhir.r4.model.Parameters result =
                                operation
                                        .withParameters(getR4MeasureEvaluationParameters(
                                                test.getPatientId(),
                                                test.getPeriodStart(),
                                                test.getPeriodEnd(),
                                                test.getReportType(),
                                                test.getPractitioner())
                                        )
                                        .useHttpGet()
                                        .execute();
                        testExpectedR4Response(result, test);
                    }
                    } catch (Exception e) {
                        System.out.println("Error Processing: " + test.getMeasureId() + ", " + test.getPatientId());
                        System.out.println(e.getMessage());
                    }
            }
        }
        return null;
    }

    private org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent getStu3GroupById(org.hl7.fhir.dstu3.model.MeasureReport report, String id)
    {
        if (report.hasGroup()) {
            for (org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
                if (group.hasIdentifier() && group.getIdentifier().hasValue()) {
                    if (group.getIdentifier().getValue().equals(id)) {
                        return group;
                    }
                }
            }
        }

        return null;
    }

    private org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent getStu3PopulationByCode(org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent group, String codeToMatch) {
        if (group.hasPopulation()) {
            for (org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                if (population.hasCode()) {
                    if (population.getCode().hasCoding()) {
                        for (org.hl7.fhir.dstu3.model.Coding coding : population.getCode().getCoding()) {
                            if (coding.hasCode() && coding.getCode().equals(codeToMatch)) {
                                return population;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private void testExpectedStu3Response(org.hl7.fhir.dstu3.model.Parameters response, Test test) {
        if (response == null) {
            throw new RuntimeException("Null response for test: " + test.getId());
        }
        if (!response.getParameterFirstRep().hasResource()) {
            throw new RuntimeException("Unexpected result found for test " + test.getId() + " Expected a MeasureReport");
        }
        if (!(response.getParameterFirstRep().getResource() instanceof org.hl7.fhir.dstu3.model.MeasureReport)) {
            throw new RuntimeException("Unexpected result found for test " + test.getId() + " Expected a MeasureReport");
        }
        for (GroupItems items : test.getExpectedResponse().getGroup()) {
            org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent group
                    = getStu3GroupById((org.hl7.fhir.dstu3.model.MeasureReport) response.getParameterFirstRep().getResource(), items.getId());
            if (group == null) continue;
            if (items.getId() == null) continue;

            if (items.getInitialPopulation() != null) {
                org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent population =
                        getStu3PopulationByCode(group, "initial-population");
                if (population != null && population.hasCount()) {
                    if (population.getCount() != items.getInitialPopulation().intValue()) {
                        output(test.getId(), "Initial Population", population.getCount(), items.getInitialPopulation().intValue());
                        return;
                    }
                }
            }

            if (items.getDenominator() != null) {
                org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent population =
                        getStu3PopulationByCode(group, "denominator");
                if (population != null && population.hasCount()) {
                    if (population.getCount() != items.getDenominator().intValue()) {
                        output(test.getId(), "Denominator", population.getCount(), items.getDenominator().intValue());
                        return;
                    }
                }
            }

            if (items.getNumerator() != null) {
                org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent population =
                        getStu3PopulationByCode(group, "numerator");
                if (population != null && population.hasCount()) {
                    if (population.getCount() != items.getNumerator().intValue()) {
                        output(test.getId(), "Numerator", items.getNumerator().intValue(), population.getCount());
                        return;
                    }
                }
            }

            if (items.getDenominatorExclusion() != null) {
                org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent population =
                        getStu3PopulationByCode(group, "denominator-exclusion");
                if (population != null && population.hasCount()) {
                    if (population.getCount() != items.getDenominatorExclusion().intValue()) {
                        output(test.getId(), "Denominator Exclusion", population.getCount(), items.getDenominatorExclusion().intValue());
                        return;
                    }
                }
            }

            if (items.getMeasureScore() != null) {
                if (group.hasMeasureScore()) {
                    if (!items.getMeasureScore().equals(group.getMeasureScore())) {
                        output(test.getId(), "Measure Score", items.getMeasureScore(), group.getMeasureScore());
                        return;
                    }
                }
            }
        }

        System.out.println("Test " + test.getId() + " Passed");
        // TODO - aggregate output of tests passed and failed
    }

    private org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent getR4GroupById(org.hl7.fhir.r4.model.MeasureReport report, String id)
    {
        if (report.hasGroup()) {
            for (org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
                if (group.hasId()) {
                   if(group.getId().equals(id)) {
                       return group;
                   }
                }
                else if (group.hasCode() && group.getCode().hasCoding()) {
                    for (org.hl7.fhir.r4.model.Coding coding : group.getCode().getCoding()) {
                        if (coding.getCode().equals(id)) {
                            return group;
                        }
                    }
                }
            }
        }

        return null;
    }

    private org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent getR4PopulationByCode(org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent group, String codeToMatch) {
        if (group.hasPopulation()) {
            for (org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                if (population.hasCode()) {
                    if (population.getCode().hasCoding()) {
                        for (org.hl7.fhir.r4.model.Coding coding : population.getCode().getCoding()) {
                            if (coding.hasCode() && coding.getCode().equals(codeToMatch)) {
                                return population;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private void testExpectedR4Response(org.hl7.fhir.r4.model.Parameters response, Test test) {
        if (response == null) {
            throw new RuntimeException("Null response for test: " + test.getId());
        }
        if (!response.getParameterFirstRep().hasResource()) {
            throw new RuntimeException("Unexpected result found for test " + test.getId() + " Expected a MeasureReport");
        }
        if (!(response.getParameterFirstRep().getResource() instanceof org.hl7.fhir.r4.model.MeasureReport)) {
            throw new RuntimeException("Unexpected result found for test " + test.getId() + " Expected a MeasureReport");
        }
        for (GroupItems items : test.getExpectedResponse().getGroup()) {
            org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent group
                    = getR4GroupById((org.hl7.fhir.r4.model.MeasureReport) response.getParameterFirstRep().getResource(), items.getId());
            if (group == null) continue;
            if (items.getId() == null) continue;

            if (items.getInitialPopulation() != null) {
                org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent population =
                        getR4PopulationByCode(group, "initial-population");
                if (population != null && population.hasCount()) {
                    if (population.getCount() != items.getInitialPopulation().intValue()) {
                        output(test.getId(), "Initial Population", population.getCount(), items.getInitialPopulation().intValue());
                        return;
                    }
                }
            }

            if (items.getDenominator() != null) {
                org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent population =
                        getR4PopulationByCode(group, "denominator");
                if (population != null && population.hasCount()) {
                    if (population.getCount() != items.getDenominator().intValue()) {
                        output(test.getId(), "Denominator", population.getCount(), items.getDenominator().intValue());
                        return;
                    }
                }
            }

            if (items.getNumerator() != null) {
                org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent population =
                        getR4PopulationByCode(group, "numerator");
                if (population != null && population.hasCount()) {
                    if (population.getCount() != items.getNumerator().intValue()) {
                        output(test.getId(), "Numerator", population.getCount(), items.getNumerator().intValue());
                        return;
                    }
                }
            }

            if (items.getDenominatorExclusion() != null) {
                org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent population =
                        getR4PopulationByCode(group, "denominator-exclusion");
                if (population != null && population.hasCount()) {
                    if (population.getCount() != items.getDenominatorExclusion().intValue()) {
                        output(test.getId(), "Denominator Exclusion", population.getCount(), items.getDenominatorExclusion().intValue());
                        return;
                    }
                }
            }

            if (items.getMeasureScore() != null) {
                if (group.hasMeasureScore() && group.getMeasureScore().hasValue()) {
                    if (!items.getMeasureScore().equals(group.getMeasureScore().getValue())) {
                        output(test.getId(), "Measure Score", items.getMeasureScore(), group.getMeasureScore());
                        return;
                    }
                }
            }
        }

        System.out.println("Test " + test.getId() + " Passed");
        // TODO - aggregate output of tests passed and failed
    }

    private org.hl7.fhir.dstu3.model.Parameters getStu3MeasureEvaluationParameters(
            String patientId, String periodStart, String periodEnd, String reportType, String practitioner
    ) {
        org.hl7.fhir.dstu3.model.Parameters params = new org.hl7.fhir.dstu3.model.Parameters();
        params.addParameter(
                new org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent().setName("patient").setValue(new org.hl7.fhir.dstu3.model.StringType(patientId))
        )
                .addParameter(
                        new org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent().setName("periodStart").setValue(new org.hl7.fhir.dstu3.model.StringType(periodStart))
                )
                .addParameter(
                        new org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent().setName("periodEnd").setValue(new org.hl7.fhir.dstu3.model.StringType(periodEnd))
                )
                .addParameter(
                        new org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent().setName("reportType").setValue(new org.hl7.fhir.dstu3.model.StringType(reportType))
                )
                .addParameter(
                        new org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent().setName("practitioner").setValue(new org.hl7.fhir.dstu3.model.StringType(practitioner))
                );

        return params;
    }

    private org.hl7.fhir.r4.model.Parameters getR4MeasureEvaluationParameters(
            String patientId, String periodStart, String periodEnd, String reportType, String practitioner
    ) {
        org.hl7.fhir.r4.model.Parameters params = new org.hl7.fhir.r4.model.Parameters();
        params.addParameter(
                new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent().setName("patient").setValue(new org.hl7.fhir.r4.model.StringType(patientId))
        )
                .addParameter(
                        new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent().setName("periodStart").setValue(new org.hl7.fhir.r4.model.StringType(periodStart))
                )
                .addParameter(
                        new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent().setName("periodEnd").setValue(new org.hl7.fhir.r4.model.StringType(periodEnd))
                )
                .addParameter(
                        new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent().setName("reportType").setValue(new org.hl7.fhir.r4.model.StringType(reportType))
                )
                .addParameter(
                        new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent().setName("practitioner").setValue(new org.hl7.fhir.r4.model.StringType(practitioner))
                );

        return params;
    }

    private void loadArtifacts(Context context) {
        IGenericClient client = context.getClient(getFhirVersion());
        HashSet<String> bundlePaths = IOUtils.getBundlePaths(context.getFhirContext(getFhirVersion()));
        if(getLoadArtifacts() != null) {
            for (String artifact : getLoadArtifacts().getBundle()) {
                ResourceLoadingUtils.loadBundleAt(artifact, client);
            }
            for (GitHubResourceItems artifact : getLoadArtifacts().getGitHubResource()) {
                ResourceLoadingUtils.readAndLoadFromGitHub(artifact, context.getRepository(artifact.getRepositoryName()), client);
            }
            for (ResourceItems artifact : getLoadArtifacts().getResource()) {
                ResourceLoadingUtils.loadResourceAt(artifact.getPath(), client);
                context.getFhirContext(getFhirVersion());
            }
        }
        else if (!bundlePaths.isEmpty()) {
            for (String artifactPath : bundlePaths)
            {
                try {
                    if (artifactPath.replaceAll("_", "-").contains(getMeasureId().replaceAll("measure-", ""))) {
                        ResourceLoadingUtils.loadBundleAt(artifactPath, client);
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        else {
            for (String measurePath : IOUtils.getMeasurePaths(context.getFhirContext(getFhirVersion()))) {
                ResourceLoadingUtils.loadResourceAt(measurePath, client);
            }
            for (String libraryPath : IOUtils.getLibraryPaths(context.getFhirContext(getFhirVersion()))) {
                ResourceLoadingUtils.loadResourceAt(libraryPath, client);
            }
            for (String terminologyPath : IOUtils.getTerminologyPaths(context.getFhirContext(getFhirVersion()))) {
                ResourceLoadingUtils.loadResourceAt(terminologyPath, client);
            }
        }


    }

    private void loadPatientData(Context context, Test test) {
        IGenericClient client = context.getClient(getFhirVersion());
        for (String artifactPath : IOUtils.getPatientPaths(context.getFhirContext(getFhirVersion())))
        {
            ResourceLoadingUtils.loadResourceAt(artifactPath, client);
        }
        if(test.getLoadPatientData() != null) {
            for (String artifact : test.getLoadPatientData().getBundle()) {
                ResourceLoadingUtils.loadBundleAt(artifact, client);
            }
            for (GitHubResourceItems artifact : test.getLoadPatientData().getGitHubResource()) {
                ResourceLoadingUtils.readAndLoadFromGitHub(artifact, context.getRepository(artifact.getRepositoryName()), client);
            }
            for (ResourceItems artifact : test.getLoadPatientData().getResource()) {
                ResourceLoadingUtils.loadResourceAt(artifact.getPath(), client);
            }
        }
    }

    private void output(String id, String population, Object expectedCount, Object foundCount) {
        System.out.println(String.format("Test %s Failed. Expected %s count to be %s found %s", id, population, expectedCount, foundCount));
    }

}

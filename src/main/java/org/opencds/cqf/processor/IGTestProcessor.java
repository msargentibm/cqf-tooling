package org.opencds.cqf.processor;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.Bundle;
import org.hl7.fhir.MeasureReport;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.opencds.cqf.parameter.PostBundlesInDirParameters;
import org.opencds.cqf.parameter.TestIGParameters;
import org.opencds.cqf.test.Context;
import org.opencds.cqf.test.TestCase;
import org.opencds.cqf.test.exception.InvalidTestCaseException;
import org.opencds.cqf.testcase.ResourceItems;
import org.opencds.cqf.utilities.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.context.FhirContext;

import java.io.File;
import java.io.IOException;


public class IGTestProcessor {
    public static final String testCasesPath = "input/tests/";

    public static void testIg(TestIGParameters params) {
        String igPath = params.igPath;
        String testCasesPath = params.testCasesPath;
        IGUtils.FHIRVersion FHIRVersion = params.FHIRVersion;
        String fhirServerUri = params.fhirServerUri;

        try
        {
            igPath = Paths.get(igPath).toFile().getCanonicalPath();
        }
        catch(IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        IOUtils.setIgPath(igPath);
        FhirContext fhirContext = (FHIRVersion == null) ? IGUtils.getFhirContextFromIni(igPath) : IGUtils.getFhirContext(FHIRVersion);

        File scriptDirectory = new File(testCasesPath);
        if (!scriptDirectory.isDirectory()) {
            throw new RuntimeException("The path to the test scripts must point to a directory");
        }

        // 1. refresh/generate test bundles
        System.out.println("refreshing test cases...");
        TestCaseProcessor.refreshTestCases(testCasesPath, IOUtils.Encoding.JSON, fhirContext);

        // run the tests
        String testBundlesDirectoryPath = FilenameUtils.concat(igPath, IGTestProcessor.testCasesPath);
        RunTestCases(testBundlesDirectoryPath, fhirServerUri, fhirContext);
    }

    private static void RunTestCases(String testBundlesDirectoryPath, String fhirServerUri, FhirContext fhirContext) {
        // Now foreach test case/bundle:
        // 1. Identify and index each test case
        List<Bundle> testCaseBundles = BundleUtils.GetBundlesInDirectory(testBundlesDirectoryPath, false, fhirContext);
        // Maybe ought to use a Map<String, TestCase> instead to ensure unique test case ids?
        List<TestCase> tests = new ArrayList<TestCase>();
        for (Bundle testCase : testCaseBundles) {
            TestCase test = new TestCase();
            test.setId(testCase.getId().toString());

            try {
                MeasureReport expectedResult = GetExpectedResultMeasureReport(testCase);
                test.setExpectedResult(expectedResult);
            } catch (InvalidTestCaseException exception) {
                test.setTestResultMessage(exception.getMessage());
            }

            // 2. PostBundles
            BundleUtils.postBundle(fhirServerUri, testCase, IOUtils.Encoding.JSON, fhirContext);
        }





//        PostTestBundles(fhirServerUri, testBundlesDirectoryPath, IOUtils.Encoding.JSON, fhirContext);

        //PROBLEM: I need each bundle here so that I can extract and preserve the MeasureReports.

//        Context context = new Context();
//        IGenericClient client = context.getClient(fhirContext.getVersion().getVersion().getFhirVersionString());


//        HashSet<String> bundlePaths = IOUtils.getBundlePaths(fhirContext);
//        if(getLoadArtifacts() != null) {
//            for (String artifact : getLoadArtifacts().getBundle()) {
//                ResourceLoadingUtils.loadBundleAt(artifact, client);
//            }
//        }

        // 3.
       /*
            1. Generate Test bundles according to the spec (I believe the cqf-tooling already does this as part of RefreshIG)
                1a. The test Bundle includes a MeasureReport which represents the Expected Results of an evaluation.
            2. Load that Bundle of test data into the ruler
            3. Evaluate the Measure and Patient in that Bundle
            4. Compare the Actual Result of the evaluation with the Expected Result that's in the Bundle
        */
//        List<MeasureTestConfig> configs = ScriptUtils.setupMeasureTestConfigs(igPath, fhirContext);
//        System.out.println("Building Scripts...");
//        ScriptUtils.buildScripts(configs, scriptsPath, fhirContext);
//
//        for (TestScript script : ScriptUtils.getScripts(scriptDirectory)) {
//            script.run(context);
//        }
    }
//    private static void PostTestBundles(String fhirServerUri, String testBundlesDirectoryPath, IOUtils.Encoding encoding, FhirContext fhirContext) {
//        List<Bundle> bundles = new ArrayList<Bundle>();
//
//        PostBundlesInDirProcessor postBundlesProcessor = new PostBundlesInDirProcessor();
//        PostBundlesInDirParameters postBundlesParams = new PostBundlesInDirParameters();
//        postBundlesParams.directoryPath = testBundlesDirectoryPath;
//        postBundlesParams.encoding = IOUtils.Encoding.JSON;
//        postBundlesParams.fhirUri = fhirContext
//
////        List<String> testCasePaths = IOUtils.getFilePaths(testCasesDirectoryPath, false);
////
////        for (String filePath : testCasePaths) {
////            IAnyResource bundle = IOUtils.readResource(filePath, fhirContext, true);
////            bundle.getClass()
////        }
//    }

    private static MeasureReport GetExpectedResultMeasureReport(Bundle bundle) throws InvalidTestCaseException {
        List<MeasureReport> measureReports = BundleUtils.GetMeasureReportsFromBundle(bundle);

        if (measureReports == null || measureReports.size() == 0) {
            throw new InvalidTestCaseException(
                String.format(
                    "Bundle '%s' does not contain a MeasureReport resource specifying the expected result.",
                    bundle.getId().toString()
                )
            );
        } else if (measureReports.size() > 1) {
            throw new InvalidTestCaseException(
                String.format(
                    "Bundle '%s' contains more than one entry with a MeasureReport resource - the expected results are ambiguous. Test case bundles should contain only one MeasureReport.",
                    bundle.getId().toString()
                )
            );
        }

        MeasureReport measureReport = measureReports.get(0);
        return measureReport;
    }
}
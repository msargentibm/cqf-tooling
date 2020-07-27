//package org.opencds.cqf.test.evaluate;
//
//import ca.uhn.fhir.rest.client.api.IGenericClient;
//import com.google.gson.JsonObject;
//import org.opencds.cqf.test.Context;
//import org.opencds.cqf.testcase.CDSHooksTestScript;
//import org.opencds.cqf.testcase.GitHubResourceItems;
//import org.opencds.cqf.testcase.ResourceItems;
//import org.opencds.cqf.test.utilities.GithubUtils;
//import org.opencds.cqf.utilities.HttpClientUtils;
//import org.opencds.cqf.test.utilities.JsonUtils;
//import org.opencds.cqf.test.utilities.ResourceLoadingUtils;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
//
//public class CDSHooksTestScriptEvaluator extends CDSHooksTestScript {
//
//    @Override
//    public Object run(Context context) {
//        //TODO: gitHubResource fails with files > 1MB
//        //For now, use bundle or resource for files > 1MB
//        if (isEnabled()) {
//            loadArtifacts(context);
//            Map<String, Boolean> results = new HashMap<String, Boolean>();
//            String testId = "";
//            for (Test test : getTest()) {
//                JsonObject request;
//                testId = test.getId();
//                if (test.getRequestJsonSource().getResource() != null) {
//                    String path = test.getRequestJsonSource().getResource().getPath();
//                    request = JsonUtils.getJsonObjectFromString(
//                            ResourceLoadingUtils.getFileAsString(path));
//                } else if (test.getRequestJsonSource().getGitHubResource() != null) {
//                    GitHubResourceItems item = test.getRequestJsonSource().getGitHubResource();
//                    InputStream content = GithubUtils.readContentFromGitHub(item,
//                            context.getRepository(item.getRepositoryName()));
//                    request = JsonUtils.getJsonObjectFromInputStream(content);
//                } else {
//                    throw new RuntimeException(
//                            "Test " + testId + " must specify where the request json source is located");
//                }
//
//                JsonObject expectedResponse;
//                if (test.getExpectedResponseJsonSource().getResource() != null) {
//                    String path = test.getExpectedResponseJsonSource().getResource().getPath();
//                    expectedResponse = JsonUtils.getJsonObjectFromString(
//                            ResourceLoadingUtils.getFileAsString(path));
//                } else if (test.getExpectedResponseJsonSource().getGitHubResource() != null) {
//                    GitHubResourceItems item = test.getExpectedResponseJsonSource().getGitHubResource();
//                    InputStream content = GithubUtils.readContentFromGitHub(item,
//                            context.getRepository(item.getRepositoryName()));
//                            expectedResponse = JsonUtils.getJsonObjectFromInputStream(content);
//                } else {
//                    throw new RuntimeException(
//                            "Test " + testId + " must specify where the expected response json source is located");
//                }
//
//                String requestUrl = context.getCdsDiscoveryEndpoint(getFhirVersion());
//
//                if (test.getCdsServiceId() == null) {
//                    throw new RuntimeException("Test " + testId + " must specify the cds service id");
//                }
//
//                requestUrl += "/" + test.getCdsServiceId();
//
//                String response = "";
//                try {
//                    response = HttpClientUtils.post(requestUrl, request.toString());
//                } catch (IOException e) {
//                    throw new RuntimeException(
//                        "Test " + testId + " unable to post to fhir server: " + requestUrl + "\r\n" + e.getMessage());
//                }
//
//                if (response == null || response.equals("")) {
//                    throw new RuntimeException(
//                        "Test " + testId + " unable to post to fhir server: " + requestUrl + ". Empty response.");
//                }
//
//                Boolean result = testExpectedResponse(testId, expectedResponse.toString(), response);
//                results.put(testId, result);
//            }
//            //TODO: REALLY need parent context printed to output here, but there's nothing with that context in scope atm.
//            String resultsMessage = "\r\nCDS Hooks test:\r\n" + results.values().stream().filter(value -> value == true).count() + " PASSED\r\n";
//            resultsMessage += results.values().stream().filter(value -> value ==false).count() + " FAILED\r\n";
//            System.out.println(resultsMessage);
//        }
//        return null;
//    }
//
//    private Boolean testExpectedResponse(String testId, String expected, String actual) {
//        expected = normalize(expected);
//        actual = normalize(actual);
//        if (expected == null || expected.equals("") || !expected.equals(normalize(actual))) {
//            System.out.print("\r\nTest " + testId + " - Failed.\r\nExpected:\r\n" + expected + "\r\nFound:\r\n" + actual + "\r\n");
//            return false;
//        }
//
//        System.out.print("\r\nTest " + testId + " - Passed.\r\nFound:\r\n" + actual + "\r\n");
//        return true;
//    }
//
//    private static String normalize(String input) {
//        String result = input.replaceAll(" ", "").toLowerCase();
//
//        return result;
//    }
//
//    private void loadArtifacts(Context context) {
//        IGenericClient client = context.getClient(getFhirVersion());
//        //TODO: hack until we figure out what to do about the history sync issue
//        try {
//            for (String artifact : getLoadArtifacts().getBundle()) {
//                ResourceLoadingUtils.loadBundleAt(artifact, client);
//            }
//            for (GitHubResourceItems artifact : getLoadArtifacts().getGitHubResource()) {
//                ResourceLoadingUtils.readAndLoadFromGitHub(artifact, context.getRepository(artifact.getRepositoryName()), client);
//            }
//            for (ResourceItems artifact : getLoadArtifacts().getResource()) {
//                ResourceLoadingUtils.loadResourceAt(artifact.getPath(), client);
//            }
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//    }
//}

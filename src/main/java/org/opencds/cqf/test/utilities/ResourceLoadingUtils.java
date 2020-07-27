package org.opencds.cqf.test.utilities;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.kohsuke.github.GHRepository;
import org.opencds.cqf.testcase.GitHubResourceItems;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

public class ResourceLoadingUtils {

    public static void loadBundleAt(String pathToBundle, IGenericClient client) {
        String bundle = getFileAsString(pathToBundle);
        performTransaction(bundle, client);
    }

    public static void loadResourceAt(String pathToResource, IGenericClient client) {
        String resourceString = getFileAsString(pathToResource);
        performUpdate(resourceString, client);
    }

    public static String getFileAsString(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error parsing file " + path + " to string: " + e.getMessage());
        }
    }

    public static void performTransaction(String bundle, IGenericClient client) {
        client.transaction().withBundle(bundle).execute();
    }

    public static void performUpdate(String resourceString, IGenericClient client) {
        client.update().resource(resourceString).execute();
    }

//    private static HashSet<String> cachedResources = new HashSet<String>();
//    public static void readAndLoadFromGitHub(GitHubResourceItems artifact, GHRepository repo, IGenericClient client)
//    {
//        if (artifact.getPath() == null) {
//            throw new RuntimeException("Path to the resource within the GitHub repository is required");
//        }
//
//        if (cachedResources.contains(artifact.getPath())) {
//            return;
//        }
//
//        InputStream content = null;
//        try {
//            content = GithubUtils.readContentFromGitHub(artifact, repo);
//
//            IParser parser = artifact.getPath().endsWith("json") ? client.getFhirContext().newJsonParser() : client.getFhirContext().newXmlParser();
//
//            IBaseResource resource = parser.parseResource(content);
//            String contentString = parser.setPrettyPrint(true).encodeResourceToString(resource);
//            if (resource instanceof IBaseBundle) {
//                performTransaction(contentString, client);
//            } else {
//                performUpdate(contentString, client);
//            }
//            cachedResources.add(artifact.getPath());
//        } catch (Exception e) {
//            e.printStackTrace();
//            String errorMessage = "Error loading artifact: " + artifact.getPath() + "\r\n";
//            if (e instanceof org.kohsuke.github.HttpException ) {
//                org.kohsuke.github.HttpException ghe = (org.kohsuke.github.HttpException)e;
//                if (ghe.getMessage().indexOf("This API returns blobs up to 1 MB in size") > -1) {
//                    errorMessage += " use bundle or resource type to load artifact for files greater than 1 MB in size:\r\n";
//                }
//            }
//            throw new RuntimeException(errorMessage + e.getMessage());
//        }
//        finally {
//            try {
//                content.close();
//                content = null;
//            } catch (IOException e) {
//                     e.printStackTrace();
//            }
//        }
//    }

}

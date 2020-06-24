package org.opencds.cqf.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.opencds.cqf.test.utilities.PropertyUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Context {

    private FhirContext stu3Context;
    private FhirContext r4Context;

    private IGenericClient stu3Client;
    private IGenericClient r4Client;

    private GitHub gitHub;
    private Map<String, GHRepository> repositories;

    private String cdsDiscoveryStu3Endpoint;
    private String cdsDiscoveryR4Endpoint;

    Context() {
        this.stu3Context = FhirContext.forDstu3();
        this.r4Context = FhirContext.forR4();
        this.stu3Client = stu3Context.newRestfulGenericClient(PropertyUtils.getStu3Endpoint());
        this.r4Client = r4Context.newRestfulGenericClient(PropertyUtils.getR4Endpoint());
        try {
            this.gitHub = GitHub.connectAnonymously();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error connecting to GitHub: " + e.getMessage());
        }
        this.repositories = new HashMap<>();
        this.cdsDiscoveryStu3Endpoint = PropertyUtils.getStu3CdsDiscovery();
        this.cdsDiscoveryR4Endpoint = PropertyUtils.getR4CdsDiscovery();
    }

    public boolean isStu3(String versionString) {
        return FhirVersionEnum.valueOf(versionString) == FhirVersionEnum.DSTU3;
    }

    public boolean isR4(String versionString) {
        return FhirVersionEnum.valueOf(versionString) == FhirVersionEnum.R4;
    }

    public IGenericClient getClient(String versionString) {
        if (isStu3(versionString)) {
            return getStu3Client();
        }
        else if (isR4(versionString)) {
            return getR4Client();
        }
        else {
            throw new RuntimeException("Unsupported version: " + versionString);
        }
    }

    public FhirContext getFhirContext(String versionString) {
        if (isStu3(versionString)) {
            return getStu3Context();
        }
        else if (isR4(versionString)) {
            return getR4Context();
        }
        else {
            throw new RuntimeException("Unsupported version: " + versionString);
        }
    }

    public GHRepository getRepository(String name) {
        if (repositories.containsKey(name)) {
            return repositories.get(name);
        }
      
        try {            
            //TODO: this is where it locks
            GHRepository repo = gitHub.getRepository(name);
            repositories.put(name, repo);
             return repo;    
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error retrieving from GitHub: " + e.getMessage());
        }

    }

    public String getCdsDiscoveryEndpoint(String versionString) {
        if (isStu3(versionString)) {
            return getCdsDiscoveryStu3Endpoint();
        }
        else if (isR4(versionString)) {
            return getCdsDiscoveryR4Endpoint();
        }
        else {
            throw new RuntimeException("Unsupported version: " + versionString);
        }
    }

}

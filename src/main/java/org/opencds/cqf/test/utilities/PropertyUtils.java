package org.opencds.cqf.test.utilities;

import ca.uhn.fhir.context.ConfigurationException;
import org.apache.commons.lang3.ArrayUtils;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class PropertyUtils {

    public static void loadProperties(String[] args) {
        loadDefaultProperties();
        for (String arg : args) {
            String[] nameValuePair = arg.split("=");
            if (ArrayUtils.getLength(nameValuePair) < 2) {
                continue;
            }

            // TODO - ensure only applicable properties are set
            System.setProperty(nameValuePair[0].replaceFirst("-", ""), nameValuePair[1]);
        }
    }

    private static void loadDefaultProperties() {
        Properties properties = System.getProperties();
        try (InputStream in = PropertyUtils.class.getResourceAsStream("test.properties")) {
            properties.load(Objects.requireNonNull(in));
            System.setProperties(properties);
        } catch (Exception e) {
            throw new ConfigurationException("Could not load test properties", e);
        }
    }

    public static String getStu3Endpoint() {
        String stu3Endpoint = System.getProperty("stu3.url");
        if (stu3Endpoint == null) {
            throw new RuntimeException("URL for STU3 FHIR server must be specified! Use the -stu3.url tag");
        }
        
        return stu3Endpoint;
    }

    public static String getR4Endpoint() {
        String r4Endpoint = System.getProperty("r4.url");
        if (r4Endpoint == null) {
            throw new RuntimeException("URL for R4 FHIR server must be specified! Use the -r4.url tag");
        }

        return r4Endpoint;
    }

    public static String getStu3CdsDiscovery() {
        String stu3DiscoveryEndpoint = System.getProperty("stu3.cds.discovery");
        if (stu3DiscoveryEndpoint == null) {
            throw new RuntimeException("URL for STU3 CDS discovery must be specified! Use the -stu3.cds.discovery tag");
        }

        return stu3DiscoveryEndpoint;
    }

    public static String getR4CdsDiscovery() {
        String r4DiscoveryEndpoint = System.getProperty("r4.cds.discovery");
        if (r4DiscoveryEndpoint == null) {
            throw new RuntimeException("URL for R4 CDS discovery must be specified! Use the -r4.cds.discovery tag");
        }

        return r4DiscoveryEndpoint;
    }
}

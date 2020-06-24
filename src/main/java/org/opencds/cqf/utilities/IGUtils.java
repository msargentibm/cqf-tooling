package org.opencds.cqf.utilities;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class IGUtils {
    public static String getImplementationGuideCanonicalBase(String url) {
        String canonicalBase = null;

        if (url != null && !url.isEmpty()) {
            canonicalBase = url.substring(0, url.indexOf("/ImplementationGuide/"));
        }

        return canonicalBase;
    }

    public static FhirContext getFhirContextFromIni(String igPath){
        FhirContext fhirContext;
        List<File> igPathFiles = IOUtils.getFilePaths(igPath, false).stream()
                .map(path -> new File(path))
                .collect(Collectors.toList());
        for (File file : igPathFiles) {
            if (FilenameUtils.getExtension(file.getName()).equals("ini")) {
                fhirContext = tryToReadIni(file);
                if (fhirContext != null) {
                    return fhirContext;
                }
            }
        }
        throw new IllegalArgumentException("Fhir Context not found in ig.ini");
    }

    private static FhirContext tryToReadIni(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            String igIniContent = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));
            String[] contentLines = igIniContent.split("\n");
            inputStream.close();
            return parseVersion(contentLines);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private static FhirContext parseVersion(String[] contentLines) {
        for (String line : contentLines) {
            if (line.toLowerCase().startsWith("fhirspec"))
            {
                if (line.contains("R4") || line.contains("r4")){
                    return FhirContext.forR4();
                }
                else if (line.contains("stu3") || line.contains("STU3") || line.contains("dstu3") || line.contains("DSTU3")) {
                    return FhirContext.forDstu3();
                }
            }
        }
        return null;
    }
}

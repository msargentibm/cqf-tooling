package org.opencds.cqf.processor;

import org.opencds.cqf.test.Context;
import org.opencds.cqf.test.TestScript;
import org.opencds.cqf.parameter.TestIGParameters;
import org.opencds.cqf.utilities.IOUtils;
import org.opencds.cqf.test.utilities.MeasureTestConfig;
import org.opencds.cqf.test.utilities.ScriptUtils;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.FhirContext;

import java.io.File;
import java.io.IOException;

public class IGTestProcessor {

    public static void testIg(TestIGParameters params) {
        String igPath = params.igPath;
        String scriptsPath = params.scriptsPath;
        IGVersion igVersion = params.igVersion;

        List<String> resourceDirs = params.resourceDirs;

        try
        {
            for (String dir : resourceDirs){
                IOUtils.resourceDirectories.add(Paths.get(dir).toFile().getCanonicalPath());
            }

            igPath = Paths.get(igPath).toFile().getCanonicalPath();
        }
        catch(IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        IOUtils.setIgPath(igPath);
        FhirContext fhirContext = (igVersion == null) ? IOUtils.getFhirContextFromIni(igPath) : getIgFhirContext(igVersion);


        File scriptDirectory = new File(scriptsPath);
        if (!scriptDirectory.isDirectory()) {
            throw new RuntimeException("The path to the test scripts must point to a directory");
        }

        Context context = new Context();
        IOUtils.resourceDirectories.add(igPath);
        List<MeasureTestConfig> configs = ScriptUtils.setupMeasureTestConfigs(igPath, fhirContext);
        System.out.println("Building Scripts...");
        ScriptUtils.buildScripts(configs, scriptsPath, fhirContext);

        for (TestScript script : ScriptUtils.getScripts(scriptDirectory)) {
            script.run(context);
        }

    }

    public static FhirContext getIgFhirContext(org.opencds.cqf.processor.IGTestProcessor.IGVersion igVersion)
    {
        switch (igVersion) {
            case FHIR3:
                return FhirContext.forDstu3();
            case FHIR4:
                return FhirContext.forR4();
            default:
                throw new IllegalArgumentException("Unknown IG version: " + igVersion);
        }
    }

    public enum IGVersion {
        FHIR3("fhir3"), FHIR4("fhir4");

        private String string;

        public String toString() {
            return this.string;
        }

        private IGVersion(String string) {
            this.string = string;
        }

        public static org.opencds.cqf.processor.IGTestProcessor.IGVersion parse(String value) {
            switch (value) {
                case "fhir3":
                    return FHIR3;
                case "fhir4":
                    return FHIR4;
                default:
                    throw new RuntimeException("Unable to parse IG version value:" + value);
            }
        }
    }
}
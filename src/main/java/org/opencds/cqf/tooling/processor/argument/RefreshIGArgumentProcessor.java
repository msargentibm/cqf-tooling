package org.opencds.cqf.tooling.processor.argument;

import static java.util.Arrays.asList;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.opencds.cqf.tooling.parameter.RefreshIGParameters;
import org.opencds.cqf.tooling.processor.IGProcessor;
import org.opencds.cqf.tooling.utilities.ArgUtils;
import org.opencds.cqf.tooling.utilities.IOUtils.Encoding;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;


public class RefreshIGArgumentProcessor {

    public static final String[] OPERATION_OPTIONS = {"RefreshIG"};

    public static final String[] INI_OPTIONS = {"ini"};
    public static final String[] ROOT_DIR_OPTIONS = {"root-dir"};
    public static final String[] IG_PATH_OPTIONS = {"ip", "ig-path"};
    public static final String[] IG_OUTPUT_ENCODING = {"e", "encoding"};
    public static final String[] INCLUDE_ELM_OPTIONS = {"elm", "include-elm"};
    public static final String[] INCLUDE_DEPENDENCY_LIBRARY_OPTIONS = {"d", "include-dependencies"};
    public static final String[] INCLUDE_TERMINOLOGY_OPTIONS = {"t", "include-terminology"};
    public static final String[] INCLUDE_PATIENT_SCENARIOS_OPTIONS = {"p", "include-patients"};
    public static final String[] VERSIONED_OPTIONS = {"v", "versioned"};
    public static final String[] CDS_HOOKS_OPTIONS = {"cdsig", "cds-hooks-ig"};
    public static final String[] FHIR_URI_OPTIONS = {"fs", "fhir-uri"};
    public static final String[] MEASURE_TO_REFRESH_PATH = {"mtrp", "measure-to-refresh-path"};
    public static final String[] RESOURCE_PATH_OPTIONS = {"rp", "resourcepath"};

    public OptionParser build() {
        OptionParser parser = new OptionParser();

        OptionSpecBuilder iniBuilder = parser.acceptsAll(asList(INI_OPTIONS), "Path to ig ini file");
        OptionSpecBuilder rootDirBuilder = parser.acceptsAll(asList(ROOT_DIR_OPTIONS), "Root directory of the ig");
        OptionSpecBuilder igPathBuilder = parser.acceptsAll(asList(IG_PATH_OPTIONS),"Path to the IG, relative to the root directory");
        OptionSpecBuilder resourcePathBuilder = parser.acceptsAll(asList(RESOURCE_PATH_OPTIONS),"Use multiple times to define multiple resource directories.");
        OptionSpecBuilder igOutputEncodingBuilder = parser.acceptsAll(asList(IG_OUTPUT_ENCODING), "If omitted, output will be generated using JSON encoding.");
        OptionSpecBuilder fhirUriBuilder = parser.acceptsAll(asList(FHIR_URI_OPTIONS),"If omitted the final bundle will not be loaded to a FHIR server.");
        OptionSpecBuilder measureToRefreshPathBuilder = parser.acceptsAll(asList(MEASURE_TO_REFRESH_PATH), "Path to Measure to refresh.");

        OptionSpec<String> ini = iniBuilder.withOptionalArg().describedAs("Path to the IG ini file");
        OptionSpec<String> rootDir = rootDirBuilder.withOptionalArg().describedAs("Root directory of the IG");
        OptionSpec<String> igPath = igPathBuilder.withRequiredArg().describedAs("Path to the IG, relative to the root directory");
        OptionSpec<String> resourcePath = resourcePathBuilder.withOptionalArg().describedAs("directory of resources");
        OptionSpec<String> igOutputEncoding = igOutputEncodingBuilder.withOptionalArg().describedAs("desired output encoding for resources");
        OptionSpec<String> measureToRefreshPath = measureToRefreshPathBuilder.withOptionalArg().describedAs("Path to Measure to refresh.");

        //TODO: FHIR user / password (and other auth options)
        OptionSpec<String> fhirUri = fhirUriBuilder.withOptionalArg().describedAs("uri of fhir server");  

        parser.acceptsAll(asList(OPERATION_OPTIONS),"The operation to run.");
        parser.acceptsAll(asList(INCLUDE_ELM_OPTIONS),"If omitted ELM will not be produced or packaged.");
        parser.acceptsAll(asList(INCLUDE_DEPENDENCY_LIBRARY_OPTIONS),"If omitted only the primary CQL library will be packaged.");
        parser.acceptsAll(asList(INCLUDE_TERMINOLOGY_OPTIONS),"If omitted terminology will not be packaged.");
        parser.acceptsAll(asList(INCLUDE_PATIENT_SCENARIOS_OPTIONS),"If omitted patient scenario information will not be packaged.");
        parser.acceptsAll(asList(VERSIONED_OPTIONS),"If omitted resources must be uniquely named.");
        parser.acceptsAll(asList(CDS_HOOKS_OPTIONS),"If omitted defaulted to non cds-hooks ig.");

        OptionSpec<Void> help = parser.acceptsAll(asList(ArgUtils.HELP_OPTIONS), "Show this help page").forHelp();

        return parser;
    }

    public RefreshIGParameters parseAndConvert(String[] args) {
        OptionParser parser = build();
        OptionSet options = ArgUtils.parse(args, parser);

        ArgUtils.ensure(OPERATION_OPTIONS[0], options);

        String ini = (String)options.valueOf(INI_OPTIONS[0]);

        String rootDir = (String)options.valueOf(ROOT_DIR_OPTIONS[0]);
        String igPath = (String)options.valueOf(IG_PATH_OPTIONS[0]);

        List<String> resourcePaths = (List<String>)options.valuesOf(RESOURCE_PATH_OPTIONS[0]);
        //could not easily use the built-in default here because it is based on the value of the igPath argument.
        String igEncoding = (String)options.valueOf(IG_OUTPUT_ENCODING[0]);
        Encoding outputEncodingEnum = Encoding.JSON;
        if (igEncoding != null) {
            outputEncodingEnum = Encoding.parse(igEncoding.toLowerCase());
        }
        Boolean includeELM = options.has(INCLUDE_ELM_OPTIONS[0]);  
        Boolean includeDependencies = options.has(INCLUDE_DEPENDENCY_LIBRARY_OPTIONS[0]);
        Boolean includeTerminology = options.has(INCLUDE_TERMINOLOGY_OPTIONS[0]);
        Boolean includePatientScenarios = options.has(INCLUDE_PATIENT_SCENARIOS_OPTIONS[0]);
        Boolean versioned = options.has(VERSIONED_OPTIONS[0]);
        Boolean cdsHooksIg = options.has(CDS_HOOKS_OPTIONS[0]);
        String fhirUri = (String)options.valueOf(FHIR_URI_OPTIONS[0]);
        String measureToRefreshPath = (String)options.valueOf(MEASURE_TO_REFRESH_PATH[0]);

        ArrayList<String> paths = new ArrayList<String>();
        paths.addAll(resourcePaths);
    
        RefreshIGParameters ip = new RefreshIGParameters();
        ip.ini = ini;
        ip.rootDir = rootDir;
        ip.igPath = igPath;
        ip.outputEncoding = outputEncodingEnum;
        ip.includeELM = includeELM;
        ip.includeDependencies = includeDependencies;
        ip.includeTerminology = includeTerminology;
        ip.includePatientScenarios = includePatientScenarios;
        ip.versioned = versioned;
        ip.cdsHooksIg = cdsHooksIg;
        ip.resourceDirs = paths;
        ip.fhirUri = fhirUri;
        ip.measureToRefreshPath = measureToRefreshPath;
       
        return ip;
    }
}
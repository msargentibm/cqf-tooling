package org.opencds.cqf.processor.argument;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;


import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;

import org.opencds.cqf.parameter.TestIGParameters;
import org.opencds.cqf.processor.IGTestProcessor;
import org.opencds.cqf.utilities.ArgUtils;


public class TestIGArgumentsProcessor {

    public static final String[] OPERATION_OPTIONS = {"TestIG"};

    public static final String[] IG_PATH_OPTIONS = {"ip", "ig-path"};
    public static final String[] SCRIPTS_PATH_OPTIONS = {"sp", "scripts-path"};
    public static final String[] IG_VERSION_OPTIONS = {"iv", "ig-version"};
    public static final String[] IG_OUTPUT_ENCODING = {"e", "encoding"};
    public static final String[] VERSIONED_OPTIONS = {"v", "versioned"};
    public static final String[] FHIR_URI_OPTIONS = {"fs", "fhir-uri"};
    public static final String[] RESOURCE_PATH_OPTIONS = {"rp", "resourcepath"};

    public OptionParser build() {
        OptionParser parser = new OptionParser();

        OptionSpecBuilder igPathBuilder = parser.acceptsAll(asList(IG_PATH_OPTIONS),"Limited to a single version of FHIR.");
        OptionSpecBuilder scriptsPathBuilder = parser.acceptsAll(asList(SCRIPTS_PATH_OPTIONS),"Required");
        OptionSpecBuilder resourcePathBuilder = parser.acceptsAll(asList(RESOURCE_PATH_OPTIONS),"Use multiple times to define multiple resource directories.");
        OptionSpecBuilder igVersionBuilder = parser.acceptsAll(asList(IG_VERSION_OPTIONS),"If omitted the root of the IG Path will be used.");
        OptionSpecBuilder igOutputEncodingBuilder = parser.acceptsAll(asList(IG_OUTPUT_ENCODING), "If omitted, output will be generated using JSON encoding.");
        OptionSpecBuilder fhirUriBuilder = parser.acceptsAll(asList(FHIR_URI_OPTIONS),"If omitted the final bundle will not be loaded to a FHIR server.");

        OptionSpec<String> igPath = igPathBuilder.withRequiredArg().describedAs("root directory of the ig");
        OptionSpec<String> scriptsPath = scriptsPathBuilder.withRequiredArg().describedAs("root directory of the scripts");
        OptionSpec<String> resourcePath = resourcePathBuilder.withOptionalArg().describedAs("directory of resources");
        OptionSpec<String> igVersion = igVersionBuilder.withOptionalArg().describedAs("ig fhir version");
        OptionSpec<String> igOutputEncoding = igOutputEncodingBuilder.withOptionalArg().describedAs("desired output encoding for resources");

        //TODO: FHIR user / password (and other auth options)
        OptionSpec<String> fhirUri = fhirUriBuilder.withRequiredArg().describedAs("uri of fhir server");

        parser.acceptsAll(asList(OPERATION_OPTIONS),"The operation to run.");
        parser.acceptsAll(asList(VERSIONED_OPTIONS),"If omitted resources must be uniquely named.");

        OptionSpec<Void> help = parser.acceptsAll(asList(ArgUtils.HELP_OPTIONS), "Show this help page").forHelp();

        return parser;
    }

    public TestIGParameters parseAndConvert(String[] args) {
        OptionParser parser = build();
        OptionSet options = ArgUtils.parse(args, parser);

        ArgUtils.ensure(OPERATION_OPTIONS[0], options);

        String igPath = (String)options.valueOf(IG_PATH_OPTIONS[0]);
        String scriptsPath = (String)options.valueOf(SCRIPTS_PATH_OPTIONS[0]);
        List<String> resourcePaths = (List<String>)options.valuesOf(RESOURCE_PATH_OPTIONS[0]);
        String fhirUri = (String)options.valueOf(FHIR_URI_OPTIONS[0]);
        String igVersion = (String)options.valueOf(IG_VERSION_OPTIONS[0]);

        ArrayList<String> paths = new ArrayList<String>();
        paths.addAll(resourcePaths);

        TestIGParameters ip = new TestIGParameters();
        ip.scriptsPath = scriptsPath;
        ip.igPath = igPath;
        ip.resourceDirs = paths;
        ip.fhirUri = fhirUri;
        ip.igVersion = IGTestProcessor.IGVersion.parse(igVersion);

        return ip;
    }
}
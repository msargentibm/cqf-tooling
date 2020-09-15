package org.opencds.cqf.tooling.operation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.fhir.ucum.UcumService;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Library;
import org.opencds.cqf.tooling.Operation;
import org.opencds.cqf.tooling.datarequirements.DataRequirementsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class DataRequirementsOperation extends Operation {

    protected Logger logger = LoggerFactory.getLogger(DataRequirementsOperation.class);

    final protected static String[] acceptedFormats = new String[] { "XML", "JSON" };

    /**
     * Takes inputDirectory, outputDirectory, and format (optional - must be XML or
     * JSON - defaults to JSON
     */
    @Override
    public void execute(String[] args) {

        if (args.length < 3) {
            throw new IllegalArgumentException("inputDirectory and outputDirectory must be specified");
        }

        String inputDirectory = args[1];
        String outputDirectory = args[2];

        String format = "JSON";
        if (args.length >= 4) {
            format = args[3];
            if (!Arrays.asList(acceptedFormats).contains(format))
            {
                throw new IllegalArgumentException("output format must be XML or JSON");
            }
        }

        final Path source = new File(inputDirectory).toPath();
        final Path destination = new File(outputDirectory).toPath();

        Map<Path, Path> inOutMap = new HashMap<>();
        if (source.toFile().isDirectory()) {
            if (destination.toFile().exists() && !destination.toFile().isDirectory()) {
                throw new IllegalArgumentException("Output must be a valid folder if input is a folder!");
            }

            try {
                Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toFile().getName().endsWith(".cql") || file.toFile().getName().endsWith(".CQL")) {
                            Path destinationFolder = destination.resolve(source.relativize(file.getParent()));
                            if (!destinationFolder.toFile().exists() && !destinationFolder.toFile().mkdirs()) {
                                logger.error(String.format("Problem creating %s%n", destinationFolder));
                            }
                            inOutMap.put(file, destinationFolder);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(String.format("error attempting to walk input directory %s", inputDirectory),
                        e);
            }
        } else {
            inOutMap.put(source, destination);
        }

        FhirContext fhirContext = FhirContext.forDstu3();

        for (Map.Entry<Path, Path> inOut : inOutMap.entrySet()) {
            Path in = inOut.getKey();
            Path out = inOut.getValue();
            if (out.toFile().isDirectory()) {
                // Use input filename with ".xml", ".json", or ".coffee" extension
                String name = in.toFile().getName();
                if (name.lastIndexOf('.') != -1) {
                    name = name.substring(0, name.lastIndexOf('.'));
                }

                name += "." + format.toLowerCase();
                out = out.resolve(name);
            }

            if (out.equals(in)) {
                throw new IllegalArgumentException("input and output file must be different!");
            }

            writeDataReqs(in, out, format, fhirContext);
        }
    }

    protected void writeDataReqs(Path inPath, Path outPath, String format, FhirContext context) {
        logger.info(String.format("Generating data reqs for: %s", inPath.toString()));

        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new LibraryManager(modelManager);
        UcumService ucumService = null;

        // TODO: enable ucum
        // if (options.getValidateUnits()) {
        // try {
        // ucumService = new
        // UcumEssenceService(UcumEssenceService.class.getResourceAsStream("/ucum-essence.xml"));
        // } catch (UcumException e) {
        // System.err.println("Could not create UCUM validation service:");
        // e.printStackTrace();
        // }
        // }

        // TODO: Translator options from the command line
        CqlTranslatorOptions options = CqlTranslatorOptions.defaultOptions();

        libraryManager.getLibrarySourceLoader().registerProvider(new DefaultLibrarySourceProvider(inPath.getParent()));
        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());

        CqlTranslator translator;
        try {
            translator = CqlTranslator.fromFile(inPath.toFile(), modelManager, libraryManager, ucumService, options);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("error attempting to create translator for file %s", inPath.toString()), e);
        }
        // libraryManager.getLibrarySourceLoader().clearProviders();
        if (translator.getErrors().size() > 0) {
            logger.error("Translation failed due to errors:");
            outputExceptions(translator.getExceptions());
        } else if (!options.getVerifyOnly()) {
            if (translator.getExceptions().size() == 0) {
                logger.info("Translation completed successfully.");
            } else {
                logger.warn("Translation completed with messages:");
                outputExceptions(translator.getExceptions());
            }

            DataRequirementsProcessor dataRequirementsProcessor = new DataRequirementsProcessor(libraryManager);
            TranslatedLibrary translatedLibrary = translator.getTranslatedLibrary();

            List<DataRequirement> requirements;
            try {
                requirements = dataRequirementsProcessor.createDataRequirements(translatedLibrary);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error generating DataRequirements for Library %s",
                        translatedLibrary.getIdentifier().getId()), e);
            }

            Library library = new Library().setType(new CodeableConcept(
                    new Coding("http://terminology.hl7.org/CodeSystem/library-type", "module-definition", null)));
            library.setDataRequirement(requirements);

            // Add data-reqs to a list;
            IParser parser = format.equals("JSON") ? context.newJsonParser() : context.newXmlParser();

            try (PrintWriter pw = new PrintWriter(outPath.toFile(), "UTF-8")) {
                pw.println(parser.encodeResourceToString(library));
            } catch (Exception e) {
                throw new RuntimeException(String.format("error writing DataRequirements to %s", outPath.toString()),
                        e);
            }

            logger.info(String.format("DataRequirements written to: %s", outPath.toString()));
        }
    }

    private void outputExceptions(Iterable<CqlTranslatorException> exceptions) {
        for (CqlTranslatorException error : exceptions) {
            TrackBack tb = error.getLocator();
            String lines = tb == null ? "[n/a]"
                    : String.format("[%d:%d, %d:%d]", tb.getStartLine(), tb.getStartChar(), tb.getEndLine(),
                            tb.getEndChar());
            logger.warn(String.format("%s:%s %s%n", error.getSeverity(), lines, error.getMessage()));
        }
    }
}
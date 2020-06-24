package org.opencds.cqf.test.utilities;

import ca.uhn.fhir.context.*;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor;
import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.opencds.cqf.test.TestScript;
import org.opencds.cqf.test.evaluate.ObjectFactoryEx;
import org.opencds.cqf.testcase.GroupItems;
import org.opencds.cqf.testcase.MeasureTestScript;
import org.opencds.cqf.testcase.MeasureTestScript.Test;
import org.opencds.cqf.testcase.MeasureTestScript.Test.ExpectedResponse;
import org.opencds.cqf.testcase.ObjectFactory;
import org.opencds.cqf.utilities.IGUtils;
import org.opencds.cqf.utilities.IOUtils;
import org.opencds.cqf.utilities.ResourceUtils;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;

public class ScriptUtils {

    public static List<TestScript> getScripts(File scriptDirectory) {
        List<TestScript> testScripts = new ArrayList<>();
        JAXBContext jaxbContext;
        Unmarshaller unmarshaller;
        try {
            jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            unmarshaller = jaxbContext.createUnmarshaller();
            try {
                unmarshaller.setProperty("com.sun.xml.bind.ObjectFactory", new ObjectFactoryEx());
            } catch (PropertyException e) {
                // for jdk environment
                unmarshaller.setProperty("com.sun.xml.internal.bind.ObjectFactory", new ObjectFactoryEx());
            }
            File[] scripts = Optional.ofNullable(scriptDirectory.listFiles()).orElseThrow(() -> new NoSuchElementException());
            for (File script : scripts) {
                if (script.isDirectory()) {
                    testScripts.addAll(getScripts(script));
                } else {
                    testScripts.add((TestScript) unmarshaller.unmarshal(script));
                }
            }
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException("Error parsing script: " + e.getMessage());
        }

        return testScripts;
    }

    public static List<MeasureTestConfig> setupMeasureTestConfigs(String pathToIG, FhirContext fhirContext) {

        List<MeasureTestConfig> measureTestConfigs = new ArrayList<MeasureTestConfig>();

        String fhirVersion = fhirContext.getVersion().getVersion().getFhirVersionString();
        if (fhirVersion == null)
        {
            fhirVersion = IGUtils.getFhirContextFromIni(pathToIG).getVersion().getVersion().getFhirVersionString();
        }

        File bundlesDir = new File(FilenameUtils.concat(pathToIG, "bundles"));
        RuntimeResourceDefinition measureReportDefinition = (RuntimeResourceDefinition) IOUtils.getResourceDefinition(fhirContext, "MeasureReport");
        BaseRuntimeChildDefinition measureChildDefinition = measureReportDefinition.getChildByName("measure");
        BaseRuntimeChildDefinition patientChildDefinition = measureReportDefinition.getChildByName("patient");
        BaseRuntimeChildDefinition subjectChildDefinition = measureReportDefinition.getChildByName("subject");
        BaseRuntimeChildDefinition periodChildDefinition = measureReportDefinition.getChildByName("period");
        BaseRuntimeChildDefinition groupChildDefinition = measureReportDefinition.getChildByName("group");
        RuntimeResourceBlockDefinition groupDefinition = (RuntimeResourceBlockDefinition)groupChildDefinition.getChildByName("group");
        RuntimeChildCompositeDatatypeDefinition identifierChildDefinition = (RuntimeChildCompositeDatatypeDefinition)groupDefinition.getChildByName("identifier");
        RuntimeResourceBlockDefinition measureReportGroupComponentDefinition = (RuntimeResourceBlockDefinition)groupChildDefinition.getChildByName("group");
        RuntimeChildResourceBlockDefinition populationChildDefinition = (RuntimeChildResourceBlockDefinition)measureReportGroupComponentDefinition.getChildByName("population");
        RuntimeResourceBlockDefinition measureReportPopulationDefinition = (RuntimeResourceBlockDefinition)populationChildDefinition.getChildByName("population");
        RuntimeChildPrimitiveDatatypeDefinition countChildDefinition = (RuntimeChildPrimitiveDatatypeDefinition)measureReportPopulationDefinition.getChildByName("count");
        RuntimeChildCompositeDatatypeDefinition codeChildDefinition = (RuntimeChildCompositeDatatypeDefinition)measureReportPopulationDefinition.getChildByName("code");
        RuntimeCompositeDatatypeDefinition periodDefinition = (RuntimeCompositeDatatypeDefinition)periodChildDefinition.getChildByName("period");
        BaseRuntimeChildDefinition startChildDefinition = periodDefinition.getChildByName("start");
        BaseRuntimeChildDefinition endChildDefinition = periodDefinition.getChildByName("end");
        if (bundlesDir.exists()) {
            List<String> measureBundles = IOUtils.getDirectoryPaths(bundlesDir.getPath(), false);
            for (String dirPath : measureBundles) {
                List<String> resourcePaths = IOUtils.getFilePaths(dirPath, true);
                for (String path : resourcePaths) {
                    try {
                        if (IOUtils.getMeasureReportPaths(fhirContext).contains(path)) {
                            IAnyResource resource = IOUtils.readResource(path, fhirContext, true);
                            MeasureTestConfig measureTestConfig = new MeasureTestConfig();
                            measureTestConfig.setPathToIG(pathToIG);
                            measureTestConfig.setFhirVersion(fhirVersion);
                            IBase periodChild = getFirstFromDefintion(resource, periodChildDefinition);
                            if (periodChild == null) {
                                continue;
                            }
                            String periodStart = getStringValueFromPrimitiveDefinition(periodChild, startChildDefinition);
                            int timeStartIndex = periodStart.contains("T") ? periodStart.indexOf("T") : periodStart.length();
                            String periodEnd = getStringValueFromPrimitiveDefinition(periodChild, endChildDefinition);
                            int timeEndIndex = periodEnd.contains("T") ? periodEnd.indexOf("T") : periodEnd.length();

                            measureTestConfig.setPeriodStart(periodStart.substring(0, timeStartIndex));
                            measureTestConfig.setPeriodEnd(periodEnd.substring(0, timeEndIndex));

                            IBase measureChild = getFirstFromDefintion(resource, measureChildDefinition);
                            if (measureChild == null) {
                                continue;
                            }
                            IBase patientChild = null;
                            //This is a Hack for now
                            if (patientChildDefinition == null) {
                                patientChild = getFirstFromDefintion(resource, subjectChildDefinition);
                            }
                            else patientChild = getFirstFromDefintion(resource, patientChildDefinition);
                            if (patientChild == null) {
                                continue;
                            }

                            String patientChildString = ResourceUtils.resolveProperty(patientChild, "reference", fhirContext).toString();
                            String patientChildIdAsFileName = patientChildString.replace("Patient/", "") + ".json";
                            Optional<String> optionalPatientPath = IOUtils.getPatientPaths(fhirContext).stream()
                                    .filter(patientPath -> patientPath.replaceAll("_", "-").contains(patientChildIdAsFileName)).findFirst();
                            if(optionalPatientPath.isPresent()) {
                                String patientPath = optionalPatientPath.get();
                                measureTestConfig.setPatientPath(patientPath);
                            }
                            else continue;

                            String measureChildString;
                            Object measureChildReferenceProperty = ResourceUtils.resolveProperty(measureChild, "reference", fhirContext);
                            if (measureChildReferenceProperty instanceof CanonicalType) {
                                measureChildString = ((CanonicalType)measureChildReferenceProperty).asStringValue();
                            }
                            else measureChildString = measureChildReferenceProperty.toString();
                            String measureChildIdAsFileName = measureChildString.replace("Measure/", "") + ".json";
                            Optional<String> optionalMeasurePath = IOUtils.getMeasurePaths(fhirContext).stream()
                                    .filter(measurePath -> measurePath.replaceAll("_", "-").contains(measureChildIdAsFileName)).findFirst();
                            if(optionalMeasurePath.isPresent()) {
                                String measurePath = optionalMeasurePath.get();
                                measureTestConfig.setMeasurePath(measurePath);
                            }

                            IBase groupChild = getFirstFromDefintion(resource, groupChildDefinition);
                            if (groupChild == null) {
                                continue;
                            }
                            String groupValueChildString;
                            if (identifierChildDefinition == null) {
                                groupValueChildString = ResourceUtils.resolveProperty(groupChild, "id", fhirContext).toString();
                            }
                            else  {
                                IBase identifierChild = getFirstFromDefintion(groupChild, identifierChildDefinition);
                                groupValueChildString = ResourceUtils.resolveProperty(identifierChild, "value", fhirContext).toString();
                            }

                            Map<String, BigInteger> populationValueCountMap = new HashMap<String, BigInteger>();
                            List<IBase> populationChildren = ResourceUtils.getFromDefinition(groupChild, populationChildDefinition);
                            if (populationChildren.isEmpty()) {
                                continue;
                            }

                            for (IBase populationChild : populationChildren) {
                                IBase codeChild = getFirstFromDefintion(populationChild, codeChildDefinition);
                                String populationCode = CodeUtil.getCodesFromObject(codeChild, fhirContext).get(0);
                                populationValueCountMap.put(
                                    populationCode,
                                    ResourceUtils.getBigIntegerValueFromPrimitiveDefinition(populationChild, countChildDefinition));
                            }
                            fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4);
                            Object measureScore = ResourceUtils.resolveProperty(groupChild, "measureScore", fhirContext);
                            BigDecimal measureScoreValue;
                            if (measureScore instanceof BigDecimal) {
                                measureScoreValue = (BigDecimal)measureScore;
                            }
                            else {
                                measureScoreValue = (BigDecimal) ResourceUtils.resolveProperty(measureScore, "value", fhirContext);
                            }
                            if (measureScoreValue == null) {
                                    measureTestConfig.setExpectedResponse(new ExpectedResponse().withGroup(
                                    new GroupItems()
                                        .withId(groupValueChildString)
                                        .withInitialPopulation(populationValueCountMap.get("initial-population"))
                                        .withNumerator(populationValueCountMap.get("numerator"))
                                        .withDenominator(populationValueCountMap.get("denominator"))
                                    ));
                                    measureTestConfigs.add(measureTestConfig);
                            }
                            else {
                                    measureTestConfig.setExpectedResponse(new ExpectedResponse().withGroup(
                                    new GroupItems()
                                        .withId(groupValueChildString)
                                        .withInitialPopulation(populationValueCountMap.get("initial-population"))
                                        .withNumerator(populationValueCountMap.get("numerator"))
                                        .withDenominator(populationValueCountMap.get("denominator"))
                                        .withMeasureScore(measureScoreValue)
                                    ));
                                    measureTestConfigs.add(measureTestConfig);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
        return measureTestConfigs;
    }

    public static void buildScripts(List<MeasureTestConfig> measureTestConfigs, String outputPath, FhirContext fhirContext) {
        Map<String, MeasureTestScript> testScripts = buildMeasureScript(measureTestConfigs, fhirContext);
        JAXBContext jaxbContext;
        Marshaller marshaller;
        try {
            jaxbContext = JAXBContext.newInstance(MeasureTestScript.class);
            marshaller = jaxbContext.createMarshaller();
            for (Entry<String, MeasureTestScript> entry : testScripts.entrySet()) {
                JAXBElement<MeasureTestScript> jbe = new JAXBElement<MeasureTestScript>(
                        new QName("", "MeasureTestScript"), MeasureTestScript.class, null, entry.getValue());

                // Create Marshaller
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

                // Print XML String to Console
                StringWriter sw = new StringWriter();

                // Write XML to StringWriter
                jaxbMarshaller.marshal(jbe, sw);

                writeOutput(entry.getValue().getId(), sw.toString(), outputPath);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }

    }

    private static Map<String, MeasureTestScript> buildMeasureScript(List<MeasureTestConfig> measureTestConfigs, FhirContext fhirContext) {
        RuntimeResourceDefinition measureDefinition = (RuntimeResourceDefinition) IOUtils.getResourceDefinition(fhirContext, "Measure");
        BaseRuntimeChildDefinition description = measureDefinition.getChildByName("description");
        Map<String, MeasureTestScript> scripts = new HashMap<String, MeasureTestScript>();
        for (MeasureTestConfig measureTestConfig : measureTestConfigs) {
            IAnyResource measure = IOUtils.getCachedResource(measureTestConfig.getMeasurePath());
            if(measure == null) {
                continue;
            }
            if(IOUtils.getCachedResource(measureTestConfig.getPatientPath()) == null) {
                continue;
            }
            if (scripts.containsKey(measureTestConfig.getMeasurePath())) {
                scripts.get(measureTestConfig).getTest().add(buildMeasureTest(measureTestConfig, measureTestConfig.getMeasurePath(), measureTestConfig.getPatientPath(), fhirContext));
            }
                String measureId = FilenameUtils.getName(measureTestConfig.getMeasurePath()).replaceAll(".json", "").replaceAll("_", "-");
                String patientId = FilenameUtils.getName(measureTestConfig.getPatientPath()).replaceAll(".json", "").replaceAll("_", "-");
                if (scripts.containsKey(measureId)) {
                    scripts.get(measureId).getTest().add(buildMeasureTest(measureTestConfig, measureId, patientId, fhirContext));
                }
                else {
                    MeasureTestScript measureTestScript = new MeasureTestScript()
                    .withId("script-" + measureId)
                    .withMeasureId(measureId)
                    .withEnabled(true)
                    .withFhirVersion(fhirContext.getVersion().getVersion().toString())
                    .withDescription(getStringValueFromPrimitiveDefinition(measure, description))
                    .withPathToIG(measureTestConfig.getPathToIG())
                    .withTest(buildMeasureTest(measureTestConfig, measureId, patientId, fhirContext))
                    ;
                    scripts.put(measureId, measureTestScript);
                }
        }
        return scripts;
    }

    private static Test buildMeasureTest(MeasureTestConfig measureTestConfig, String measureId, String patientId, FhirContext fhirContext) {
                return new Test()
                .withId(patientId)
                .withMeasureId(measureId)
                .withPatientId(patientId)
                .withPeriodStart(measureTestConfig.getPeriodStart())
                .withPeriodEnd(measureTestConfig.getPeriodEnd())
                .withReportType("patient")
                .withExpectedResponse(measureTestConfig.getExpectedResponse())
                ;
    }

    //should be in a ResourceUtils
    private static IBase getFirstFromDefintion(Object value, BaseRuntimeChildDefinition definition) {
        IAccessor accessor = definition.getAccessor();
        if (value == null || accessor == null) {
			return null;
		}

		List<IBase> values = accessor.getValues((IBase)value);
		if (values == null || values.isEmpty()) {
			return null;
		}

		if (values.size() > 1) {
			throw new IllegalArgumentException("More than one value returned while attempting to access primitive value.");
		}

		return values.get(0);
    }
    private static String getStringValueFromPrimitiveDefinition(IBase value, BaseRuntimeChildDefinition definition) {
        IAccessor accessor = definition.getAccessor();
		if (value == null || accessor == null) {
			return null;
		}

		List<IBase> values = accessor.getValues(value);
		if (values == null || values.isEmpty()) {
			return null;
		}

		if (values.size() > 1) {
			throw new IllegalArgumentException("More than one value returned while attempting to access primitive value.");
		}

		IBase baseValue = values.get(0);

		if (!(baseValue instanceof IPrimitiveType)) {
			throw new IllegalArgumentException("Non-primitive value encountered while trying to access primitive value.");
		}
		else {
			return ((IPrimitiveType)baseValue).getValueAsString();
		}
    }
    
    private static void writeOutput(String fileName, String content, String outputPath) throws IOException {
        File file = new File(FilenameUtils.concat(outputPath, fileName));
        if (file.exists()) {
            return;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream writer = new FileOutputStream(FilenameUtils.concat(outputPath, fileName + ".xml"))) {
            writer.write(content.getBytes());
            writer.flush();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}

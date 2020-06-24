/* This was directly copied from cql-cli, this should be shared*/
package org.opencds.cqf.test.utilities;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeCompositeDatatypeDefinition;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.ArrayList;
import java.util.List;

public class CodeUtil {

    public static List<String> getCodesFromObject(IBase object, FhirContext fhirContext) {
        return tryIterableThenConcept(fhirContext, object);
    }

    private static List<String> tryIterableThenConcept(FhirContext fhirContext, IBase object) {
        List<String> codes = new ArrayList<String>();
        codes.addAll(tryConceptThenCoding(fhirContext, object));
        return codes;
    }

    private static List<String> tryConceptThenCoding(FhirContext fhirContext, IBase object) {
        RuntimeCompositeDatatypeDefinition conceptDefinition = (RuntimeCompositeDatatypeDefinition)getElementDefinition(fhirContext, "CodeableConcept");
        List<IBase> codingObjects = getCodingObjectsFromDefinition(conceptDefinition, object);
        if(codingObjects == null) {
            return getCodesInCoding(fhirContext, object);
        }
        //would like to get the coding element definition from the codingObject rather than hardcoding it here
        RuntimeCompositeDatatypeDefinition codingDefinition = (RuntimeCompositeDatatypeDefinition)getElementDefinition(fhirContext, "Coding");
        return getCodeChildren(codingDefinition, codingObjects);
    }

    private static List<String> getCodesInCoding(FhirContext fhirContext, IBase object) {
        //would like to get the coding element definition from the codingObject rather than hardcoding it here
        RuntimeCompositeDatatypeDefinition codingDefinition = (RuntimeCompositeDatatypeDefinition)getElementDefinition(fhirContext, "Coding");
        List<IBase> codingObjects = getCodingObjectsFromDefinition(codingDefinition, object);
        if (codingObjects == null) {
            return null;
        }
        return getCodeChildren(codingDefinition, codingObjects);
    }

    private static List<String> getCodeChildren(RuntimeCompositeDatatypeDefinition codingDefinition, List<IBase> codingObjects) {
        BaseRuntimeChildDefinition versionDefinition = (BaseRuntimeChildDefinition)codingDefinition.getChildByName("version");
        BaseRuntimeChildDefinition codeDefinition = (BaseRuntimeChildDefinition)codingDefinition.getChildByName("code");
        BaseRuntimeChildDefinition systemDefinition = (BaseRuntimeChildDefinition)codingDefinition.getChildByName("system");
        BaseRuntimeChildDefinition displayDefinition = (BaseRuntimeChildDefinition)codingDefinition.getChildByName("display");

        return generateCodes(codingObjects, versionDefinition, codeDefinition, systemDefinition, displayDefinition);
    }

    private static List<String> generateCodes(List<IBase> codingObjects, BaseRuntimeChildDefinition versionDefinition,
                                              BaseRuntimeChildDefinition codeDefinition, BaseRuntimeChildDefinition systemDefinition,
                                              BaseRuntimeChildDefinition displayDefinition) {

        List<String> codes = new ArrayList<>();
        for (IBase coding : codingObjects) {
            String code = getStringValueFromPrimitiveDefinition(coding, codeDefinition);
				codes.add(code);
        }
        return codes;
    }

    private static BaseRuntimeElementDefinition getElementDefinition(FhirContext fhirContext, String ElementName) {
        BaseRuntimeElementDefinition<?> def = fhirContext.getElementDefinition(ElementName);
        return def;
    }

    private static List<IBase> getCodingObjectsFromDefinition(RuntimeCompositeDatatypeDefinition definition, IBase object) {
        BaseRuntimeChildDefinition coding = (BaseRuntimeChildDefinition)definition.getChildByName("coding");
        List<IBase> codingObject = null;
        try {
            codingObject = coding.getAccessor().getValues(object);
        } catch (Exception e) {
            //TODO: handle exception
        }
        return codingObject;
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

        
}
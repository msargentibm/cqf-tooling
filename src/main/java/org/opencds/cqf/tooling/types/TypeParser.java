package org.opencds.cqf.tooling.types;

import java.math.BigDecimal;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Type;

public class TypeParser {

    public static Object parse(String type, String value) {
        switch (type) {
            case "Boolean":
                return Boolean.parseBoolean(value);
            case "Integer":
                int intValue;
                try {
                    intValue = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Bad format for Integer literal");
                }
                return intValue;
            case "Decimal":
                BigDecimal bigDecimalValue;

                try {
                    bigDecimalValue = new BigDecimal(value);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(nfe.getMessage());
                }
                return bigDecimalValue;
            case "String":
                return value;
            default:
                throw new IllegalArgumentException(
                        String.format("Cannot construct literal value for type '%s'.", type));
        }

    }

    public static Type convert(String targetType, Object value) {

        if (value instanceof Type && value.getClass().getSimpleName().equals(targetType)) {
            return (Type)value;
        } 
  
        throw new IllegalArgumentException("don't know how to convert types yet");
    }

    public static boolean isTemporalType(String type) {
        // HACK: -o-rific
        String lower = type.toLowerCase();
        return lower.contains("date") || lower.contains("time") || 
            lower.contains("period") || lower.contains("duration");
    }

    public static Type parseTemporalType(String type, String value) {
        switch(type) {
            case "Period":
                // Start and end from interval?
                break;
            // TODO: Yeah, need to handle both cql and fhir types
            case "DateTime":
            case "date":
            case "datetime":
                return new DateTimeType(value);
        }

        throw new IllegalArgumentException("Can't parse unknown type.");
    }

    public static Type parseFhirType(String type, String value) {
        switch(type) {
            case "Period":
                // Start and end from interval?
                break;
            // TODO: Yeah, need to handle both cql and fhir types
            case "DateTime":
            case "date":
            case "datetime":
                return new DateTimeType(value);
        }

        throw new IllegalArgumentException("Can't parse unknown type.");
    }

}
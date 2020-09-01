package org.opencds.cqf.tooling.cqltypes;

import java.math.BigDecimal;

public class CqlTypeParser {

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

}
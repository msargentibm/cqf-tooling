package org.opencds.cqf.tooling.datarequirements;

import static org.junit.Assert.assertTrue;
import static org.opencds.cqf.tooling.datarequirements.DataRequirementOperations.convertCodeFilters;

import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.junit.Test;

public class DataRequirementOperationsTest {

    @Test
    public void TestDateFilterConversionNoValues() {

        DataRequirement initial = new DataRequirement();
        initial.addCodeFilter().setPath("period").addExtension("type", new StringType("date"));

        DataRequirement expected = new DataRequirement();
        expected.addDateFilter().setPath("period").addExtension("type", new StringType("date"));

        DataRequirement actual = convertCodeFilters(initial);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void TestDateFilterConversionNoDates() {

        DataRequirement initial = new DataRequirement();
        initial.addCodeFilter().setPath("patient").addExtension("type", new StringType("Patient"));

        DataRequirement expected = initial.copy();

        DataRequirement actual = convertCodeFilters(initial);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test(expected = IllegalArgumentException.class)
    public void TestDateFilterConversionNoTypeExtension() {

        DataRequirement initial = new DataRequirement();
        initial.addCodeFilter().setPath("patient").addExtension("type", new StringType("Patient"));
        initial.getCodeFilterFirstRep().addValueCoding().setCode("24");

        convertCodeFilters(initial);
    }

    @Test(expected = IllegalArgumentException.class)
    public void TestDateFilterConversionNoValueExtension() {

        DataRequirement initial = new DataRequirement();
        initial.addCodeFilter().setPath("patient").addExtension("type", new StringType("Patient"));
        initial.getCodeFilterFirstRep().addValueCoding().setCode("24");

        convertCodeFilters(initial);
    }

    @Test(expected = IllegalArgumentException.class)
    public void TestDateFilterConversionPreExistingDateFilter() {

        DataRequirement initial = new DataRequirement();
        initial.addDateFilter().setPath("period");

        convertCodeFilters(initial);
    }

    @Test()
    public void TestDateFilterConversionNoDateValues() {

        DataRequirement initial = new DataRequirement();
        initial.addCodeFilter().setPath("patient").addExtension("type", new StringType("Patient"));
        initial.getCodeFilterFirstRep().addValueCoding().setCode("24");
        initial.getCodeFilterFirstRep().getValueCodingFirstRep().addExtension("type", new StringType("integer"));

        DataRequirement expected = initial.copy();

        DataRequirement actual = convertCodeFilters(initial);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test(expected = IllegalArgumentException.class)
    public void TestDateFilterConversionMismatchedDateValues() {

        DataRequirement initial = new DataRequirement();
        initial.addCodeFilter().setPath("period").addExtension("type", new StringType("date"));
        initial.getCodeFilterFirstRep().addValueCoding().setCode("2018-01-01");
        initial.getCodeFilterFirstRep().getValueCodingFirstRep().addExtension("type", new StringType("integer"));

        DataRequirement expected = initial.copy();

        DataRequirement actual = convertCodeFilters(initial);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test()
    public void TestDateFilterConversionDateValue() {

        DataRequirement initial = new DataRequirement();
        initial.addCodeFilter().setPath("period").addExtension("type", new StringType("date"));
        initial.getCodeFilterFirstRep().addValueCoding().setCode("2018-01-01");
        initial.getCodeFilterFirstRep().getValueCodingFirstRep().addExtension("type", new StringType("date"));

        DataRequirement expected = new DataRequirement();
        expected.addDateFilter().setPath("period").addExtension("type", new StringType("date"));
        expected.getDateFilterFirstRep().setValue(new DateTimeType("2018-01-01"));

        DataRequirement actual = convertCodeFilters(initial);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test()
    public void TestDateFilterConversionCodeAndDateValues() {

        DataRequirement initial = new DataRequirement();
        initial.addCodeFilter().setPath("period").addExtension("type", new StringType("date"));
        initial.getCodeFilterFirstRep().addValueCoding().setCode("2018-01-01");
        initial.getCodeFilterFirstRep().getValueCodingFirstRep().addExtension("type", new StringType("date"));

        initial.addCodeFilter().setPath("patient").addExtension("type", new StringType("integer"));
        initial.getCodeFilter().get(1).addValueCoding().setCode("25");
        initial.getCodeFilter().get(1).getValueCodingFirstRep().addExtension("type", new StringType("integer"));

        DataRequirement expected = new DataRequirement();
        expected.addDateFilter().setPath("period").addExtension("type", new StringType("date"));
        expected.getDateFilterFirstRep().setValue(new DateTimeType("2018-01-01"));

        expected.addCodeFilter().setPath("patient").addExtension("type", new StringType("integer"));
        expected.getCodeFilterFirstRep().addValueCoding().setCode("25");
        expected.getCodeFilterFirstRep().getValueCodingFirstRep().addExtension("type", new StringType("integer"));

        DataRequirement actual = convertCodeFilters(initial);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test(expected = NullPointerException.class)
    public void TestMergeLeftNull() {
        DataRequirement left = null;
        DataRequirement right = new DataRequirement();

        DataRequirementOperations.merge(left, right);
    }

    @Test(expected = NullPointerException.class)
    public void TestMergeRightNull() {
        DataRequirement left = new DataRequirement();
        DataRequirement right = null;
        DataRequirementOperations.merge(left, right);
    }

    @Test(expected = IllegalArgumentException.class)
    public void TestMergeMismatchedTypes() {
        DataRequirement left = new DataRequirement(new CodeType("Y"));
        DataRequirement right = new DataRequirement(new CodeType("X"));
        DataRequirementOperations.merge(left, right);
    }

    @Test
    public void TestMergeTwoCodePaths() {
        DataRequirement left = new DataRequirement();
        left.addCodeFilter().setPath("one");

        DataRequirement right = new DataRequirement();
        right.addCodeFilter().setPath("two");


        DataRequirement expected = new DataRequirement();
        expected.addCodeFilter().setPath("one");
        expected.addCodeFilter().setPath("two");


        DataRequirement actual = DataRequirementOperations.merge(left, right);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void TestMergeTwoDatePaths() {
        DataRequirement left = new DataRequirement();
        left.addDateFilter().setPath("one");

        DataRequirement right = new DataRequirement();
        right.addDateFilter().setPath("two");


        DataRequirement expected = new DataRequirement();
        expected.addDateFilter().setPath("one");
        expected.addDateFilter().setPath("two");


        DataRequirement actual = DataRequirementOperations.merge(left, right);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void TestMergeTwoCodeFilterComponents() {
        DataRequirementCodeFilterComponent left = new DataRequirementCodeFilterComponent();
        left.setPath("one");
    }

}

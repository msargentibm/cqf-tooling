package org.opencds.cqf.tooling.datarequirements.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.visiting.DepthFirstTraverserImpl;
import org.hl7.elm.r1.ExpressionDef;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.Test;
import org.opencds.cqf.tooling.TestLibrarySourceProvider;

public class DataRequirementsVisitorTest {
    private static final ModelManager modelManager = new ModelManager();
    private LibraryManager libraryManager;

    protected LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(modelManager);
            libraryManager.getNamespaceManager().addNamespace(
                "org.opencds.cqf.tooling.datarequirements",
                "/org/opencds/cqf/tooling/datarequirements");
            libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
            libraryManager.getLibrarySourceLoader().registerProvider(new TestLibrarySourceProvider());
        }

        return libraryManager;
    }

    protected TranslatedLibrary getLibrary(String name) {
        List<CqlTranslatorException> errors = new ArrayList<>();
        org.hl7.elm.r1.VersionedIdentifier identifier = new org.hl7.elm.r1.VersionedIdentifier().withId(name)
                .withSystem("/org/opencds/cqf/tooling/datarequirements");

        TranslatedLibrary library = getLibraryManager().resolveLibrary(identifier,
                CqlTranslatorOptions.defaultOptions(), errors);
        if (errors.size() > 0) {
            throw new IllegalArgumentException(String.format("Library %s had translation exceptions", name));
        }

        return library;
    }

    protected ExpressionDef getExpressionFromLibrary(TranslatedLibrary library, String name) {
        return library.getLibrary().getStatements().getDef().stream()
        .filter(x -> x.getName().equals(name)).findFirst().get();
    }

    protected DataRequirementsVisitor getVisitorForLibrary(TranslatedLibrary library) {
        return getVisitorForLibrary(library, null);
    }

    protected DataRequirementsVisitor getVisitorForLibrary(TranslatedLibrary library, Map<String, Object> parameters) {
        return new DataRequirementsVisitor(library, getLibraryManager(), parameters);
    }

    protected DataRequirementsTraversingVisitor getTraversingVisitorForLibrary(TranslatedLibrary library) {
        return getTraversingVisitorForLibrary(library, null);
    }

    protected DataRequirementsTraversingVisitor getTraversingVisitorForLibrary(TranslatedLibrary library, Map<String, Object> parameters) {
        return new DataRequirementsTraversingVisitor(new DepthFirstTraverserImpl<>(), getVisitorForLibrary(library, parameters));
    }

    @Test
    public void testSimpleRetrieve() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsVisitor visitor = this.getVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "ESRD Observations");

        DataRequirement expected = new DataRequirement(new CodeType("Observation"));
        expected.addCodeFilter()
            .setPath("code")
            .setValueSet(new StringType("http://fakeurl.com/ersd-diagnosis"));

        List<DataRequirement> actual = observations.getExpression().accept(visitor);
        assertNotNull(actual);
        assertEquals(1, actual.size());

        assertTrue(actual.get(0).equalsDeep(expected));
    }

    @Test
    public void testSimpleExpression() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsVisitor visitor = new DataRequirementsVisitor(library, getLibraryManager());
        ExpressionDef observations = this.getExpressionFromLibrary(library, "ESRD Observations");

        // The traversal is depth-first, meaning the children get visited first.
        observations.getExpression().accept(visitor);
        List<DataRequirement> actual = observations.accept(visitor);
        assertNotNull(actual);
        assertEquals(1, actual.size());

        DataRequirement expected = new DataRequirement(new CodeType("Observation"));
        expected.addCodeFilter()
            .setPath("code")
            .setValueSet(new StringType("http://fakeurl.com/ersd-diagnosis"));

        assertTrue(actual.get(0).equalsDeep(expected));
    }

    @Test
    public void testSimpleTraversing() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "ESRD Observations");

        List<DataRequirement> actual = traversingVisitor.visit(observations);

        assertNotNull(actual);
        assertEquals(1, actual.size());

        DataRequirement expected = new DataRequirement(new CodeType("Observation"));
        expected.addCodeFilter()
            .setPath("code")
            .setValueSet(new StringType("http://fakeurl.com/ersd-diagnosis"));

        assertTrue(actual.get(0).equalsDeep(expected));
    }

    @Test
    public void testRetrieveWithConstantWhere() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "Observations");

        List<DataRequirement> actual = traversingVisitor.visit(observations);
        assertNotNull(actual);
        assertEquals(1, actual.size());

        DataRequirement expected = new DataRequirement(new CodeType("Observation"));
        expected.addCodeFilter()
            .setPath("status")
            .addValueCoding().setCode("final");

        assertTrue(actual.get(0).equalsDeep(expected));
    }

    @Test
    public void testSubqueryExists() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "HospiceEncounterClaimsA");

        List<DataRequirement> actual = traversingVisitor.visit(observations);
        assertNotNull(actual);
        assertEquals(1, actual.size());

        DataRequirement expected = new DataRequirement(new CodeType("Claim"));
        expected.addCodeFilter()
            .setPath("item.revenue")
            .setValueSet(new StringType("http://fakeurl.com/hospice-encounter"));

        assertTrue(actual.get(0).equalsDeep(expected));
    }

    @Test
    public void testDateSubqueryUnboundParameter() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "HospiceEncounterClaimsBUnboundDate");

        List<DataRequirement> actual = traversingVisitor.visit(observations);
        assertNotNull(actual);
        assertEquals(1, actual.size());

        DataRequirement expected = new DataRequirement(new CodeType("Claim"));
        expected.addDateFilter().setPath("item.serviced.start");
        assertTrue(actual.get(0).equalsDeep(expected));
    }

    // @Test
    public void testDateSubqueryBoundParameter() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("Measurement Period", "Interval[@2019-01-01T00:00:00.0, @2020-01-01T00:00:00.0)");
        
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library, parameters);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "HospiceEncounterClaimsBBoundDate");

        List<DataRequirement> actual = traversingVisitor.visit(observations);
        assertNotNull(actual);
        assertEquals(1, actual.size());

        DataRequirement expected = new DataRequirement(new CodeType("Claim"));
        expected.addDateFilter().setPath("item.serviced.start");
        assertTrue(actual.get(0).equalsDeep(expected));
    }
}
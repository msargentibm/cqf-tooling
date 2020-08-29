package org.opencds.cqf.tooling.datarequirements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.NamespaceManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.hl7.elm.r1.ExpressionDef;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.junit.Test;
import org.opencds.cqf.tooling.TestLibrarySourceProvider;

public class DataRequirementsVisitorTest {
    private ModelManager modelManager;

    protected ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
        }

        return modelManager;
    }

    private NamespaceManager namespaceManager;

    protected NamespaceManager getNamespaceManager() {
        if (namespaceManager == null) {
            namespaceManager = new NamespaceManager();
        }

        return namespaceManager;
    }

    private LibraryManager libraryManager;

    protected LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(getModelManager());
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
        return new DataRequirementsVisitor(library, getLibraryManager());
    }

    protected DataRequirementsTraversingVisitor getTraversingVisitorForLibrary(TranslatedLibrary library) {
        DataRequirementsTraverser traverser = new DataRequirementsTraverser();
        return new DataRequirementsTraversingVisitor(traverser, getVisitorForLibrary(library));
    }

    @Test
    public void testSimpleRetrieve() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsVisitor visitor = this.getVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "ESRD Observations");

        List<DataRequirement> dataReqs = observations.getExpression().accept(visitor);
        assertNotNull(dataReqs);
        assertEquals(1, dataReqs.size());

        DataRequirement expected = new DataRequirement(new CodeType("Observation"));
        expected.addCodeFilter()
            .setPath("code")
            .setValueSet("http://fakeurl.com/ersd-diagnosis");

        assertTrue(dataReqs.get(0).equalsDeep(expected));
    }

    @Test
    public void testSimpleExpression() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsVisitor visitor = new DataRequirementsVisitor(library, getLibraryManager());
        ExpressionDef observations = this.getExpressionFromLibrary(library, "ESRD Observations");

        // The traversal is depth-first, meaning the children get visited first.
        observations.getExpression().accept(visitor);
        List<DataRequirement> dataReqs = observations.accept(visitor);
        assertNotNull(dataReqs);
        assertEquals(1, dataReqs.size());

        DataRequirement expected = new DataRequirement(new CodeType("Observation"));
        expected.addCodeFilter()
            .setPath("code")
            .setValueSet("http://fakeurl.com/ersd-diagnosis");

        assertTrue(dataReqs.get(0).equalsDeep(expected));
    }

    @Test
    public void testSimpleTraversing() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "ESRD Observations");

        List<DataRequirement> dataReqs = traversingVisitor.visit(observations);

        assertNotNull(dataReqs);
        assertEquals(1, dataReqs.size());

        DataRequirement expected = new DataRequirement(new CodeType("Observation"));
        expected.addCodeFilter()
            .setPath("code")
            .setValueSet("http://fakeurl.com/ersd-diagnosis");

        assertTrue(dataReqs.get(0).equalsDeep(expected));
    }

    @Test
    public void testRetrieveWithConstantWhere() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "Observations");

        List<DataRequirement> dataReqs = traversingVisitor.visit(observations);
        assertNotNull(dataReqs);
        assertEquals(1, dataReqs.size());

        DataRequirement expected = new DataRequirement(new CodeType("Observation"));
        expected.addCodeFilter()
            .setPath("status")
            .addCode().setCode("final");

        assertTrue(dataReqs.get(0).equalsDeep(expected));
    }

    @Test
    public void testSubqueryExists() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "HospiceEncounterClaimsA");

        List<DataRequirement> dataReqs = traversingVisitor.visit(observations);
        assertNotNull(dataReqs);
        assertEquals(1, dataReqs.size());

        DataRequirement expected = new DataRequirement(new CodeType("Claim"));
        expected.addCodeFilter()
            .setPath("item.revenue")
            .setValueSet("http://fakeurl.com/hospice-encounter");

        assertTrue(dataReqs.get(0).equalsDeep(expected));
    }

    @Test
    public void testDateSubqueryUnboundParameter() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "HospiceEncounterClaimsBUnboundDate");

        List<DataRequirement> dataReqs = traversingVisitor.visit(observations);
        assertNotNull(dataReqs);
        assertEquals(1, dataReqs.size());

        DataRequirement expected = new DataRequirement(new CodeType("Claim"));
        expected.addDateFilter().setPath("item.serviced.start");
        assertTrue(dataReqs.get(0).equalsDeep(expected));
    }

    @Test
    public void testDateSubqueryParameter() throws Exception {
        TranslatedLibrary library = getLibrary("DataRequirements");
        DataRequirementsTraversingVisitor traversingVisitor = getTraversingVisitorForLibrary(library);
        ExpressionDef observations = this.getExpressionFromLibrary(library, "HospiceEncounterClaimsBBoundDate");

        List<DataRequirement> dataReqs = traversingVisitor.visit(observations);
        assertNotNull(dataReqs);
        assertEquals(1, dataReqs.size());

        DataRequirement expected = new DataRequirement(new CodeType("Claim"));
        expected.addDateFilter().setPath("item.serviced.start");
        assertTrue(dataReqs.get(0).equalsDeep(expected));
    }
}
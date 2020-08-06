package org.opencds.cqf.tooling.datarequirements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.NamespaceManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.hl7.elm.r1.ExpressionDef;
import org.hl7.elm.r1.Retrieve;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.junit.Test;
import org.opencds.cqf.tooling.TestLibrarySourceProvider;
import org.opencds.cqf.tooling.npm.LibraryLoader;

public class DataRequirementsVisitorTest {
    private static ModelManager modelManager;

    protected static ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
        }

        return modelManager;
    }

    private static NamespaceManager namespaceManager;

    protected static NamespaceManager getNamespaceManager() {
        if (namespaceManager == null) {
            namespaceManager = new NamespaceManager();
        }

        return namespaceManager;
    }

    private static LibraryManager libraryManager;

    protected static LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(getModelManager());
            libraryManager.getNamespaceManager().addNamespace("org.opencds.cqf.tooling.datarequirements",
                    "/org/opencds/cqf/tooling/datarequirements");
            libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
            libraryManager.getLibrarySourceLoader().registerProvider(new TestLibrarySourceProvider());
        }

        return libraryManager;
    }

    protected TranslatedLibrary getLibrary(String system, String name) {
        List<CqlTranslatorException> errors = new ArrayList<>();
        org.hl7.elm.r1.VersionedIdentifier identifier = new org.hl7.elm.r1.VersionedIdentifier().withId(name)
                .withSystem(system);

        TranslatedLibrary library = getLibraryManager().resolveLibrary(identifier,
                CqlTranslatorOptions.defaultOptions(), errors);
        if (errors.size() > 0) {
            throw new IllegalArgumentException(String.format("Library %s/%s had translation exceptions", system, name));
        }

        return library;
    }

    @Test
    public void testSimpleRetrieve() throws Exception {
        TranslatedLibrary library = getLibrary("/org/opencds/cqf/tooling/datarequirements", "DataRequirements");
        DataRequirementsVisitor visitor = new DataRequirementsVisitor(library, getLibraryManager());

        ExpressionDef ersdObservations = library.getLibrary().getStatements().getDef().stream()
                .filter(x -> x.getName().equals("ESRD Observations")).findFirst().get();

        List<DataRequirement> dataReqs = ersdObservations.getExpression().accept(visitor);
        assertNotNull(dataReqs);
        assertEquals(1, dataReqs.size());

        DataRequirement dataReq = dataReqs.get(0);
        assertEquals("Observation", dataReq.getType());
        assertNotNull(dataReq.getCodeFilter());
        assertNotNull(dataReq.getCodeFilterFirstRep());

        DataRequirementCodeFilterComponent drcfc = dataReq.getCodeFilterFirstRep();
        assertEquals("code", drcfc.getPath());
        assertEquals("http://fakeurl.com/ersd-diagnosis", drcfc.getValueSet());
    }
}
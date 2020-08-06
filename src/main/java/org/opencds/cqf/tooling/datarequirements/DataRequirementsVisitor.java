package org.opencds.cqf.tooling.datarequirements;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hl7.elm.r1.Retrieve;
import org.hl7.fhir.r4.model.DataRequirement;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.visiting.BaseVisitor;

public class DataRequirementsVisitor extends BaseVisitor<List<DataRequirement>, Exception> {

    private LibraryManager libraryManager;
    private DataRequirementsExtractor dataRequirementsExtractor;
    private TranslatedLibrary library;

    private Map<String, Object> parameters;

    public DataRequirementsVisitor(TranslatedLibrary library, LibraryManager libraryManager) {
        this(library, libraryManager, null);
    }

    public DataRequirementsVisitor(TranslatedLibrary library, LibraryManager libraryManager, Map<String, Object> parameters) {
        this.dataRequirementsExtractor = new DataRequirementsExtractor();
        this.libraryManager = Objects.requireNonNull(libraryManager, "libraryManager can not be null");
        this.library = Objects.requireNonNull(library, "library can not be null.");
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    @Override public List<DataRequirement> visit(Retrieve retrieve) {
        return Collections.singletonList(
            this.dataRequirementsExtractor.toDataRequirement(retrieve, this.library, this.libraryManager));
    }
}
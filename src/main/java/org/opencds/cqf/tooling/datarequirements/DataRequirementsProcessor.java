package org.opencds.cqf.tooling.datarequirements;

import java.util.List;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.visiting.DepthFirstTraverserImpl;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.opencds.cqf.tooling.datarequirements.visitor.DataRequirementsTraversingVisitor;
import org.opencds.cqf.tooling.datarequirements.visitor.DataRequirementsVisitor;

public class DataRequirementsProcessor {

    protected LibraryManager libraryManager;

    public DataRequirementsProcessor(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }


    public List<DataRequirement> createDataRequirements(TranslatedLibrary translatedLibrary) throws Exception {
        // TODO: Parameters
        DataRequirementsVisitor dataRequirementsVisitor = new DataRequirementsVisitor(translatedLibrary, this.libraryManager);
        DataRequirementsTraversingVisitor dataRequirementsTraversingVisitor = new DataRequirementsTraversingVisitor(new DepthFirstTraverserImpl<>(), dataRequirementsVisitor);

        return dataRequirementsTraversingVisitor.visit(translatedLibrary.getLibrary());
    }
    
}

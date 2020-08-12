package org.opencds.cqf.tooling.datarequirements;

import java.util.List;

import org.cqframework.cql.elm.visiting.TraversingVisitor;
import org.hl7.fhir.r4.model.DataRequirement;

public class DataRequirementsTraversingVisitor extends TraversingVisitor<List<DataRequirement>, Exception> {

    public DataRequirementsTraversingVisitor(DataRequirementsTraverser aTraverser,
            DataRequirementsVisitor aVisitor) {
        super(aTraverser, aVisitor);
        this.setTraverseFirst(true);
    }
}
package org.opencds.cqf.tooling.datarequirements.visitor;

import java.util.List;

import org.cqframework.cql.elm.visiting.DepthFirstTraverserImpl;
import org.cqframework.cql.elm.visiting.TraversingVisitor;
import org.hl7.elm.r1.Query;
import org.hl7.fhir.dstu3.model.DataRequirement;

public class DataRequirementsTraversingVisitor extends TraversingVisitor<List<DataRequirement>, Exception> {

    public DataRequirementsTraversingVisitor(DepthFirstTraverserImpl<Exception> aTraverser,
            DataRequirementsVisitor aVisitor) {
        super(aTraverser, aVisitor);
        this.setTraverseFirst(true);
    }

    @Override
    public List<DataRequirement> visit(Query aBean)
        throws Exception
    {
        ((DataRequirementsVisitor)this.getVisitor()).enterQueryContext();

        if (getTraverseFirst() == true) {
  
            getTraverser().traverse(aBean, this);
            if (this.getProgressMonitor()!= null) {
                this.getProgressMonitor().traversed(aBean);
            }
        }
        List<DataRequirement>  returnVal;
        returnVal = aBean.accept(getVisitor());
        if (this.getProgressMonitor()!= null) {
            this.getProgressMonitor().visited(aBean);
        }
        if (getTraverseFirst() == false) {
            getTraverser().traverse(aBean, this);
            if (this.getProgressMonitor()!= null) {
                this.getProgressMonitor().traversed(aBean);
            }
        }

        ((DataRequirementsVisitor)this.getVisitor()).exitQueryContext();
        return returnVal;
    }
}
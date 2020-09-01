package org.opencds.cqf.tooling.datarequirements;

import java.util.HashSet;

import org.cqframework.cql.cql2elm.model.LibraryRef;
import org.cqframework.cql.elm.visiting.DepthFirstTraverserImpl;
import org.cqframework.cql.elm.visiting.Visitor;
import org.hl7.elm.r1.ExpressionDef;
import org.hl7.elm.r1.FunctionRef;
import org.hl7.elm.r1.Query;

import java.util.Set;

public class DataRequirementsTraverser extends DepthFirstTraverserImpl<Exception> {
    private Set<String> expressionsTraversed = new HashSet<>();

    public void traverse(LibraryRef library, Visitor<?, Exception> visitor) throws Exception {
        // TODO: LibraryRef traversal logic.
        return;
    }

    @Override
    public void traverse(ExpressionDef expression, Visitor<?, Exception> visitor) throws Exception {
        if (expressionsTraversed.contains(expression.getName())) {
            return;
        }
        else {
            super.traverse(expression, visitor);
            expressionsTraversed.add(expression.getName());
        }
    }

    @Override
    public void traverse(Query query, Visitor<?, Exception> visitor) throws Exception {
        if (visitor instanceof DataRequirementsTraversingVisitor) {
            DataRequirementsTraversingVisitor dVisitor = (DataRequirementsTraversingVisitor) visitor;
            if (dVisitor != null) {
                if (dVisitor.getVisitor() != null) {
                    if (dVisitor.getVisitor() instanceof DataRequirementsVisitor) {
                        ((DataRequirementsVisitor)(dVisitor.getVisitor())).enterQueryContext();
                    }
                }
            }
        }

        super.traverse(query, visitor);
    }

    @Override 
    public void traverse(FunctionRef functionRef,  Visitor<?, Exception> visitor) throws Exception {
       // Redirect to traversal of FunctionDef
    }
}
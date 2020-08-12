package org.opencds.cqf.tooling.datarequirements;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.crypto.Data;

import java.util.Objects;
import java.util.Stack;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.LibraryRef;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.visiting.BaseVisitor;
import org.hl7.elm.r1.*;
import org.hl7.fhir.CodeSystemFilter;
import org.hl7.fhir.DataRequirementDateFilter;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementDateFilterComponent;

import org.hl7.fhir.r4.model.StringType;

public class DataRequirementsVisitor extends BaseVisitor<List<DataRequirement>, Exception> {

    public DataRequirementsVisitor(TranslatedLibrary library, LibraryManager libraryManager) {
        this(library, libraryManager, null);
    }

    public DataRequirementsVisitor(TranslatedLibrary library, LibraryManager libraryManager,
            Map<String, Object> parameters) {
        this.dataRequirementsExtractor = new DataRequirementsExtractor();
        this.libraryManager = Objects.requireNonNull(libraryManager, "libraryManager can not be null");
        this.library = Objects.requireNonNull(library, "library can not be null.");
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    private LibraryManager libraryManager;
    private DataRequirementsExtractor dataRequirementsExtractor;
    private TranslatedLibrary library;

    private Map<String, Object> parameters;

    private Map<String, StackFrame> expressionDataRequirementsCache = new HashMap<>();

    // Each node in a query has a set of Ors
    // Each Or has a set of Types
    // Each Type has a set of Filters (Ands)
    private Stack<StackFrame> stack = new Stack<>();

    private int querylevel = 0;

    public void enterQueryContext() {
        querylevel++;
    }

    public void exitQueryContext() {
        querylevel--;

        if (querylevel < 0) {
            throw new IllegalStateException("can't have negative query context levels");
        }
    }

    public Boolean isQueryContext() {
        return querylevel > 0;
    }

    // TODO: LibraryRef visit logic
    public List<DataRequirement> visit(LibraryRef libraryRef) {
        return null;
    }

    // A Retrieve is the only root source of data requirements because it represents
    // the actual execution
    // against the data store. All the other nodes can only extend or mutate the
    // data requirement.
    @Override
    public List<DataRequirement> visit(Retrieve retrieve) {
        DataRequirement dataReq = this.dataRequirementsExtractor.toDataRequirement(retrieve, this.library,
                this.libraryManager);
        this.push(StackFrame.of(dataReq));
        return this.peek().flatten();
    }

    @Override
    public List<DataRequirement> visit(And and) {
        if (!this.isQueryContext()) {
            return null;
        }

        StackFrame right = this.peek();
        StackFrame left = this.peek();

        // Detect lack of disjunctive normal form
        // (an OR inside an AND)
        if (left.size() > 1 || right.size() > 1) {
            throw new IllegalStateException("queries not in disjunctive normal form");
        }

        this.push(this.simpleMergeTwoOperands());
        return this.peek().flatten();
    }

    // The expression def is the top-level item, so it should it clear out
    // any stack that's associated with generating it. If called multiple times it
    // should
    // return a cached list of data requirements.
    @Override
    public List<DataRequirement> visit(ExpressionDef expressionDef) {
        if (!expressionDataRequirementsCache.containsKey(expressionDef.getName())) {
            // All the data reqs should be fully resolved at this point
            // because an expressionDef can only have one expression.
            if (this.stack.size() > 1) {
                throw new IllegalStateException("data requirements for expression %s were not fully resolved");
            }

            this.expressionDataRequirementsCache.put(expressionDef.getName(), this.pop());
        }

        if (!this.stack.empty()) {
            throw new IllegalStateException(
                    String.format("children of expression %s were visited more than once.", expressionDef.getName()));
        }

        return expressionDataRequirementsCache.get(expressionDef.getName()).flatten();
    }

    @Override
    public List<DataRequirement> visit(Exists exists) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        return this.peek().flatten();
    }

    @Override
    public List<DataRequirement> visit(InValueSet inValueSet) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        StackFrame valueSet = this.pop();
        StackFrame code = this.pop();

        if ((!valueSet.hasSingleOr() || !code.hasSingleOr())
                || (!valueSet.getSingleOr().hasSingleRequirement() || !code.getSingleOr().hasSingleRequirement())) {
            throw new IllegalArgumentException("InValueSet requires exactly one code path and one valueset");
        }

        DataRequirement codeReq = code.getSingleOr().getSingle();
        DataRequirement valueReq = valueSet.getSingleOr().getSingle();

        codeReq.getCodeFilterFirstRep().setValueSet(valueReq.getCodeFilterFirstRep().getValueSet());

        this.push(StackFrame.of(codeReq));
        return this.peek().flatten();
    }

    @Override
    public List<DataRequirement> visit(ValueSetRef ref) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        ValueSetDef def = this.dataRequirementsExtractor.resolveValueSetRef(ref, library, libraryManager);

        DataRequirement dr = new DataRequirement();
        dr.addCodeFilter().setValueSet(def.getId());

        this.push(StackFrame.of(dr));

        return this.peek().flatten();
    }

    @Override
    public List<DataRequirement> visit(Literal literal) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        DataRequirement dataReq = new DataRequirement();

        // Need to ensure that ELM always has type information.
        String localName = literal.getResultType().toString();
        if (localName.toLowerCase().contains("date") || localName.toLowerCase().contains("period") || 
        localName.toLowerCase().contains("duration") || localName.toLowerCase().contains("time")) {
            dataReq.addDateFilter().setValue(new StringType(literal.getValue()));
        } else {
            dataReq.addCodeFilter().addCode().setCode(literal.getValue());
        }

        this.push(StackFrame.of(dataReq));

        return this.peek().flatten();
    }

    @Override
    public List<DataRequirement> visit(Property property) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        DataRequirement dataReq = new DataRequirement(new CodeType(property.getScope()));
        String localName = property.getResultType().toString();
        if (localName.toLowerCase().contains("date") || localName.toLowerCase().contains("period") || 
            localName.toLowerCase().contains("duration") || localName.toLowerCase().contains("time")) {
            dataReq.addDateFilter().setPath(property.getPath());
        } else {
            dataReq.addCodeFilter().setPath(property.getPath());
        }

        if (property.getSource() != null) {
            StackFrame sourceFrame = this.pop();
            if (sourceFrame.size() > 0) {

                if (sourceFrame.size() > 1 || sourceFrame.get(0).size() != 1) {
                    throw new IllegalArgumentException("cant merge complex property references yet.");
                }

                // TODO: This is shared "merge-path logic" with alias resolution
                DataRequirement inner = sourceFrame.getSingleOr().getSingle();

                dataReq.setType(inner.getType());

                String pathPrepend = null;
                if (inner.hasCodeFilter()) {
                    pathPrepend = inner.getCodeFilterFirstRep().getPath();
                }

                if (inner.hasDateFilter()) {
                    pathPrepend = inner.getDateFilterFirstRep().getPath();
                }

                if (pathPrepend != null) {
                    if (dataReq.hasCodeFilter()) {
                        for (DataRequirementCodeFilterComponent filter : dataReq.getCodeFilter()) {
                            if (filter.hasPath()) {
                                filter.setPath(pathPrepend + "." + filter.getPath());
                            }
                        }
                    }

                    if (dataReq.hasDateFilter()) {
                        for (DataRequirementDateFilterComponent filter : dataReq.getDateFilter()) {
                            if (filter.hasPath()) {
                                filter.setPath(pathPrepend + "." + filter.getPath());
                            }
                        }
                    }
                }
            }
        }

        this.push(StackFrame.of(dataReq));

        return this.peek().flatten();
    }

    @Override
    public List<DataRequirement> visit(Query query) throws Exception {
        // These are done in reverse order of the traverse / visit.

        // Renable these as they are visited
        // if (query.getSort()!= null) {
        // dataReqs.addAll(this.stack.pop().flatten());
        // }

        // if (query.getAggregate()!= null) {
        // dataReqs.addAll(this.stack.pop().flatten());
        // }

        // if (query.getReturn()!= null) {
        // dataReqs.addAll(this.stack.pop().flatten());
        // }

        StackFrame whereFrame = null;
        if (query.getWhere() != null) {
            whereFrame = this.stack.pop();
        }

        // if (query.getRelationship() != null) {
        // List<List<DataRequirement>> relReqs =
        // this.pop(query.getRelationship().size());
        // relReqs.forEach(x -> dataReqs.addAll(x));
        // }

        // if (query.getLet() != null) {
        // List<List<DataRequirement>> letReqs = this.pop(query.getLet().size());
        // letReqs.forEach(x -> dataReqs.addAll(x));
        // }

        // List<StackFrame> sources = new ArrayList<>();

        // Are sources logically anded?
        List<DataRequirement> sources = new ArrayList<>();
        if (query.getSource() != null) {
            List<StackFrame> sourceFrames = this.pop(query.getSource().size());

            for (StackFrame frame : sourceFrames) {
                sources.addAll(frame.flatten());
            }
        }

        List<DataRequirement> wheres = this.applyWhere(sources, whereFrame.flatten());

        this.push(StackFrame.of(wheres));

        this.exitQueryContext();

        return this.peek().flatten();
    }

    @Override
    public List<DataRequirement> visit(AliasedQuerySource aliasedQuerySource) throws Exception {
        StackFrame source = this.peek();

        if (source.size() > 1) {
            throw new IllegalStateException("query source can not have Ors");
        }

        DataRequirementMap requirements = source.get(0);

        if (requirements.size() > 1) {
            throw new IllegalStateException("query source can not multiple types of data requirements");
        }

        requirements.getSingle().addExtension("alias", new StringType(aliasedQuerySource.getAlias()));

        return source.flatten();
    }

    // Don't trace functions for now...
    // The traverser will have to be smart enough to resolve and visit those
    @Override
    public List<DataRequirement> visit(FunctionRef functionRef) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        List<StackFrame> frames = this.pop(functionRef.getOperand().size());

        List<DataRequirement> requirements = new ArrayList<>();

        for (StackFrame frame : frames) {
            requirements.addAll(frame.flatten());
        }

        this.push(StackFrame.of(requirements));
        return this.peek().flatten();
    }

    public List<DataRequirement> visit(In in) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        this.push(this.simpleMergeTwoOperands());
        return this.peek().flatten();
    }

    public List<DataRequirement> visit(ParameterRef parameterRef) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        ParameterDef parameterDef = this.dataRequirementsExtractor.resolveParameterRef(parameterRef, library,
                libraryManager);

        if (parameterDef == null || parameterDef.getName() == null
                || !this.parameters.containsKey(parameterDef.getName())) {
            this.push(StackFrame.empty());
            return this.peek().flatten();
        }

        Object parameter = this.parameters.get(parameterDef.getName());
        DataRequirement dataReq = new DataRequirement();
        String localName = parameterDef.getResultType().toString();
        if (localName.contains("Date") || localName.contains("Period") || localName.contains("Duration")) {
            dataReq.addDateFilter().setValue(parameter != null ? new StringType(parameter.toString()) : null);
        } else {
            dataReq.addCodeFilter().addCode().setCode(parameter != null ? parameter.toString() : null);
        }

        this.push(StackFrame.of(dataReq));

        return this.peek().flatten();
    }

    public List<DataRequirement> visit(Equal equal) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        this.push(this.simpleMergeTwoOperands());
        return this.peek().flatten();
    }

    // Dumb union for now.
    // Needs to be smart enough to detect duplicate paths for code and date filters
    // and merge
    // appropriately
    private DataRequirement union(DataRequirement left, DataRequirement right) {
        Objects.requireNonNull(left, "left can not be null");
        Objects.requireNonNull(right, "right can not be null");

        if (left.getType() != null && right.getType() != null && !left.getType().equals(right.getType())) {
            throw new IllegalArgumentException("if type is explicit left and right must be of same type");
        }

        if (right.hasType()) {
            if (!left.hasType()) {
                left.setType(right.getType());
            }
        }

        if (right.hasCodeFilter()) {
            for (DataRequirementCodeFilterComponent cfc : right.getCodeFilter()) {
                left.addCodeFilter(cfc);
            }
        }

        if (left.hasCodeFilter()) {
            // Bind paths to constants
            if (left.getCodeFilter().size() == 2) {
                DataRequirementCodeFilterComponent one = left.getCodeFilter().get(0);
                DataRequirementCodeFilterComponent two = left.getCodeFilter().get(1);

                if (one.getPath() != null && two.getPath() == null) {
                    if (!one.hasCode()) {
                        one.addCode().setCode(two.getCodeFirstRep().getCode());
                        left.getCodeFilter().remove(1);
                    }
                }

                if (two.getPath() != null && one.getPath() == null) {
                    if (!two.hasCode()) {
                        two.addCode().setCode(one.getCodeFirstRep().getCode());
                        left.getCodeFilter().remove(0);
                    }
                }
            }
        }

        if (right.hasDateFilter()) {
            for (DataRequirementDateFilterComponent dfc : right.getDateFilter()) {
                left.addDateFilter(dfc);
            }
        }

        if (left.hasCodeFilter()) {
            // Bind date filter constants
            if (left.getDateFilter().size() == 2) {
                DataRequirementDateFilterComponent one = left.getDateFilter().get(0);
                DataRequirementDateFilterComponent two = left.getDateFilter().get(1);

                if (one.getPath() != null && two.getPath() == null) {
                    if (!one.hasValue()) {
                        one.setValue(two.getValue());
                        left.getCodeFilter().remove(1);
                    }
                }

                if (two.getPath() != null && one.getPath() == null) {
                    if (!two.hasValue()) {
                        two.setValue(one.getValue());
                        left.getCodeFilter().remove(0);
                    }
                }
            }
        }

        return left;
    }

    private List<DataRequirement> groupUnion(List<DataRequirement> dataRequirements) {
        List<DataRequirement> mergedRequirements = new ArrayList<>();

        Map<String, List<DataRequirement>> groupedRequirements = dataRequirements.stream()
                .collect(groupingBy(DataRequirement::getType));

        for (Entry<String, List<DataRequirement>> entry : groupedRequirements.entrySet()) {
            DataRequirement mergedRequirement = this.union(entry.getValue());
            mergedRequirements.add(mergedRequirement);
        }

        return mergedRequirements;
    }

    private DataRequirement union(List<DataRequirement> dataRequirements) {
        DataRequirement unioned = dataRequirements.get(0);

        for (int i = 1; i < dataRequirements.size(); i++) {
            DataRequirement next = dataRequirements.get(i);
            if (!next.getType().equals(unioned.getType())) {
                throw new IllegalArgumentException("attempted to union with dataRequirements of a different type");
            }

            unioned = union(unioned, next);
        }

        return unioned;
    }

    private void push(StackFrame stackFrame) {
        this.stack.push(stackFrame);
    }

    private StackFrame peek() {
        return this.stack.peek();
    }

    private StackFrame pop() {
        return this.stack.pop();
    }

    private List<StackFrame> pop(int count) {
        List<StackFrame> stackFrames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            stackFrames.add(this.pop());
        }

        return stackFrames;
    }

    public StackFrame mergeAnd(StackFrame left, StackFrame right) {
        List<DataRequirement> requirements = left.flatten();
        requirements.addAll(right.flatten());

        requirements = groupUnion(requirements);

        return StackFrame.of(requirements);
    }

    public StackFrame mergeOr(StackFrame left, StackFrame right) {
        StackFrame stackFrame = new StackFrame();

        stackFrame.addAll(left);
        stackFrame.addAll(right);

        return stackFrame;
    }

    private List<DataRequirement> applyWhere(List<DataRequirement> sources, List<DataRequirement> wheres) {
        for (DataRequirement source : sources) {
            String aliasName = source.getExtensionByUrl("alias").getValue().toString();

            if (source.hasCodeFilter() && source.hasDateFilter()) {
                throw new IllegalArgumentException("sources can't alias more than one property");
            }

            String pathPrepend = null;
            if (source.hasCodeFilter()) {
                pathPrepend = source.getCodeFilterFirstRep().getPath();
            }

            if (source.hasDateFilter()) {
                pathPrepend = source.getDateFilterFirstRep().getPath();
            }
            // An alias can be:
            // 1. The object directly
            // 1. One codePath
            // 1. One datePath
            // A datePath can alter a codePath (e.g. Claim C -> C.item I -> I.date =
            // Claim.item.date)
            // But not the other way around (?)
            // IOW, the where clause has the definitive type.
            for (DataRequirement where : wheres) {
                if (where.getType().equals(aliasName)) {
                    source.getCodeFilter().clear();

                    // Use the where to extend the source
                    if (where.hasDateFilter()) {
                        for (DataRequirementDateFilterComponent filter : where.getDateFilter()) {
                            String combinedPath = (pathPrepend != null ? pathPrepend + "." : "")
                                    + (filter.getPath() != null ? filter.getPath() : "");
                            source.addDateFilter().setPath(combinedPath).setValue(filter.getValue());
                        }
                    }

                    if (where.hasCodeFilter()) {
                        for (DataRequirementCodeFilterComponent filter : where.getCodeFilter()) {
                            String combinedPath = (pathPrepend != null ? pathPrepend + "." : "")
                                    + (filter.getPath() != null ? filter.getPath() : "");
                            source.addCodeFilter().setPath(combinedPath);
                            if (filter.hasCode()) {
                                source.getCodeFilterFirstRep().addCode().setCode(filter.getCodeFirstRep().getCode());

                            }
                            if (filter.hasValueSet()) {
                                source.getCodeFilterFirstRep().setValueSet(filter.getValueSet());
                            }
                        }
                    }
                }
            }
        }

        for (DataRequirement source : sources) {
            source.removeExtension("alias");
        }

        return sources;
    }

    // This could be generalized to multiple operands...
    private StackFrame simpleMergeTwoOperands() {
        StackFrame left = this.pop();
        StackFrame right = this.pop();

        if (left.size() > 1 || right.size() > 1) {
            throw new IllegalStateException("can't yet merge complex stack frames with multiple Ors");
        }

        DataRequirementMap lMap = left.isEmpty() ? null : left.getSingleOr();
        DataRequirementMap rMap = right.isEmpty() ? null : right.getSingleOr();

        if ((lMap != null && lMap.size() > 1) || (rMap != null && rMap.size() > 1)) {
            throw new IllegalStateException("can't yet merge stack frames with multiple Types");
        }

        DataRequirement l = lMap != null ? lMap.getSingle() : null;
        DataRequirement r = rMap != null ? rMap.getSingle() : null;

        StackFrame merged;
        if (l == null && r == null) {
            merged = StackFrame.empty();
        } else if (r == null) {
            merged = StackFrame.of(l);
        } else if (l == null) {
            merged = StackFrame.of(r);
        } else {
            DataRequirement mergedReq = this.union(l, r);

            merged = StackFrame.of(mergedReq);
        }

        return merged;
    }
}
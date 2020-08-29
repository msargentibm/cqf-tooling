package org.opencds.cqf.tooling.datarequirements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.LibraryRef;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.visiting.BaseVisitor;
import org.hl7.elm.r1.*;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DataRequirement;
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

    protected void push(StackFrame stackFrame) {
        this.stack.push(stackFrame);
    }

    protected StackFrame peek() {
        return this.stack.peek();
    }

    protected  StackFrame pop() {
        return this.stack.pop();
    }

    protected List<StackFrame> pop(int count) {
        List<StackFrame> stackFrames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            stackFrames.add(this.pop());
        }

        return stackFrames;
    }

    protected List<DataRequirement> defaultElementVisit(Element element) {
        if(!this.isQueryContext()) {
            return null;
        }

        if (element instanceof UnaryExpression) {
            this.push(StackFrameOperations.mergeFrames(this.pop(1), true));
        }

        if (element instanceof BinaryExpression) {
            this.push(StackFrameOperations.mergeFrames(this.pop(2), true));
        }

        if (element instanceof TernaryExpression) {
            this.push(StackFrameOperations.mergeFrames(this.pop(3), true));
        }

        return this.peek().flatten();
    }

    // visit implementations

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

    // TODO: The right thing to do when encountering something that's not disjunctive normal form is to
    // clear the code and date filters
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

        this.push(StackFrameOperations.simpleMerge(this.pop(), this.pop()));
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

        List<DataRequirement> wheres = DataRequirementOperations.applyWhere(sources, whereFrame.flatten());

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

    // TODO: Use the precision of the operator to determine how constant values should be represented.
    // We should most precision possible.
    public List<DataRequirement> visit(In in) throws Exception {
        if (!this.isQueryContext()) {
            return null;
        }

        this.push(StackFrameOperations.simpleMerge(this.pop(), this.pop()));
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

        this.push(StackFrameOperations.simpleMerge(this.pop(), this.pop()));
        return this.peek().flatten();
    }

    @Override
     public List<DataRequirement> visit(Abs aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Add aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(After aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Aggregate aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(AggregateClause aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(AliasRef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(AllTrue aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(AnyInCodeSystem aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(AnyInValueSet aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(AnyTrue aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(As aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Avg aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Before aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ByColumn aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ByDirection aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ByExpression aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CalculateAge aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CalculateAgeAt aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CanConvert aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CanConvertQuantity aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Case aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CaseItem aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Ceiling aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Children aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ChoiceTypeSpecifier aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Coalesce aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Code aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CodeDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CodeRef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CodeSystemDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(CodeSystemRef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Collapse aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Combine aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Concatenate aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Concept aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConceptDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConceptRef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Contains aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ContextDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Convert aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertQuantity aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToBoolean aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToDate aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToDateTime aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToDecimal aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToInteger aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToLong aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToQuantity aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToRatio aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToString aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ConvertsToTime aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Count aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Current aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Date aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(DateFrom aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(DateTime aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(DateTimeComponentFrom aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Descendents aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(DifferenceBetween aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Distinct aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Divide aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(DurationBetween aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(End aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Ends aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(EndsWith aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Equivalent aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Except aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Exp aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Expand aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ExpressionRef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Filter aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(First aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Flatten aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Floor aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ForEach aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(FunctionDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(GeometricMean aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Greater aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(GreaterOrEqual aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(HighBoundary aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IdentifierRef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(If aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Implies aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(InCodeSystem aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IncludeDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IncludeElement aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IncludedIn aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(org.hl7.elm.r1.Includes aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IndexOf aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Indexer aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Instance aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(InstanceElement aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Intersect aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Interval aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IntervalTypeSpecifier aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Is aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IsFalse aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IsNull aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(IsTrue aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Iteration aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Last aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(LastPositionOf aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Length aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Less aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(LessOrEqual aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(LetClause aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Library aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Library.CodeSystems aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Library.Codes aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Library.Concepts aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Library.Contexts aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Library.Includes aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Library.Parameters aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Library.Statements aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Library.Usings aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Library.ValueSets aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(ListTypeSpecifier aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Ln aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Log aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(LowBoundary aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Lower aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Matches aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Max aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(MaxValue aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Median aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Meets aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(MeetsAfter aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(MeetsBefore aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Message aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Min aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(MinValue aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Mode aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Modulo aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Multiply aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(NamedTypeSpecifier aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Negate aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Not aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(NotEqual aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Now aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Null aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(OperandDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(OperandRef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Or aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Overlaps aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(OverlapsAfter aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(OverlapsBefore aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ParameterDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(PointFrom aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(PopulationStdDev aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(PopulationVariance aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(PositionOf aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Power aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Precision aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Predecessor aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Product aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ProperContains aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ProperIn aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ProperIncludedIn aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ProperIncludes aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Quantity aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(QueryLetRef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Ratio aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Repeat aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ReplaceMatches aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ReturnClause aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Round aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(SameAs aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(SameOrAfter aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(SameOrBefore aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(SingletonFrom aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Size aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Slice aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Sort aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(SortClause aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Split aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(SplitOnMatches aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Start aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Starts aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(StartsWith aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(StdDev aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Substring aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(SubsumedBy aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Subsumes aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Subtract aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Successor aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Sum aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Time aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(TimeFrom aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(TimeOfDay aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Times aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(TimezoneOffsetFrom aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToBoolean aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToChars aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToConcept aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToDate aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToDateTime aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToDecimal aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToInteger aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToList aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToLong aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToQuantity aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToRatio aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToString aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ToTime aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Today aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Total aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Truncate aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(TruncatedDivide aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Tuple aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(TupleElement aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(TupleElementDefinition aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(TupleTypeSpecifier aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Union aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Upper aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(UsingDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(ValueSetDef aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Variance aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(VersionedIdentifier aBean)
        throws Exception
    {
        return null;
    }

    @Override
     public List<DataRequirement> visit(Width aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(With aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Without aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

    @Override
     public List<DataRequirement> visit(Xor aBean)
        throws Exception
    {
        return this.defaultElementVisit(aBean);
    }

}
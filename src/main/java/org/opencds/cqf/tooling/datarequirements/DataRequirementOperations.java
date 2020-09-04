package org.opencds.cqf.tooling.datarequirements;

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.hl7.elm.r1.TypeSpecifier;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementDateFilterComponent;
import org.opencds.cqf.tooling.types.TypeParser;

public class DataRequirementOperations {

    public static List<DataRequirement> applyWhere(List<DataRequirement> sources, List<DataRequirement> wheres) {
        for (DataRequirement source : sources) {
            String aliasName = source.getExtensionByUrl("alias").getValue().toString();

            if (source.hasCodeFilter() && source.hasDateFilter()) {
                throw new IllegalArgumentException("sources can't alias more than one property");
            }

            String pathPrepend = null;
            if (source.hasCodeFilter()) {
                pathPrepend = source.getCodeFilterFirstRep().getPath();
            }

            // An alias can be:
            // 1. The object directly
            // 1. One codePath
            // 1. One datePath - Actually, we're going to convert date filters at the end
            // A datePath can alter a codePath (e.g. Claim C -> C.item I -> I.date =
            // Claim.item.date)
            // But not the other way around (?)
            // IOW, the where clause has the definitive type.
            for (DataRequirement where : wheres) {
                if (where.getType().equals(aliasName)) {
                    source.getCodeFilter().clear();

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

                    if (where.hasDateFilter()) {
                        for (DataRequirementDateFilterComponent filter : where.getDateFilter()) {
                            String combinedPath = (pathPrepend != null ? pathPrepend + "." : "")
                                    + (filter.getPath() != null ? filter.getPath() : "");
                            source.addDateFilter().setPath(combinedPath);
                            if (filter.hasValue()) {
                                source.getDateFilterFirstRep().setValue(filter.getValue());

                            }

                            // if (filter.hasExtension("type")) {
                            //     source.getDateFilterFirstRep().addExtension("type", filter.getExtensionByUrl("type"));
                            // }
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

    /**
     * Creates a version of a DataRequirement with all Codes / ValueSets removed
     * @param dataRequirement
     * @return
     */
    public static DataRequirement clearCodeValues(DataRequirement dataRequirement) {
        Objects.requireNonNull(dataRequirement, "dataRequirement can not be null");
        if (dataRequirement.hasDateFilter()) {
            throw new IllegalArgumentException("dataRequirement has already had date filters converted");
        }

        DataRequirement newReq = dataRequirement.copy();
        if (newReq.hasCodeFilter()) {
            for (DataRequirementCodeFilterComponent drcfc : newReq.getCodeFilter()){
                drcfc.setCode(null);
                drcfc.setValueSet(null);
            }
        }

        return newReq;
    } 

    /***
     * Converts Code Filters that are typed appropriately into Date Filters
     * @param dataRequirement
     * @return
     */
    public static DataRequirement convertCodeFilters(DataRequirement dataRequirement) {
        Objects.requireNonNull(dataRequirement, "dataRequirement can not be null");
        if (dataRequirement.hasDateFilter()) {
            throw new IllegalArgumentException("dataRequirement has already had date filters converted");
        }
        
        // Don't want to alter the actual data requirements on the stack
        DataRequirement newReq = dataRequirement.copy();
        if (newReq.hasCodeFilter()) {
            for (int i = 0; i < newReq.getCodeFilter().size(); i++) {
                DataRequirementCodeFilterComponent codeFilterComponent = newReq.getCodeFilter().get(i);
                // Date
                if (codeFilterComponent.hasCode() && codeFilterComponent.hasValueSet()) {
                    continue;
                }

                String pathType = null;
                String codeType = null;

                if (codeFilterComponent.hasCode() && !codeFilterComponent.getCodeFirstRep().hasExtension("type")) {
                    throw new IllegalArgumentException("code filter code existed but was missing type extension");
                }
                else if (codeFilterComponent.hasCode()) {
                    codeType = codeFilterComponent.getCodeFirstRep().getExtensionString("type");
                }

                if (codeFilterComponent.hasPath() && !codeFilterComponent.hasExtension("type")) {
                    throw new IllegalArgumentException("code filter path existed but was missing type extension");
                }
                else if (codeFilterComponent.hasPath()) {
                    pathType = codeFilterComponent.getExtensionString("type");
                }

                if (codeFilterComponent.hasCode() && codeFilterComponent.hasPath() && (isTermporalType(codeType) ^ isTermporalType(pathType))) {
                    throw new IllegalArgumentException("code filter path and code types are not compatible");
                }

                if ((codeFilterComponent.hasCode() && isTermporalType(codeType)) || (codeFilterComponent.hasPath()&& isTermporalType(pathType))) {
                    newReq.getCodeFilter().remove(codeFilterComponent);

                    DataRequirementDateFilterComponent dateFilterComponent = newReq.addDateFilter();;
                    
                    if (codeFilterComponent.hasPath()) {
                        dateFilterComponent.setPath(codeFilterComponent.getPath());
                        dateFilterComponent.addExtension("type", codeFilterComponent.getExtensionByUrl("type").getValue());
                    }

                    if (codeFilterComponent.hasCode()) {
                        // Period, Duration, DateTime are possible options.
                        dateFilterComponent.setValue(TypeParser.parseFhirType(
                            codeFilterComponent.getCodeFirstRep().getExtensionString("type"), 
                            codeFilterComponent.getCodeFirstRep().getCode()));
                    }
                }
            }
        }

        return newReq;
    }


    // this should probably be using TypeSpecifier
    public static boolean isTemporalType(TypeSpecifier type) {
        // TODO..

        return true;
    }
    public static boolean isTermporalType(String type) {
        // TODO: intervals
        switch(type.toLowerCase()) {
            case "duration":
            case "period":
            case "date":
            case "datetime":
                return true;
        }

        return false;
    }

    // Dumb union for now.
    // Needs to be smart enough to detect duplicate paths for code and date filters
    // and merge
    // appropriately
    // TODO: https://cql.hl7.org/05-languagesemantics.html#artifact-data-requirements
    public static DataRequirement merge(DataRequirement left, DataRequirement right) {
        Objects.requireNonNull(left, "left can not be null");
        Objects.requireNonNull(right, "right can not be null");

        return oldMerge(left, right);

        // return newMerge(left, right);

    }
    private static DataRequirement oldMerge(DataRequirement left, DataRequirement right) {

        if (left.getType() != null && right.getType() != null && !left.getType().equals(right.getType())) {
            throw new IllegalArgumentException("if type is explicit left and right must be of same type");
        }

        left.setType(firstNonNull(left.getType(), right.getType()));

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

    // private static DataRequirement newMerge(DataRequirement left, DataRequirement right) {
    //     if (left.getType() != null && right.getType() != null && !left.getType().equals(right.getType())) {
    //         throw new IllegalArgumentException("if type is explicit left and right must be of same type");
    //     }

    //     DataRequirement newReq = new DataRequirement();
    //     newReq.setType(firstNonNull(left.getType(), right.getType()));

    //     List<DataRequirementCodeFilterComponent> codeFilters = new ArrayList<DataRequirementCodeFilterComponent>();
    //     codeFilters.addAll(left.getCodeFilter());
    //     codeFilters.addAll(right.getCodeFilter());

    //     //codeFilters = union(codeFilters);
    //     newReq.setCodeFilter(codeFilters);
        

    //     List<DataRequirementDateFilterComponent> dateFilters = new ArrayList<DataRequirementDateFilterComponent>();

    //     dateFilters.addAll(left.getDateFilter());
    //     dateFilters.addAll(right.getDateFilter());

    //     newReq.setDateFilter(dateFilters);

    //     return newReq;
    // }

    public static List<DataRequirement> groupUnion(List<DataRequirement> dataRequirements) {
        List<DataRequirement> mergedRequirements = new ArrayList<>();

        Map<String, List<DataRequirement>> groupedRequirements = dataRequirements.stream()
                .collect(groupingBy(DataRequirement::getType));

        for (Entry<String, List<DataRequirement>> entry : groupedRequirements.entrySet()) {
            DataRequirement mergedRequirement = union(entry.getValue());
            mergedRequirements.add(mergedRequirement);
        }

        return mergedRequirements;
    }

    public static DataRequirement union(List<DataRequirement> dataRequirements) {
        DataRequirement unioned = dataRequirements.get(0);

        for (int i = 1; i < dataRequirements.size(); i++) {
            DataRequirement next = dataRequirements.get(i);
            if (!next.getType().equals(unioned.getType())) {
                throw new IllegalArgumentException("attempted to union with dataRequirements of a different type");
            }

            unioned = merge(unioned, next);
        }

        return unioned;
    }
}
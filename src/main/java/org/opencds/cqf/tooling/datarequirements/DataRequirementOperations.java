package org.opencds.cqf.tooling.datarequirements;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementDateFilterComponent;

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

    // Dumb union for now.
    // Needs to be smart enough to detect duplicate paths for code and date filters
    // and merge
    // appropriately
    // TODO: https://cql.hl7.org/05-languagesemantics.html#artifact-data-requirements
    public static DataRequirement union(DataRequirement left, DataRequirement right) {

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

            unioned = union(unioned, next);
        }

        return unioned;
    }
}
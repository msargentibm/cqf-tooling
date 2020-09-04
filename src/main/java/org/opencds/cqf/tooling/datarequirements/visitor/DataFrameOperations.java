package org.opencds.cqf.tooling.datarequirements.visitor;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.DataRequirement;
import org.opencds.cqf.tooling.datarequirements.DataRequirementOperations;

public class DataFrameOperations {

    public static DataFrame mergeAnd(DataFrame left, DataFrame right) {
        List<DataRequirement> requirements = left.flatten();
        requirements.addAll(right.flatten());

        requirements = DataRequirementOperations.groupUnion(requirements);

        return DataFrame.of(requirements);
    }

    public static DataFrame mergeOr(DataFrame left, DataFrame right) {
        left.addAll(right);

        return left;
    }

    // This could be generalized to multiple operands...
    public static DataFrame simpleMerge(DataFrame left, DataFrame right) {

        if (left.size() > 1 || right.size() > 1) {
            throw new IllegalStateException("can't yet merge complex stack frames with multiple Ors");
        }

        DataRequirementMap lMap = left.isEmpty() ? null : left.asSingleOr();
        DataRequirementMap rMap = right.isEmpty() ? null : right.asSingleOr();

        if ((lMap != null && lMap.size() > 1) || (rMap != null && rMap.size() > 1)) {
            throw new IllegalStateException("can't yet merge stack frames with multiple Types");
        }

        DataRequirement l = lMap != null ? lMap.asSingle() : null;
        DataRequirement r = rMap != null ? rMap.asSingle() : null;

        DataFrame merged;
        if (l == null && r == null) {
            merged = DataFrame.empty();
        } else if (r == null) {
            merged = DataFrame.of(l);
        } else if (l == null) {
            merged = DataFrame.of(r);
        } else {
            DataRequirement mergedReq = DataRequirementOperations.merge(l, r);

            merged = DataFrame.of(mergedReq);
        }

        return merged;
    }

    /**
     * Merges frames into one
     * @param frames The frames to merge
     * @param clearCodes Whether the constant values should be removed (for example, if a transformation is not sargeable)
     * @return
     */
    public static DataFrame mergeFrames(List<DataFrame> frames, boolean clearCodes) {
        if (frames.size() < 1) {
            throw new IllegalArgumentException("frames must contain at least one DateFrame");
        }

        DataFrame current = frames.get(0);
        if (clearCodes) {
            current = clearCodeValues(current);
        }
        for (int i = 1; i < frames.size(); i++) {
            DataFrame next = frames.get(i);
            if (clearCodes) {
                next = clearCodeValues(next);
            }

            current = DataFrameOperations.simpleMerge(current, next);
        }

        return current;
    }

    public static DataFrame clearCodeValues(DataFrame frame) {
        DataFrame df = new DataFrame();

        for (DataRequirementMap map : frame) {
            DataRequirementMap newMap = new DataRequirementMap();
            for (Map.Entry<String, DataRequirement>  entry : map.entrySet()) {
                newMap.put(entry.getKey(), DataRequirementOperations.clearCodeValues(entry.getValue()));
            }
        }

        return df;
    }
}
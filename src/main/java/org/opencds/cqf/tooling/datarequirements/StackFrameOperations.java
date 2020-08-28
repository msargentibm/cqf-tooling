package org.opencds.cqf.tooling.datarequirements;

import java.util.List;

import org.hl7.fhir.r4.model.DataRequirement;

public class StackFrameOperations {

    public static StackFrame mergeAnd(StackFrame left, StackFrame right) {
        List<DataRequirement> requirements = left.flatten();
        requirements.addAll(right.flatten());

        requirements = DataRequirementOperations.groupUnion(requirements);

        return StackFrame.of(requirements);
    }

    public static StackFrame mergeOr(StackFrame left, StackFrame right) {
        StackFrame stackFrame = new StackFrame();

        stackFrame.addAll(left);
        stackFrame.addAll(right);

        return stackFrame;
    }

    // This could be generalized to multiple operands...
    public static StackFrame simpleMerge(StackFrame left, StackFrame right) {

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
            DataRequirement mergedReq = DataRequirementOperations.union(l, r);

            merged = StackFrame.of(mergedReq);
        }

        return merged;
    }

    /**
     * Merges frames into one
     * @param frames The frames to merge
     * @param stripConstants Whether the constant values should be removed (for example, if a transformation is not sargeable)
     * @return
     */
    public static StackFrame mergeFrames(List<StackFrame> frames, boolean stripConstants) {
        if (frames.size() < 1) {
            throw new IllegalArgumentException("frames must contain at least one StackFrame");
        }

        StackFrame current = frames.get(0);
        if (!stripConstants) {
            current = stripConstants(current);
        }
        for (int i = 1; i < frames.size(); i++) {
            StackFrame next = frames.get(i);
            if (!stripConstants) {
                next = stripConstants(next);
            }

            current = StackFrameOperations.simpleMerge(current, next);
        }

        return current;
    }

    // TODO: implement
    public static StackFrame stripConstants(StackFrame frame) {
        return frame;
    }
}
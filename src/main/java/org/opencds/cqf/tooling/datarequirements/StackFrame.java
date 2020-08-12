package org.opencds.cqf.tooling.datarequirements;

import static org.junit.Assume.assumeNoException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.DataRequirement;

public class StackFrame extends ArrayList<DataRequirementMap> {

    private static final StackFrame empty = new StackFrame();

    public static StackFrame of(DataRequirement... dataRequirements) {
        StackFrame sf = new StackFrame();
        sf.add(DataRequirementMap.of(dataRequirements));
        return sf;
    }

    public static StackFrame of(Collection<DataRequirement> dataRequirements) {
        StackFrame sf = new StackFrame();
        sf.add(DataRequirementMap.of(dataRequirements));
        return sf;
    }

    public static StackFrame ofOrMaps(DataRequirementMap... dataRequirementMaps) {
        StackFrame sf = new StackFrame();
        for (DataRequirementMap element : dataRequirementMaps) {
            sf.add(element);
        }

        return sf;
    }
    
    public static StackFrame ofOrMaps(Collection<DataRequirementMap> dataRequirementMaps) {
        StackFrame sf = new StackFrame();
        sf.addAll(dataRequirementMaps);
        return sf;
    }

    public static StackFrame empty() {
        return empty;
    }

    public List<DataRequirement> flatten() {
        List<DataRequirement> drs = new ArrayList<>();
        for (DataRequirementMap map : this) {
            for (Map.Entry<String, DataRequirement> entry : map.entrySet()) {
                drs.add(entry.getValue());
            }
        }

        return drs;
    }

    public Boolean hasSingleOr() {
        return this.size() == 1;
    }

    public DataRequirementMap getSingleOr() {
        if (!hasSingleOr()) {
            throw new IllegalArgumentException("stack frame must have exactly 1 or to getSingleOr");
        }

        return this.get(0);
    }
}
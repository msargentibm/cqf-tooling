package org.opencds.cqf.tooling.datarequirements.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hl7.fhir.r4.model.DataRequirement;

public class DataFrame extends ArrayList<DataRequirementMap> {
    private static final long serialVersionUID = 1L;

    private static final DataFrame empty = new DataFrame();

    public static DataFrame empty() {
        return empty;
    }

    public static DataFrame of(DataRequirement... dataRequirements) {
        DataFrame sf = new DataFrame();
        sf.add(DataRequirementMap.of(dataRequirements));
        return sf;
    }

    public static DataFrame of(Collection<DataRequirement> dataRequirements) {
        DataFrame sf = new DataFrame();
        sf.add(DataRequirementMap.of(dataRequirements));
        return sf;
    }

    public static DataFrame ofOrMaps(DataRequirementMap... dataRequirementMaps) {
        DataFrame sf = new DataFrame();
        for (DataRequirementMap element : dataRequirementMaps) {
            sf.add(element);
        }

        return sf;
    }

    public static DataFrame ofOrMaps(Collection<DataRequirementMap> dataRequirementMaps) {
        DataFrame sf = new DataFrame();
        sf.addAll(dataRequirementMaps);
        return sf;
    }

    public List<DataRequirement> flatten() {
        List<DataRequirement> drs = new ArrayList<>();
        for (DataRequirementMap map : this) {
            drs.addAll(map.values());
        }

        return drs;
    }

    public Boolean isSingleOr() {
        return this.size() == 1;
    }

    public DataRequirementMap asSingleOr() {
        if (!isSingleOr()) {
            throw new IllegalArgumentException("stack frame must have exactly 1 or to getSingleOr");
        }

        return this.get(0);
    }
}
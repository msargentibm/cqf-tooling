package org.opencds.cqf.tooling.datarequirements;

import java.util.Collection;
import java.util.HashMap;

import org.hl7.fhir.r4.model.DataRequirement;

public class DataRequirementMap extends HashMap<String, DataRequirement> {
    private static final long serialVersionUID = 1L;

    public static DataRequirementMap of(Collection<DataRequirement> dataRequirements) {
        return of(dataRequirements.toArray(new DataRequirement[dataRequirements.size()]));
    }

    public static DataRequirementMap of(DataRequirement... dataRequirements) {
        DataRequirementMap map = new DataRequirementMap();
        for (DataRequirement dr : dataRequirements) {
            if (map.containsKey(dr.getType())) {
                throw new IllegalArgumentException("dataRequirements must be normalized in order to construct a map");
            }

            map.put(dr.getType(), dr);
        }

        return map;
    }

    public DataRequirement getSingle() {
        if (this.size() == 0 || this.size() > 1) {
            throw new IllegalStateException("can't getSingle on a map unless there is exactly one item");
        }

        return this.get(this.keySet().toArray()[0]);
    }

    public Boolean hasSingleRequirement() {
        return this.size() == 1;
    }

}

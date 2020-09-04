package org.opencds.cqf.tooling.datarequirements.visitor;

import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementDateFilterComponent;

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

    public DataRequirement asSingle() {
        if (this.size() == 0 || this.size() > 1) {
            throw new IllegalStateException("can't getSingle on a map unless there is exactly one item");
        }

        return this.get(this.keySet().toArray()[0]);
    }

    public Boolean isSingle() {
        return this.size() == 1;
    }

     /**
     * @return type and value
     */
    public Pair<String, String> asConstantCode() {
        Coding coding = this.asSingle().getCodeFilterFirstRep().getCodeFirstRep();
        return Pair.of(coding.getExtensionString("type"), coding.getCode());
    }


    /**
     * @return type and value
     */
    public Pair<String, org.hl7.fhir.r4.model.Type> asConstantTemporal() {
        DataRequirementDateFilterComponent date = this.asSingle().getDateFilterFirstRep();
        return Pair.of(date.getExtensionString("type"), date.getValue());
    }
}

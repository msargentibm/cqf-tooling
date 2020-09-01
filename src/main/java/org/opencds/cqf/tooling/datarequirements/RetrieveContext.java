package org.opencds.cqf.tooling.datarequirements;

import java.util.Objects;

import org.hl7.fhir.DataRequirement;

// https://cql.hl7.org/05-languagesemantics.html#artifact-data-requirements
public class RetrieveContext {

    // First, create a retrieve context for each unique type of retrieve using the retrieve data type (and template identifier) for each retrieve.
    DataRequirement dr;
    RetrieveContext(DataRequirement dr) {
        this.dr = Objects.requireNonNull(dr, "dr can not be null");
        if (this.dr.getType() == null || this.dr.getType().getValue() == null) {
            throw new IllegalArgumentException("dr must have a type");
        }
    }

    public String getKey() {
        if (dr.getProfile() == null) {
            return this.dr.getType().getValue();
        }
        else {
            return (this.dr.getType().getValue() + "-" + this.dr.getProfile().getReference());
        }
    }


    // Next, for each retrieve, add the codes to the matching retrieve context (by data type), 
    // recording the associated date range, if any, for each code.

    // I interpret that to mean that for a given DataRequirement all the filters are ANDed together. For DRs:
    // Code.path.one = ALL
    // Code.path.two = "A"
    // Date.path.one = "2019-2020"

    // and

    // Code.path.one = ALL
    // Code.path.two = "A"
    // Date.path.one = "2021-2022"

    // As date ranges are recorded, they must be merged so that for each code in each retrieve context, no two date range intervals overlap or meet.

    // Code.path.one = ALL
    // Code.path.two = "A"
    // Date.path.one = "2019-2020"
    // Date.path.one = "2021-2022"

    //Once the date ranges for each code within each unique retrieve context are determined, the unique set of date ranges for all codes is calculated

    // Hmm.. nope. Doesn't seem the be the case. The date range isn't associated with a code. You also can't count on the them not representing an OR.
}
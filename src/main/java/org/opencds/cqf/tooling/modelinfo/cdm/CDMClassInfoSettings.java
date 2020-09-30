package org.opencds.cqf.tooling.modelinfo.cdm;

import java.util.HashMap;
import java.util.HashSet;

import org.opencds.cqf.tooling.modelinfo.ClassInfoSettings;


class CDMClassInfoSettings extends ClassInfoSettings {

    public CDMClassInfoSettings() {
        this.modelName = "CDM";
        this.modelPrefix = "CDM";
        this.helpersLibraryName = "FHIRHelpers";
        this.useCQLPrimitives = true;

        this.codeableTypes = new HashSet<String>() {
            {
                add("System.String");
                add("System.Code");
                add("System.Concept");
            }
        };

        this.urlToModel.put("http://hl7.org/fhir", "CDM");

        this.primitiveTypeMappings = new HashMap<String, String>() {
            {
                put("CDM.base64Binary", "System.String");
                put("CDM.boolean", "System.Boolean");
                put("CDM.canonical", "System.String");
                put("CDM.code", "System.String");
                put("CDM.date", "System.Date");
                put("CDM.dateTime", "System.DateTime");
                put("CDM.decimal", "System.Decimal");
                put("CDM.id", "System.String");
                put("CDM.instant", "System.DateTime");
                put("CDM.integer", "System.Integer");
                put("CDM.markdown", "System.String");
                put("CDM.oid", "System.String");
                put("CDM.positiveInt", "System.Integer");
                put("CDM.string", "System.String");
                put("CDM.time", "System.Time");
                put("CDM.unsignedInt", "System.Integer");
                put("CDM.uri", "System.String");
                put("CDM.url", "System.String");
                put("CDM.uuid", "System.String");
                put("CDM.xhtml", "System.String");
            }
        };

        this.cqlTypeMappings = new HashMap<String, String>() {
            {
                put("CDM.xsd:base64Binary", "System.String");
                put("CDM.base64Binary", "System.String");
                put("CDM.xsd:boolean", "System.Boolean");
                put("CDM.boolean", "System.Boolean");
                put("CDM.canonical", "System.String");
                put("CDM.xsd:token", "System.String");
                put("CDM.code", "System.String");
                put("CDM.xsd:gYear OR xsd:gYearMonth OR xsd:date", "System.Date");
                put("CDM.xsd:date", "System.Date");
                put("CDM.date", "System.Date");
                put("CDM.xsd:gYear OR xsd:gYearMonth OR xsd:date OR xsd:dateTime", "System.DateTime");
                put("CDM.dateTime", "System.DateTime");
                put("CDM.xsd:decimal OR xsd:double", "System.Decimal");
                put("CDM.decimal", "System.Decimal");
                put("CDM.id", "System.String");
                put("CDM.xsd:dateTime", "System.DateTime");
                put("CDM.instant", "System.DateTime");
                put("CDM.xsd:int", "System.Integer");
                put("CDM.integer", "System.Integer");
                put("CDM.markdown", "System.String");
                put("CDM.oid", "System.String");
                put("CDM.xsd:positiveInteger", "System.Integer");
                put("CDM.positiveInt", "System.Integer");
                put("CDM.xsd:string", "System.String");
                put("CDM.string", "System.String");
                put("CDM.xsd:time", "System.Time");
                put("CDM.time", "System.Time");
                put("CDM.xsd:nonNegativeInteger", "System.Integer");
                put("CDM.unsignedInt", "System.Integer");
                put("CDM.xsd:anyURI", "System.String");
                put("CDM.uri", "System.String");
                put("CDM.url", "System.String");
                put("CDM.uuid", "System.String");
                put("CDM.xhtml:div", "System.String");
                put("CDM.xhtml", "System.String");
                put("CDM.Coding", "System.Code");
                put("CDM.CodeableConcept", "System.Concept");
                put("CDM.Period", "Interval<System.DateTime>");
                put("CDM.Range", "Interval<System.Quantity>");
                put("CDM.Quantity", "System.Quantity");
                put("CDM.Age", "System.Quantity");
                put("CDM.Distance", "System.Quantity");
                put("CDM.SimpleQuantity", "System.Quantity");
                put("CDM.Duration", "System.Quantity");
                put("CDM.Count", "System.Quantity");
                put("CDM.MoneyQuantity", "System.Quantity");
                put("CDM.Money", "System.Decimal");
                put("CDM.Ratio", "System.Ratio");
            }
        };

/*
        this.typeNameMappings = new HashMap<String, String>() {
            {
                put("CarePlanProfile", "CarePlan");
                put("DiagnosticReportProfileLaboratoryReporting", "DiagnosticReport"); // TODO: Support synonyms?
                put("DiagnosticReportProfileNoteExchange", "DiagnosticReportNote");
                put("DocumentReferenceProfile", "DocumentReference");
                put("EncounterProfile", "Encounter");
                put("GoalProfile", "Goal");
                put("ImmunizationProfile", "Immunizataion");
                put("ImplantableDeviceProfile", "Device"); // TODO: Support synonyms?
                put("LaboratoryResultObservationProfile", "Observation"); // TODO: Support synonyms?
                put("MedicationProfile", "Medication");
                put("MedicationRequestProfile", "MedicationRequest");
                put("OrganizationProfile", "Organization");
                put("PatientProfile", "Patient");
                put("PediatricBMIforAgeObservationProfile", "PediatricBMIforAgeObservation");
                put("PediatricWeightForHeightObservationProfile", "PediatricWeightForHeightObservation");
                put("PractitionerProfile", "Practitioner");
                put("PractitionerRoleProfile", "PractitionerRole");
                put("ProcedureProfile", "Procedure");
                put("PulseOximetryProfile", "PulseOximetry");
                put("SmokingStatusProfile", "SmokingStatus");
            }
        };
 */

        this.primaryCodePath = new HashMap<String, String>() {
            {
                put("ActivityDefinition", "topic");
                put("AdverseEvent", "type");
                put("AllergyIntolerance", "code");
                put("Appointment", "serviceType");
                put("Basic", "code");
                put("CarePlan", "category");
                put("CareTeam", "category");
                put("ChargeItemDefinition", "code");
                put("Claim", "type");
                put("ClinicalImpression", "code");
                put("Communication", "category");
                put("CommunicationRequest", "category");
                put("Composition", "type");
                put("Condition", "code");
                put("Consent", "category");
                put("Coverage", "type");
                put("DetectedIssue", "category");
                put("Device", "type");
                put("DeviceMetric", "type");
                put("DeviceRequest", "codeCodeableConcept");
                put("DeviceUseStatement", "device.code");
                put("DiagnosticReport", "code");
                put("Encounter", "type");
                put("EpisodeOfCare", "type");
                put("ExplanationOfBenefit", "type");
                put("Flag", "code");
                put("Goal", "category");
                put("GuidanceResponse", "module");
                put("HealthcareService", "type");
                put("Immunization", "vaccineCode");
                put("Library", "topic");
                put("Measure", "topic");
                put("MeasureReport", "measure.topic");
                put("Medication", "code");
                put("MedicationAdministration", "medication");
                put("MedicationDispense", "medication");
                put("MedicationRequest", "medication");
                put("MedicationStatement", "medication");
                put("MessageDefinition", "event");
                put("Observation", "code");
                put("OperationOutcome", "issue.code");
                put("Procedure", "code");
                put("ProcedureRequest", "code");
                put("Questionnaire", "name");
                put("ReferralRequest", "type");
                put("RiskAssessment", "code");
                put("SearchParameter", "target");
                put("Sequence", "type");
                put("Specimen", "type");
                put("Substance", "code");
                put("SupplyDelivery", "type");
                put("SupplyRequest", "category");
                put("Task", "code");
            }
        };
    }
}
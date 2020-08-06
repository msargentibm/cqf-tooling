library DataRequirements

using FHIR version '4.0.0'

include FHIRHelpers version '4.0.0'
include org.opencds.cqf.tooling.datarequirements.DataRequirementsCommon called Common

valueset "ESRD Diagnosis" : 'http://fakeurl.com/ersd-diagnosis'
valueset "Hospice Encounter" : 'http://fakeurl.com/hospice-encounter'
valueset "HEDIS2020_Independent_Laboratory": 'com:testmi:valueset:xyz-33475'
valueset "ED": 'http://fakeurl.com/ed'

parameter "Measurement Period" Interval<DateTime>
  default Interval[@2019-01-01T00:00:00.0, @2020-01-01T00:00:00.0)

context Patient

/*
1.
DataRequirement
type: Observation
codeFilter: { path:  code, valueSet:  'http://fakeurl.com/ersd-diagnosis' }
*/
define "ESRD Observations":
  [Observation: "ESRD Diagnosis"]

/*
2.
DataRequirement
type: Claim
codeFilter: { path:  item.revenue, valueSet: 'http://fakeurl.com/hospice-encounter' }
dateFilter: { path: item.serviced.start }
*/
define "HospiceEncounterClaims":
  [Claim] CEncounter
  		where exists CEncounter.item IEncounter where IEncounter.revenue in "Hospice Encounter"
  		and date from IEncounter.serviced."start" in day of "Measurement Period"

/*
3.
DataRequirement
type: Condition
codeFilter: { path:  code, valueSet: 'http://fakeurl.com/hospice-encounter' }
dateFilter: { path: onset }
*/
define "HospiceEncounterConditions": [Condition: "Hospice Encounter"] HospiceEncounterACond
  		where IsActiveAndConfirmed(HospiceEncounterACond)
  		and date from HospiceEncounterACond.onset in day of "Measurement Period"


define function "IsActiveAndConfirmed"(c FHIR.Condition):
  true

/*
4.
DataRequirement
type: Claim
codeFilter: { path:  item.location, valueSet: 'com:testmi:valueset:xyz-33475' }
*/
define "LeadScreeningClaims":
  [Claim] C where exists(C.item I where I.location in "HEDIS2020_Independent_Laboratory")


/*
5. - Could potentially get a dateFilter too if "Measurement Period" were constant
DataRequirement
type: Claim
codeFilter: { path : item.careTeam }
dateFilter: { path : item.serviced.start }
*/
define "Has Nephrologist Claim Support":
 [Claim] C where IsNephrologistVisit(C)
return NephrologistVisitSupport(C)

define function "IsNephrologistVisit"(C Claim):
  exists ((ClaimAndItemsForClaim(C)) T
            let c: T.c,
                ci: T.ci
            where HasValidNephrologistProvider(C, ci.careTeamLinkId)
            and date from ci.serviced."start" in day of "Measurement Period")

define function "ClaimAndItemsForClaim"(C Claim):
   C.item CI
      let i : { careTeamLinkId : C.careTeam, serviced : CI.serviced}
    return { c: C, ci: i}

// Hmm.. this would probably be some filter on the practioner .
define function "HasValidNephrologistProvider"(c Claim, ct List<FHIR.Claim.CareTeam>):
  true

define function "NephrologistVisitSupport"(C Claim):
  true

/*
6. - Actually, a simpler version of the last one..
DataRequirement
type: Claim
codeFilter: { path : item.careTeam }
dateFilter: { path : item.serviced.start }
*/
define "Has Nephrologist Claim":
	 exists [Claim] C where IsNephrologistVisit(C)

/*
7. Could potentially add a value to the date filter if "i" were constant
DataRequirement
type: Procedure
codeFilter: { path: code, url : http://fakeurl.com/ed }
dateFilter: { path: performed.start }

DataRequirement
type: Claim
codeFilter: { path: id}
*/
define function EDFractureProcedureVisitsDuring(i Interval<DateTime>):
	[Procedure: ED] Proc
      let ci: ClaimItem(Proc)
      with "All Fracture Dates" Dt such that Dt = date from start of (Proc.performed as FHIR.Period)
      where (date from Proc.performed."start" in day of i)
    return VisitDescriptor(Proc.performed, 'ed')

define function VisitDescriptor(P FHIR.Period, V String):
 { period : P, type : V }

define function "ClaimIdForReference"(ref String):
  Split(ref, '#')[0]

define function "Claim"(ref String):
  First([Claim] C where EndsWith(ClaimIdForReference(ref), C.id))

define function "ClaimItem"(P Procedure):
  null

define "All Fracture Dates":
  { @2012-01-01  }

/*
8a. - Could potentially get a dateFilter too if "Measurement Period" were constant
DataRequirement
Patient
dateFilter : { path : birthDate }
*/

define "Patient Age": AgeInYearsAt(end of "Measurement Period")
define "In Age Range": "Patient Age" >= 67 and "Patient Age" <= 85

/*
8b.
DataRequirement
Patient
codeFilter : { path : gender,  code: 'female'}
*/
define "Is Female": Patient.gender = 'female'

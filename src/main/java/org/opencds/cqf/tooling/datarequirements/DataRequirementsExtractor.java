package org.opencds.cqf.tooling.datarequirements;

import java.util.Collections;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.hl7.elm.r1.Code;
import org.hl7.elm.r1.CodeDef;
import org.hl7.elm.r1.CodeRef;
import org.hl7.elm.r1.CodeSystemDef;
import org.hl7.elm.r1.CodeSystemRef;
import org.hl7.elm.r1.Concept;
import org.hl7.elm.r1.ConceptDef;
import org.hl7.elm.r1.ConceptRef;
import org.hl7.elm.r1.Expression;
import org.hl7.elm.r1.IncludeDef;
import org.hl7.elm.r1.ParameterDef;
import org.hl7.elm.r1.ParameterRef;
import org.hl7.elm.r1.Retrieve;
import org.hl7.elm.r1.ValueSetDef;
import org.hl7.elm.r1.ValueSetRef;
import org.hl7.elm.r1.VersionedIdentifier;

public class DataRequirementsExtractor {

    public org.hl7.fhir.r4.model.DataRequirement toDataRequirement(Retrieve retrieve, TranslatedLibrary library,
            LibraryManager libraryManager) {
        org.hl7.fhir.r4.model.DataRequirement dr = new org.hl7.fhir.r4.model.DataRequirement();

        dr.setType(org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes.fromCode(retrieve.getDataType().getLocalPart())
                .toCode());

        // Set profile if specified
        if (retrieve.getTemplateId() != null) {
            dr.setProfile(Collections.singletonList(new org.hl7.fhir.r4.model.CanonicalType(retrieve.getTemplateId())));
        }

        // Set code path if specified
        if (retrieve.getCodeProperty() != null) {
            org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent cfc = new org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent();

            cfc.setPath(retrieve.getCodeProperty());

            // TODO: Support retrieval when the target is a CodeSystemRef

            if (retrieve.getCodes() instanceof ValueSetRef) {
                ValueSetRef vsr = (ValueSetRef) retrieve.getCodes();
                cfc.setValueSet(toReference(resolveValueSetRef(vsr, library, libraryManager)));
            }

            if (retrieve.getCodes() instanceof org.hl7.elm.r1.ToList) {
                org.hl7.elm.r1.ToList toList = (org.hl7.elm.r1.ToList) retrieve.getCodes();
                resolveCodeFilterCodes(cfc, toList.getOperand(), library, libraryManager);
            }

            if (retrieve.getCodes() instanceof org.hl7.elm.r1.List) {
                org.hl7.elm.r1.List codeList = (org.hl7.elm.r1.List) retrieve.getCodes();
                for (Expression e : codeList.getElement()) {
                    resolveCodeFilterCodes(cfc, e, library, libraryManager);
                }
            }

            dr.getCodeFilter().add(cfc);
        }

        // TODO: Set date range filters if literal

        return dr;
    }

    private void resolveCodeFilterCodes(org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent cfc,
            Expression e, TranslatedLibrary library, LibraryManager libraryManager) {
        if (e instanceof org.hl7.elm.r1.CodeRef) {
            CodeRef cr = (CodeRef) e;
            cfc.addCode(toCoding(toCode(resolveCodeRef(cr, library, libraryManager)), library, libraryManager));
        }

        if (e instanceof org.hl7.elm.r1.Code) {
            cfc.addCode(toCoding((org.hl7.elm.r1.Code) e, library, libraryManager));
        }

        if (e instanceof org.hl7.elm.r1.ConceptRef) {
            ConceptRef cr = (ConceptRef) e;
            org.hl7.fhir.r4.model.CodeableConcept c = toCodeableConcept(
                    toConcept(resolveConceptRef(cr, library, libraryManager), library, libraryManager), library,
                    libraryManager);
            for (org.hl7.fhir.r4.model.Coding code : c.getCoding()) {
                cfc.addCode(code);
            }
        }

        if (e instanceof org.hl7.elm.r1.Concept) {
            org.hl7.fhir.r4.model.CodeableConcept c = toCodeableConcept((org.hl7.elm.r1.Concept) e, library,
                    libraryManager);
            for (org.hl7.fhir.r4.model.Coding code : c.getCoding()) {
                cfc.addCode(code);
            }
        }
    }

    private org.hl7.fhir.r4.model.Coding toCoding(Code code, TranslatedLibrary library, LibraryManager libraryManager) {
        CodeSystemDef codeSystemDef = resolveCodeSystemRef(code.getSystem(), library, libraryManager);
        org.hl7.fhir.r4.model.Coding coding = new org.hl7.fhir.r4.model.Coding();
        coding.setCode(code.getCode());
        coding.setDisplay(code.getDisplay());
        coding.setSystem(codeSystemDef.getId());
        coding.setVersion(codeSystemDef.getVersion());
        return coding;
    }

    private org.hl7.fhir.r4.model.CodeableConcept toCodeableConcept(Concept concept, TranslatedLibrary library,
            LibraryManager libraryManager) {
        org.hl7.fhir.r4.model.CodeableConcept codeableConcept = new org.hl7.fhir.r4.model.CodeableConcept();
        codeableConcept.setText(concept.getDisplay());
        for (Code code : concept.getCode()) {
            codeableConcept.addCoding(toCoding(code, library, libraryManager));
        }
        return codeableConcept;
    }

    // private String toReference(CodeSystemDef codeSystemDef) {
    //     return codeSystemDef.getId() + (codeSystemDef.getVersion() != null ? ("|" + codeSystemDef.getVersion()) : "");
    // }

    private String toReference(ValueSetDef valueSetDef) {
        return valueSetDef.getId() + (valueSetDef.getVersion() != null ? ("|" + valueSetDef.getVersion()) : "");
    }

    // TODO: Move to the CQL-to-ELM translator

    private org.hl7.elm.r1.Concept toConcept(ConceptDef conceptDef, TranslatedLibrary library,
            LibraryManager libraryManager) {
        org.hl7.elm.r1.Concept concept = new org.hl7.elm.r1.Concept();
        concept.setDisplay(conceptDef.getDisplay());
        for (org.hl7.elm.r1.CodeRef codeRef : conceptDef.getCode()) {
            concept.getCode().add(toCode(resolveCodeRef(codeRef, library, libraryManager)));
        }
        return concept;
    }

    private org.hl7.elm.r1.Code toCode(CodeDef codeDef) {
        return new org.hl7.elm.r1.Code().withCode(codeDef.getId()).withSystem(codeDef.getCodeSystem())
                .withDisplay(codeDef.getDisplay());
    }

    private org.hl7.elm.r1.CodeDef resolveCodeRef(CodeRef codeRef, TranslatedLibrary library,
            LibraryManager libraryManager) {
        // If the reference is to another library, resolve to that library
        if (codeRef.getLibraryName() != null) {
            library = resolveLibrary(codeRef.getLibraryName(), library, libraryManager);
        }

        return library.resolveCodeRef(codeRef.getName());
    }

    private org.hl7.elm.r1.ConceptDef resolveConceptRef(ConceptRef conceptRef, TranslatedLibrary library,
            LibraryManager libraryManager) {
        // If the reference is to another library, resolve to that library
        if (conceptRef.getLibraryName() != null) {
            library = resolveLibrary(conceptRef.getLibraryName(), library, libraryManager);
        }

        return library.resolveConceptRef(conceptRef.getName());
    }

    private CodeSystemDef resolveCodeSystemRef(CodeSystemRef codeSystemRef, TranslatedLibrary library,
            LibraryManager libraryManager) {
        if (codeSystemRef.getLibraryName() != null) {
            library = resolveLibrary(codeSystemRef.getLibraryName(), library, libraryManager);
        }

        return library.resolveCodeSystemRef(codeSystemRef.getName());
    }

    public ValueSetDef resolveValueSetRef(ValueSetRef valueSetRef, TranslatedLibrary library,
            LibraryManager libraryManager) {
        // If the reference is to another library, resolve to that library
        if (valueSetRef.getLibraryName() != null) {
            library = resolveLibrary(valueSetRef.getLibraryName(), library, libraryManager);
        }

        return library.resolveValueSetRef(valueSetRef.getName());
    }

    public ParameterDef resolveParameterRef(ParameterRef parameterRef, TranslatedLibrary library,
            LibraryManager libraryManager) {
        // If the reference is to another library, resolve to that library
        if (parameterRef.getLibraryName() != null) {
            library = resolveLibrary(parameterRef.getLibraryName(), library, libraryManager);
        }

        return library.resolveParameterRef(parameterRef.getName());
    }

    private TranslatedLibrary resolveLibrary(String localLibraryName, TranslatedLibrary library,
            LibraryManager libraryManager) {
        IncludeDef includeDef = library.resolveIncludeRef(localLibraryName);
        return resolveLibrary(libraryManager,
                new VersionedIdentifier().withId(includeDef.getPath()).withVersion(includeDef.getVersion()));
    }

    private TranslatedLibrary resolveLibrary(LibraryManager libraryManager, VersionedIdentifier libraryIdentifier) {
        if (libraryManager.getTranslatedLibraries().containsKey(libraryIdentifier.getId())) {
            return libraryManager.getTranslatedLibraries().get(libraryIdentifier.getId());
        }

        throw new IllegalArgumentException(
                String.format("Could not resolve reference to translated library %s", libraryIdentifier.getId()));
    }
}

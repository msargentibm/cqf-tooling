package org.opencds.cqf.tooling.modelinfo.cdm;


import java.util.Map;

import org.hl7.elm_modelinfo.r1.TypeSpecifier;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.opencds.cqf.tooling.modelinfo.ClassInfoBuilder;

public class CDMClassInfoBuilder extends ClassInfoBuilder {

    public CDMClassInfoBuilder(Map<String, StructureDefinition> structureDefinitions) {
        super(new CDMClassInfoSettings(), structureDefinitions);
    }

    @Override
    protected void innerBuild() {
        if (!this.settings.useCQLPrimitives) {
            System.out.println("Building Primitives");
            this.buildFor("CDM", (x -> x.getKind() == StructureDefinition.StructureDefinitionKind.PRIMITIVETYPE));
        }

        System.out.println("Building ComplexTypes");
        this.buildFor("CDM", (x -> x.getKind() == StructureDefinition.StructureDefinitionKind.COMPLEXTYPE && (x.getBaseDefinition() == null
                || !x.getBaseDefinition().equals("http://hl7.org/fhir/StructureDefinition/Extension"))));

        System.out.println("Building Resources");
        this.buildFor("CDM", (x -> x.getKind() == StructureDefinition.StructureDefinitionKind.RESOURCE
                && (!x.hasDerivation() || x.getDerivation() == StructureDefinition.TypeDerivationRule.SPECIALIZATION)));
    }

    @Override
    protected TypeSpecifier resolveContentReference(String modelName, String path) throws Exception {
        // This is necessary because USCore doesn't have a straight Observation type, so this content reference fails
//        if (path.equals("#Observation.referenceRange")) {
//            return resolveContentReference(modelName,"#LaboratoryResultObservationProfile.referenceRange");
//        }

        return super.resolveContentReference(modelName, path);
    }
}
package org.opencds.cqf.common.stu3;

import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.Main;
import org.opencds.cqf.common.BaseCqfmSoftwareSystemHelper;
import org.opencds.cqf.common.CqfmSoftwareSystem;

import java.util.ArrayList;
import java.util.List;

public class CqfmSoftwareSystemHelper extends BaseCqfmSoftwareSystemHelper {
    public <T extends DomainResource> void ensureSoftwareSystemExtensionAndDevice(T resource, List<CqfmSoftwareSystem> softwareSystems) {
        if (resource == null) {
            throw new IllegalArgumentException("No resource provided.");
        }

        String fhirType = resource.fhirType();
        if (!fhirType.equals("Library") && !fhirType.equals("Measure")) {
            throw new IllegalArgumentException(String.format("cqfm-softwaresystem extension is only supported for Library and Measure resources, not %s", fhirType));
        }

        if (softwareSystems != null && !softwareSystems.isEmpty()) {
            for (CqfmSoftwareSystem system : softwareSystems) {
                ensureSoftwareSystemExtensionAndDevice(resource, system);
            }
        }
    }

    public <T extends DomainResource> void ensureSoftwareSystemExtensionAndDevice(T resource, CqfmSoftwareSystem system) {
        String fhirType = resource.fhirType();
        if (!fhirType.equals("Library") && !fhirType.equals("Measure")) {
            throw new IllegalArgumentException(String.format("cqfm-softwaresystem extension is only supported for Library and Measure resources, not %s", fhirType));
        }

        if (this.getSystemIsValid(system)) {
            String systemReferenceID = "#" + system.getName();

            /* Extension */
            List<Extension> extensions = resource.getExtension();
            Extension softwareSystemExtension = null;
            for (Extension ext : extensions) {
                if (ext.getValue().fhirType().equals("Reference") && ((Reference) ext.getValue()).getReference().equals(systemReferenceID)) {
                    softwareSystemExtension = ext;
                }
            }

            if (softwareSystemExtension == null) {
                softwareSystemExtension = new Extension();
                softwareSystemExtension.setUrl(this.getCqfmSoftwareSystemExtensionUrl());
                Reference reference = new Reference();
                reference.setReference(systemReferenceID);
                softwareSystemExtension.setValue(reference);

                resource.addExtension(softwareSystemExtension);
            }

            /* Contained Device Resource */
            Device softwareDevice = null;
            for (Resource containedResource : resource.getContained()) {
                if (containedResource.getId().equals(systemReferenceID) && containedResource.getResourceType() == ResourceType.Device) {
                    softwareDevice = (Device) containedResource;
                }
            }

            if (softwareDevice == null) {
                softwareDevice = createSoftwareSystemDevice(system);
                resource.addContained(softwareDevice);
            } else {
                softwareDevice.setVersion(system.getVersion());
            }
        }
    }

    private Device createSoftwareSystemDevice(CqfmSoftwareSystem system) {
        Device device = null;

        if (this.getSystemIsValid(system)) {
            device = new Device();
            device.setId(system.getName());

            /* meta.profile */
            Meta meta = new Meta();
            meta.addProfile("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/device-softwaresystem-cqfm");
            device.setMeta(meta);

            device.setManufacturer(system.getName());

            /* type */
            Coding typeCoding = new Coding();
            typeCoding.setSystem("http://hl7.org/fhir/us/cqfmeasures/CodeSystem/software-system-type");
            typeCoding.setCode("tooling");

            List<Coding> typeCodingList = new ArrayList();
            typeCodingList.add(typeCoding);

            CodeableConcept type = new CodeableConcept();
            type.setCoding(typeCodingList);
            device.setType(type);

            /* version */
            String version = system.getVersion();
            device.setVersion(version);
        }

        return device;
    }

    /* cqf-tooling specific logic */
    private Device createCqfToolingDevice() {
        CqfmSoftwareSystem softwareSystem = new CqfmSoftwareSystem(this.getCqfToolingDeviceName(), Main.class.getPackage().getImplementationVersion());
        Device device = createSoftwareSystemDevice(softwareSystem);

        return device;
    }

    public <T extends DomainResource> void ensureCQFToolingExtensionAndDevice(T resource) {
        /* Contained Device Resource */
        Device cqfToolingDevice = null;
        for (Resource containedResource : resource.getContained()) {
            if (containedResource.getId().equals(this.getCqfToolingDeviceReferenceID()) && containedResource.getResourceType() == ResourceType.Device) {
                cqfToolingDevice = (Device)containedResource;
            }
        }

        if (cqfToolingDevice == null) {
            cqfToolingDevice = createCqfToolingDevice();
            resource.addContained(cqfToolingDevice);
        }
        else {
            String version = Main.class.getPackage().getImplementationVersion();
            cqfToolingDevice.setVersion(version);
        }
    }
}

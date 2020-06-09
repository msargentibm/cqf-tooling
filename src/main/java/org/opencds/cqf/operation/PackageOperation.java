package org.opencds.cqf.operation;

import org.opencds.cqf.Operation;
import org.opencds.cqf.parameter.PackageIGParameters;
import org.opencds.cqf.processor.IGProcessor;
import org.opencds.cqf.processor.argument.PackageIGArgumentProcessor;

public class PackageOperation extends Operation {
    public PackageOperation() {
    }

    @Override
    public void execute(String[] args) {
        PackageIGParameters params = null;
        try {
            params = new PackageIGArgumentProcessor().parseAndConvert(args);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        IGProcessor.packageIG(params);
    }
}
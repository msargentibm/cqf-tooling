package org.opencds.cqf.operation;

import org.opencds.cqf.Operation;
import org.opencds.cqf.parameter.TestIGParameters;
import org.opencds.cqf.processor.argument.TestIGArgumentsProcessor;
import org.opencds.cqf.processor.IGTestProcessor;
import org.opencds.cqf.utilities.PropertyUtils;

public class TestIGOperation extends Operation {
    public TestIGOperation() {
    }

    @Override
    public void execute(String[] args) {

        PropertyUtils.loadProperties(args);
        TestIGParameters params = null;
        try {
            params = new TestIGArgumentsProcessor().parseAndConvert(args);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        new IGTestProcessor().testIg(params);
    }
}
package org.opencds.cqf.operation;

import org.opencds.cqf.Operation;
import org.opencds.cqf.TestIGArgumentsProcessor;
import org.opencds.cqf.TestIGParameters;
import org.opencds.cqf.TestIGProcessor;

public class TestIGOperation extends Operation {
    public TestIGOperation() {
    }

    @Override
    public void execute(String[] args) {

        TestIGParameters params = null;
        try {
            params = new TestIGArgumentsProcessor().parseAndConvert(args);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        new TestIGProcessor().testIg(params);
    }
}
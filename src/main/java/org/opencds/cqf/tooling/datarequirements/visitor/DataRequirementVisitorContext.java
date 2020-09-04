package org.opencds.cqf.tooling.datarequirements.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DataRequirementVisitorContext {

    private Stack<DataFrame> stack = new Stack<>();

    public void push(DataFrame stackFrame) {
        this.stack.push(stackFrame);
    }

    public boolean empty() {
        return this.stack.empty();
    }

    public int size() {
        return this.stack.size();
    }

    public DataFrame peek() {
        return this.stack.peek();
    }

    public DataFrame pop() {
        return this.stack.pop();
    }

    public List<DataFrame> pop(int count) {
        List<DataFrame> stackFrames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            stackFrames.add(this.pop());
        }

        return stackFrames;
    }

    // Query levels
    private int querylevel = 0;

    public void enterQueryContext() {
        querylevel++;
    }

    public void exitQueryContext() {
        querylevel--;

        if (querylevel < 0) {
            throw new IllegalStateException("can't have negative query context levels");
        }
    }

    public Boolean isQueryContext() {
        return querylevel > 0;
    }
    
}


package demoecho;

import com.sap.aii.af.lib.ra.cci.XIInteractionSpec;
import javax.resource.NotSupportedException;

public class XIInteractionSpecImpl implements XIInteractionSpec {
    static final long serialVersionUID = 123L;
    private static final XITrace TRACE = new XITrace(XIInteractionSpecImpl.class.getName());
    protected String functionName;
    private Integer executionTimeout;
    protected int interactionVerb;

    public void setFunctionName(String functionName) throws NotSupportedException {
        String SIGNATURE = "setFunctionName(String)";
        TRACE.entering(SIGNATURE, new Object[]{functionName});
        if (!functionName.equals("Send") && !functionName.equals("Call")) {
            NotSupportedException nse = new NotSupportedException("Invalid function name: " + functionName);
            TRACE.throwing(SIGNATURE, nse);
            throw nse;
        } else {
            this.functionName = functionName;
            TRACE.exiting(SIGNATURE);
        }
    }

    public String getFunctionName() {
        String SIGNATURE = "getFunctionName()";
        TRACE.entering(SIGNATURE);
        TRACE.exiting(SIGNATURE);
        return this.functionName;
    }

    public void setExecutionTimeout(Integer timeout) throws NotSupportedException {
        String SIGNATURE = "setExecutionTimeout(Integer)";
        TRACE.entering(SIGNATURE, new Object[]{timeout});
        this.executionTimeout = timeout;
        if (timeout < 0) {
            NotSupportedException nse = new NotSupportedException("Invalid timeout: " + timeout);
            TRACE.throwing(SIGNATURE, nse);
            throw nse;
        } else {
            this.executionTimeout = timeout;
            TRACE.exiting(SIGNATURE);
        }
    }

    public Integer getExecutionTimeout() {
        String SIGNATURE = "getExecutionTimeout()";
        TRACE.entering(SIGNATURE);
        TRACE.exiting(SIGNATURE, this.executionTimeout);
        return this.executionTimeout;
    }

    public void setInteractionVerb(int interactionVerb) throws NotSupportedException {
        String SIGNATURE = "setInteractionVerb(int)";
        TRACE.entering(SIGNATURE, new Object[]{new Integer(interactionVerb)});
        this.interactionVerb = interactionVerb;
        if (interactionVerb >= 0 && interactionVerb <= 2) {
            TRACE.exiting(SIGNATURE);
        } else {
            NotSupportedException nse = new NotSupportedException("Invalid interaction verb: " + interactionVerb);
            TRACE.throwing(SIGNATURE, nse);
            throw nse;
        }
    }

    public int getInteractionVerb() {
        String SIGNATURE = "getInteractionVerb()";
        TRACE.entering(SIGNATURE);
        TRACE.exiting(SIGNATURE, new Integer(this.interactionVerb));
        return this.interactionVerb;
    }

    public boolean isValid() {
        String SIGNATURE = "validate()";
        TRACE.entering(SIGNATURE);
        boolean isValid = false;
        if (this.interactionVerb == 0 && this.functionName.equals("Send")) {
            isValid = true;
        }

        if (this.interactionVerb == 1 && this.functionName.equals("Call") && this.executionTimeout > 0) {
            isValid = true;
        }

        if (!this.functionName.equals("Send") && !this.functionName.equals("Call")) {
            isValid = true;
        }

        TRACE.exiting(SIGNATURE, new Boolean(isValid));
        return isValid;
    }
}

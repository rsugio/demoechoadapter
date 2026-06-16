
package demoecho;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.*;
import javax.resource.spi.IllegalStateException;

public class CCIConnection implements Connection {
    private static final XITrace TRACE = new XITrace(CCIConnection.class.getName());
    private SPIManagedConnection mc;

    CCIConnection(SPIManagedConnection mc) {
        String SIGNATURE = "CciConnection(SpiManagedConnection)";
        TRACE.entering(SIGNATURE, new Object[]{mc});
        this.mc = mc;
        TRACE.exiting(SIGNATURE);
    }

    public Interaction createInteraction() throws ResourceException {
        String SIGNATURE = "createInteraction()";
        TRACE.entering(SIGNATURE);
        if (this.mc == null) {
            throw new ResourceException("Connection is invalid");
        } else {
            CCIInteraction interaction = new CCIInteraction(this);
            TRACE.exiting(SIGNATURE);
            return interaction;
        }
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local Transaction not supported!!");
    }

    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException("ResultSet is not supported.");
    }

    public void close() throws ResourceException {
        String SIGNATURE = "close()";
        TRACE.entering(SIGNATURE);
        if (this.mc != null) {
            this.mc.removeCciConnection(this);
            this.mc.sendEvent(1, (Exception) null, this);
            this.mc = null;
            TRACE.exiting(SIGNATURE);
        }
    }

    public ConnectionMetaData getMetaData() throws ResourceException {
        String SIGNATURE = "getMetaData()";
        TRACE.entering(SIGNATURE);
        CCIConnectionMetaData cmd = new CCIConnectionMetaData(this.mc);
        TRACE.exiting(SIGNATURE);
        return cmd;
    }

    void associateConnection(SPIManagedConnection newMc) throws ResourceException {
        String SIGNATURE = "associateConnection(SPIManagedConnection newMc)";
        TRACE.entering(SIGNATURE);

        try {
            this.checkIfValid();
        } catch (ResourceException ex) {
            TRACE.catching(SIGNATURE, ex);
            throw new IllegalStateException("Connection is invalid");
        }

        this.mc.removeCciConnection(this);
        newMc.addCciConnection(this);
        this.mc = newMc;
        TRACE.exiting(SIGNATURE);
    }

    public SPIManagedConnection getManagedConnection() {
        String SIGNATURE = "getManagedConnection()";
        TRACE.entering(SIGNATURE);
        TRACE.exiting(SIGNATURE);
        return this.mc;
    }

    void checkIfValid() throws ResourceException {
        String SIGNATURE = "checkIfValid()";
        TRACE.entering(SIGNATURE);
        if (this.mc == null) {
            throw new ResourceException("Connection is invalid");
        } else {
            TRACE.exiting(SIGNATURE);
        }
    }

    void invalidate() {
        String SIGNATURE = "invalidate()";
        TRACE.entering(SIGNATURE);
        this.mc = null;
        TRACE.exiting(SIGNATURE);
    }
}

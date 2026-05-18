package demoecho;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import java.io.Serializable;

public class SPIConnectionManager implements ConnectionManager, Serializable {
    static final long serialVersionUID = 123L;
    private static final XITrace TRACE = new XITrace(SPIConnectionManager.class.getName());

    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info) throws ResourceException {
        String SIGNATURE = "allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)";
        TRACE.entering("allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)", new Object[]{mcf});
        ManagedConnection mc = mcf.createManagedConnection((Subject) null, info);
        Object cciConnection = mc.getConnection((Subject) null, info);
        TRACE.exiting("allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo info)");
        return cciConnection;
    }
}

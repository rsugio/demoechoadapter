
package demoecho;

import javax.resource.ResourceException;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.ManagedConnectionMetaData;

public class SPIManagedConnectionMetaData implements ManagedConnectionMetaData {
    private static final XITrace TRACE = new XITrace(SPIManagedConnectionMetaData.class.getName());
    private SPIManagedConnection mc;

    public SPIManagedConnectionMetaData(SPIManagedConnection mc) {
        this.mc = mc;
    }

    public String getEISProductName() throws ResourceException {
        return EchoAdapterConstants.raEis;
    }

    public String getEISProductVersion() throws ResourceException {
        return EchoAdapterConstants.adapterVersion;
    }

    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    public String getUserName() throws ResourceException {
        String SIGNATURE = "getUserName()";
        TRACE.entering("getUserName()");
        if (this.mc.isDestroyed()) {
            throw new IllegalStateException("ManagedConnection has been destroyed");
        } else {
            String userName = "fakeUserName";
//            if (this.mc.getPasswordCredential() != null) {
//                userName = this.mc.getPasswordCredential().getUserName();
//            }

            TRACE.exiting("getUserName()");
            return userName;
        }
    }
}

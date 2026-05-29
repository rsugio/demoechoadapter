
package demoecho;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;

public class CCIConnectionMetaData implements ConnectionMetaData {
    private static final XITrace TRACE = new XITrace(CCIConnectionMetaData.class.getName());
    private SPIManagedConnection mc;

    public CCIConnectionMetaData(SPIManagedConnection mc) {
        this.mc = mc;
    }

    public String getEISProductName() throws ResourceException {
        return EchoAdapterConstants.raEis;
    }

    public String getEISProductVersion() throws ResourceException {
        return EchoAdapterConstants.adapterVersion;
    }

    public String getUserName() throws ResourceException {
        String SIGNATURE = "getUserName()";
        TRACE.entering("getUserName()");
        String userName = null;
        if (this.mc.isDestroyed()) {
            throw new ResourceException("ManagedConnection is destroyed");
        } else {
//            PasswordCredential cred = this.mc.getPasswordCredential();
//            if (cred != null) {
//                userName = cred.getUserName();
//            }

            TRACE.entering("getUserName()");
            return "fakeUserName";
        }
    }
}

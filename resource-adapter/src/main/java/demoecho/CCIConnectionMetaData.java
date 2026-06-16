
package demoecho;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;

public class CCIConnectionMetaData implements ConnectionMetaData {
    private static final XITrace TRACE = new XITrace(CCIConnectionMetaData.class.getName());
    final SPIManagedConnection mc;

    public CCIConnectionMetaData(SPIManagedConnection mc) {
        this.mc = mc;
    }

    @Override
    public String getEISProductName() throws ResourceException {
        return EchoAdapterConstants.raEis;
    }

    @Override
    public String getEISProductVersion() throws ResourceException {
        return EchoAdapterConstants.adapterVersion;
    }

    @Override
    public String getUserName() throws ResourceException {
        return null;
    }

}

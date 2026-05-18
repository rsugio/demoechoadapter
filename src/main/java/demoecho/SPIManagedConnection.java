package demoecho;

import com.sap.aii.af.service.cpa.Channel;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.IllegalStateException;
import java.util.HashSet;
import java.util.Set;

public class SPIManagedConnection implements ManagedConnection {
    private static final XITrace TRACE = new XITrace(SPIManagedConnection.class.getName());
    private XIConnectionEventListenerManager cciListener;
    //    private PasswordCredential credential;
    private SPIManagedConnectionFactory mcf;
    private PrintWriter logWriter;
    private boolean supportsLocalTx;
    private boolean destroyed;
    private Set<CCIConnection> connectionSet;
    //private FileOutputStream physicalConnection;
    //    private String outFileNamePrefix = null;
    private String channelID = null;
    private Channel channel = null;
//    private String fileMode = null;
//    private String directory = null;
//    private String prefix = null;
//    private File outFile = null;
//    private boolean asmaGet = false;
//    private boolean asmaError = false;

    SPIManagedConnection(SPIManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID, Channel channel) throws ResourceException, NotSupportedException {
        String SIGNATURE = "SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)";
        TRACE.entering("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", new Object[]{mcf, credential, new Boolean(supportsLocalTx), channelID});
//        String outFileName = "(not set)";
//        String privKeyView = null;
//        String privKeyAlias = null;
        if (supportsLocalTx) {
            throw new NotSupportedException("Local transactions are not supported!");
        } else {
            this.mcf = mcf;
//            this.credential = credential;
            this.supportsLocalTx = supportsLocalTx;
            this.channelID = channelID;
            this.channel = channel;
            this.connectionSet = new HashSet();
            this.cciListener = new XIConnectionEventListenerManager(this);

//            try {
//                this.directory = channel.getValueAsString("fileOutDir");
//                this.prefix = channel.getValueAsString("fileOutPrefix");
//                this.fileMode = channel.getValueAsString("fileMode");
//                this.asmaGet = channel.getValueAsBoolean("enableDynConfigReceiver");
//                if (this.asmaGet) {
//                    this.asmaGet = channel.getValueAsBoolean("dynConfigJCAChannelID");
//                }
//
//                this.asmaError = channel.getValueAsBoolean("dynConfigFailOnMissingProperties");
//            } catch (Exception e) {
//                TRACE.catching("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", e);
//                TRACE.errorT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0008", "Cannot access the channel parameters of channel: " + channelID + ". Defaults will be set.");
//            }

//            if (this.directory == null || this.directory.length() == 0) {
//                TRACE.warningT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "Unable to determine output file directory. Take default: c:/temp");
//                this.directory = "c:/temp";
//            }
//
//            if (this.prefix == null || this.prefix.length() == 0) {
//                TRACE.warningT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "Unable to determine output file prefix. Take default: sample_ra_output");
//                this.prefix = "sample_ra_output";
//            }
//
//            if (this.fileMode == null || this.fileMode.length() == 0) {
//                TRACE.warningT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "Unable to determine output file mode. Take default: new");
//                this.fileMode = "new";
//            }
//
//            this.outFileNamePrefix = new String(this.directory + "/" + this.prefix);
            TRACE.debugT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "//TODO delete loc:93");

//            try {
//                privKeyView = channel.getValueAsString("secViewPrivateKey");
//                privKeyAlias = channel.getValueAsString("secAliasPrivateKey");
//                if (privKeyView != null && privKeyAlias != null) {
//                    TRACE.infoT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "Read configured private key now. View: {0} Alias: {1}", new Object[]{privKeyView, privKeyAlias});
//                    SAPSecurityResources secRes = SAPSecurityResources.getInstance();
//                    KeyStoreManager ksMgr = secRes.getKeyStoreManager(PermissionMode.SYSTEM_LEVEL, new String[]{"sap.com/com.sap.aii.adapter.sample.ra"});
//                    KeyStore ks = ksMgr.getKeyStore(privKeyView);
//                    ISsfProfile privKeyProf = ksMgr.getISsfProfile(ks, privKeyAlias, (String) null);
//                    PrivateKey privKey = privKeyProf.getPrivateKey();
//                    TRACE.infoT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "Got configured private key {0}", new Object[]{privKey.toString()});
//                } else if (privKeyView == null) {
//                    TRACE.debugT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "Private key won't be read since view is not configured.");
//                } else if (privKeyAlias == null) {
//                    TRACE.debugT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "Private key won't be read since alias is not configured.");
//                }
//            } catch (Exception e) {
//                TRACE.catching("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", e);
//                TRACE.errorT("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0009", "Unable to retrieve selected private key alias from channel configuration due to {0}", new Object[]{e.getMessage()});
//            }

//            try {
//                if (0 == this.fileMode.compareToIgnoreCase("replace")) {
//                    this.outFile = new File(this.outFileNamePrefix);
//                    this.physicalConnection = new FileOutputStream(this.outFile);
//                } else {
//                    outFileName = mcf.getOutFileName(this.outFileNamePrefix);
//                    this.physicalConnection = new FileOutputStream(outFileName);
//                }
//            } catch (Exception e) {
//                TRACE.catching("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)", e);
//                throw new ResourceException(e.getMessage());
//            }

            TRACE.exiting("SpiManagedConnection(ManagedConnectionFactory mcf, PasswordCredential credential, boolean supportsLocalTx, String channelID)");
        }
    }

    public void setSupportsLocalTx(boolean ltx) throws NotSupportedException {
        if (ltx) {
            throw new NotSupportedException("Local transactions are not supported!");
        } else {
            this.supportsLocalTx = ltx;
        }
    }

    public boolean getSupportsLocalTx() {
        return this.supportsLocalTx;
    }

    public void setManagedConnectionFactory(SPIManagedConnectionFactory mcf) {
        this.mcf = mcf;
    }

    public ManagedConnectionFactory getManagedConnectionFactory() {
        return this.mcf;
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo info) throws ResourceException {
        String SIGNATURE = "getConnection(Subject subject, ConnectionRequestInfo info)";
        TRACE.entering("getConnection(Subject subject, ConnectionRequestInfo info)", new Object[]{subject, info});
        this.checkIfDestroyed();
        CCIConnection cciConnection = new CCIConnection(this);
        this.addCciConnection(cciConnection);
        TRACE.exiting("getConnection(Subject subject, ConnectionRequestInfo info)");
        return cciConnection;
    }

    public void destroy() throws ResourceException {
        String SIGNATURE = "destroy()";
        TRACE.entering("destroy()");
        this.destroy(false);
        TRACE.exiting("destroy()");
    }

    void destroy(boolean fromMCF) throws ResourceException {
        String SIGNATURE = "destroy(boolean fromMCF)";
        TRACE.entering("destroy(boolean fromMCF)", new Object[]{new Boolean(fromMCF)});
        if (!this.destroyed) {
            try {
                this.destroyed = true;

                for (CCIConnection cciCon : this.connectionSet) {
                    cciCon.invalidate();
                }

                this.connectionSet.clear();
                //this.physicalConnection.close();
            } catch (Exception ex) {
                TRACE.catching("destroy(boolean fromMCF)", ex);
                throw new ResourceException(ex.getMessage());
            }
        }

        if (!fromMCF) {
            this.mcf.removeManagedConnection(this.channelID);
        }

        TRACE.exiting("destroy(boolean fromMCF)");
    }

    public void cleanup() throws ResourceException {
        String SIGNATURE = "cleanup()";
        TRACE.entering("cleanup()");

        try {
            this.checkIfDestroyed();

            for (CCIConnection cciCon : this.connectionSet) {
                cciCon.invalidate();
            }

            this.connectionSet.clear();
            //this.physicalConnection.close();
        } catch (Exception ex) {
            TRACE.catching("cleanup()", ex);
            throw new ResourceException(ex.getMessage());
        }

        TRACE.exiting("cleanup()");
    }

    public void associateConnection(Object connection) throws ResourceException {
        String SIGNATURE = "associateConnection(Object connection)";
        TRACE.entering("associateConnection(Object connection)");
        this.checkIfDestroyed();
        if (connection instanceof CCIConnection) {
            CCIConnection cciCon = (CCIConnection) connection;
            cciCon.associateConnection(this);
            TRACE.exiting("associateConnection(Object connection)");
        } else {
            IllegalStateException ise = new IllegalStateException("Invalid connection object: " + connection);
            TRACE.throwing("associateConnection(Object connection)", ise);
            throw ise;
        }
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        String SIGNATURE = "addConnectionEventListener(ConnectionEventListener listener)";
        TRACE.entering("addConnectionEventListener(ConnectionEventListener listener)");
        this.cciListener.addConnectorListener(listener);
        TRACE.exiting("addConnectionEventListener(ConnectionEventListener listener)");
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        String SIGNATURE = "removeConnectionEventListener(ConnectionEventListener listener)";
        TRACE.entering("removeConnectionEventListener(ConnectionEventListener listener)");
        this.cciListener.removeConnectorListener(listener);
        TRACE.exiting("removeConnectionEventListener(ConnectionEventListener listener)");
    }

    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("XA transaction not supported");
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local transaction not supported");
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        this.checkIfDestroyed();
        return new SPIManagedConnectionMetaData(this);
    }

    public void setLogWriter(PrintWriter out) throws ResourceException {
        String SIGNATURE = "setLogWriter(PrintWriter out)";
        TRACE.entering("setLogWriter(PrintWriter out)", new Object[]{out});
        this.logWriter = out;
        out.print("XI AF Sample Adapter has received a J2EE container log writer.");
        out.print("XI AF Sample Adapter will not use the J2EE container log writer. See the trace file for details.");
        TRACE.exiting("setLogWriter(PrintWriter out)");
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return this.logWriter;
    }

    boolean isDestroyed() {
        return this.destroyed;
    }

    public void sendEvent(int eventType, Exception ex) {
        this.cciListener.sendEvent(eventType, ex, (Object) null);
    }

    public void sendEvent(int eventType, Exception ex, Object connectionHandle) {
        this.cciListener.sendEvent(eventType, ex, connectionHandle);
    }

    public void addCciConnection(CCIConnection cciCon) {
        this.connectionSet.add(cciCon);
    }

    public void removeCciConnection(CCIConnection cciCon) {
        this.connectionSet.remove(cciCon);
    }

    public void start() throws ResourceException {
        this.mcf.startMCF();
    }

    public void stop() throws ResourceException {
        this.mcf.stopMCF();
    }

    private void checkIfDestroyed() throws ResourceException {
        if (this.destroyed) {
            throw new javax.resource.spi.IllegalStateException("Managed connection is closed");
        }
    }

    public String getChannelID() {
        return this.channelID;
    }
}

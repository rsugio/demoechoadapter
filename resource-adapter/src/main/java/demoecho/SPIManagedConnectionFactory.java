package demoecho;

import com.sap.aii.af.lib.mp.processor.ModuleProcessor;
import com.sap.aii.af.lib.mp.processor.ModuleProcessorFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.idmap.MessageIDMapper;
import com.sap.aii.af.service.monitor.impl.SAPResources;
import com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccess;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.exception.MessagingException;
import com.sap.guid.GUID;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;

public class SPIManagedConnectionFactory implements ManagedConnectionFactory, ManagedConnectionFactoryActivation, Serializable, Runnable {
    //implements ResourceAdapterAssociation -- в OData есть, смысла не нашёл
    private static final XITrace TRACE = new XITrace(SPIManagedConnectionFactory.class.getName());
    private static final String CLASSNAME = SPIManagedConnectionFactory.class.getSimpleName();
    private static final long serialVersionUID = 1L;
    private static final Object _synchronizer = new Object();
    private final GUID mcfLocalGuid;
    final CentralFileLog centralFileLogDirectory;
    final InitialContext _ctx;
    final SAPResources _msRes;
    private int threadStatus = 0;
    private final Timer _controlTimer = new Timer();
    private final Map<String, ManagedConnection> managedConnections = Collections.synchronizedMap(new HashMap<String, ManagedConnection>());
    private String adapterType = null;

    // ManagedConnectionFactory.setLogWriter(), ManagedConnectionFactory.getLogWriter()
    private PrintWriter logWriter;

    // в init(), ManagedConnectionFactoryActivation.stop(), ManagedConnectionFactoryActivation.run()
    private PublicAPIAccess _publicAPIAccess = null;
    private AuditAccess _auditAccess = null;
    private transient MessageIDMapper _messageIDMapper = null;
    private transient XIMessageFactoryImpl _xiMessageFactory = null;
    private XIConfiguration xIConfiguration = null;
    // ===================================
    private static int waitTime = 5000;
    private final int propWaitNum = 10;
    private final int propWaitTime = 1000;

    //откуда в конструкторе идёт
    public SPIManagedConnectionFactory() throws ResourceException {
        String SIGNATURE = CLASSNAME + "()";
        TRACE.entering(SIGNATURE);

        try {
            _ctx = new InitialContext();
            _msRes = (SAPResources) _ctx.lookup(SAPResources.JNDI_NAME);
            centralFileLogDirectory = new CentralFileLog(_ctx);
            mcfLocalGuid = new GUID();
            centralFileLogDirectory.log_init("вход в %s, thread=%s, mcfLocalGuid=%s", SIGNATURE, Thread.currentThread().getName(), mcfLocalGuid.toHexString());
        } catch (NamingException | IOException e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, SIGNATURE + " failed");
            throw new ResourceException(SIGNATURE, e.getCause());
        }
    }

    @Override   //ManagedConnectionFactoryActivation
    public void start() {
        String SIGNATURE = "start()";
        TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} is started now. ({1})", new Object[]{mcfLocalGuid.toHexString(), SPIManagedConnectionFactory.class.getClassLoader()});
        centralFileLogDirectory.log_init("%s.%s mcfLocalGuid=%s, classloader=%s", CLASSNAME, SIGNATURE, mcfLocalGuid.toHexString(), SPIManagedConnectionFactory.class.getClassLoader());

        try {
            this._publicAPIAccess = PublicAPIAccessFactory.getPublicAPIAccess();
            this._auditAccess = _publicAPIAccess.getAuditAccess();
            this._xiMessageFactory = new XIMessageFactoryImpl(EchoAdapterConstants.adapterType, EchoAdapterConstants.adapterNamespace);
        } catch (MessagingException | ResourceException e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "SOA.apt_sample.0035", "Unable to access the XI AF audit log. Reason: {0}. Adapter cannot not start the inbound processing!", e);
            TRACE.exiting(SIGNATURE);
            return;
        }

        try {
            this.startMCF();
            this.startTimer();
            TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} was started successfully.", new Object[]{mcfLocalGuid.toHexString()});
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0038", "Start of MCF failed. Reason: {0}", e.getMessage());
        }
        this._messageIDMapper = MessageIDMapper.getInstance();
        if (this._messageIDMapper == null) {
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT, "SOA.apt_sample.0036", "Gut null as MessageIDMapper singleton instance. Adapter cannot start the inbound processing!");
        } else {
//            try {
//                ClassUtil.setClassLoader("com.sap.aii.af.sample.module.ConvertCRLFfromToLF0", ConvertCRLFfromToLF0.class.getClassLoader());
//            } catch (Exception e) {
//                TRACE.catching(SIGNATURE, e);
//                TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0039", "Unable to register pojo modules. Reason: {0}", e.getMessage());
//            }
        }
        TRACE.exiting(SIGNATURE);
    }


    // ---------------------------------------------------------------------------------------------
    private ModuleProcessor lookUpModuleProcessor(int retryNum) throws ResourceException {
        String SIGNATURE = "lookUpModuleProcessor()";
        TRACE.entering(SIGNATURE);
        ModuleProcessor mp;

        try {
            mp = ModuleProcessorFactory.getModuleProcessor(true, retryNum, this.propWaitTime);
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0012", "Cannot get access to the XI AF module processor. Ejb might not have been started yet.");
            throw new ResourceException("Cannot get access to the XI AF module processor. Ejb might not have been started yet.");
        }

//        TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Lookup of XI AF MP entry ejb was succesfully.");
//        TRACE.exiting(SIGNATURE);
        return mp;
    }

    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        String SIGNATURE = "createConnectionFactory(ConnectionManager cxManager)";
        TRACE.entering(SIGNATURE, new Object[]{cm});
        CCIConnectionFactory factory = new CCIConnectionFactory(this, cm);
        TRACE.exiting(SIGNATURE);
        return factory;
    }

    public Object createConnectionFactory() throws ResourceException {
        String SIGNATURE = "createConnectionFactory()";
        TRACE.entering(SIGNATURE);
        CCIConnectionFactory factory = new CCIConnectionFactory(this, (ConnectionManager) null);
        TRACE.exiting(SIGNATURE);
        return factory;
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo info) throws ResourceException {
        String SIGNATURE = "createManagedConnection(Subject subject, ConnectionRequestInfo info)";
        TRACE.entering(SIGNATURE, new Object[]{subject, info});
        String channelID = null;
        Channel channel;
        SPIManagedConnection mc = null;
        if (!(info instanceof CCIConnectionRequestInfo)) {
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0013", "Received an unknown ConnectionRequestInfo. Cannot determine channelId!");
            ResourceException re = new ResourceException("Received an unknown ConnectionRequestInfo. Cannot determine channelId!");
            TRACE.throwing(SIGNATURE, re);
            throw re;
        } else {
            try {
                channelID = ((CCIConnectionRequestInfo) info).getChannelId();
                channel = CPAFactory.getInstance().getLookupManager().getCPAObject(CPAObjectType.CHANNEL, channelID);
            } catch (Exception e) {
                TRACE.catching(SIGNATURE, e);
                TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0014", "Cannot access the channel parameters of channel: " + channelID + ". Check whether the channel is stopped in the administrator console.");
                throw new ResourceException("Cannot access the channel parameters of channel: " + channelID + ". Check whether the channel is stopped in the administrator console.");
            }

            PasswordCredential credential = XISecurityUtilities.getPasswordCredential(this, subject, info);
            mc = new SPIManagedConnection(this, credential, false, channelID, channel);
            if (mc != null) {
                this.managedConnections.put(channelID, mc);
                TRACE.debugT("createManagedConnection(Subject subject, ConnectionRequestInfo info)", XIAdapterCategories.CONNECT_AF, "For channelID {0} this managed connection is stored: {1}", new Object[]{channelID, mc});
            }

            TRACE.exiting("createManagedConnection(Subject subject, ConnectionRequestInfo info)");
            return mc;
        }
    }

    void destroyManagedConnection(String channelID) {
        String SIGNATURE = "destroyManagedConnection(String channelID)";
        TRACE.entering(SIGNATURE, new Object[]{channelID});
        SPIManagedConnection mc;

        try {
            mc = (SPIManagedConnection) this.managedConnections.get(channelID);
            if (mc != null) {
                mc.sendEvent(1, (Exception) null, mc);
                this.managedConnections.remove(channelID);
                mc.destroy(true);
                TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "ManagedConnection for channel ID {0} found and destroyed.", new Object[]{channelID});
            } else {
                TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "ManagedConnection for channel ID {0} not found.", new Object[]{channelID});
            }
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0015", "Received exception during ManagedConnection destroy: " + e.getMessage());
        }

        TRACE.exiting(SIGNATURE);
    }

    void removeManagedConnection(String channelID) {
        String SIGNATURE = "removeManagedConnection(String channelID)";
        TRACE.entering(SIGNATURE, new Object[]{channelID});
        this.managedConnections.remove(channelID);
        TRACE.exiting(SIGNATURE);
    }

    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info) throws ResourceException {
        String SIGNATURE = "matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)";
        TRACE.entering("matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)", new Object[]{connectionSet, subject, info});
        SPIManagedConnection mcFound = null;
        CCIConnectionRequestInfo cciInfo = null;
        PasswordCredential pc = XISecurityUtilities.getPasswordCredential(this, subject, info);
        if (!(info instanceof CCIConnectionRequestInfo)) {
            TRACE.errorT("matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)", XIAdapterCategories.CONNECT_AF, "Unknown ConnectionRequestInfo parameter received. Cannot match connection");
            return null;
        } else {
            cciInfo = (CCIConnectionRequestInfo) info;
            Iterator it = connectionSet.iterator();

            while (it.hasNext() && mcFound == null) {
                Object obj = it.next();
                if (obj instanceof SPIManagedConnection) {
                    SPIManagedConnection mc = (SPIManagedConnection) obj;
                    if (!mc.isDestroyed()) {
                        ManagedConnectionFactory mcf = mc.getManagedConnectionFactory();
                        mcFound = mc;
                    } else {
                        TRACE.debugT("matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)", XIAdapterCategories.CONNECT, "Destroyed sample ManagedConnection in container set. Ignore.");
                    }
                } else {
                    TRACE.debugT("matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)", XIAdapterCategories.CONNECT, "This is not a sample ManagedConnection in container set. Ignore.");
                }
            }

            TRACE.exiting("matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo info)");
            return mcFound;
        }
    }

    @Override   //
    public void setLogWriter(PrintWriter out) throws ResourceException {
        String SIGNATURE = "setLogWriter(PrintWriter out)";
        TRACE.entering(SIGNATURE, new Object[]{out});
        out.printf("XI AF Sample Adapter has received a J2EE container log writer.");
        out.print("XI AF Sample Adapter will not use the J2EE container log writer. See the trace file for details.");
        TRACE.exiting(SIGNATURE);
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    public AuditAccess getAuditAccess() {
        return this._auditAccess;
    }

    public XIMessageFactoryImpl getXIMessageFactoryImpl() {
        return this._xiMessageFactory;
    }

    public boolean equals(Object obj) {
        TRACE.entering("equals(Object obj)", new Object[]{obj});
        boolean equal = false;
        if (obj instanceof SPIManagedConnectionFactory) {
            SPIManagedConnectionFactory other = (SPIManagedConnectionFactory) obj;
            boolean bns = EchoAdapterConstants.adapterNamespace.equals(other.getAdapterNamespace());
            boolean bn = EchoAdapterConstants.adapterType.equals(other.getAdapterType());
            boolean bam = EchoAdapterConstants.adapterAddressMode.equals(other.getAddressMode());
            equal = bns && bn && bam;
        }
        return equal;
    }

    public int hashCode() {
        TRACE.entering("hashCode()");
        return (EchoAdapterConstants.adapterNamespace + EchoAdapterConstants.adapterType + EchoAdapterConstants.adapterAddressMode).hashCode();
    }

    public String getAddressMode() {
        return EchoAdapterConstants.adapterAddressMode;
    }

    public void setAddressMode(String ignored) {
    }

    public void startMCF() throws ResourceException {
        String SIGNATURE = "startMCF()";
        TRACE.entering(SIGNATURE);
        if (this.threadStatus != 1) {
            try {
                this.threadStatus = 1;
                this._msRes.startRunnable(this);
            } catch (Exception e) {
                TRACE.catching(SIGNATURE, e);
                this.threadStatus = 2;
                TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0016", "Cannot start inbound message thread");
                ResourceException re = new ResourceException(e.getMessage());
                TRACE.throwing(SIGNATURE, re);
                throw re;
            }
        }

        TRACE.exiting(SIGNATURE);
    }

    public void stopMCF() throws ResourceException {
        String SIGNATURE = "stopMCF()";
        TRACE.entering(SIGNATURE);
        this.threadStatus = 2;

        try {
            synchronized (this) {
                this.notify();
                this.wait((long) (waitTime + 1000));
            }

            this.xIConfiguration.stop();
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0017", "Cannot stop inbound message thread. Reason: " + e.getMessage());
            ResourceException re = new ResourceException(e.getMessage());
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }

        TRACE.exiting("stopMCF()");
    }

    public void startTimer() {
        String SIGNATURE = "startTimer()";
        TRACE.entering(SIGNATURE);
        if (this.mcfLocalGuid != null) {
            this._controlTimer.scheduleAtFixedRate(new XIManagedConnectionFactoryController(this, this._ctx), 120000L, 60000L);
        }
        TRACE.exiting(SIGNATURE);
    }

    public void stopTimer() {
        String SIGNATURE = "stopTimer()";
        TRACE.entering(SIGNATURE);
        this._controlTimer.cancel();
        TRACE.exiting(SIGNATURE);
    }

    @Override
    public void run() {
        String SIGNATURE = "run()";
        TRACE.entering(SIGNATURE);
        String oldThreadName = Thread.currentThread().getName();
        String newThreadName = EchoAdapterConstants.adapterThread + mcfLocalGuid;

        try {
            Thread.currentThread().setName(newThreadName);
            TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Switched thread name to: {0}", new Object[]{newThreadName});
            centralFileLogDirectory.log_init("entering %s.%s, newThreadName=%s", CLASSNAME, SIGNATURE, newThreadName);
            boolean set = false;
            int numTry = 0;
            int pollTime = -1;

            while (!set && numTry < propWaitNum) {
                set = this.adapterType.equals(EchoAdapterConstants.adapterType);
                centralFileLogDirectory.log_init("try %d from %d", numTry, propWaitNum);
                numTry++;
                try {
                    Thread.sleep((long) propWaitTime);
                } catch (Exception e) {
                    TRACE.catching(SIGNATURE, e);
                }
            }

//            this.addressMode = EchoAdapterConstants.adapterAddressMode;
            this.adapterType = EchoAdapterConstants.adapterType;
//            this.adapterNamespace = EchoAdapterConstants.adapterNamespace;

            ModuleProcessor mp = null;

            try {
                mp = this.lookUpModuleProcessor(propWaitNum);
            } catch (Exception e) {
                TRACE.catching(SIGNATURE, e);
                TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Cannot instatiate the XI AF module processor bean. The inbound processing is stopped. Exception:" + e.toString());
                this.threadStatus = 2;
            }

            if (this.xIConfiguration == null) {
                try {
                    this.xIConfiguration = new XIConfiguration(EchoAdapterConstants.adapterType, EchoAdapterConstants.adapterNamespace);
                    this.xIConfiguration.init(this);
                } catch (Exception e) {
                    TRACE.catching(SIGNATURE, e);
                    TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0018", "Cannot instatiate the XI CPA handler. The inbound processing is stopped. Exception:" + e.toString());
                    this.threadStatus = 2;
                }
            }

            while (this.threadStatus == 1) {
                try {
                    synchronized (this) {
                        if (pollTime <= 0) {
                            this.wait((long) waitTime);
                        } else {
                            this.wait((long) pollTime);
                        }
                    }
                } catch (InterruptedException e1) {
                    TRACE.catching("run()", e1);
                    TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0020", "Inbound thread stopped. Received exception during wait period: " + e1.getMessage());
                    this.threadStatus = 2;
                }
            }
        } finally {
            Thread.currentThread().setName(oldThreadName);
            TRACE.debugT("run()", XIAdapterCategories.CONNECT_AF, "Switched thread name back to: {0}", new Object[]{oldThreadName});
        }

    }

    public GUID getMcfLocalGuid() {
        return mcfLocalGuid;
    }

    @Override   //ManagedConnectionFactoryActivation
    public void stop() {
        String SIGNATURE = "stop()";
        TRACE.entering(SIGNATURE);
        String controlledMcfGuid = mcfLocalGuid.toHexString();
        TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The running MCF with GUID {0} will be stopped now", new Object[]{controlledMcfGuid.toString()});
//        ClassUtil.removeClassLoader("com.sap.aii.af.sample.module.ConvertCRLFfromToLF0");

        try {
            stopMCF();
            stopTimer();
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
        }

        TRACE.infoT("stop()", XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} was stopped successfully.", new Object[]{controlledMcfGuid.toString()});
        TRACE.exiting("stop()");
    }

    public boolean isRunning() {
        return this.threadStatus == 1;
    }

    class XIManagedConnectionFactoryController extends TimerTask {
        private final SPIManagedConnectionFactory controlledMcf;

        public XIManagedConnectionFactoryController(SPIManagedConnectionFactory mcf, InitialContext ctx) {
            Objects.requireNonNull(mcf);
            this.controlledMcf = mcf;
        }

        public void run() {
            String SIGNATURE = XIManagedConnectionFactoryController.class.getSimpleName() + ".run()";
            centralFileLogDirectory.log_init("%s MCF with GUID %s is running", SIGNATURE, controlledMcf.mcfLocalGuid.toHexString());
        }
    }

    //TODO понять, есть ли какой-то интерфейс что это реализует?
    public void setAdapterNamespace(String ignored) {
    }

    public void setAdapterType(String n) {
        this.adapterType = n;
    }

    public String getAdapterNamespace() {
        return EchoAdapterConstants.adapterNamespace;
    }

    public String getAdapterType() {
        return this.adapterType;
    }


}

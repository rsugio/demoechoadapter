
package demoecho;

import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.processor.ModuleProcessor;
import com.sap.aii.af.lib.mp.processor.ModuleProcessorFactory;
import com.sap.aii.af.lib.ra.cci.XIAdapterException;
import com.sap.aii.af.lib.util.ClassUtil;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAInboundRuntimeLookupManager;
import com.sap.aii.af.service.administration.api.monitoring.ChannelDirection;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManager;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManagerFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContext;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContextFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessState;
import com.sap.aii.af.service.cpa.Binding;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;
import com.sap.aii.af.service.cpa.NormalizationManager;
import com.sap.aii.af.service.cpa.Party;
import com.sap.aii.af.service.cpa.Service;
import com.sap.aii.af.service.idmap.MessageIDMapper;
import com.sap.aii.af.service.resource.SAPAdapterResources;
import com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation;
import com.sap.engine.interfaces.messaging.api.DeliverySemantics;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.TextPayload;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.exception.RetryControlException;
import com.sap.engine.interfaces.messaging.api.exception.RetryMode;
import com.sap.guid.GUID;
import com.sap.transaction.TransactionTicket;
import com.sap.transaction.TxException;
import com.sap.transaction.TxManager;
import com.sap.transaction.TxRollbackException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.naming.InitialContext;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

public class SPIManagedConnectionFactory implements ManagedConnectionFactory, Serializable, Runnable, ManagedConnectionFactoryActivation {
    static final long serialVersionUID = -2387046407149571208L;
    private static final XITrace TRACE = new XITrace(SPIManagedConnectionFactory.class.getName());
    private AuditAccess audit = null;
    private GUID mcfLocalGuid = null;
    private Timer controlTimer = new Timer();
    private static final int TIMER_FIRST_RUN = 120000;
    private static final int TIMER_PERIOD = 60000;
    private static int fileCounter = 0;
    private static int waitTime = 5000;
    public static final String JNDI_NAME = EchoAdapterConstants.jndi;
    transient PrintWriter logWriter;
    private static Object synchronizer = new Object();
    private SAPAdapterResources msRes = null;
    private int threadStatus = 0;
    private static final int TH_INIT = 0;
    private static final int TH_STARTED = 1;
    private static final int TH_STOPPED = 2;
    private InitialContext ctx = null;
    private XIConfiguration xIConfiguration = null;
    private Map managedConnections = Collections.synchronizedMap(new HashMap());
    private transient MessageIDMapper messageIDMapper = null;
    private transient XIMessageFactoryImpl mf = null;
    static final String AS_ACTIVE = "active";
    static final String AS_INACTIVE = "inactive";
    private static final String AM_CPA = "CPA";
    private static final String AM_MSG = "MSG";
    private static final String ADDR_AGENCY_EAN = "009";
    private static final String ADDR_SCHEMA_GLN = "GLN";
    private String addressMode = null;
    private String adapterType = null;
    private String adapterNamespace = null;
    private int propWaitNum = 10;
    private int propWaitTime = 1000;
    static final String OUT_DIR = "c:/temp";
    static final String OUT_PREFIX = "sample_ra_output";
    static final String IN_DIR = "c:/temp";
    static final String IN_NAME = "sample_ra_input";
    private static final String PM_TEST = "test";
    private static final String PM_RENAME = "rename";
    static final String FM_NEW = "new";
    static final String FM_REPLACE = "replace";
    private static final String QOS_EO = "EO";
    private static final String QOS_EOIO = "EOIO";
    private static final String QOS_BE = "BE";
    private static final String ERR_NONE = "none";
    private static final String ERR_ROLLBACK = "rollback";
    static final String ASMA_NAME = "JCAChannelID";

    public SPIManagedConnectionFactory() throws ResourceException {
        String SIGNATURE = "SpiManagedConnectionFactory()";
        TRACE.entering("SpiManagedConnectionFactory()");

        try {
            this.ctx = new InitialContext();
            Object res = this.ctx.lookup("SAPAdapterResources");
            this.msRes = (SAPAdapterResources)res;
        } catch (Exception e) {
            TRACE.catching("SpiManagedConnectionFactory()", e);
            TRACE.errorT("SpiManagedConnectionFactory()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0011", "Access to XI AF MS resource failed. Adapter cannot be started.");
        }

        try {
            synchronized(synchronizer) {
                this.mcfLocalGuid = new GUID();
                TRACE.infoT("SpiManagedConnectionFactory()", XIAdapterCategories.CONNECT_AF, "This SPIManagedConnectionFactory has the GUID: " + this.mcfLocalGuid.toString());
            }
        } catch (Exception e) {
            TRACE.catching("SpiManagedConnectionFactory()", e);
            TRACE.debugT("SpiManagedConnectionFactory()", XIAdapterCategories.CONNECT_AF, "Creation of MCF GUID failed. Thus no periodic status report possible! Reason: " + e.getMessage());
        }

        TRACE.exiting("SpiManagedConnectionFactory()");
    }

    private void traceSample(String someText) {
        String SIGNATURE = "traceSample()";
        TRACE.entering("traceSample()");
        TRACE.entering("traceSample()", new Object[]{someText});

        try {
            XIAdapterException te = new XIAdapterException("Test exception only");
            TRACE.throwing("traceSample()", te);
            throw te;
        } catch (Exception e) {
            TRACE.catching("traceSample()", e);
            TRACE.debugT("traceSample()", "A test TraceException was catched and ignored!");
            TRACE.exiting("traceSample()");
            TRACE.exiting("traceSample()", "some return value");
        }
    }

    private ModuleProcessor lookUpModuleProcessor(int retryNum) throws ResourceException {
        String SIGNATURE = "lookUpModuleProcessor()";
        TRACE.entering("lookUpModuleProcessor()");
        ModuleProcessor mp = null;

        try {
            mp = ModuleProcessorFactory.getModuleProcessor(true, retryNum, this.propWaitTime);
        } catch (Exception e) {
            TRACE.catching("lookUpModuleProcessor()", e);
            TRACE.errorT("lookUpModuleProcessor()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0012", "Cannot get access to the XI AF module processor. Ejb might not have been started yet.");
            ResourceException re = new ResourceException("Cannot get access to the XI AF module processor. Ejb might not have been started yet.");
            throw re;
        }

        TRACE.debugT("lookUpModuleProcessor()", XIAdapterCategories.CONNECT_AF, "Lookup of XI AF MP entry ejb was succesfully.");
        TRACE.exiting("lookUpModuleProcessor()");
        return mp;
    }

    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        String SIGNATURE = "createConnectionFactory(ConnectionManager cxManager)";
        TRACE.entering("createConnectionFactory(ConnectionManager cxManager)", new Object[]{cm});
        CCIConnectionFactory factory = new CCIConnectionFactory(this, cm);
        TRACE.exiting("createConnectionFactory(ConnectionManager cxManager)");
        return factory;
    }

    public Object createConnectionFactory() throws ResourceException {
        String SIGNATURE = "createConnectionFactory()";
        TRACE.entering("createConnectionFactory()");
        CCIConnectionFactory factory = new CCIConnectionFactory(this, (ConnectionManager)null);
        TRACE.exiting("createConnectionFactory()");
        return factory;
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo info) throws ResourceException {
        String SIGNATURE = "createManagedConnection(Subject subject, ConnectionRequestInfo info)";
        TRACE.entering("createManagedConnection(Subject subject, ConnectionRequestInfo info)", new Object[]{subject, info});
        String channelID = null;
        Channel channel = null;
        SPIManagedConnection mc = null;
        if (!(info instanceof CCIConnectionRequestInfo)) {
            TRACE.errorT("createManagedConnection(Subject subject, ConnectionRequestInfo info)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0013", "Received an unknown ConnectionRequestInfo. Cannot determine channelId!");
            ResourceException re = new ResourceException("Received an unknown ConnectionRequestInfo. Cannot determine channelId!");
            TRACE.throwing("createManagedConnection(Subject subject, ConnectionRequestInfo info)", re);
            throw re;
        } else {
            try {
                channelID = ((CCIConnectionRequestInfo)info).getChannelId();
                channel = (Channel)CPAFactory.getInstance().getLookupManager().getCPAObject(CPAObjectType.CHANNEL, channelID);
            } catch (Exception e) {
                TRACE.catching("createManagedConnection(Subject subject, ConnectionRequestInfo info)", e);
                TRACE.errorT("createManagedConnection(Subject subject, ConnectionRequestInfo info)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0014", "Cannot access the channel parameters of channel: " + channelID + ". Check whether the channel is stopped in the administrator console.");
                ResourceException re = new ResourceException("Cannot access the channel parameters of channel: " + channelID + ". Check whether the channel is stopped in the administrator console.");
                throw re;
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

    void destroyManagedConnection(String channelID) throws ResourceException {
        String SIGNATURE = "destroyManagedConnection(String channelID)";
        TRACE.entering("destroyManagedConnection(String channelID)", new Object[]{channelID});
        SPIManagedConnection mc = null;

        try {
            mc = (SPIManagedConnection)this.managedConnections.get(channelID);
            if (mc != null) {
                mc.sendEvent(1, (Exception)null, mc);
                this.managedConnections.remove(channelID);
                mc.destroy(true);
                TRACE.debugT("destroyManagedConnection(String channelID)", XIAdapterCategories.CONNECT_AF, "ManagedConnection for channel ID {0} found and destroyed.", new Object[]{channelID});
            } else {
                TRACE.warningT("destroyManagedConnection(String channelID)", XIAdapterCategories.CONNECT_AF, "ManagedConnection for channel ID {0} not found.", new Object[]{channelID});
            }
        } catch (Exception e) {
            TRACE.catching("destroyManagedConnection(String channelID)", e);
            TRACE.errorT("destroyManagedConnection(String channelID)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0015", "Received exception during ManagedConnection destroy: " + e.getMessage());
        }

        TRACE.exiting("destroyManagedConnection(String channelID)");
    }

    void removeManagedConnection(String channelID) {
        String SIGNATURE = "removeManagedConnection(String channelID)";
        TRACE.entering("removeManagedConnection(String channelID)", new Object[]{channelID});
        this.managedConnections.remove(channelID);
        TRACE.exiting("removeManagedConnection(String channelID)");
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
            cciInfo = (CCIConnectionRequestInfo)info;
            Iterator it = connectionSet.iterator();

            while(it.hasNext() && mcFound == null) {
                Object obj = it.next();
                if (obj instanceof SPIManagedConnection) {
                    SPIManagedConnection mc = (SPIManagedConnection)obj;
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

    public void setLogWriter(PrintWriter out) throws ResourceException {
        String SIGNATURE = "setLogWriter(PrintWriter out)";
        TRACE.entering("setLogWriter(PrintWriter out)", new Object[]{out});
        out.print("XI AF Sample Adapter has received a J2EE container log writer.");
        out.print("XI AF Sample Adapter will not use the J2EE container log writer. See the trace file for details.");
        this.logWriter = out;
        TRACE.exiting("setLogWriter(PrintWriter out)");
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return this.logWriter;
    }

    public AuditAccess getAuditAccess() {
        return this.audit;
    }

    public XIMessageFactoryImpl getXIMessageFactoryImpl() {
        return this.mf;
    }

    public String getOutFileName(String outFileNamePrefix) {
        String SIGNATURE = "getFileName()";
        TRACE.entering("getFileName()");
        int cnt = 0;
        String fileName = null;
        synchronized(synchronizer) {
            cnt = fileCounter++;
        }

        fileName = new String(outFileNamePrefix + "." + Integer.toString(cnt) + ".txt");
        TRACE.debugT("getFileName()", XIAdapterCategories.CONNECT, "Output file name =" + fileName);
        TRACE.exiting("getFileName()");
        return fileName;
    }

    public boolean equals(Object obj) {
        String SIGNATURE = "equals(Object obj)";
        TRACE.entering("equals(Object obj)", new Object[]{obj});
        boolean equal = false;
        if (obj instanceof SPIManagedConnectionFactory) {
            SPIManagedConnectionFactory other = (SPIManagedConnectionFactory)obj;
            if (this.adapterNamespace.equals(other.getAdapterNamespace()) && this.adapterType.equals(other.getAdapterType()) && this.addressMode.equals(other.getAddressMode())) {
                equal = true;
            }
        }

        TRACE.exiting("equals(Object obj)");
        return equal;
    }

    public int hashCode() {
        String SIGNATURE = "hashCode()";
        TRACE.entering("hashCode()");
        int hash = 0;
        String propset = this.adapterNamespace + this.adapterType + this.addressMode;
        hash = propset.hashCode();
        TRACE.exiting("hashCode()");
        return hash;
    }

    public static int getFileCounter() {
        return fileCounter;
    }

    public String getAddressMode() {
        String SIGNATURE = "getAddressMode()";
        TRACE.entering("getAddressMode()");
        TRACE.debugT("getAddressMode()", XIAdapterCategories.CONNECT, "Address determination mode =" + this.addressMode);
        TRACE.exiting("getAddressMode()");
        return this.addressMode;
    }

    public void setAddressMode(String addressMode) {
        this.addressMode = addressMode;
    }

    public void startMCF() throws ResourceException {
        String SIGNATURE = "startMCF()";
        TRACE.entering("startMCF()");
        if (this.threadStatus != 1) {
            try {
                this.threadStatus = 1;
                this.msRes.startRunnable(this);
            } catch (Exception e) {
                TRACE.catching("startMCF()", e);
                this.threadStatus = 2;
                TRACE.errorT("startMCF()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0016", "Cannot start inbound message thread");
                ResourceException re = new ResourceException(e.getMessage());
                TRACE.throwing("startMCF()", re);
                throw re;
            }
        }

        TRACE.exiting("startMCF()");
    }

    public void stopMCF() throws ResourceException {
        String SIGNATURE = "stopMCF()";
        TRACE.entering("stopMCF()");
        this.threadStatus = 2;

        try {
            synchronized(this) {
                this.notify();
                this.wait((long)(waitTime + 1000));
            }

            this.xIConfiguration.stop();
        } catch (Exception e) {
            TRACE.catching("stopMCF()", e);
            TRACE.errorT("stopMCF()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0017", "Cannot stop inbound message thread. Reason: " + e.getMessage());
            ResourceException re = new ResourceException(e.getMessage());
            TRACE.throwing("stopMCF()", re);
            throw re;
        }

        TRACE.exiting("stopMCF()");
    }

    public void startTimer() {
        String SIGNATURE = "startTimer()";
        TRACE.entering("startTimer()");
        if (this.mcfLocalGuid != null) {
            try {
                this.controlTimer.scheduleAtFixedRate(new XIManagedConnectionFactoryController(this, this.ctx), 120000L, 60000L);
            } catch (Exception e) {
                TRACE.catching("startTimer()", e);
                TRACE.debugT("startTimer()", XIAdapterCategories.CONNECT_AF, "Creation of MCF controller failed. No periodic MCF status reports available! Reason: " + e.getMessage());
            }
        }

        TRACE.exiting("startTimer()");
    }

    public void stopTimer() {
        String SIGNATURE = "stopTimer()";
        TRACE.entering("stopTimer()");
        this.controlTimer.cancel();
        TRACE.exiting("stopTimer()");
    }

    public void run() {
        String SIGNATURE = "run()";
        TRACE.entering("run()");
        String oldThreadName = Thread.currentThread().getName();
        String newThreadName = "XI AF Sample Adapter MCF " + this.mcfLocalGuid;

        try {
            Thread.currentThread().setName(newThreadName);
            TRACE.debugT("run()", XIAdapterCategories.CONNECT_AF, "Switched thread name to: {0}", new Object[]{newThreadName});
            boolean notSet = true;
            int numTry = 0;
            int pollTime = -1;

            while(notSet && numTry < this.propWaitNum) {
                if (this.addressMode != null && this.adapterType != null && this.adapterNamespace != null) {
                    notSet = false;
                }

                ++numTry;
                TRACE.debugT("run()", XIAdapterCategories.CONNECT_AF, "MCF waits for setter completion. Try: {0} of {1}.", new Object[]{Integer.toString(numTry), Integer.toString(this.propWaitNum)});

                try {
                    Thread.sleep((long)this.propWaitTime);
                } catch (Exception e) {
                    TRACE.catching("run()", e);
                }
            }

            if (this.addressMode == null) {
                this.addressMode = "CPA";
            }

            if (this.adapterType == null) {
                this.adapterType = EchoAdapterConstants.adapterType;
            }

            if (this.adapterNamespace == null) {
                this.adapterNamespace = EchoAdapterConstants.adapterNamespace;
            }

            ModuleProcessor mp = null;

            try {
                mp = this.lookUpModuleProcessor(this.propWaitNum);
            } catch (Exception e) {
                TRACE.catching("run()", e);
                TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF, "Cannot instatiate the XI AF module processor bean. The inbound processing is stopped. Exception:" + e.toString());
                this.threadStatus = 2;
            }

            if (this.xIConfiguration == null) {
                try {
                    this.xIConfiguration = new XIConfiguration(this.adapterType, this.adapterNamespace);
                    this.xIConfiguration.init(this);
                } catch (Exception e) {
                    TRACE.catching("run()", e);
                    TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0018", "Cannot instatiate the XI CPA handler. The inbound processing is stopped. Exception:" + e.toString());
                    this.threadStatus = 2;
                }
            }

            while(this.threadStatus == 1) {
                try {
                    LinkedList channels = this.xIConfiguration.getCopy(Direction.INBOUND);

                    for(int i = 0; i < channels.size(); ++i) {
                        Channel channel = (Channel)channels.get(i);

                        try {
                            String directory = null;
                            String name = null;
                            String processMode = null;
                            String qos = null;
                            String psec = null;
                            String pmsec = null;
                            String raiseError = null;
                            String channelAddressMode = null;
                            boolean set_asma = false;

                            try {
                                directory = channel.getValueAsString("fileInDir");
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            try {
                                name = channel.getValueAsString("fileInName");
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            try {
                                processMode = channel.getValueAsString("processMode");
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            try {
                                qos = channel.getValueAsString("qos");
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            try {
                                psec = channel.getValueAsString("filePollInterval");
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            try {
                                pmsec = channel.getValueAsString("filePollIntervalMsecs");
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            try {
                                raiseError = channel.getValueAsString("raiseError");
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            try {
                                channelAddressMode = channel.getValueAsString("channelAddressMode");
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            try {
                                set_asma = channel.getValueAsBoolean("enableDynConfigSender");
                                if (set_asma) {
                                    set_asma = channel.getValueAsBoolean("dynConfigJCAChannelID");
                                }
                            } catch (Exception e) {
                                TRACE.catching("run()", e);
                            }

                            int ptime = 0;
                            if (psec != null && psec.length() > 0) {
                                ptime = Integer.valueOf(psec) * 1000;
                            }

                            if (pmsec != null && pmsec.length() > 0) {
                                ptime += Integer.valueOf(pmsec);
                            }

                            if (pollTime < 0 || ptime < pollTime) {
                                pollTime = ptime;
                            }

                            if (directory == null || directory.length() == 0) {
                                TRACE.warningT("run()", XIAdapterCategories.CONNECT_AF, "Unable to determine input file directory. Take default: c:/temp");
                                directory = "c:/temp";
                            }

                            if (name == null || name.length() == 0) {
                                TRACE.warningT("run()", XIAdapterCategories.CONNECT_AF, "Unable to determine input file prefix. Take default: sample_ra_input");
                                name = "sample_ra_input";
                            }

                            if (processMode == null || processMode.length() == 0) {
                                TRACE.warningT("run()", XIAdapterCategories.CONNECT_AF, "Unable to determine processing mode. Take default: test");
                                processMode = "test";
                            }

                            if (qos == null || qos.length() == 0) {
                                TRACE.warningT("run()", XIAdapterCategories.CONNECT_AF, "Unable to determine QOS. Take default: EO");
                                qos = "EO";
                            }

                            if (raiseError == null || raiseError.length() == 0) {
                                TRACE.warningT("run()", XIAdapterCategories.CONNECT_AF, "Unable to determine error raise condition. Take default: none");
                                raiseError = "none";
                            }

                            if (channelAddressMode == null || channelAddressMode.length() == 0) {
                                TRACE.warningT("run()", XIAdapterCategories.CONNECT_AF, "Unable to determine address mode. Take default from JCA property: " + this.addressMode);
                                channelAddressMode = this.addressMode;
                            }

                            String completeName = directory + "/" + name;
                            this.sendMessageFromFile(completeName, channel, processMode, qos, raiseError, channelAddressMode, set_asma);
                        } catch (Exception e) {
                            TRACE.catching("run()", e);
                            TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF, "Cannot send message to channel {0}. Received exception: {1}", new Object[]{channel.getObjectId(), e.getMessage()});
                        }
                    }
                } catch (Exception e) {
                    TRACE.catching("run()", e);
                    TRACE.errorT("run()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0019", "Cannot access inbound channel configuration. Received exception: " + e.getMessage());
                }

                try {
                    synchronized(this) {
                        if (pollTime <= 0) {
                            this.wait((long)waitTime);
                        } else {
                            this.wait((long)pollTime);
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

    public static String getExternalMessageID(File f) {
        int keymaxlen = 127;
        String extMsgId = "JCASample::" + (f == null ? "NULL" : f.getAbsolutePath() + "_" + f.lastModified());
        if (extMsgId.length() > 127) {
            String digest = "....." + XISecurityUtilities.digest(extMsgId);
            extMsgId = extMsgId.substring(0, 127 - digest.length()).concat(digest);
        }

        return extMsgId;
    }

    private void sendMessageFromFile(String inFileName, Channel channel, String processMode, String qos, String raiseError, String channelAddressMode, boolean set_asma) {
        String SIGNATURE = "sendMessageFromFile(String inFileName)";
        String msgText = new String();
        File inputFile = null;
        String channelId = null;
        String xiMsgId = null;
        boolean fileRead = true;

        try {
            inputFile = new File(inFileName);
            if (!inputFile.exists()) {
                fileRead = false;
            }
        } catch (Exception e) {
            TRACE.catching("sendMessageFromFile(String inFileName)", e);
            TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0021", "Input file " + inFileName + " attributes cannot be read. Received exception: " + e.getMessage());
            fileRead = false;
        }

        String extMsgId = getExternalMessageID(inputFile);
        TRACE.infoT("sendMessageFromFile(String inFileName)", "External message ID is '" + extMsgId + "'");
        if (fileRead && 0 != processMode.compareToIgnoreCase("test") && (qos.equalsIgnoreCase("EOIO") || qos.equalsIgnoreCase("EO"))) {
            if ((xiMsgId = this.messageIDMapper.getMappedId(extMsgId)) != null) {
                TRACE.infoT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Duplicated and already processed file (message) with id {0} detected.  It will be ignored.", new Object[]{extMsgId});
                MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
                this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Duplicated and already processed file (message) with id {0} detected.  It will be ignored.", new Object[]{extMsgId});
                this.audit.flushAuditLogEntries(amk);
                if (0 == processMode.compareToIgnoreCase("rename")) {
                    try {
                        this.renameFile(inFileName, inputFile);
                    } catch (Exception e) {
                        TRACE.catching("sendMessageFromFile(String inFileName)", e);
                    }
                }

                TRACE.exiting("sendMessageFromFile(String inFileName)");
                return;
            }

            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Duplicate check passed succesfully. New message, no duplicate (id {0})", new Object[]{extMsgId});
        }

        if (fileRead) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(inputFile));

                String line;
                for(line = null; (line = in.readLine()) != null; msgText = msgText + line + "\n") {
                }

                in.close();
                TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "File message text: " + msgText);
            } catch (Exception e) {
                TRACE.catching("sendMessageFromFile(String inFileName)", e);
                TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0022", "Input file " + inFileName + " cannot be opened. Retry in " + Integer.toString(waitTime) + " milliseconds! Received exception: " + e.getMessage());
                fileRead = false;
            }
        }

        if (fileRead) {
            try {
                String fromParty = null;
                String toParty = null;
                String fromService = null;
                String toService = null;
                String action = null;
                String actionNS = null;
                if (channelAddressMode.equalsIgnoreCase("CPA")) {
                    channelId = channel.getObjectId();
                    Binding binding = CPAFactory.getInstance().getLookupManager().getBindingByChannelId(channelId);
                    action = binding.getActionName();
                    actionNS = binding.getActionNamespace();
                    fromParty = binding.getFromParty();
                    fromService = binding.getFromService();
                    toParty = binding.getToParty();
                    toService = binding.getToService();
                } else {
                    TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Input file " + inFileName + " was read.");
                    fromParty = this.findValue("FromParty:", msgText);
                    toParty = this.findValue("ToParty:", msgText);
                    fromService = this.findValue("FromService:", msgText);
                    toService = this.findValue("ToService:", msgText);
                    action = this.findValue("Action:", msgText);
                    actionNS = this.findValue("ActionNS:", msgText);
                    String areGLN = this.findValue("GLNMode:", msgText);
                    if (areGLN != null && areGLN.compareToIgnoreCase("true") == 0) {
                        TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Access the normalization manager now.");
                        NormalizationManager normalizer = NormalizationManager.getInstance();
                        Service fromXIService = normalizer.getXIService(fromParty, "GLN", fromService);
                        if (fromXIService != null && fromXIService.getService() != null && fromXIService.getService().length() > 0) {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Address normalization for service: {0} is: {1}", new Object[]{fromService, fromXIService.getService()});
                            fromService = fromXIService.getService();
                        } else {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Address normalization is not defined for service: {0}", new Object[]{fromService});
                        }

                        Party fromXIParty = normalizer.getXIParty("009", "GLN", fromParty);
                        if (fromXIParty != null && fromXIParty.getParty() != null && fromXIParty.getParty().length() > 0) {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Address normalization for party: {0} is: {1}", new Object[]{fromParty, fromXIParty.getParty()});
                            fromParty = fromXIParty.getParty();
                        } else {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Address normalization is not defined for party: {0}", new Object[]{fromParty});
                        }

                        Service toXIService = normalizer.getXIService(toParty, "GLN", toService);
                        if (toXIService != null && toXIService.getService() != null && toXIService.getService().length() > 0) {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Address normalization for service: {0} is: {1}", new Object[]{toService, toXIService.getService()});
                            toService = toXIService.getService();
                        } else {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Address normalization is not defined for service: {0}", new Object[]{toService});
                        }

                        Party toXIParty = normalizer.getXIParty("009", "GLN", toParty);
                        if (toXIParty != null && toXIParty.getParty() != null && toXIParty.getParty().length() > 0) {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Address normalization for party: {0} is: {1}", new Object[]{toParty, toXIParty.getParty()});
                            toParty = toXIParty.getParty();
                        } else {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT, "Address normalization is not defined for party: {0}", new Object[]{toParty});
                        }
                    }

                    CPAInboundRuntimeLookupManager channelLookup = CPAFactory.getInstance().createInboundRuntimeLookupManager(this.adapterType, this.adapterNamespace, fromParty, toParty, fromService, toService, action, actionNS);
                    channel = channelLookup.getChannel();
                    if (channel == null) {
                        TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0023", "The channel ID cannot be determined. Reason: No agreement (binding) for the FP,TP,FS,TS,A combination available. Message will be processed later!");
                        return;
                    }

                    channelId = channel.getObjectId();
                }

                if (fromParty == null || fromParty.equals("*")) {
                    fromParty = new String("");
                }

                if (fromService == null || fromService.equals("*")) {
                    fromService = new String("");
                }

                if (toParty == null || toParty.equals("*")) {
                    toParty = new String("");
                }

                if (toService == null || toService.equals("*")) {
                    toService = new String("");
                }

                if (action == null || action.equals("*")) {
                    action = new String("");
                }

                if (actionNS == null || actionNS.equals("*")) {
                    actionNS = new String("");
                }

                TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "The following address data were extracted (FP,TP,FS,TS,A): " + fromParty + "," + toParty + "," + fromService + "," + toService + "," + action);
                TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "The channel ID is: " + channelId);
                Message msg = this.mf.createMessageRecord(fromParty, toParty, fromService, toService, action, actionNS);
                if (qos.equalsIgnoreCase("BE")) {
                    msg.setDeliverySemantics(DeliverySemantics.BestEffort);
                } else if (qos.equalsIgnoreCase("EOIO")) {
                    msg.setDeliverySemantics(DeliverySemantics.ExactlyOnceInOrder);
                } else {
                    msg.setDeliverySemantics(DeliverySemantics.ExactlyOnce);
                }

                XMLPayload xp = msg.createXMLPayload();
                if (msgText.indexOf("<?xml") != -1) {
                    xp.setText(msgText);
                    xp.setName("MainDocument");
                    xp.setDescription("XI AF Sample Adapter Input: XML document as MainDocument");
                } else {
                    xp.setContent(msgText.getBytes("UTF-8"));
                    xp.setContentType("application/octet-stream");
                    xp.setName("MainDocument");
                    xp.setDescription("XI AF Sample Adapter Input: Binary as MainDocument");
                }

                if (set_asma) {
                    msg.setMessageProperty(this.adapterNamespace + "/" + this.adapterType, "JCAChannelID", channelId);
                    TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "The adapter specific message attribute (ASMA) {0} was set.", new Object[]{"JCAChannelID"});
                } else {
                    TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "The adapter specific message attribute (ASMA) {0} was not set since the setting is switched off in the channel configuration.", new Object[]{"JCAChannelID"});
                }

                msg.setDocument(xp);
                TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Message object created and filled.");
                ModuleData md = new ModuleData();
                md.setPrincipalData(msg);
                if (!qos.equalsIgnoreCase("BE")) {
                    TransactionTicket txTicket = null;

                    try {
                        TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Get transaction ticket now.");
                        txTicket = TxManager.required();
                        TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Got transaction ticket: {0}", new Object[]{txTicket.toString()});
                        xiMsgId = msg.getMessageId();
                        MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
                        md.setSupplementalData("audit.key", amk);
                        if (MessageDirection.OUTBOUND == MessageDirection.valueOf("OUTBOUND")) {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "AuditDirection typesafe enum works well!");
                        } else {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "AuditDirection typesafe enum works quite bad!");
                        }

                        if (AuditLogStatus.ERROR == AuditLogStatus.valueOf("ERR")) {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "AuditLogStatus typesafe enum works well!");
                        } else {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "AuditLogStatus typesafe enum works quite bad!");
                        }

                        MessageKey amk2 = msg.getMessageKey();
                        if (amk2.equals(amk)) {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "MessageKey amk and amk2 are equal!");
                        } else {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "MessageKey amk and amk2 are not equal!");
                        }

                        TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "The last audit message key being used was: amk: {0}, dir: {1}, msgid: {2}, msgkey: {3}, stat: {4}.", new Object[]{amk.toString(), amk.getDirection().toString(), amk.getMessageId().toString(), amk.toString(), AuditLogStatus.SUCCESS.toString()});
                        this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Asynchronous message was read from file and will be forwarded to the XI AF MS now.");
                        this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Name of the processed file: {0}.", new Object[]{inFileName});
                        this.audit.addAuditLogEntry(amk, AuditLogStatus.WARNING, "Demo: This is a warning audit log message");
                        this.audit.flushAuditLogEntries(amk);
                        this.audit.flushAuditLogEntries(amk);
                        TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Message will be forwarded to XI AF MP and channel: " + channelId);
                        this.lookUpModuleProcessor(1).process(channelId, md);
                        TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "The message with ID " + msg.getMessageId() + " was forwarded to the XI AF succesfully.");
                        if (0 != processMode.compareToIgnoreCase("test")) {
                            this.messageIDMapper.createIDMap(extMsgId, xiMsgId, System.currentTimeMillis() + 86400000L, true);
                        }

                        if (0 == raiseError.compareToIgnoreCase("rollback")) {
                            this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Channel error mode is set to rollback. An Exception is thrown now to demonstrate a rollback behavior.");
                            this.audit.flushAuditLogEntries(amk);
                            TRACE.infoT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Channel error mode is set to rollback. An Exception is thrown now to demonstrate a rollback behavior.");

                            try {
                                MonitoringManager mm = MonitoringManagerFactory.getInstance().getMonitoringManager();
                                ProcessContextFactory.ParamSet ps = ProcessContextFactory.getParamSet().message(msg).channel(channel);
                                ProcessContext pc = ProcessContextFactory.getInstance().createProcessContext(ps);
                                mm.reportProcessStatus(this.adapterNamespace, this.adapterType, ChannelDirection.SENDER, ProcessState.FATAL, "Rollback triggered (as demo) since channel error mode was set to rollback", pc);
                            } catch (Exception e) {
                                TRACE.catching("sendMessageFromFile(String inFileName)", e);
                                TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0024", "Process state propagation failed due to: {0}", new Object[]{e.getMessage()});
                            }

                            RetryControlException e = new RetryControlException("Sample rollback simulation test exception", RetryMode.STOP_RETRIES);
                            TRACE.throwing("sendMessageFromFile(String inFileName)", e);
                            throw e;
                        }

                        if (0 == processMode.compareToIgnoreCase("rename")) {
                            this.renameFile(inFileName, inputFile);
                        }
                    } catch (TxRollbackException e) {
                        TRACE.catching("sendMessageFromFile(String inFileName)", e);
                        TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0025", "Rollback was performed explicitly!. Reason: {0}. Message will be processed again later.", new Object[]{e.getMessage()});
                    } catch (TxException e) {
                        TRACE.catching("sendMessageFromFile(String inFileName)", e);
                        TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0026", "Internal transaction manager exception received. Rollback is performed!. Reason: {0}. Message will be processed again later.", new Object[]{e.getMessage()});
                    } catch (Exception e) {
                        TRACE.catching("sendMessageFromFile(String inFileName)", e);
                        TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0027", "Inbound processing failed, transaction is being rollback'ed. Reason: {0}.Message will be processed again later.", new Object[]{e.getMessage()});
                        TxManager.setRollbackOnly();
                    } finally {
                        if (txTicket == null) {
                            TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0028", "Got no valid transaction ticket (was null).");
                        } else {
                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Transaction level will be committed now.");

                            try {
                                TxManager.commitLevel(txTicket);
                            } catch (Exception e) {
                                TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0029", "Internal transaction manager exception received. Rollback is performed!. Reason: {0}. Message will be processed again later.", new Object[]{e.getMessage()});
                            }

                            TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Transaction level was committed succesfully.");
                        }

                    }
                } else {
                    try {
                        xiMsgId = msg.getMessageId();
                        MessageKey amk = new MessageKey(xiMsgId, MessageDirection.OUTBOUND);
                        md.setSupplementalData("audit.key", amk);
                        this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "Synchronous message was read from file and will be forwarded to the XI AF MS now.");
                        TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Message will be forwarded to XI AF MP and channel: " + channelId);
                        ModuleData result = this.lookUpModuleProcessor(1).process(channelId, md);
                        TRACE.debugT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "The synchronous message with ID " + msg.getMessageId() + " was processed by the XI AF succesfully.");
                        Object principal = result.getPrincipalData();
                        if (principal instanceof Message) {
                            Message response = (Message)principal;
                            TRACE.infoT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Got back a response message. ID/FP/FS/TP/TS/IF/IFNS/Class: {0}/{1}/{2}/{3}/{4}/{5}/{6}/{7}", new Object[]{response.getMessageId(), response.getFromParty().toString(), response.getFromService().toString(), response.getToParty().toString(), response.getToService().toString(), response.getAction().getName(), response.getAction().getType(), response.getMessageClass().toString()});
                            Payload payload = response.getDocument();
                            if (payload instanceof TextPayload) {
                                TextPayload text = (TextPayload)payload;
                                TRACE.infoT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Payload: {0}", new Object[]{text.getText()});
                            } else {
                                TRACE.infoT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Received a binary response {0}", new Object[]{new String(payload.getContent())});
                            }

                            Payload att = response.getAttachment("Attachment");
                            if (att != null & att instanceof TextPayload) {
                                TextPayload text = (TextPayload)att;
                                TRACE.infoT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Payload: {0}", new Object[]{text.getText()});
                            } else if (att != null) {
                                TRACE.infoT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "Received a binary response {0}", new Object[]{new String(att.getContent())});
                            }
                        } else {
                            TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0030", "Received not a XI message as response. Class is: {0}", new Object[]{principal.getClass().getName()});
                        }

                        if (0 == processMode.compareToIgnoreCase("rename")) {
                            this.renameFile(inFileName, inputFile);
                        }
                    } catch (Exception e) {
                        TRACE.catching("sendMessageFromFile(String inFileName)", e);
                        TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0031", "Synchronous inbound processing failed. Received exception: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                TRACE.catching("sendMessageFromFile(String inFileName)", e);
                TRACE.errorT("sendMessageFromFile(String inFileName)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0032", "Received exception: " + e.getMessage());
            }
        }

    }

    private void renameFile(String inFileName, File inputFile) throws Exception {
        String SIGNATURE = "renameFile(String inFileName, File inputFile)";

        try {
            File renamed = new File(inFileName + ".sent");
            renamed.delete();
            if (!inputFile.renameTo(renamed)) {
                TRACE.errorT("renameFile(String inFileName, File inputFile)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0033", "Input file " + inFileName + " cannot be renamed. It will be sent again!");
            }

        } catch (Exception e) {
            TRACE.catching("renameFile(String inFileName, File inputFile)", e);
            TRACE.errorT("renameFile(String inFileName, File inputFile)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0034", "Input file " + inFileName + " cannot be renamed. Received exception: " + e.getMessage());
            throw e;
        }
    }

    private String findValue(String key, String text) {
        String SIGNATURE = "findValue(String key, String text)";
        int startIndex = text.indexOf(key);
        if (startIndex < 0) {
            return new String("");
        } else {
            startIndex += key.length();
            int endIndex = text.indexOf(";", startIndex);
            if (endIndex < 0) {
                endIndex = text.lastIndexOf(text);
            }

            String value = text.substring(startIndex, endIndex);
            TRACE.debugT("findValue(String key, String text)", XIAdapterCategories.CONNECT_AF, "findValue data (key,value,start,end): " + key + "," + value + "," + Integer.toString(startIndex) + "," + Integer.toString(endIndex));
            return value;
        }
    }

    public String getAdapterNamespace() {
        String SIGNATURE = "getAdapterNamespace()";
        TRACE.entering("getAdapterNamespace()");
        TRACE.exiting("getAdapterNamespace()");
        return this.adapterNamespace;
    }

    public String getAdapterType() {
        return this.adapterType;
    }

    public void setAdapterNamespace(String adapterNamespace) {
        String SIGNATURE = "setAdapterNamespace(String adapterNamespace)";
        TRACE.entering("setAdapterNamespace(String adapterNamespace)", new Object[]{adapterNamespace});
        this.adapterNamespace = adapterNamespace;
        TRACE.exiting("setAdapterNamespace(String adapterNamespace)");
    }

    public void setAdapterType(String adapterType) {
        String SIGNATURE = "setAdapterType(String adapterType)";
        TRACE.entering("setAdapterType(String adapterType)", new Object[]{adapterType});
        this.adapterType = adapterType;
        TRACE.exiting("setAdapterType(String adapterType)");
    }

    public GUID getMcfLocalGuid() {
        return this.mcfLocalGuid;
    }

    public void start() {
        String SIGNATURE = "start()";
        TRACE.entering("start()");
        String controlledMcfGuid = this.getMcfLocalGuid().toHexString();
        TRACE.infoT("start()", XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} is started now. ({1})", new Object[]{controlledMcfGuid.toString(), SPIManagedConnectionFactory.class.getClassLoader()});

        try {
            this.audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
        } catch (Exception e) {
            TRACE.catching("start()", e);
            TRACE.errorT("start()", XIAdapterCategories.CONNECT, "SOA.apt_sample.0035", "Unable to access the XI AF audit log. Reason: {0}. Adapter cannot not start the inbound processing!", new Object[]{e});
            TRACE.exiting("start()");
            return;
        }

        this.messageIDMapper = MessageIDMapper.getInstance();
        if (this.messageIDMapper == null) {
            TRACE.errorT("start()", XIAdapterCategories.CONNECT, "SOA.apt_sample.0036", "Gut null as MessageIDMapper singleton instance. Adapter cannot not start the inbound processing!");
            TRACE.exiting("start()");
        } else {
            try {
                this.mf = new XIMessageFactoryImpl(this.adapterType, this.adapterNamespace);
            } catch (Exception e) {
                TRACE.catching("start()", e);
                TRACE.errorT("start()", XIAdapterCategories.CONNECT, "SOA.apt_sample.0037", "Unable to create XI message factory. Adapter cannot not start the inbound processing!");
                TRACE.exiting("start()");
                return;
            }

            try {
                this.startMCF();
                this.startTimer();
                TRACE.infoT("start()", XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} was started successfully.", new Object[]{controlledMcfGuid.toString()});
            } catch (Exception e) {
                TRACE.catching("start()", e);
                TRACE.errorT("start()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0038", "Start of MCF failed. Reason: {0}", new Object[]{e.getMessage()});
            }

            try {
                ClassUtil.setClassLoader("com.sap.aii.af.sample.module.ConvertCRLFfromToLF0", ConvertCRLFfromToLF0.class.getClassLoader());
            } catch (Exception e) {
                TRACE.catching("start()", e);
                TRACE.errorT("start()", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0039", "Unable to register pojo modules. Reason: {0}", new Object[]{e.getMessage()});
            }

            TRACE.exiting("start()");
        }
    }

    public void stop() {
        String SIGNATURE = "stop()";
        TRACE.entering("stop()");
        String controlledMcfGuid = this.getMcfLocalGuid().toHexString();
        TRACE.infoT("stop()", XIAdapterCategories.CONNECT_AF, "The running MCF with GUID {0} will be stopped now", new Object[]{controlledMcfGuid.toString()});
        ClassUtil.removeClassLoader("com.sap.aii.af.sample.module.ConvertCRLFfromToLF0");

        try {
            this.stopMCF();
            this.stopTimer();
        } catch (Exception e) {
            TRACE.catching("stop()", e);
        }

        TRACE.infoT("stop()", XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} was stopped successfully.", new Object[]{controlledMcfGuid.toString()});
        TRACE.exiting("stop()");
    }

    public boolean isRunning() {
        return this.threadStatus == 1;
    }

    class XIManagedConnectionFactoryController extends TimerTask {
        private SPIManagedConnectionFactory controlledMcf;

        public XIManagedConnectionFactoryController(SPIManagedConnectionFactory mcf, InitialContext ctx) {
            this.controlledMcf = mcf;
        }

        public void run() {
            String SIGNATURE = "XIManagedConnectionFactoryController.run()";
            String controlledMcfGuid = null;

            try {
                if (this.controlledMcf != null) {
                    controlledMcfGuid = this.controlledMcf.getMcfLocalGuid().toHexString();
                }

                SPIManagedConnectionFactory.TRACE.debugT("XIManagedConnectionFactoryController.run()", XIAdapterCategories.CONNECT_AF, "MCF with GUID {0} is running. ({1})", new Object[]{controlledMcfGuid.toString(), SPIManagedConnectionFactory.class.getClassLoader()});
            } catch (Exception e) {
                SPIManagedConnectionFactory.TRACE.catching("XIManagedConnectionFactoryController.run()", e);
                SPIManagedConnectionFactory.TRACE.warningT("XIManagedConnectionFactoryController.run()", XIAdapterCategories.CONNECT_AF, "Processing of control timer failed. Reason: " + e.getMessage());
            }

        }
    }
}

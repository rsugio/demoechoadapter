package demoecho;

import com.sap.aii.af.service.administration.api.AdapterCallback;
import com.sap.aii.af.service.administration.api.AdapterCapability;
import com.sap.aii.af.service.administration.api.AdapterRegistry;
import com.sap.aii.af.service.administration.api.AdapterRegistryFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPALookupManager;
import com.sap.aii.af.service.administration.api.cpa.ChannelLifecycleCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationNotPossibleException;
import com.sap.aii.af.service.administration.api.monitoring.*;
import com.sap.aii.af.service.administration.monitoring.Adapter;
import com.sap.aii.af.service.administration.monitoring.MonitoringAdapterAdminManagerFactory;
import com.sap.aii.af.service.cpa.*;
import com.sap.aii.af.service.util.adapterstatus.AAMStatusMonitor;
import com.sap.aii.utilxi.rtcheck.base.SingleTestResult;
import com.sap.aii.utilxi.rtcheck.base.TestResult;
import com.sap.aii.utilxi.rtcheck.base.TestSuitResult;
import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesAccess;

import javax.naming.InitialContext;
import javax.resource.ResourceException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Properties;

public class XIConfiguration implements ChannelLifecycleCallback, ChannelStatusCallback, LocalizationCallback, ChannelSelfTestCallback {
    private static final XITrace TRACE = new XITrace(XIConfiguration.class.getName());
    public static final String ADAPTER_TYPE = EchoAdapterConstants.adapterType;
    public static final String ADAPTER_NAMESPACE = EchoAdapterConstants.adapterNamespace;
    private String adapterType;
    private String adapterNamespace;
    private LinkedList<Channel> outboundChannels;
    //    private LinkedList<Channel> inboundChannels;
    private CPALookupManager lookupManager;
    private AdapterRegistry adapterRegistry;
    private LocalizationCallback localizer;
    private PartyChangeCallBackHandler partyChangeCallBackHandler;
    private SPIManagedConnectionFactory mcf;

    // из коммуникационного канала:
    private String text64 = null, adapterStatus = null, throwFault;
    // из пропертей
//    final PropertyConfiguration propertyListener = new PropertyConfiguration();
    ApplicationPropertiesAccess applicationConfiguration = null;
    String centralFileLogDirectory = null;

    public XIConfiguration() {
        this(ADAPTER_TYPE, ADAPTER_NAMESPACE);
    }

    public XIConfiguration(String adapterType, String adapterNamespace) {
        String SIGNATURE = "XIConfiguration(String adapterType, String adapterNamespace)";
        TRACE.entering(SIGNATURE, new Object[]{adapterType, adapterNamespace});
        this.outboundChannels = null;
        this.lookupManager = null;
        this.adapterRegistry = null;
        this.localizer = null;
        this.partyChangeCallBackHandler = null;
        this.mcf = null;
        this.adapterType = adapterType;
        this.adapterNamespace = adapterNamespace;

        try {
            CPAFactory cf = CPAFactory.getInstance();
            this.lookupManager = cf.getLookupManager();
            this.partyChangeCallBackHandler = PartyChangeCallBackHandler.getInstance();
            MonitoringAdapterAdminManagerFactory maamf = MonitoringAdapterAdminManagerFactory.getInstance();
            Adapter[] s = maamf.getMonitoringAdapterRegistry().getRegistetredAdapters();
            TRACE.warningT(SIGNATURE, "{}", maamf.getSchedulingManager().getAllSchedules());
            AAMStatusMonitor aamStatusMonitor = new AAMStatusMonitor(null);
            TRACE.warningT(SIGNATURE, aamStatusMonitor.toString());
            aamStatusMonitor.reportMessageProcessed(null);
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "SOA.apt_sample.0040", "CPALookupManager cannot be instantiated due to {0}", e.getMessage());
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "SOA.apt_sample.0041", "No channel configuration can be read, no message exchange possible!");
        }

        TRACE.exiting(SIGNATURE);
    }

    public void channelAdded(Channel channel) {
        String SIGNATURE = "channelAdded(Channel channel)";
        TRACE.entering(SIGNATURE, new Object[]{channel});

        synchronized (this) {
            if (channel.getDirection() == Direction.INBOUND) {
//                this.inboundChannels.add(channel);
//
//                try {
//                    dir = channel.getValueAsString("fileInDir");
//                    name = channel.getValueAsString("fileInName");
//                } catch (Exception e) {
//                    TRACE.catching("channelAdded(Channel channel)", e);
//                    TRACE.errorT("channelAdded(Channel channel)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0042", "Channel configuration value cannot be read due to {0}", new Object[]{e.getMessage()});
//                }
            } else if (channel.getDirection() == Direction.OUTBOUND) {
                this.outboundChannels.add(channel);

                try {
                    readChannelAttributes(channel);
//                    text64 = channel.getValueAsString("text64");
//                    adapterStatus = channel.getValueAsString("adapterStatus");
                } catch (Exception e) {
                    TRACE.catching(SIGNATURE, e);
                    TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0043", "Channel configuration value cannot be read due to {0}", e.getMessage());
                }
            }

            try {
                PartyCallBackController.getInstance().registerForPartyEvent(channel.getParty(), this.partyChangeCallBackHandler);
                Party party = NormalizationManager.getInstance().getXIParty("http://sap.com/xi/XI", "XIParty", channel.getParty());

                for (PartyIdentifier partyIdentifier : this.lookupManager.getPartyIdentifiersByParty(party)) {
                    String schema = partyIdentifier.getPartySchema();
                    if (schema.equals("DUNS")) {
                        this.partyChangeCallBackHandler.addParty(channel.getParty(), partyIdentifier.getPartyIdentifier());
                    }
                }
            } catch (Exception e) {
                TRACE.catching(SIGNATURE, e);
                TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0044", "Party Cannot be registered for Callback due to {0}", new Object[]{e.getMessage()});
            }
        }

        TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} added (direction is {3}).", new Object[]{channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()});
        TRACE.exiting(SIGNATURE);
    }

    public void channelUpdated(Channel channel) {
        String SIGNATURE = "channelUpdated(Channel channel)";
        TRACE.entering(SIGNATURE);
        try {
            readChannelAttributes(channel);
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0043", "Channel configuration value cannot be read due to {0}", e.getMessage());
        }
//        this.channelRemoved(channel);
//        this.channelAdded(channel);
        TRACE.exiting(SIGNATURE);
    }

    public void channelRemoved(Channel channel) {
        String SIGNATURE = "channelRemoved(Channel channel)";
        TRACE.entering(SIGNATURE, new Object[]{channel});
        LinkedList<Channel> channels = null;
        TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} will be removed now. (direction is {3}).", new Object[]{channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()});
        String channelID = channel.getObjectId();
        if (channel.getDirection() == Direction.INBOUND) {
//            channels = this.inboundChannels;
        } else {
            channels = this.outboundChannels;
        }

        try {
            PartyCallBackController.getInstance().unregisterForPartyEvent(channel.getParty(), this.partyChangeCallBackHandler);
            this.partyChangeCallBackHandler.removeParty(channel.getParty());
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0045", "Party Cannot be unregistered for Callback due to {0}", new Object[]{e.getMessage()});
        }

        synchronized (this) {
            for (int i = 0; i < channels.size(); ++i) {
                Channel storedChannel = channels.get(i);
                if (storedChannel.getObjectId().equalsIgnoreCase(channelID)) {
                    channels.remove(i);
                    if (channel.getDirection() == Direction.OUTBOUND) {
                        try {
                            this.mcf.destroyManagedConnection(channelID);
                        } catch (Exception e) {
                            TRACE.catching(SIGNATURE, e);
                            TRACE.warningT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The ManagedConnection for channel {0} cannot be destroyed. Configuration update might not work.", new Object[]{channelID});
                        }
                    }
                    break;
                }
            }
        }

        TRACE.exiting("channelRemoved(Channel channel)");
    }

    public void init(SPIManagedConnectionFactory mcf) throws ResourceException {
        String SIGNATURE = "init(mcf)";
        TRACE.entering(SIGNATURE);
//        String dir = null;
//        String name = null;
        this.mcf = mcf;

        try {
            this.localizer = XILocalizationUtilities.getLocalizationCallback();
            AdapterRegistryFactory arf = AdapterRegistryFactory.getInstance();
            this.adapterRegistry = arf.getAdapterRegistry();
            this.adapterRegistry.registerAdapter(this.adapterNamespace, this.adapterType, new AdapterCapability[]{AdapterCapability.PUSH_PROCESS_STATUS}, new AdapterCallback[]{this});
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            ResourceException re = new ResourceException("XI AAM registration failed due to: " + e.getMessage());
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }

        synchronized (this) {
//            this.inboundChannels = new LinkedList();
            this.outboundChannels = new LinkedList<>();

            try {
                LinkedList<Channel> allChannels = this.lookupManager.getChannelsByAdapterType(this.adapterType, this.adapterNamespace);
                TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "The XI AAM service returned {0} channels for adapter type {1} with namespace {2}", new Object[]{allChannels.size(), this.adapterType, this.adapterNamespace});

                for (int i = 0; i < allChannels.size(); ++i) {
                    Channel channel = (Channel) allChannels.get(i);
                    if (channel.getDirection() == Direction.INBOUND) {
//                        this.inboundChannels.add(channel);
//                        dir = channel.getValueAsString("fileInDir");
//                        name = channel.getValueAsString("fileInName");
                    } else {
                        if (channel.getDirection() != Direction.OUTBOUND) {
                            continue;
                        }

                        this.outboundChannels.add(channel);
                        readChannelAttributes(channel);
//                        dir = channel.getValueAsString("fileOutDir");
//                        name = channel.getValueAsString("fileOutPrefix");
                    }

                    TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} added (direction is {3}).", new Object[]{channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()});
                }
            } catch (Exception e) {
                TRACE.catching(SIGNATURE, e);
                ResourceException re = new ResourceException("XI CPA lookup failed due to: " + e.getMessage());
                TRACE.throwing(SIGNATURE, re);
                throw re;
            }
        }

        TRACE.exiting(SIGNATURE);
    }

    public void stop() throws ResourceException {
        String SIGNATURE = "stop()";
        TRACE.entering(SIGNATURE);

        try {
            try {
                for (String partyName : this.partyChangeCallBackHandler.getRegisteredParties()) {
                    PartyCallBackController.getInstance().unregisterForPartyEvent(partyName, this.partyChangeCallBackHandler);
                }

                this.partyChangeCallBackHandler.clear();
            } catch (CPAException e) {
                TRACE.catching(SIGNATURE, e);
            }

            this.adapterRegistry.unregisterAdapter(this.adapterNamespace, this.adapterType);
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            ResourceException re = new ResourceException("XI AAM unregistration failed due to: " + e.getMessage());
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }

        TRACE.exiting(SIGNATURE);
    }

    public LinkedList getCopy(Direction direction) throws ResourceException {
        String SIGNATURE = "getCopy(Direction direction)";
        LinkedList<Channel> out = null;
//        if (this.inboundChannels == null || this.outboundChannels == null) {
//            this.init(this.mcf);
//        }

        synchronized (this) {
            if (direction == Direction.INBOUND) {
//                out = (LinkedList) this.inboundChannels.clone();
            } else {
                if (direction != Direction.OUTBOUND) {
                    ResourceException re = new ResourceException("Direction invalid");
                    TRACE.throwing("getCopy(Direction direction)", re);
                    throw re;
                }

                out = (LinkedList) this.outboundChannels.clone();
            }

            return out;
        }
    }

    /**
     * из ChannelStatusCallback - пинга здесь нет
     *
     * @param channel
     * @param locale
     * @return
     * @throws ChannelUnknownException
     */
    @Override
    public ChannelStatus getChannelStatus(Channel channel, Locale locale) throws ChannelUnknownException {
        String SIGNATURE = "getChannelStatus(Channel channel, Locale locale)";
        TRACE.entering(SIGNATURE, new Object[]{channel, locale});
        boolean channelFound = false;
        Channel storedChannel = null;
        String channelID = "<unknown>";
        Exception cause = null;
        ChannelStatus cs;

        try {
            channelID = channel.getObjectId();
            LinkedList<Channel> channels = new LinkedList<>();
            if (channel.getDirection() == Direction.OUTBOUND) {
                channels = this.outboundChannels;
            }

            synchronized (this) {
                for (int i = 0; i < channels.size(); ++i) {
                    storedChannel = channels.get(i);
                    if (storedChannel.getObjectId().equals(channelID)) {
                        channelFound = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            cause = e;
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "SOA.apt_sample.0046", "Channel lookup failed due to {0}.", new Object[]{e.getMessage()});
        }

        if (!channelFound) {
            ChannelUnknownException cue = new ChannelUnknownException("Channel with ID " + channelID + " is not known.", cause);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "SOA.apt_sample.0047", "Channel {0} is not known.", new Object[]{channelID});
            TRACE.throwing(SIGNATURE, cue);
            throw cue;
        } else {
            ChannelStatusFactory csf = ChannelStatusFactory.getInstance();
            if (csf == null) {
                ChannelUnknownException cue = new ChannelUnknownException("Internal error: Unable to get instance of ChannelStatusFactory.", cause);
                TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "SOA.apt_sample.0048", "Unable to get instance of ChannelStatusFactory.");
                TRACE.throwing(SIGNATURE, cue);
                throw cue;
            } else {
                try {
                    if (storedChannel.getDirection() == Direction.OUTBOUND) {
                        readChannelAttributes(channel);
                        String directory = "."; // channel.getValueAsString("fileOutDir");
                        if (directory == null || directory.length() == 0) {
                            cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Output file directory name is not set.");
                            TRACE.exiting(SIGNATURE, new Object[]{cs});
                            return cs;
                        }
                    } else {
                        if (!this.mcf.isRunning()) {
                            cs = csf.createChannelStatus(channel, ChannelState.ERROR, "The JCA adapter inbound thread is not working correctly. No inbound messages possible!");
                            TRACE.exiting(SIGNATURE, new Object[]{cs});
                            return cs;
                        }
                    }

                    cs = csf.createChannelStatus(channel, ChannelState.OK, this.localizer.localizeString("CHANNEL_OK", locale));
                } catch (Exception e) {
                    TRACE.catching(SIGNATURE, e);
                    TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0049", "Cannot retrieve status for channel {0}. Received exception: {1}", new Object[]{channel.getChannelName(), e.getMessage()});
                    cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Cannot retrieve status for this channel due to: " + e.getMessage());
                    TRACE.exiting(SIGNATURE, new Object[]{cs});
                    return cs;
                }

                TRACE.exiting(SIGNATURE, new Object[]{cs});
                return cs;
            }
        }
    }

    public String localizeString(String str, Locale locale) throws LocalizationNotPossibleException {
        return this.localizer.localizeString(str, locale);
    }

    @Override
    public com.sap.aii.utilxi.rtcheck.base.TestSuitResult testChannel(Channel channel, Locale locale) {
        String SIGNATURE = "testChannel(Channel channel, Locale locale)";
        TRACE.entering(SIGNATURE, new Object[]{channel, locale});
        TestSuitResult tsr = new TestSuitResult();
        tsr.setOverallResult(2);
        TestResult tr = new SingleTestResult(2, "text64", text64);
        tsr.addTestResult(tr);
        tr = new SingleTestResult(2, "adapterStatus", adapterStatus);
        tsr.addTestResult(tr);
        tr = new SingleTestResult(2, "centralFileLogDirectory", centralFileLogDirectory);
        tsr.addTestResult(tr);
        tr = new SingleTestResult(2, "throwFault", throwFault);
        tsr.addTestResult(tr);
        return tsr;
    }

    //TODO сделать имена параметров из EchoAdapterConstants
    private void readChannelAttributes(Channel channel) throws CPAException {
        text64 = channel.getValueAsString("text64");
        adapterStatus = channel.getValueAsString("adapterStatus");
        throwFault = channel.getValueAsString("throwFault");
        if (applicationConfiguration == null) {
            try {
                InitialContext ctx = new InitialContext();
                applicationConfiguration = (ApplicationPropertiesAccess) ctx.lookup("ApplicationConfiguration");
//                if (applicationConfiguration != null) {
//                    applicationConfiguration.addApplicationPropertiesChangedListener(propertyListener);
//                }
            } catch (Exception e) {
            }
        }

        if (applicationConfiguration != null && centralFileLogDirectory == null) {
            Properties application = applicationConfiguration.getApplicationProperties();
            Properties system = applicationConfiguration.getSystemProfile();
            if (application != null && application.containsKey("centralFileLogDirectory")) {
                centralFileLogDirectory = application.getProperty("centralFileLogDirectory");
            } else if (system != null) {
                // SYS_GLOBAL_DIR=/usr/sap/JXD/SYS/global
                // centralFileLogDirectory=$SYS_GLOBAL_DIR/xi_customer_logs/echoadapter
                centralFileLogDirectory = system.getProperty("SYS_GLOBAL_DIR") + EchoAdapterConstants.centralFileLogDirectorySuffix;
            }
        }
    }

//    class PropertyConfiguration implements ApplicationPropertiesChangeListener {
//        private PropertyConfiguration() {
//        }
//        public void propertiesChanged() {
//            try {
//                InitialContext ctx = new InitialContext();
//                ApplicationPropertiesAccess applicationConfiguration = (ApplicationPropertiesAccess) ctx.lookup("ApplicationConfiguration");
//                if (applicationConfiguration != null) {
//                    Properties application = applicationConfiguration.getApplicationProperties();
//                    if (application != null) {
//                        centralFileLogDirectory = "####### " + application + " #######";
//                    } else {
//                        centralFileLogDirectory = "# null (no application properties found) #";
//                    }
//                }
//            } catch (Exception e) {
//            }
//            //            ApplicationConfiguration.updateProperties();
////            ODataHelpServiceRegistration.registerHelpService();
//        }
//    }

}

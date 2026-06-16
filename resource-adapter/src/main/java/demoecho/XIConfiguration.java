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
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;
import com.sap.aii.utilxi.rtcheck.base.SingleTestResult;
import com.sap.aii.utilxi.rtcheck.base.TestResult;
import com.sap.aii.utilxi.rtcheck.base.TestSuitResult;
import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesAccess;

import javax.resource.ResourceException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class XIConfiguration implements ChannelLifecycleCallback, ChannelStatusCallback, LocalizationCallback, ChannelSelfTestCallback, Serializable {
    private static final XITrace TRACE = new XITrace(XIConfiguration.class.getName());

    //    private final String adapterType = EchoAdapterConstants.adapterType;
//    private final String adapterNamespace = EchoAdapterConstants.adapterNamespace;
    private final LinkedList<Channel> channels = new LinkedList<>();

    CPAFactory cpaFactory;
    CPALookupManager lookupManager;
    AdapterRegistry adapterRegistry;
    LocalizationCallback localizer;
    //    PartyChangeCallBackHandler partyChangeCallBackHandler;
    SPIManagedConnectionFactory mcf;

    // из коммуникационного канала:
//    ChannelProperties channelProperties = new ChannelProperties();
    // из пропертей
    String centralFileLogDirectory = null;
    //    final PropertyConfiguration propertyListener = new PropertyConfiguration();
    ApplicationPropertiesAccess applicationConfiguration = null;

    XIConfiguration(String adapterType, String adapterNamespace) {
        String SIGNATURE = "XIConfiguration(String adapterType, String adapterNamespace)";
        TRACE.entering(SIGNATURE, new Object[]{adapterType, adapterNamespace});
//        this.adapterRegistry = null;
//        this.localizer = null;
//        this.mcf = null;
//        this.adapterType = adapterType;
//        this.adapterNamespace = adapterNamespace;

        try {
            this.cpaFactory = CPAFactory.getInstance();
            this.lookupManager = this.cpaFactory.getLookupManager();
//            this.partyChangeCallBackHandler = PartyChangeCallBackHandler.getInstance();
//            MonitoringAdapterAdminManagerFactory maamf = MonitoringAdapterAdminManagerFactory.getInstance();
//            Adapter[] s = maamf.getMonitoringAdapterRegistry().getRegistetredAdapters();
//            TRACE.warningT(SIGNATURE, "{}", maamf.getSchedulingManager().getAllSchedules());
//            AAMStatusMonitor aamStatusMonitor = new AAMStatusMonitor(null);
//            TRACE.warningT(SIGNATURE, aamStatusMonitor.toString());
//            aamStatusMonitor.reportMessageProcessed(null);
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "SOA.apt_sample.0040", "CPALookupManager cannot be instantiated due to {0}", e.getMessage());
            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONFIG, "SOA.apt_sample.0041", "No channel configuration can be read, no message exchange possible!");
        }

        TRACE.exiting(SIGNATURE);
    }

    // ChannelLifecycleCallback
    @Override
    public void channelAdded(Channel channel) {
        String SIGNATURE = "channelAdded(Channel channel)";
        TRACE.entering(SIGNATURE, new Object[]{channel});
        Objects.requireNonNull(channel);
        synchronized (channels) {
            //TODO Party
            channels.add(channel);
        }
//        TRACE.infoT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} added (direction is {3}).", new Object[]{channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()});
//        TRACE.exiting(SIGNATURE);
    }

    @Override
    public void channelUpdated(Channel channel) {
        String SIGNATURE = "channelUpdated(Channel channel)";
        TRACE.entering(SIGNATURE, new Object[]{channel});
        Objects.requireNonNull(channel);
        this.channelRemoved(channel);
        this.channelAdded(channel);
    }

    @Override
    public void channelRemoved(Channel channel) {
        String SIGNATURE = "channelRemoved(Channel channel)";
        TRACE.entering(SIGNATURE, new Object[]{channel});
        String channelID = Objects.requireNonNull(channel).getObjectId();
//        TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} will be removed now. (direction is {3}).", new Object[]{channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()});
//        try {
//            //PartyCallBackController.getInstance().unregisterForPartyEvent(channel.getParty(), this.partyChangeCallBackHandler);
//            //this.partyChangeCallBackHandler.removeParty(channel.getParty());
//        } catch (Exception e) {
//            TRACE.catching(SIGNATURE, e);
//            TRACE.errorT(SIGNATURE, XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0045", "Party Cannot be unregistered for Callback due to {0}", new Object[]{e.getMessage()});
//        }

        Optional<Channel> storedChannel = channels.stream()
                .filter(x -> x.getObjectId().equals(channelID))
                .findFirst();
        if (storedChannel.isPresent())
            synchronized (channels) {
                channels.remove(storedChannel.get());
                mcf.destroyManagedConnection(channelID);
            }
        TRACE.exiting(SIGNATURE);
    }

    public void init(SPIManagedConnectionFactory mcf) throws ResourceException {
        String SIGNATURE = "init(mcf)";
        TRACE.entering(SIGNATURE);
        this.mcf = mcf;

        try {
            this.localizer = XILocalizationUtilities.getLocalizationCallback();
            AdapterRegistryFactory arf = AdapterRegistryFactory.getInstance();
            this.adapterRegistry = arf.getAdapterRegistry();
            this.adapterRegistry.registerAdapter(EchoAdapterConstants.adapterNamespace, EchoAdapterConstants.adapterType,
                    new AdapterCapability[]{AdapterCapability.PUSH_PROCESS_STATUS}, new AdapterCallback[]{this});
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            ResourceException re = new ResourceException("XI AAM registration failed due to: " + e.getMessage());
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }

        synchronized (channels) {
            channels.clear();

            try {
                LinkedList<Channel> allChannels = this.lookupManager.getChannelsByAdapterType(EchoAdapterConstants.adapterType, EchoAdapterConstants.adapterNamespace);
                channels.addAll(allChannels);
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
//            try {
//                for (String partyName : this.partyChangeCallBackHandler.getRegisteredParties()) {
//                    PartyCallBackController.getInstance().unregisterForPartyEvent(partyName, this.partyChangeCallBackHandler);
//                }
//
//                this.partyChangeCallBackHandler.clear();
//            } catch (CPAException e) {
//                TRACE.catching(SIGNATURE, e);
//            }

            this.adapterRegistry.unregisterAdapter(EchoAdapterConstants.adapterNamespace, EchoAdapterConstants.adapterType);
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

                out = (LinkedList) this.channels.clone();
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
                channels = this.channels;
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
//                        readChannelAttributes(channel);
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

    @Override
    public String localizeString(String str, Locale locale) throws LocalizationNotPossibleException {
        return this.localizer.localizeString(str, locale);
    }

    @Override
    public TestSuitResult testChannel(Channel channel, Locale locale) {
        String SIGNATURE = "testChannel(Channel channel, Locale locale)";
        TRACE.entering(SIGNATURE, new Object[]{channel, locale});
        TestSuitResult tsr = new TestSuitResult();
        tsr.setOverallResult(2);
        ChannelProperties cprop = new ChannelProperties();
        cprop.readChannelAttributes(channel);
        TestResult tr = new SingleTestResult(2, "properties", cprop.toString());
        tsr.addTestResult(tr);
        tr = new SingleTestResult(2, "centralFileLogDirectory", centralFileLogDirectory);
        tsr.addTestResult(tr);
        return tsr;
    }

}

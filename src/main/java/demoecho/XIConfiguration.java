
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
import com.sap.aii.af.service.administration.api.monitoring.ChannelState;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatus;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatusCallback;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatusFactory;
import com.sap.aii.af.service.administration.api.monitoring.ChannelUnknownException;
import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;
import com.sap.aii.af.service.cpa.NormalizationManager;
import com.sap.aii.af.service.cpa.Party;
import com.sap.aii.af.service.cpa.PartyCallBackController;
import com.sap.aii.af.service.cpa.PartyIdentifier;
import java.io.File;
import java.util.LinkedList;
import java.util.Locale;
import javax.resource.ResourceException;

public class XIConfiguration implements ChannelLifecycleCallback, ChannelStatusCallback, LocalizationCallback {
    private static final XITrace TRACE = new XITrace(XIConfiguration.class.getName());
    private static final String DEFAULT_AGENCY = "http://sap.com/xi/XI";
    private static final String DEFAULT_SCHEMA = "XIParty";
    public static final String ADAPTER_TYPE = EchoAdapterConstants.adapterType;
    public static final String ADAPTER_NAMESPACE = EchoAdapterConstants.adapterNamespace;
    private String adapterType;
    private String adapterNamespace;
    private LinkedList outboundChannels;
    private LinkedList inboundChannels;
    private CPALookupManager lookupManager;
    private AdapterRegistry adapterRegistry;
    private LocalizationCallback localizer;
    private PartyChangeCallBackHandler partyChangeCallBackHandler;
    private SPIManagedConnectionFactory mcf;

    public XIConfiguration() {
        this(ADAPTER_TYPE, ADAPTER_NAMESPACE);
    }

    public XIConfiguration(String adapterType, String adapterNamespace) {
        this.outboundChannels = null;
        this.inboundChannels = null;
        this.lookupManager = null;
        this.adapterRegistry = null;
        this.localizer = null;
        this.partyChangeCallBackHandler = null;
        this.mcf = null;
        String SIGNATURE = "XIConfiguration(String adapterType, String adapterNamespace)";
        TRACE.entering("XIConfiguration(String adapterType, String adapterNamespace)", new Object[]{adapterType, adapterNamespace});

        try {
            CPAFactory cf = CPAFactory.getInstance();
            this.lookupManager = cf.getLookupManager();
            this.partyChangeCallBackHandler = PartyChangeCallBackHandler.getInstance();
        } catch (Exception e) {
            TRACE.catching("XIConfiguration(String adapterType, String adapterNamespace)", e);
            TRACE.errorT("XIConfiguration(String adapterType, String adapterNamespace)", XIAdapterCategories.CONFIG, "SOA.apt_sample.0040", "CPALookupManager cannot be instantiated due to {0}", new Object[]{e.getMessage()});
            TRACE.errorT("XIConfiguration(String adapterType, String adapterNamespace)", XIAdapterCategories.CONFIG, "SOA.apt_sample.0041", "No channel configuration can be read, no message exchange possible!");
        }

        this.adapterType = adapterType;
        this.adapterNamespace = adapterNamespace;
        TRACE.exiting("XIConfiguration(String adapterType, String adapterNamespace)");
    }

    public void channelAdded(Channel channel) {
        String SIGNATURE = "channelAdded(Channel channel)";
        TRACE.entering("channelAdded(Channel channel)", new Object[]{channel});
//        String dir = null;
//        String name = null;
        synchronized(this) {
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
                    //channel.getValueAsString("azaza")
//                    dir = channel.getValueAsString("fileOutDir");
//                    name = channel.getValueAsString("fileOutPrefix");
                } catch (Exception e) {
                    TRACE.catching("channelAdded(Channel channel)", e);
                    TRACE.errorT("channelAdded(Channel channel)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0043", "Channel configuration value cannot be read due to {0}", new Object[]{e.getMessage()});
                }
            }

            try {
                PartyCallBackController.getInstance().registerForPartyEvent(channel.getParty(), this.partyChangeCallBackHandler);
                Party party = NormalizationManager.getInstance().getXIParty("http://sap.com/xi/XI", "XIParty", channel.getParty());

                for(PartyIdentifier partyIdentifier : this.lookupManager.getPartyIdentifiersByParty(party)) {
                    String schema = partyIdentifier.getPartySchema();
                    if (schema.equals("DUNS")) {
                        this.partyChangeCallBackHandler.addParty(channel.getParty(), partyIdentifier.getPartyIdentifier());
                    }
                }
            } catch (Exception e) {
                TRACE.catching("channelAdded(Channel channel)", e);
                TRACE.errorT("channelAdded(Channel channel)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0044", "Party Cannot be registered for Callback due to {0}", new Object[]{e.getMessage()});
            }
        }

        TRACE.infoT("channelAdded(Channel channel)", XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} added (direction is {3}).", new Object[]{channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()});
        TRACE.exiting("channelAdded(Channel channel)");
    }

    public void channelUpdated(Channel channel) {
        String SIGNATURE = "channelUpdated(Channel channel)";
        TRACE.entering("channelUpdated(Channel channel)");
        this.channelRemoved(channel);
        this.channelAdded(channel);
        TRACE.exiting("channelUpdated(Channel channel)");
    }

    public void channelRemoved(Channel channel) {
        String SIGNATURE = "channelRemoved(Channel channel)";
        TRACE.entering("channelRemoved(Channel channel)", new Object[]{channel});
        LinkedList channels = null;
        TRACE.infoT("channelRemoved(Channel channel)", XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} will be removed now. (direction is {3}).", new Object[]{channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()});
        String channelID = channel.getObjectId();
        if (channel.getDirection() == Direction.INBOUND) {
            channels = this.inboundChannels;
        } else {
            channels = this.outboundChannels;
        }

        try {
            PartyCallBackController.getInstance().unregisterForPartyEvent(channel.getParty(), this.partyChangeCallBackHandler);
            this.partyChangeCallBackHandler.removeParty(channel.getParty());
        } catch (Exception e) {
            TRACE.catching("channelRemoved(Channel channel)", e);
            TRACE.errorT("channelRemoved(Channel channel)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0045", "Party Cannot be unregistered for Callback due to {0}", new Object[]{e.getMessage()});
        }

        synchronized(this) {
            for(int i = 0; i < channels.size(); ++i) {
                Channel storedChannel = (Channel)channels.get(i);
                if (storedChannel.getObjectId().equalsIgnoreCase(channelID)) {
                    channels.remove(i);
                    if (channel.getDirection() == Direction.OUTBOUND) {
                        try {
                            this.mcf.destroyManagedConnection(channelID);
                        } catch (Exception e) {
                            TRACE.catching("channelRemoved(Channel channel)", e);
                            TRACE.warningT("channelRemoved(Channel channel)", XIAdapterCategories.CONNECT_AF, "The ManagedConnection for channel {0} cannot be destroyed. Configuration update might not work.", new Object[]{channelID});
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
        TRACE.entering("init(mcf)");
//        String dir = null;
//        String name = null;
        this.mcf = mcf;

        try {
            this.localizer = XILocalizationUtilities.getLocalizationCallback();
            AdapterRegistryFactory arf = AdapterRegistryFactory.getInstance();
            this.adapterRegistry = arf.getAdapterRegistry();
            this.adapterRegistry.registerAdapter(this.adapterNamespace, this.adapterType, new AdapterCapability[]{AdapterCapability.PUSH_PROCESS_STATUS}, new AdapterCallback[]{this});
        } catch (Exception e) {
            TRACE.catching("init(mcf)", e);
            ResourceException re = new ResourceException("XI AAM registration failed due to: " + e.getMessage());
            TRACE.throwing("init(mcf)", re);
            throw re;
        }

        synchronized(this) {
            this.inboundChannels = new LinkedList();
            this.outboundChannels = new LinkedList();

            try {
                LinkedList allChannels = this.lookupManager.getChannelsByAdapterType(this.adapterType, this.adapterNamespace);
                TRACE.debugT("init(mcf)", XIAdapterCategories.CONNECT_AF, "The XI AAM service returned {0} channels for adapter type {1} with namespace {2}", new Object[]{new Integer(allChannels.size()), this.adapterType, this.adapterNamespace});

                for(int i = 0; i < allChannels.size(); ++i) {
                    Channel channel = (Channel)allChannels.get(i);
                    if (channel.getDirection() == Direction.INBOUND) {
                        this.inboundChannels.add(channel);
//                        dir = channel.getValueAsString("fileInDir");
//                        name = channel.getValueAsString("fileInName");
                    } else {
                        if (channel.getDirection() != Direction.OUTBOUND) {
                            continue;
                        }

                        this.outboundChannels.add(channel);
//                        dir = channel.getValueAsString("fileOutDir");
//                        name = channel.getValueAsString("fileOutPrefix");
                    }

                    TRACE.infoT("init(mcf)", XIAdapterCategories.CONNECT_AF, "Channel with ID {0} for party {1} and service {2} added (direction is {3}).", new Object[]{channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString()});
                }
            } catch (Exception e) {
                TRACE.catching("init(mcf)", e);
                ResourceException re = new ResourceException("XI CPA lookup failed due to: " + e.getMessage());
                TRACE.throwing("init(mcf)", re);
                throw re;
            }
        }

        TRACE.exiting("init(mcf)");
    }

    public void stop() throws ResourceException {
        String SIGNATURE = "stop()";
        TRACE.entering("stop()");

        try {
            try {
                for(String partyName : this.partyChangeCallBackHandler.getRegisteredParties()) {
                    PartyCallBackController.getInstance().unregisterForPartyEvent(partyName, this.partyChangeCallBackHandler);
                }

                this.partyChangeCallBackHandler.clear();
            } catch (CPAException e) {
                TRACE.catching("stop()", e);
            }

            this.adapterRegistry.unregisterAdapter(this.adapterNamespace, this.adapterType);
        } catch (Exception e) {
            TRACE.catching("stop()", e);
            ResourceException re = new ResourceException("XI AAM unregistration failed due to: " + e.getMessage());
            TRACE.throwing("stop()", re);
            throw re;
        }

        TRACE.exiting("stop()");
    }

    public LinkedList getCopy(Direction direction) throws ResourceException {
        String SIGNATURE = "getCopy(Direction direction)";
        LinkedList out = null;
        if (this.inboundChannels == null || this.outboundChannels == null) {
            this.init(this.mcf);
        }

        synchronized(this) {
            if (direction == Direction.INBOUND) {
                out = (LinkedList)this.inboundChannels.clone();
            } else {
                if (direction != Direction.OUTBOUND) {
                    ResourceException re = new ResourceException("Direction invalid");
                    TRACE.throwing("getCopy(Direction direction)", re);
                    throw re;
                }

                out = (LinkedList)this.outboundChannels.clone();
            }

            return out;
        }
    }

    public ChannelStatus getChannelStatus(Channel channel, Locale locale) throws ChannelUnknownException {
        String SIGNATURE = "getChannelStatus(Channel channel, Locale locale)";
        TRACE.entering("getChannelStatus(Channel channel, Locale locale)", new Object[]{channel, locale});
        boolean channelFound = false;
        Channel storedChannel = null;
        String channelID = "<unknown>";
        Exception cause = null;
        ChannelStatus cs = null;

        try {
            channelID = channel.getObjectId();
            LinkedList channels = null;
            if (channel.getDirection() == Direction.INBOUND) {
                channels = this.inboundChannels;
            } else {
                channels = this.outboundChannels;
            }

            synchronized(this) {
                for(int i = 0; i < channels.size(); ++i) {
                    storedChannel = (Channel)channels.get(i);
                    if (storedChannel.getObjectId().equalsIgnoreCase(channelID)) {
                        channelFound = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            TRACE.catching("getChannelStatus(Channel channel, Locale locale)", e);
            cause = e;
            TRACE.errorT("getChannelStatus(Channel channel, Locale locale)", XIAdapterCategories.CONFIG, "SOA.apt_sample.0046", "Channel lookup failed due to {0}.", new Object[]{e.getMessage()});
        }

        if (!channelFound) {
            ChannelUnknownException cue = new ChannelUnknownException("Channel with ID " + channelID + " is not known.", cause);
            TRACE.errorT("getChannelStatus(Channel channel, Locale locale)", XIAdapterCategories.CONFIG, "SOA.apt_sample.0047", "Channel {0} is not known.", new Object[]{channelID});
            TRACE.throwing("getChannelStatus(Channel channel, Locale locale)", cue);
            throw cue;
        } else {
            ChannelStatusFactory csf = ChannelStatusFactory.getInstance();
            if (csf == null) {
                ChannelUnknownException cue = new ChannelUnknownException("Internal error: Unable to get instance of ChannelStatusFactory.", cause);
                TRACE.errorT("getChannelStatus(Channel channel, Locale locale)", XIAdapterCategories.CONFIG, "SOA.apt_sample.0048", "Unable to get instance of ChannelStatusFactory.");
                TRACE.throwing("getChannelStatus(Channel channel, Locale locale)", cue);
                throw cue;
            } else {
                try {
                    if (storedChannel.getDirection() != Direction.INBOUND) {
                        String directory = "."; // channel.getValueAsString("fileOutDir");
                        if (directory == null || directory.length() == 0) {
                            cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Output file directory name is not set.");
                            TRACE.exiting("getChannelStatus(Channel channel, Locale locale)", new Object[]{cs});
                            return cs;
                        }

                        File dir = new File(directory);
                        if (!dir.exists()) {
                            cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Output file directory " + directory + " does not exists.");
                            TRACE.exiting("getChannelStatus(Channel channel, Locale locale)", new Object[]{cs});
                            return cs;
                        }
                    } else {
                        String directory = "."; //channel.getValueAsString("fileInDir");
                        if (directory == null || directory.length() == 0) {
                            TRACE.warningT("getChannelStatus(Channel channel, Locale locale)", XIAdapterCategories.CONFIG, "Unable to determine input file directory. Take default: c:/temp");
                            directory = "c:/temp";
                        }

                        String name = "fakeFileInName"; //channel.getValueAsString("fileInName");
                        if (name == null || name.length() == 0) {
                            TRACE.warningT("getChannelStatus(Channel channel, Locale locale)", XIAdapterCategories.CONFIG, "Unable to determine input file prefix. Take default: sample_ra_input");
                            name = "sample_ra_input";
                        }

                        File dir = new File(directory);
                        if (!dir.exists()) {
                            cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Input file directory " + directory + " does not exists.");
                            TRACE.exiting("getChannelStatus(Channel channel, Locale locale)", new Object[]{cs});
                            return cs;
                        }

                        if (!this.mcf.isRunning()) {
                            cs = csf.createChannelStatus(channel, ChannelState.ERROR, "The JCA adapter inbound thread is not working correctly. No inbound messages possible!");
                            TRACE.exiting("getChannelStatus(Channel channel, Locale locale)", new Object[]{cs});
                            return cs;
                        }
                    }

                    cs = csf.createChannelStatus(channel, ChannelState.OK, this.localizer.localizeString("CHANNEL_OK", locale));
                } catch (Exception e) {
                    TRACE.catching("getChannelStatus(Channel channel, Locale locale)", e);
                    TRACE.errorT("getChannelStatus(Channel channel, Locale locale)", XIAdapterCategories.CONNECT_AF, "SOA.apt_sample.0049", "Cannot retrieve status for channel {0}. Received exception: {1}", new Object[]{channel.getChannelName(), e.getMessage()});
                    cs = csf.createChannelStatus(channel, ChannelState.ERROR, "Cannot retrieve status for this channel due to: " + e.getMessage());
                    TRACE.exiting("getChannelStatus(Channel channel, Locale locale)", new Object[]{cs});
                    return cs;
                }

                TRACE.exiting("getChannelStatus(Channel channel, Locale locale)", new Object[]{cs});
                return cs;
            }
        }
    }

    public String localizeString(String str, Locale locale) throws LocalizationNotPossibleException {
        return this.localizer.localizeString(str, locale);
    }
}

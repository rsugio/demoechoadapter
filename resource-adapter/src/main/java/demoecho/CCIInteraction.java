package demoecho;

import com.sap.aii.adapter.xi.ms.XIErrorInfoImpl;
import com.sap.aii.adapter.xi.ms.XIMessage;
import com.sap.aii.af.lib.ra.cci.XIInteraction;
import com.sap.aii.af.lib.ra.cci.XIInteractionSpec;
import com.sap.aii.af.lib.ra.cci.XIMessageRecord;
import com.sap.aii.af.sdk.xi.util.ErrorCategory;
import com.sap.aii.af.service.administration.api.cpa.CPAChannelStoppedException;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPALookupManager;
import com.sap.aii.af.service.administration.api.cpa.CPAOutboundRuntimeLookupManager;
import com.sap.aii.af.service.cpa.*;
import com.sap.aii.af.service.headermapping.HeaderMapper;
import com.sap.aii.af.service.headermapping.HeaderMappingException;
import com.sap.engine.interfaces.messaging.api.*;
import com.sap.engine.interfaces.messaging.api.Party;
import com.sap.engine.interfaces.messaging.api.Service;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.exception.InvalidParamException;
import com.sap.engine.interfaces.messaging.api.exception.MessagingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;
import javax.resource.spi.IllegalStateException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class CCIInteraction implements XIInteraction {
    private static final String CLASSNAME = CCIInteraction.class.getSimpleName();
    private static final XITrace TRACE = new XITrace(CCIInteraction.class.getName());
    // устанавливается в конструкторе, закрывается в close
    private CCIConnection connection;
    private final XIMessageFactoryImpl mf;
    private final SPIManagedConnection mc;
    private final SPIManagedConnectionFactory mcf;
    private final AuditAccess audit;
    private final CentralFileLog centralFileLog;

    public CCIInteraction(Connection cciConnection) throws ResourceException {
        String SIGNATURE = "CciInteraction(Connection cciConnection)";
        TRACE.entering(SIGNATURE, new Object[]{cciConnection});
        if (cciConnection == null) {
            ResourceException re = new ResourceException("No related CCI connection in Interaction (cciConnection is null).");
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }
        this.connection = (CCIConnection) cciConnection;
        this.mc = (this.connection).getManagedConnection();
        if (this.mc == null) {
            ResourceException re = new ResourceException("No related managed connection in CCI connection (mc is null).");
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }
        this.mcf = (SPIManagedConnectionFactory) this.mc.getManagedConnectionFactory();
        if (this.mcf == null) {
            ResourceException re = new ResourceException("No related managed connection factory in managed connection (mcf is null).");
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }
        this.audit = this.mcf.getAuditAccess();
        this.mf = this.mcf.getXIMessageFactoryImpl();
        this.centralFileLog = mcf.centralFileLogDirectory;
        centralFileLog.log_init("%s exiting for CCIConnection=%s (%s)", SIGNATURE, connection, connection.getMetaData());
        TRACE.exiting(SIGNATURE);
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void close() throws ResourceException {
        String SIGNATURE = "close()";
        TRACE.entering(SIGNATURE);
        this.connection = null;
        centralFileLog.log_init("%s.%s", CLASSNAME, SIGNATURE);
        TRACE.exiting(SIGNATURE);
    }

    @Override
    public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {
        String SIGNATURE = "execute(InteractionSpec ispec, Record input, Record output)";
        TRACE.entering(SIGNATURE);
        centralFileLog.log_init("%s.%s ispec=%s input=%s output=%s", CLASSNAME, SIGNATURE, ispec, input, output);
        if (!(output instanceof XIMessageRecord)) {
            ResourceException re = new ResourceException("Output record is no XI AF XIMessageRecord.");
            TRACE.throwing("execute(InteractionSpec ispec, Record input, Record output)", re);
            throw re;
        }
        XIMessageRecord callerOutput = (XIMessageRecord) output;
        XIMessageRecord localOutput = (XIMessageRecord) this.execute(ispec, input);

        try {
            callerOutput.setXIMessage(localOutput.getXIMessage());
            callerOutput.setRecordName(localOutput.getRecordName());
            callerOutput.setRecordShortDescription(localOutput.getRecordShortDescription());
        } catch (Exception e) {
            TRACE.catching("execute(InteractionSpec ispec, Record input, Record output)", e);
            TRACE.errorT("execute(InteractionSpec ispec, Record input, Record output)", XIAdapterCategories.CONNECT, "SOA.apt_sample.0002", "Exception during output record transfer. Reason: {0}", new Object[]{e.getMessage()});
            ResourceException re = new ResourceException("Output record cannot be filled. Reason: " + e.getMessage());
            TRACE.throwing("execute(InteractionSpec ispec, Record input, Record output)", re);
            throw re;
        }

        TRACE.exiting("execute(InteractionSpec ispec, Record input, Record output)");
        return true;

    }

    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {
        String SIGNATURE = "execute(InteractionSpec ispec, Record input)";
        TRACE.entering(SIGNATURE, new Object[]{ispec, input});
        Record output;
        if (ispec == null) {
            ResourceException re = new ResourceException("Input ispec is null.");
            TRACE.throwing(SIGNATURE, re);
            throw re;
        } else if (!(ispec instanceof XIInteractionSpec)) {
            ResourceException re = new ResourceException("Input ispec is no XI AF InteractionSpec.");
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }
        XIInteractionSpec XIIspec = (XIInteractionSpec) ispec;
        String method = XIIspec.getFunctionName();
        XIMessageRecord xiMessageRecord = (XIMessageRecord) input;
        XIMessage xiMsg = (XIMessage) xiMessageRecord.getXIMessage();
        CPALookupManager cpaLookupManager = CPAFactory.getInstance().getLookupManager();
        Channel channel;
        try {
            channel = cpaLookupManager.getCPAObject(CPAObjectType.CHANNEL, mc.getChannelID());
        } catch (CPAException e) {
            centralFileLog.log_init("ERROR: cpaLookupManager.getCPAObject(CPAObjectType.CHANNEL, %s): %s\t%s", mc.getChannelID(), e.getMessage(), e.getCause());
            ResourceException re = new ResourceException("cpaLookupManager.getCPAObject(CPAObjectType.CHANNEL," + mc.getChannelID() + ")");
            TRACE.throwing(SIGNATURE, re);
            throw re;
        }
        Objects.requireNonNull(channel);
        centralFileLog.log_init("Found receiver channel: oid=%s, %s|%s|%s, engine=%s", channel.getObjectId(), channel.getParty(), channel.getService(), channel.getChannelName(), channel.getEngineName());

        //com.sap.aii.af.service.administration.impl.cpa.OutboundRuntimeLookupDelegate
        CPAOutboundRuntimeLookupManager outLookup;
        // com.sap.aii.af.service.cpa.impl.object.BindingImpl
        Binding binding;
        try {
            outLookup = CPAFactory.getInstance().createOutboundRuntimeLookupManager(this.mcf.getAdapterType(), this.mcf.getAdapterNamespace(), xiMsg.getFromParty().toString(), xiMsg.getToParty().toString(), xiMsg.getFromService().toString(), xiMsg.getToService().toString(), xiMsg.getAction().getName(), xiMsg.getAction().getType());
        } catch (CPAChannelStoppedException cse) {
            if (method.equals(XIInteractionSpec.CALL)) {
                String s = String.format("Receiver channel oid=%s %s|%s|%s is stopped, cannot deliver messageid=%s", channel.getObjectId(), channel.getParty(), channel.getService(), channel.getChannelName(), xiMsg.getMessageId());
                ResourceException re = new ResourceException(s, cse.getCause());
                TRACE.throwing(SIGNATURE, re);
                centralFileLog.log_init(s);
                throw re;
            } else {
                throw new NotSupportedException("Async not supported");
            }
        } catch (CPAException e) {
            String s = String.format("Receiver channel oid=%s %s|%s|%s got CPAException %s, cannot deliver messageid=%s", channel.getObjectId(), channel.getParty(), channel.getService(), channel.getChannelName(), e.getMessage(), xiMsg.getMessageId());
            ResourceException re = new ResourceException(s, e.getCause());
            TRACE.throwing(SIGNATURE, re);
            centralFileLog.log_init(s);
            throw re;
        }

        //com.sap.aii.af.service.cpa.impl.cache.CMLCacheManager.getInstance().getCPAObject(CPAObjectType.BINDING.toString(), "CID=" + channel.getObjectId());
        centralFileLog.log_init("createOutboundRuntimeLookupManager=%s", outLookup);
        byte[] rawHeaderMappingData = null;
        try {
            binding = cpaLookupManager.getBindingByChannelId(channel.getObjectId());
            rawHeaderMappingData = outLookup.getHeaderMappingConfig();
        } catch (CPAException e) {
            String s = String.format("Receiver channel oid=%s %s|%s|%s when getting binding got CPAException %s, cannot deliver messageid=%s", channel.getObjectId(), channel.getParty(), channel.getService(), channel.getChannelName(), e.getMessage(), xiMsg.getMessageId());
            ResourceException re = new ResourceException(s, e.getCause());
            TRACE.throwing(SIGNATURE, re);
            centralFileLog.log_init(s);
            throw re;
        }

        centralFileLog.log_init("binding=%s channelid=%s, %s|%s|%s|%s|{%s}%s {%s}%s %s %s %s", binding, binding.getChannelId(), binding.getFromParty(), binding.getFromService(), binding.getToParty(), binding.getToService(), binding.getActionNamespace(), binding.getActionName(), binding.getMappedActionNamespace(), binding.getMappedActionName(), Arrays.toString(binding.getHeaderMappingConfig()), binding.getAttributes(), binding.getInterfaceVersion());

        String fromParty = null;
        String fromService = null;
        String toParty = null;
        String toService = null;
        PartyIdentifier fromPartyIdentifier = null;
        PartyIdentifier toPartyIdentifier = null;
        ServiceIdentifier fromServiceIdentifier = null;
        ServiceIdentifier toServiceIdentifier = null;


        String[] result = getMappedHeaderFieldsAndNormalize(outLookup, binding, channel, xiMsg);

        switch (method) {
            case XIInteractionSpec.SEND:
                //async receiver
                centralFileLog.log_init("%s.%s method=Send (EO[IO] receiver), input messageId=%s", CLASSNAME, SIGNATURE, xiMsg.getMessageId());
                output = receive(XIIspec, input, this.mc);
                break;
            case XIInteractionSpec.CALL:
                //sync receiver, сам создаёт выходную запись
                centralFileLog.log_init("%s.%s method=Call (BE receiver), input messageId=%s", CLASSNAME, SIGNATURE, xiMsg.getMessageId());
                output = callBE(XIIspec, xiMessageRecord, this.mc, channel, binding, xiMsg, result);
                break;
            default:
                throw new IllegalStateException("not SAP");
        }
        centralFileLog.log_init("public Record execute(InteractionSpec ispec, Record input) finished");

        TRACE.exiting(SIGNATURE);
        return output;
    }

    public ResourceWarning getWarnings() throws ResourceException {
        return null;
    }

    public void clearWarnings() throws ResourceException {
    }

    private Record receive(InteractionSpec ispec, Record input, SPIManagedConnection mc) throws ResourceException {
        String SIGNATURE = "send(InteractionSpec ispec, Record input, SpiManagedConnection mc)";
        TRACE.entering(SIGNATURE, new Object[]{ispec, input, mc});
        throw new RuntimeException("SEND (ASYNC) NOT SUPPORTED YET");

    }

    private String[] getMappedHeaderFieldsAndNormalize(CPAOutboundRuntimeLookupManager outLookup, Binding binding, Channel channel, Message msg) {
        String SIGNATURE = "getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)";
        TRACE.entering(SIGNATURE);
        String fromParty = null;
        String fromService = null;
        String toParty = null;
        String toService = null;
        PartyIdentifier fromPartyIdentifier;
        PartyIdentifier toPartyIdentifier;
        ServiceIdentifier fromServiceIdentifier;
        ServiceIdentifier toServiceIdentifier;

        try {
//            TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Get receiver agreement with OutboundRuntimeLookup now.");
//            CPAOutboundRuntimeLookupManager outLookup = CPAFactory.getInstance().createOutboundRuntimeLookupManager(this.mcf.getAdapterType(), this.mcf.getAdapterNamespace(), msg.getFromParty().toString(), msg.getToParty().toString(), msg.getFromService().toString(), msg.getToService().toString(), msg.getAction().getName(), msg.getAction().getType());
//            Binding binding = outLookup.getBinding();
//            TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Get receiver agreement for channel ID {0} now.", new Object[]{channelID});
//            Binding bindingByChannel = CPAFactory.getInstance().getLookupManager().getBindingByChannelId(channelID);
////            this.readSampleConfiguration(outLookup, bindingByChannel);
//            Channel channelFromBinding = outLookup.getChannel();
            byte[] rawHeaderMappingData = outLookup.getHeaderMappingConfig();
            TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Get header mappings for message with ID {0} and receiver agreement with ID {1} now.", new Object[]{msg.getMessageId(), binding.getObjectId()});

            try {
                Map mappedFields = HeaderMapper.getMappedHeader(msg, binding);
                centralFileLog.log_init("getMappedHeaderFieldsAndNormalize mappedFields=%s", mappedFields);
                if (mappedFields != null && !mappedFields.isEmpty()) {
                    if ((fromParty = (String) mappedFields.get(HeaderMapper.FROM_PARTY)) != null) {
                        TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Header mapping: From party {0} is mapped to {1}", new Object[]{msg.getFromParty().toString(), fromParty});
                    }

                    if ((fromService = (String) mappedFields.get(HeaderMapper.FROM_SERVICE)) != null) {
                        TRACE.debugT(SIGNATURE, XIAdapterCategories.CONNECT, "Header mapping: From service {0} is mapped to {1}", new Object[]{msg.getFromService().toString(), fromService});
                    }

                    if ((toParty = (String) mappedFields.get(HeaderMapper.TO_PARTY)) != null) {
                        TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Header mapping: To party {0} is mapped to {1}", new Object[]{msg.getToParty().toString(), toParty});
                    }

                    if ((toService = (String) mappedFields.get(HeaderMapper.TO_SERVICE)) != null) {
                        TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Header mapping: To service {0} is mapped to {1}", new Object[]{msg.getToService().toString(), toService});
                    }
                } else {
                    TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Header mapping is not defined for receiver agreement: {0}", new Object[]{binding.getStringRepresentation()});
                }
            } catch (HeaderMappingException he) {
//                TRACE.catching("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", he);
//                throw new HeaderMappingException(he.getMessage());
            }
        } catch (Exception e) {
//            TRACE.catching("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", e);
//            TRACE.errorT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "SOA.apt_sample.0004", "Exception during header mapping. Reason: {0}. Error will be ignored.", new Object[]{e.getMessage()});
        }

        if (fromParty == null) {
            fromParty = msg.getFromParty().toString();
        }

        if (fromService == null) {
            fromService = msg.getFromService().toString();
        }

        if (toParty == null) {
            toParty = msg.getToParty().toString();
        }

        if (toService == null) {
            toService = msg.getToService().toString();
        }

        try {
            TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Access the normalization manager now.");
            NormalizationManager normalizer = NormalizationManager.getInstance();
            fromServiceIdentifier = normalizer.getAlternativeServiceIdentifier(fromParty, fromService, "GLN");
            if (fromServiceIdentifier != null && fromServiceIdentifier.getServiceIdentifier() != null && fromServiceIdentifier.getServiceIdentifier().length() > 0) {
                TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Address normalization for service: {0} is: {1}", new Object[]{fromService, fromServiceIdentifier.getServiceIdentifier()});
                fromService = fromServiceIdentifier.getServiceIdentifier();
            } else {
                TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Address normalization is not defined for service: {0}", new Object[]{fromService});
            }

            fromPartyIdentifier = normalizer.getAlternativePartyIdentifier("009", "GLN", fromParty);
            if (fromPartyIdentifier != null && fromPartyIdentifier.getParty() != null && fromPartyIdentifier.getParty().length() > 0) {
                TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Address normalization for party: {0} is: {1}", new Object[]{fromParty, fromPartyIdentifier.getPartyIdentifier()});
                fromParty = fromPartyIdentifier.getPartyIdentifier();
            } else {
                TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Address normalization is not defined for party: {0}", new Object[]{fromParty});
            }

            toServiceIdentifier = normalizer.getAlternativeServiceIdentifier(toParty, toService, "GLN");
            if (toServiceIdentifier != null && toServiceIdentifier.getServiceIdentifier() != null && toServiceIdentifier.getServiceIdentifier().length() > 0) {
                TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Address normalization for service: {0} is: {1}", new Object[]{toService, toServiceIdentifier.getServiceIdentifier()});
                toService = toServiceIdentifier.getServiceIdentifier();
            } else {
                TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Address normalization is not defined for service: {0}", new Object[]{toService});
            }

            toPartyIdentifier = normalizer.getAlternativePartyIdentifier("009", "GLN", toParty);
            if (toPartyIdentifier != null && toPartyIdentifier.getParty() != null && toPartyIdentifier.getParty().length() > 0) {
                TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Address normalization for party: {0} is: {1}", new Object[]{toParty, toPartyIdentifier.getPartyIdentifier()});
                toParty = toPartyIdentifier.getPartyIdentifier();
            } else {
                TRACE.debugT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "Address normalization is not defined for party: {0}", new Object[]{toParty});
            }
        } catch (Exception e) {
            TRACE.catching("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", e);
            TRACE.errorT("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)", XIAdapterCategories.CONNECT, "SOA.apt_sample.0005", "Exception during address normalization. Reason: {0}. Error will be ignored.", new Object[]{e.getMessage()});
        }

        String[] result = new String[4];
        result[0] = fromParty;
        result[1] = fromService;
        result[2] = toParty;
        result[3] = toService;
        TRACE.exiting("getMappedHeaderFields(String channelID, Message msg, String fromParty, String fromService, String toParty, String toService)");
        return result;
    }

    private String[] getFaultIF(String channelID) {
        String SIGNATURE = "getFaultIF(String channelID)";
        TRACE.entering(SIGNATURE, new Object[]{channelID});
        String[] result = new String[2];

        try {
            TRACE.debugT("getFaultIF(String channelID)", XIAdapterCategories.CONNECT, "Get channel CPA object with channelID {0}", new Object[]{channelID});
            Channel channel = (Channel) CPAFactory.getInstance().getLookupManager().getCPAObject(CPAObjectType.CHANNEL, channelID);
            result[0] = channel.getValueAsString("faultInterface");
            result[1] = channel.getValueAsString("faultInterfaceNamespace");
            TRACE.debugT("getFaultIF(String channelID)", XIAdapterCategories.CONNECT_AF, "Read this fault interface value: Name: {0} Namespace: {1}", new Object[]{result[0], result[1]});
        } catch (Exception e) {
            TRACE.catching("getFaultIF(String channelID)", e);
            result[0] = "XIAFJCASampleFault";
            result[1] = "http://sap.com/xi/XI/sample/JCA";
            TRACE.debugT("getFaultIF(String channelID)", XIAdapterCategories.CONNECT_AF, "Fault interface cannot be read from channel configuration due to {0}. Take defaults value: Name: {1} Namespace: {2}", new Object[]{e.getMessage(), result[0], result[1]});
        }

        TRACE.exiting("getFaultIF(String channelID)");
        return result;
    }

    /**
     * Обработка синхронного (BE, BestEffort) получателя (Receiver / Outbound)
     *
     * @param ispec
     * @param input
     * @param mc
     * @return
     * @throws ResourceException
     */
    XIMessageRecordImpl callBE(XIInteractionSpec ispec, XIMessageRecord input, SPIManagedConnection mc, Channel channel, Binding binding, XIMessage xiMsg, String[] result) throws ResourceException {
        Objects.requireNonNull(ispec);
        Objects.requireNonNull(input);
        Objects.requireNonNull(mc);
        Objects.requireNonNull(channel);

        String SIGNATURE = "callBE(XIInteractionSpec ispec, XIMessageRecord input, SPIManagedConnection mc)";
        TRACE.entering(SIGNATURE, new Object[]{ispec, input, mc});

        ChannelProperties channelProperties = new ChannelProperties();
        channelProperties.readChannelAttributes(channel);
        centralFileLog.log_init("Channel properties: %s", channelProperties);

//        MessageKey amk = new MessageKey(xiMsg.getMessageId(), MessageDirection.INBOUND);
        String fromParty = result[0];
        String fromService = result[1];
        String toParty = result[2];
        String toService = result[3];
        Payload appPayLoad = xiMsg.getDocument();
        String payText = new String(appPayLoad.getContent());
        StringBuilder msgLog = new StringBuilder();
        msgLog.append(String.format("Payload content-type: %s, name: %s, description: %s\n", appPayLoad.getContentType(), appPayLoad.getName(), appPayLoad.getDescription()));
        for (String n : appPayLoad.getAttributeNames()) {
            String v = appPayLoad.getAttribute(n);
            msgLog.append(String.format("Document attribute %s: %s\n", n, v));
        }
        msgLog.append(String.format("Payload text:\n%s\n<end of payload>\n", payText));
        // recordName бесполезная инфа
        //msgLog.append(String.format("recordName: %s, recordShortDescription: %s\n", input.getRecordName(), input.getRecordShortDescription()));

        boolean doError = channelProperties.throwFault.equals(EchoAdapterConstants.throwAlways);
        if (!doError && channelProperties.throwFault.equals(EchoAdapterConstants.throwXPath)) {
            // Parse payText
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = null;
            try {
                doc = dbf.newDocumentBuilder().parse(appPayLoad.getInputStream());
                Element root = doc.getDocumentElement();
                doError = root.getLocalName().equals(channelProperties.faultMessageTypeName) && root.getNamespaceURI() != null && root.getNamespaceURI().equals(channelProperties.faultMessageTypeNS);
                if (doError) {
                    msgLog.append(String.format("throwFault=%s triggered for source document!\n", EchoAdapterConstants.throwXPath));
                } else {
                    msgLog.append(String.format("throwFault=%s doesn't match given input {%s}%s\n", EchoAdapterConstants.throwXPath, root.getNamespaceURI(), root.getLocalName()));
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                // ошибка не на входе а в процессе адаптера
                doError = true;
                msgLog.append("XML parsing exception: " + e.getMessage() + "\n");
            }
        }

        Action action;
        XIMessageRecordImpl output;
        if (!doError) {
            action = new Action(xiMsg.getInterfaceName(), xiMsg.getInterfaceNamespace());
//        TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT, "Payload contains the <ApplicationError> tag that causes a application error response for testing purposes!");
//        this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Simulate application error response now.");
        } else {
            //String[] faultIF;
            //faultIF = new String[]{"FMT_Fault", "urn:demo"}; //this.getFaultIF(mc.getChannelID());
//          faultIF = new String[]{"SI_Demo_InSync", "urn:demo"}; //this.getFaultIF(mc.getChannelID());
            action = new Action(channelProperties.faultMessageTypeName, channelProperties.faultMessageTypeNS);
        }
        output = new XIMessageRecordImpl(new Party(toParty), new Party(fromParty), new Service(toService), new Service(fromService), action);
        XIMessage response = (XIMessage) output.getXIMessage();

        String s = "<x:FMT_Demo xmlns:x=\"urn:demo\"><standard><faultText>ф1</faultText><faultUrl>урл2</faultUrl><faultDetail><severity>3</severity><text>4</text><url>урл5</url><id>6</id></faultDetail></standard><addition string=\"добавка7\"/></x:FMT_Demo>";
        s = payText;
        XMLPayload xp = response.createXMLPayload();
        try {
            xp.setContentType("application/xml");
            xp.setName("MainDocument");
            xp.setContent(s.getBytes(StandardCharsets.UTF_8));
            xp.setDescription("//TODO");
            response.setDocument(xp);
            String requestId = xiMsg.getMessageId();
            response.setRefToMessageId(requestId);

            Iterator<Payload> ia = (Iterator<Payload>) xiMsg.getAttachmentIterator();
            while (ia.hasNext()) {
                Payload attachment = ia.next();
                msgLog.append(String.format("Attachment class %s, name %s, content-type %s\n", attachment.getClass().getSimpleName(), attachment.getName(), attachment.getContentType()));
                response.addAttachment(attachment);
            }

            for (Map.Entry<MessagePropertyKey, String> dcx : xiMsg.getMessagePropertyMap().entrySet()) {
                msgLog.append(String.format("DynamicConfiguration {%s}%s = %s\n", dcx.getKey().getPropertyNamespace(), dcx.getKey().getPropertyName(), dcx.getValue()));
                response.setMessageProperty(dcx.getKey(), dcx.getValue());
            }
            response.addHopListEntry(EchoAdapterConstants.adapterType, EchoAdapterConstants.adapterNamespace, "Echo loop");
//            MessagePropertyKey mpk = new MessagePropertyKey("EchoResponseName", "urn:demo");
//            response.setMessageProperty(mpk, "Error");
            if (doError) {
                XIErrorInfoImpl errorInfo = (XIErrorInfoImpl) response.createErrorInfo();
                String[] names = errorInfo.getSupportedAttributeNames();
                if (names != null) {
                    // ErrorCode, ErrorArea, ErrorCategory, AdditionalErrorText, ApplicationFaultInterface, ApplicationFaultInterfaceNamespace, P1, P2, P3, P4
                    TRACE.debugT(SIGNATURE, "ErrorInfo.class={0}, errorInfo.getSupportedAttributeNames: {1}", new Object[]{ErrorInfo.class.getName(), Arrays.toString(names)});
                }
                errorInfo.setAttribute("ErrorCategory", ErrorCategory.XI_ADAPTER.toString());
//                errorInfo.setAttribute("ErrorCode", "REST_ADAPTER_PROCESSING_ERROR");
//                errorInfo.setAttribute("ErrorArea", "REST_Adapter");
//                errorInfo.setAttribute("ErrorCode", "Markirovka");
//                errorInfo.setAttribute("ErrorArea", "EchoAdapter");
//                errorInfo.setAttribute("ErrorCategory", "XIServer");
//                errorInfo.setAttribute("ErrorCategory", "Adapter");
                errorInfo.setAdditionalErrorText("4MainDocument has contained the <ApplicationError> element that triggers the JCA adapter to create an app error response as demo!");
                errorInfo.setApplicationFaultInterface(channelProperties.faultMessageTypeName, channelProperties.faultMessageTypeNS, "Echo_Adapter");

                errorInfo.setAttribute("P1", "p1 text");
                errorInfo.setAttribute("P2", "p2 text");
                errorInfo.setAttribute("P3", "p3 text");
                errorInfo.setAttribute("P4", "p4 text");

                response.setMessageClass(MessageClass.APPLICATION_ERROR);
//                response.setMessageClass(MessageClass.APPLICATION_RESPONSE);
                response.setErrorInfo(errorInfo);
                //response.setError("UNKNOWN", "403 Unauthorized");
                msgLog.append("Error response set OK\n");
            } else {
                response.setMessageClass(MessageClass.APPLICATION_RESPONSE);
            }
            centralFileLog.log_init(msgLog.toString());

        } catch (MessagingException e) {
            TRACE.catching(SIGNATURE, e);
            ResourceException re = new ResourceException("System error: " + e.getMessage());
            TRACE.throwing(SIGNATURE, re);
            msgLog.append("SYSTEM ERROR: ").append(e.getMessage()).append("\n");
            centralFileLog.log_init(msgLog.toString());
            throw re;
        } catch (Exception e) {
            ResourceException re = new ResourceException("Fatal error: " + e.getMessage());
            TRACE.throwing(SIGNATURE, re);
            msgLog.append("FATAL ERROR: ").append(e.getMessage()).append("\n");
            centralFileLog.log_init(msgLog.toString());
            throw re;
        }
        TRACE.exiting(SIGNATURE, output);
        return output;
    }

    @Deprecated
    private Record callDeprecated(InteractionSpec ispec, Record input, SPIManagedConnection mc) throws ResourceException {
        String SIGNATURE = "call(InteractionSpec ispec, Record input, SpiManagedConnection mc)";
        TRACE.entering("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)");
        if (input == null) {
            ResourceException re = new ResourceException("Input record is null.");
            TRACE.throwing("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", re);
            throw re;
        }
        if (!(input instanceof XIMessageRecord)) {
            ResourceException re = new ResourceException("Input record is not instance of Message.");
            TRACE.throwing("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", re);
            throw re;
        }

        String newMsgIndicator = new String("≋ ≋ ≋ Start of sync. message ≋ ≋ ≋");

        FileOutputStream file = null;
        OutputStreamWriter fWriter = null;
        PrintWriter printWriter = null;
        try {
            file = new FileOutputStream("/var/tmp/2.2");
            fWriter = new OutputStreamWriter(file);
            printWriter = new PrintWriter(fWriter);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        XIMessageRecordImpl output = null;
        Message msg = ((XIMessageRecord) input).getXIMessage();
        MessageKey amk = new MessageKey(msg.getMessageId(), MessageDirection.INBOUND);
        String[] result = new String[4]; // this.getMappedHeaderFieldsAndNormalize(mc.getChannelID(), msg);
        String fromParty = result[0];
        String fromService = result[1];
        String toParty = result[2];
        String toService = result[3];
        Payload appPayLoad = msg.getDocument();
        String payText = new String(appPayLoad.getContent());

        try {
            printWriter.println(newMsgIndicator);
            printWriter.println("From (P/S): " + fromParty + "/" + fromService);
            printWriter.println("To (P/S): " + toParty + "/" + toService);
            printWriter.println("Payload: ");
            printWriter.println(payText);
            fWriter.flush();
        } catch (Exception e) {
            TRACE.catching("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", e);
            ResourceException re = new ResourceException("System error: " + e.getMessage());
            TRACE.throwing("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", re);
            throw re;
        }

        try {
            TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Create synchronous response.");
            if (!payText.contains("ApplicationError")) {
                output = new XIMessageRecordImpl(msg.getToParty(), msg.getFromParty(), msg.getToService(), msg.getFromService(), msg.getAction());
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Retrieve XI message from output: " + output.toString());
                Message response = output.getXIMessage();
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Create payload of synchronous response: " + response.toString());
                XMLPayload xp = response.createXMLPayload();
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Fill payload of synchronous response: " + xp.toString());
                xp.setText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response>OK with attachment</Response>");
                xp.setName("MainDocument");
                xp.setDescription("XI AF Sample Adapter Sync Response");
                xp.setContentType("application/xml");
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Set payload of synchronous response.");
                response.setDocument(xp);

                Payload p = response.createPayload();
                p.setContent(new byte[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57});
                p.setContentType("application/octet-stream");
                p.setName("Attachment1111");
                p.setDescription("XI AF Sample Adapter Sync Error Response binary attachment");
                response.addAttachment(p);
                MessagePropertyKey mpk = new MessagePropertyKey("EchoResponseName", "urn:demo");
                response.setMessageProperty(mpk, "Aazazazazaza");

                String requestId = msg.getMessageId();
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Set RefToMsgId of synchronous response to: " + requestId);
                response.setRefToMessageId(requestId);
                this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "≋ ≋ Sync. message was forwarded succesfully to the file system");
            } else {
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT, "Payload contains the <ApplicationError> tag that causes a application error response for testing purposes!");
                this.audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Simulate application error response now.");
                String[] faultIF = this.getFaultIF(mc.getChannelID());
                Action action = new Action(faultIF[0], faultIF[1]);
                output = new XIMessageRecordImpl(msg.getToParty(), msg.getFromParty(), msg.getToService(), msg.getFromService(), action);
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Retrieve XI message from output: " + output.toString());
                Message response = output.getXIMessage();
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Create payload of synchronous error response: " + response.toString());
                XMLPayload xp = response.createXMLPayload();
                xp.setName("MainDocument");
                xp.setDescription("XI AF Sample Adapter Sync Error Response");
                if (payText.contains("ApplicationErrorBinaryPayload")) {
                    TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Fill binary payload of synchronous ApplicationError response");
                    xp.setContent(new byte[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57});
                    xp.setContentType("application/octet-stream");
                } else if (payText.contains("ApplicationErrorTextPayload")) {
                    TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Fill text payload of synchronous ApplicationError response");
                    xp.setText("Error simulated, ApplicationError contains text payload only");
                    xp.setContentType("text/plain");
                } else if (payText.contains("ApplicationErrorXMLPayloadWithAtt")) {
                    TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Fill XML payload of synchronous ApplicationError response with binary attachment");
                    xp.setText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Failure><Error>Error simulated, ApplicationError contains XML payload with binary attachment</Error></Failure>");
                    xp.setContentType("application/xml");
                    Payload p = response.createPayload();
                    p.setContent(new byte[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57});
                    p.setContentType("application/octet-stream");
                    p.setName("Attachment1111");
                    p.setDescription("XI AF Sample Adapter Sync Error Response binary attachment");
                    response.addAttachment(p);
                } else {
                    TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Fill XML payload of synchronous ApplicationError response");
                    xp.setText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Failure><Error>≋ Error simulated, ApplicationError contains XML payload only</Error></Failure>");
                    xp.setContentType("application/xml");
                }

                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Set payload of synchronous error response.");
                response.setDocument(xp);
                String requestId = msg.getMessageId();
                TRACE.debugT("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", XIAdapterCategories.CONNECT_AF, "Set RefToMsgId of synchronous error response to: " + requestId);
                response.setRefToMessageId(requestId);

                MessagePropertyKey mpk = new MessagePropertyKey("EchoResponseName", "urn:demo");
                response.setMessageProperty(mpk, "Error");

                ErrorInfo errorInfo = response.createErrorInfo();
                errorInfo.setAttribute("ErrorCode", "SOME_APP_ERR_CODE");
                errorInfo.setAttribute("ErrorArea", "JCA");
                errorInfo.setAttribute("ErrorCategory", "Application");
                errorInfo.setAttribute("AdditionalErrorText", "MainDocument has contained the <ApplicationError> element that triggers the JCA adapter to create an app error response as demo!");
                errorInfo.setAttribute("ApplicationFaultInterface", faultIF[0]);
                errorInfo.setAttribute("ApplicationFaultInterfaceNamespace", faultIF[1]);
                response.setErrorInfo(errorInfo);
                this.audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "≋ ≋ Sync. error simulated, finished ≋ ≋");
            }
        } catch (Exception e) {
            TRACE.catching("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", e);
            ResourceException re = new ResourceException("System error: " + e.getMessage());
            TRACE.throwing("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", re);
            throw re;
        }

        TRACE.exiting("call(InteractionSpec ispec, Record input, SpiManagedConnection mc)", output);
        return output;
    }

    @Override
    public XIInteractionSpec getXIInteractionSpec() throws NotSupportedException {
        return new XIInteractionSpecImpl();
    }


    private void setApplicationError(XIMessage resultMessage, int statusCode, String statusText, String faultIn, String faultIntNS) throws InvalidParamException {
        resultMessage.setMessageClass(MessageClass.APPLICATION_ERROR);
        resultMessage.setError("UNKNOWN", "" + statusCode + " " + statusText);
        ErrorInfo xierror = resultMessage.createErrorInfo();
        xierror.setAttribute("ErrorCategory", ErrorCategory.XI_ADAPTER_FRAMEWORK.toString());
        xierror.setAttribute("ErrorCode", "REST_ADAPTER_PROCESSING_ERROR");
        xierror.setAttribute("ErrorArea", "REST_Adapter");
        xierror.setAttribute("AdditionalErrorText", statusText);
        if (faultIn != null) {
            xierror.setAttribute("ApplicationFaultInterface", faultIn);
        }

        if (faultIntNS != null) {
            xierror.setAttribute("ApplicationFaultInterfaceNamespace", faultIntNS);
        }
        resultMessage.setErrorInfo(xierror);
    }

}

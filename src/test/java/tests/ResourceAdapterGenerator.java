package tests;

import connector.*;
import demoecho.CCIConnection;
import demoecho.CCIConnectionFactory;
import demoecho.EchoAdapterConstants;
import demoecho.SPIManagedConnectionFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.lang.String;
import java.math.BigDecimal;

public class ResourceAdapterGenerator {

    private XsdStringType xsdStringType(String s) {
        XsdStringType xst = new XsdStringType();
        xst.setValue(s);
        return xst;
    }

    private FullyQualifiedClassType fullyQualifiedClassType(String s) {
        FullyQualifiedClassType t = new FullyQualifiedClassType();
        t.setValue(s);
        return t;
    }

    private ConfigPropertyType configPropertyType(String name, String value) {
        ConfigPropertyType cp = new ConfigPropertyType();
        DescriptionType d = new DescriptionType();
        d.setValue(null);
        ConfigPropertyNameType cpn = new ConfigPropertyNameType();
        cpn.setValue(name);
        ConfigPropertyTypeType cpt = new ConfigPropertyTypeType();
        cpt.setValue("java.lang.String");
        cp.getDescription().add(d);
        cp.setConfigPropertyName(cpn);
        cp.setConfigPropertyType(cpt);
        cp.setConfigPropertyValue(xsdStringType(value));
        return cp;
    }

    String generateConnectorXml() throws JAXBException {
        ConnectorType ct = new ConnectorType();
        ct.setVersion(BigDecimal.valueOf(1.5));
        DisplayNameType dnt = new DisplayNameType();
        dnt.setValue("ResourceAdapter " + EchoAdapterConstants.raName);
        ct.getDisplayName().add(dnt);
        ct.setVendorName(xsdStringType(EchoAdapterConstants.adapterVendor));
        ct.setEisType(xsdStringType(EchoAdapterConstants.raEis));
        ct.setResourceadapterVersion(xsdStringType(EchoAdapterConstants.raVersion));
        ResourceadapterType ra = new ResourceadapterType();
        ct.setResourceadapter(ra);
        OutboundResourceadapterType ora = new OutboundResourceadapterType();
        ra.setOutboundResourceadapter(ora);
        ConnectionDefinitionType cd = new ConnectionDefinitionType();
        ora.getConnectionDefinition().add(cd);
        cd.setManagedconnectionfactoryClass(fullyQualifiedClassType(SPIManagedConnectionFactory.class.getName()));
        ConfigPropertyType addressMode = configPropertyType("addressMode", "CPA");
        ConfigPropertyType adapterType = configPropertyType("adapterType", EchoAdapterConstants.adapterType);
        ConfigPropertyType adapterNamespace = configPropertyType("adapterNamespace", EchoAdapterConstants.adapterNamespace);
        cd.getConfigProperty().add(addressMode);
        cd.getConfigProperty().add(adapterType);
        cd.getConfigProperty().add(adapterNamespace);
        cd.setConnectionfactoryInterface(fullyQualifiedClassType("javax.resource.cci.ConnectionFactory"));
        cd.setConnectionfactoryImplClass(fullyQualifiedClassType(CCIConnectionFactory.class.getName()));
        cd.setConnectionInterface(fullyQualifiedClassType("javax.resource.cci.Connection"));
        cd.setConnectionImplClass(fullyQualifiedClassType(CCIConnection.class.getName()));
        TransactionSupportType ts = new TransactionSupportType();
        ts.setValue("NoTransaction");
        ora.setTransactionSupport(ts);
        AuthenticationMechanismType am = new AuthenticationMechanismType();
        am.setAuthenticationMechanismType(xsdStringType("BasicPassword"));
        CredentialInterfaceType ci = new CredentialInterfaceType();
        ci.setValue("javax.resource.spi.security.PasswordCredential");
        am.setCredentialInterface(ci);
        ora.getAuthenticationMechanism().add(am);
        TrueFalseType tf = new TrueFalseType();
        tf.setValue(false);
        ora.setReauthenticationSupport(tf);

        ObjectFactory cof = new connector.ObjectFactory();
        JAXBContext ctx = JAXBContext.newInstance("connector");
        JAXBElement<ConnectorType> jaxbElement = cof.createConnector(ct);

        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();
        marshaller.marshal(jaxbElement, sw);
        return sw.toString();
    }

}

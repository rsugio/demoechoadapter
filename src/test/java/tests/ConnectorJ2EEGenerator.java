package tests;

import connectorsap.*;
import demoecho.EchoAdapterConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

public class ConnectorJ2EEGenerator {
    // see https://help.sap.com/docs/SAP_NETWEAVER_750/c591e2679e104fcdb8dc8e77771ff524/4ac4eebebcfb22aee10000000a42189b.html?locale=en-US

    java.lang.String generateConnectorXml() throws JAXBException {
        ConnectorType con = new ConnectorType();
        con.setDescription("\uD83D\uDCE5 Колхозная система сборки RAR, ибо нефиг");
        ResourceadapterType ra = new ResourceadapterType();
        ra.setRaJndiName(EchoAdapterConstants.raName);
        OutboundResourceadapterType ora = new OutboundResourceadapterType();
        ConnectionDefinitionType cd = new ConnectionDefinitionType();
        cd.setConnectionfactoryInterface("javax.resource.cci.ConnectionFactory");   //PO const
        cd.setJndiName(EchoAdapterConstants.raName);
        ora.getConnectionDefinition().add(cd);
        ra.setOutboundResourceadapter(ora);
        con.setResourceadapter(ra);
        LoaderReferencesType lr = new LoaderReferencesType();
        for (java.lang.String link: EchoAdapterConstants.connectorLoaderReferences) {
            LoaderReferencesType.LoaderName ln = new LoaderReferencesType.LoaderName();
            ln.setStrength("hard");
            ln.setValue(link);
            lr.getLoaderName().add(ln);
        }
        ra.setLoaderReferences(lr);

        ObjectFactory cof = new ObjectFactory();
        JAXBContext ctx = JAXBContext.newInstance("connectorsap");
        JAXBElement<ConnectorType> jaxbElement = cof.createConnector(con);

        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();
        marshaller.marshal(jaxbElement, sw);
        return sw.toString();
    }
}

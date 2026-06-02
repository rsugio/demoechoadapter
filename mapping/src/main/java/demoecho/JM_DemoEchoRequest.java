package demoecho;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;
import demoecho.jaxb.DTDemoRequest;
import demoecho.jaxb.DTDemoResponse;
import demoecho.jaxb.ObjectFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;

public class JM_DemoEchoRequest extends AbstractTransformation {
    static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db;

    public JM_DemoEchoRequest() throws ParserConfigurationException {
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        dbf.setNamespaceAware(true);
        db = dbf.newDocumentBuilder();
    }

    public static void marshaller(JAXBContext ctx, Object o, OutputStream os) throws JAXBException {
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(o, os);
    }

    @Override
    public void transform(TransformationInput tin, TransformationOutput tout) throws StreamTransformationException {
        Document doc;
        JAXBContext ctx;
        Unmarshaller unmarshaller;
        JAXBElement<DTDemoRequest> jmtDemoRequest;
        try {
            doc = db.parse(tin.getInputPayload().getInputStream());
        } catch (SAXException | IOException e) {
            throw new StreamTransformationException(e.getMessage(), e.getCause());
        }
        try {
            ctx = JAXBContext.newInstance("demoecho.jaxb");
            unmarshaller = ctx.createUnmarshaller();
            jmtDemoRequest = unmarshaller.unmarshal(doc, DTDemoRequest.class);
        } catch (JAXBException e) {
            throw new StreamTransformationException(e.getMessage(), e.getCause());
        }

        DTDemoRequest root = jmtDemoRequest.getValue();

        DTDemoResponse response = new DTDemoResponse();
        response.setNumber(BigInteger.TEN);
        JAXBElement<DTDemoResponse> jresponse = new ObjectFactory().createMTDemoResponse(response);
        try {
            marshaller(ctx, jresponse, tout.getOutputPayload().getOutputStream());
        } catch (JAXBException e) {
            throw new StreamTransformationException(e.getMessage(), e.getCause());
        }
    }
}



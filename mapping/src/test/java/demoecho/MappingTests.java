package demoecho;

import com.sap.aii.mapping.api.*;
import demoecho.jaxb.DTDemoRequest;
import demoecho.jaxb.DTDemoResponse;
import demoecho.jaxb.ObjectFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.bind.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Objects;

public class MappingTests {
    static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db;

    MappingTests() throws ParserConfigurationException {
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        db = dbf.newDocumentBuilder();
    }

    public static String marshaller(JAXBContext ctx, Object o) throws JAXBException {
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();
        marshaller.marshal(o, sw);
        return sw.toString();
    }

    @Test
    public void a() throws Exception {
        InputStream request = Objects.requireNonNull(getClass().getResourceAsStream("/MT_DemoRequest.xml"));
        Document doc = db.parse(request);

        JAXBContext ctx = JAXBContext.newInstance("demoecho.jaxb");
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        JAXBElement<DTDemoRequest> jmtDemoRequest = unmarshaller.unmarshal(doc, DTDemoRequest.class);
        DTDemoRequest root = jmtDemoRequest.getValue();
        Assertions.assertEquals("123", root.getNode());

        DTDemoResponse response = new DTDemoResponse();
        response.setNumber(BigInteger.TEN);
        JAXBElement<DTDemoResponse> jresponse = new ObjectFactory().createMTDemoResponse(response);
        String rez = marshaller(ctx, jresponse);
        System.out.println(rez);
    }

    InputPayload inputPayload = new InputPayload() {
        @Override
        public InputStream getInputStream() {
            return Objects.requireNonNull(getClass().getResourceAsStream("/MT_DemoRequest.xml"));
        }
    };

    TransformationInput tin = new TransformationInput() {
        @Override
        public InputHeader getInputHeader() {
            return null;
        }

        @Override
        public InputParameters getInputParameters() {
            return null;
        }

        @Override
        public InputPayload getInputPayload() {
            return inputPayload;
        }

        @Override
        public InputAttachments getInputAttachments() {
            return null;
        }
    };

    OutputPayload outputPayload = new OutputPayload() {
        @Override
        public OutputStream getOutputStream() {
            return System.out;
        }
    };

    TransformationOutput tout = new TransformationOutput() {
        @Override
        public OutputHeader getOutputHeader() {
            return null;
        }

        @Override
        public OutputParameters getOutputParameters() {
            return null;
        }

        @Override
        public OutputPayload getOutputPayload() {
            return outputPayload;
        }

        @Override
        public OutputAttachments getOutputAttachments() {
            return null;
        }

        @Override
        public void copyInputAttachments() {

        }
    };

    @Test
    public void m() throws Exception {
        JM_DemoEchoRequest j = new JM_DemoEchoRequest();

        j.transform(tin, tout);
    }
}

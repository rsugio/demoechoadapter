package demoecho;

import com.sap.aii.adapter.xi.ms.XIMessage;
import com.sap.aii.adapter.xi.ms.processor.ProcessorProvider;
import com.sap.engine.interfaces.messaging.api.Action;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LookupTests {
    @Test
    public void lookupTest() throws Exception {
        String result = "NORESULT";

        String party = "P_BEL";
        String service = "BS_MES_D";
        String cname = "CC_007RESTReceiver_Echo";
        com.sap.aii.mapping.lookup.Channel lookupChannel = com.sap.aii.mapping.lookup.LookupService.getChannel(party, service, cname);

        com.sap.aii.af.service.administration.api.cpa.CPAFactory cf = com.sap.aii.af.service.administration.api.cpa.CPAFactory.getInstance();
        com.sap.aii.af.service.administration.api.cpa.CPALookupManager clm = cf.getLookupManager();
        List<com.sap.aii.af.service.cpa.Channel> lst = clm.getChannelsByAdapterType("REST", "http://sap.com/xi/XI/System");
        result = lookupChannel.toString();
        for (com.sap.aii.af.service.cpa.Channel ch : lst) {
            result += "\n oid=" + ch.getObjectId();

        }

        com.sap.aii.af.service.cpa.Binding ico = clm.getBindingByChannelId("b5b04989e4f63013bfd59389ba9f4694");

//        com.sap.guid.IGUID guid = com.sap.guid.GUIDGeneratorFactory.getInstance().createGUIDGenerator().createGUID();
        com.sap.engine.interfaces.messaging.api.Action action = new com.sap.engine.interfaces.messaging.api.Action("dummy_interface", "urn:dummy-namespace");
        com.sap.aii.adapter.xi.ms.XIMessage xiMessage = null; //new XIMessage(guid, null, null, null, null, action);
//        com.sap.aii.adapter.xi.ms.processor.GenericProcessingBlockProvider gpbp = new com.sap.aii.adapter.xi.ms.processor.GenericProcessingBlockProvider();
//        com.sap.aii.adapter.xi.ms.processor.ProcessorProvider pp = new com.sap.aii.adapter.xi.ms.processor.ProcessorProvider(gpbp);

    }
}

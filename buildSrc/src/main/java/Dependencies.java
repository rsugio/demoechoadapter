import io.rsug.komar.DeployReference;

import java.util.LinkedList;
import java.util.List;

public class Dependencies {

    // ленивый быстрый способ указать зависимости. Можно в EchoAdapterConstants перенести при желании
    final static List<DeployReference> deployReferenceList = new LinkedList<>();

    static {
        deployReferenceList.add(DeployReference.libraryHard("engine.j2ee14.facade"));
        deployReferenceList.add(DeployReference.libraryHard("engine.jee5.facade"));
        deployReferenceList.add(DeployReference.libraryHard("com.sap.base.technology.facade"));
        deployReferenceList.add(DeployReference.libraryHard("com.sap.aii.af.lib"));
        deployReferenceList.add(DeployReference.libraryHard("com.sap.xi.util.misc"));
        deployReferenceList.add(DeployReference.libraryHard("com.sap.aii.af.sdk.lib"));
        deployReferenceList.add(DeployReference.serviceHard("com.sap.aii.af.svc"));
        deployReferenceList.add(DeployReference.serviceHard("com.sap.aii.af.cpa.svc"));
        deployReferenceList.add(DeployReference.serviceHard("com.sap.aii.adapter.xi.svc"));
        deployReferenceList.add(DeployReference.interfaceWeak("com.sap.aii.af.ms.ifc"));

//        deployReferenceList.add(DeployReference.libraryHard("com.sap.aii.af.lib.facade"));
//        deployReferenceList.add(DeployReference.libraryHard("tc~bl~httpclient~lib"));
//        deployReferenceList.add(DeployReference.interfaceHard("com.sap.aii.af.ifc.facade"));
//        deployReferenceList.add(DeployReference.serviceHard("com.sap.aii.af.svc.facade"));
//        deployReferenceList.add(DeployReference.serviceHard("com.sap.aii.adapter.xi.svc"));
        deployReferenceList.add(DeployReference.serviceHard("tc/je/appconfiguration/api"));

//        deployReferenceList.add(DeployReference.applicationWeak("com.sap.xi.directory"));
//        deployReferenceList.add(DeployReference.applicationHard("com.sap.aii.adapter.picao.util.app"));
//        deployReferenceList.add(DeployReference.applicationWeak("com.sap.aii.utilxi.server"));
    }

}


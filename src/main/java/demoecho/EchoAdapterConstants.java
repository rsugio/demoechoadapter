package demoecho;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EchoAdapterConstants {
    public static final String kolhoz = "\uD83D\uDCE5 Колхозная, им.тов.Гредлова, система сборки RAR/SDA. Ибо нефиг.";
    public static final String adapterType = "Echo";
    public static final String adapterNamespace = "urn:demo";
    public static final String adapterVendor = "rsug.io";
    public static final String adapterSWCname = "ZSWCV";
    public static final String adapterSWCvendor = "rsug.io";

    // basic Resource Adapter
    public final static String raName = "demo.echoadapter.ra";
    public final static String raVersion = "0.1";
    public final static String raDescription = "Русское описание тут (RA)";
    public final static String raEis = "Local echo adapter";
    public final static String raSPIManagedConnectionFactory = SPIManagedConnectionFactory.class.getName();
    public final static String raCCIConnectionFactory = CCIConnectionFactory.class.getName();
    public final static String raCCIConnection = CCIConnection.class.getName();

    public static final String jarName = raName + ".jar";
    public static final String rarName = raName + ".rar";

    // Metadata
    public final static String jndi = "deployedAdapters/" + raName + "/shareable/" + raName;
    // log-configuration.xml
    // logNameTrc должен соответствовать формату DTD#ID, DTD #IDREF -- без точек и тд
    public final static String logNameTrc = raName;
    public final static String logFilePattern = "./log/applications/demo.echoadapter.ra/default.trc";

    //TODO loader references в RAR/META-INF/connector-j2ee-engine.xml
    public static final String[] connectorLoaderReferences = {
            "library:engine.jee5.facade",
            "service:engine.service.facade",
            "service:engine.application.facade",
            "service:engine.security.facade",
            "library:com.sap.base.technology.facade",
            "library:com.sap.aii.af.lib.facade",
            "interface:com.sap.aii.af.ifc.facade",
            "service:com.sap.aii.af.svc.facade",
            "application:com.sap.aii.af.app"
    };

    // *************************************************************************** SDA + Lib
    public static final String raNameLib = raName + ".lib";
    public static final String sdaName = raName + ".sda";
    public final static String sdaDescription = "Русское описание тут (SDA)";
    public static final String sdaLibName = raNameLib + ".sda";

    // SDA SAP_MANIFEST.MF dependencylist, keyvendor|keyname
    public static final String[] sdaDependencyList = {
            "sap.com|engine.j2ee14.facade",
            "sap.com|engine.jee5.facade",
            "sap.com|engine.security.facade",
            "sap.com|engine.service.facade",
            "sap.com|engine.application.facade",
            "sap.com|com.sap.base.technology.facade",
            "sap.com|com.sap.aii.af.lib",
            "sap.com|com.sap.aii.af.ms.ifc",
            "sap.com|com.sap.aii.af.svc",
            "sap.com|com.sap.aii.sec.lib",
            "sap.com|com.sap.security.api.sda",
            "sap.com|com.sap.aii.af.lib.facade",
            "sap.com|com.sap.aii.af.ifc.facade",
            "sap.com|com.sap.aii.af.svc.facade",
            "sap.com|com.sap.aii.adapter.xi.svc",
            "sap.com|com.sap.aii.af.cpa.svc",
            "sap.com|tc~je~appconfiguration~api"
    };


}

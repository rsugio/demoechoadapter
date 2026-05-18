package demoecho;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EchoAdapterConstants {
    public static final String adapterType = "Echo";
    public static final String adapterNamespace = "urn:demo";
    public static final String adapterVendor = "rsug.io";
    public static final String adapterSWCname = "ZSWCV";
    public static final String adapterSWCvendor = "rsug.io";

    // basic Resource Adapter
    public final static String raName = "demo.echoadapter.ra";
    public final static String raVersion = "0.1";
    public final static String raDescription = "Русское описание тут";
    public final static String raEis = "Local echo adapter";
    // Metadata
    public final static String jndi = "deployedAdapters/" + raName + "/shareable/" + raName;
    // log-configuration.xml
    public final static String logNameTrc = "demo_echoadapter_ra.trc";
    public static final String jarName = raName + ".jar";
    public static final String rarName = raName + ".rar";

    // loader references в connector-j2ee-engine.xml
    public static final String[] connectorLoaderReferences = {
            "service:engine.service.facade",
            "service:engine.application.facade",
            "service:engine.security.facade",
            "library:engine.j2ee14.facade",
            "library:com.sap.base.technology.facade",
            "library:com.sap.aii.af.lib.facade",
            "interface:com.sap.aii.af.ifc.facade",
            "service:com.sap.aii.af.svc.facade",
            "application:com.sap.aii.af.app"
    };

    public static String getImplementationVersion() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime localDateTime = LocalDateTime.now();
        return String.format("0.1.%s.0000", localDateTime.format(dtf));
    }

    // SDA SAP_MANIFEST.MF dependencylist, keyvendor|keyname
    public static final String[] sdaDependencyList = {
            "sap.com|engine.j2ee14.facade",
            "sap.com|engine.jee5.facade",
            "sap.com|engine.security.facade",
            "sap.com|engine.service.facade",
            "sap.com|engine.application.facade",
            "sap.com|com.sap.base.technology.facade",
            "sap.com|com.sap.aii.af.cpa.svc",
            "sap.com|com.sap.aii.af.lib",
            "sap.com|com.sap.aii.af.ms.ifc",
            "sap.com|com.sap.aii.af.svc",
            "sap.com|com.sap.aii.sec.lib",
            "sap.com|com.sap.security.api.sda",
            "sap.com|com.sap.aii.af.lib.facade",
            "sap.com|com.sap.aii.af.ifc.facade",
            "sap.com|com.sap.aii.af.svc.facade",
            "sap.com|tc~je~appconfiguration~api",
            "sap.com|com.sap.aii.adapter.xi.svc",
    };
    public static String sdaSapManifestDependencyList() {
        StringBuilder sb = new StringBuilder();
        for (String s: sdaDependencyList) {
            String kv = s.split("\\|")[0];
            String kn = s.split("\\|")[1];
            sb.append(String.format("<dependency keyname=\"%s\" keyvendor=\"%s\"/>", kn, kv));
        }
        return sb.toString();
    }

//    public final static String dependencyList = "<dependency
//    keyname=\"engine.j2ee14.facade\" keyvendor=\"sap.com\"/>" +
//            "<dependency keyname=\"\" keyvendor=\"sap.com\"/>" +
//            "<dependency keyname=\"\" keyvendor=\"sap.com\"/>" +
//            "<dependency keyname=\"tc~je~appconfiguration~api\" keyvendor=\"sap.com\"/>" +
//            "<dependency keyname=\"com.sap.aii.adapter.xi.svc\" keyvendor=\"sap.com\"/>";
//    public final static String dependencies = "<dependency  Implementation-Title=\"engine.j2ee14.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"engine.jee5.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"engine.security.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"engine.service.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"engine.application.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.base.technology.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.aii.af.cpa.svc\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.aii.af.lib\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.aii.af.ms.ifc\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.aii.af.svc\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.aii.sec.lib\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.security.api.sda\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"iaik.lib.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency Implementation-Title=\"com.sap.aii.af.lib.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.aii.af.ifc.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.aii.af.svc.facade\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"tc~je~appconfiguration~api\" Implementation-Vendor-Id=\"sap.com\" /> <dependency  Implementation-Title=\"com.sap.aii.adapter.xi.svc\" Implementation-Vendor-Id=\"sap.com\" />";
}

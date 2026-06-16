package demoecho;

//import demo.Dependencies;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class EchoAdapterConstants {
    public static final String kolhoz = "Васянская сборка";
    public static final String adapterType = "Echo";
    public static final String adapterNamespace = "urn:demo";
    public static final String adapterThread = "EchoAdapter_";
    public static final String adapterAddressMode = "CPA";
    public static final String adapterVendor = "rsug.io";
    public static final String adapterVendorLocation = "Russia, Moscow";
    public final static String adapterVersion = "1";         // должно совпадать с /AdapterTypeMetaData/@version
    public final static String raShortName = "demo.echoadapter";
    public final static String raSPIManagedConnectionFactory = SPIManagedConnectionFactory.class.getName();
    public final static String raCCIConnectionFactory = CCIConnectionFactory.class.getName();
    public final static String raCCIConnection = CCIConnection.class.getName();
    public final static String raDescription = "Модель для сборки";
    public final static String raEis = "Без EIS (локальная обработка)";
    public final static String swcName = "ZRSUGIO";

    // имена Development-компонент и context-root
    public final static String dcNameRA = raShortName + ".ra";
    public static final String dcNameLib = raShortName + ".lib";
    public static final String dcNameWeb = raShortName + ".web";
    // http://localhost:50000/rsug.io~demoecho
    public static final String webContextRoot = adapterVendor + "~demoecho";

    // полный JNDI
    public final static String jndi = "deployedAdapters/" + dcNameRA + "/shareable/" + dcNameRA;
    public final static String csncomponent = "ZDEMO";

    // centralFileLogDirectory
    // SYS_GLOBAL_DIR=/usr/sap/POD/SYS/global
    // centralFileLogDirectory=$SYS_GLOBAL_DIR/xi_customer_logs/adapter_Echo
    public final static String centralFileLogDirectorySuffix = "/xi_customer_logs/adapter_Echo";

    public final static String text64 = "text64";

    public final static String adapterStatus = "adapterStatus";
    public final static String adapterStatusActive = "active";
    public final static String adapterStatusInactive = "inactive";

    // Fault tab
    public final static String throwFault = "throwFault";
    public final static String throwNever = "never";
    public final static String throwAlways = "always";
    public final static String throwDynamicConfKey = "dynamicConf";
    public final static String throwXPath = "xpath";
    public final static String faultDynConfNS = "faultDynConfNS";
    public final static String faultDynConfName = "faultDynConfName";
    public final static String faultDynConfOperator = "faultDynConfOperator";
    public final static String faultDynConfValue = "faultDynConfValue";

    public final static String throwOperatorExist = "exist";
    public final static String throwOperatorNexst = "notexist";
    public final static String throwOperatorEqual = "equal";
    public final static String throwOperatorNequal = "notequal";

    public final static String faultMessageTypeNS = "faultMessageTypeNS";
    public final static String faultMessageTypeName = "faultMessageTypeName";
    public final static String faultErrorCategory = "faultErrorCategory";
    public final static String faultErrorCode = "faultErrorCode";
    public final static String faultErrorArea = "faultErrorArea";
    public final static String faultAdditionalErrorText = "AdditionalErrorText";

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put("kolhoz", kolhoz);
        props.put("adapterType", adapterType);
        props.put("adapterNamespace", adapterNamespace);
        props.put("adapterVendor", adapterVendor);
        props.put("adapterVendorLocation", adapterVendorLocation);
        props.put("adapterVersion", adapterVersion);
        props.put("raShortName", raShortName);
        props.put("jndi", jndi);
        props.put("raDescription", raDescription);
        props.put("raEis", raEis);
        props.put("raCCIConnection", raCCIConnection);
        props.put("raCCIConnectionFactory", raCCIConnectionFactory);
        props.put("raSPIManagedConnectionFactory", raSPIManagedConnectionFactory);
        props.put("swcName", swcName);
        props.put("dcNameRA", dcNameRA);
        props.put("dcNameLib", dcNameLib);
        props.put("dcNameWeb", dcNameWeb);
        props.put("webContextRoot", webContextRoot);
        props.put("csncomponent", csncomponent);

        Path whereTo = Paths.get("properties.xml");
        String comment = String.format("Константы для сборки адаптера %s из файла %s", adapterType, EchoAdapterConstants.class.getName());
        props.storeToXML(Files.newOutputStream(whereTo), comment, StandardCharsets.UTF_8.toString());
    }
}

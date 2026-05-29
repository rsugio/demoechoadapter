package demoecho;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class EchoAdapterConstants {
    public static final String kolhoz = "\uD83D\uDCE5 Колхозная, им.тов.Гредлова, система сборки RAR/SDA. Ибо нефиг.";
    public static final String adapterType = "Echo";
    public static final String adapterNamespace = "urn:demo";
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

    // имена Development-компонент
    public final static String dcNameRA = raShortName + ".ra";
    public static final String dcNameLib = raShortName + ".lib";

    // полный JNDI
    public final static String jndi = "deployedAdapters/" + dcNameRA + "/shareable/" + dcNameRA;

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

        Path whereTo = Paths.get("properties.xml");
        String comment = String.format("Константы для сборки адаптера %s из файла %s", adapterType, EchoAdapterConstants.class.getName());
        props.storeToXML(Files.newOutputStream(whereTo), comment, StandardCharsets.UTF_8.toString());
    }
}

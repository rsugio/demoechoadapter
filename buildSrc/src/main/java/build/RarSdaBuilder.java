package build;

import io.rsug.komar.*;
import logConfiguration.LogConfiguration;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// https://help.sap.com/docs/SAP_NETWEAVER_750/c591e2679e104fcdb8dc8e77771ff524/4a503995869c14d1e10000000a42189c.html?locale=en-US
// Developing JCA Resource Adapter for EIS

public class RarSdaBuilder {
    final Path jarFile, targetDir, libsDir, srcPath, rarFile, sdaFile, sdaLibFile, appPropertiesFile;
    public final String keyCounter;
    public final Map<String, Object> constants;
    static final String io = "commons-io-2.22.0.jar", lang3 = "commons-lang3-3.20.0.jar";
    // RAR/META-INF/single-module-dd.xml
//    private static final String singleModuleDdXml = "<?xml version=\"1.0\"?>\n<SDA><SoftwareType>single-module</SoftwareType><engine-deployment-descriptor version=\"3.0\"/></SDA>";
    // SDA/META-INF/j2ee-dd.xml
//    private static final String j2eeDdXml = "<?xml version=\"1.0\"?>\n<SDA><SoftwareType>J2EE</SoftwareType><engine-deployment-descriptor version=\"3.0\"/></SDA>";
    public final List<DeployReference> deployReferenceList = new LinkedList<>();

    public RarSdaBuilder(String version, String constantsClass, Path srcPath, Path jarFile, Path libsdir, Path targetDir) throws IOException, ClassNotFoundException, IllegalAccessException {
        this.srcPath = srcPath;
        this.jarFile = jarFile;
        this.targetDir = targetDir;
        this.libsDir = libsdir;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime localDateTime = LocalDateTime.now();
        this.keyCounter = String.format("%s.%s", version, localDateTime.format(dtf));
        this.constants = readConstantsWithDependencies(jarFile, constantsClass, libsdir);
        this.rarFile = targetDir.resolve($("rarName"));
        this.sdaFile = targetDir.resolve($("sdaName"));
        this.sdaLibFile = targetDir.resolve($("sdaLibName"));
        this.appPropertiesFile = srcPath.resolve("main/resources/sap.application.global.properties");
        deployReferenceList.add(DeployReference.libraryHard("engine.jee5.facade"));
        deployReferenceList.add(DeployReference.libraryHard("com.sap.base.technology.facade"));
        deployReferenceList.add(DeployReference.libraryHard("com.sap.aii.af.lib.facade"));
        deployReferenceList.add(DeployReference.libraryHard("tc~bl~httpclient~lib"));
        deployReferenceList.add(DeployReference.interfaceHard("com.sap.aii.af.ifc.facade"));
        deployReferenceList.add(DeployReference.serviceHard("com.sap.aii.af.svc.facade"));
        deployReferenceList.add(DeployReference.serviceHard("com.sap.aii.adapter.xi.svc"));
        deployReferenceList.add(DeployReference.serviceHard("tc/je/appconfiguration/api"));
        deployReferenceList.add(new DeployReference("hard", "library", $("adapterVendor"), $("raNameLib")));
    }

    public static Map<String, Object> readConstantsWithDependencies(Path jarFile, String className, Path jarDir) throws IOException, IllegalAccessException, ClassNotFoundException {
        Map<String, Object> result = new HashMap<>();
        List<URL> urls = new ArrayList<>();

        // 1. Добавляем основной JAR
        if (!Files.exists(jarFile)) {
            throw new IllegalArgumentException("Main JAR not found: " + jarFile);
        }
        urls.add(jarFile.toUri().toURL());

        // 2. Добавляем все JAR'ы из папки с зависимостями
        if (Files.exists(jarDir) && Files.isDirectory(jarDir)) {
            try (Stream<Path> jarFiles = Files.list(jarDir)) {
                jarFiles.filter(path -> path.toString().endsWith(".jar")).forEach(path -> {
                    try {
                        urls.add(path.toUri().toURL());
                    } catch (Exception e) {
                        System.err.println("Failed to add JAR: " + path);
                    }
                });
            }
        }

        // 3. Загружаем класс и читаем константы
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader().getParent() // bootstrap classloader
        );
        Class<?> clazz = classLoader.loadClass(className);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null) {
                    result.put(field.getName(), value);
                }
            }
        }
        return result;
    }

    private String $(String key) {
        return (String) Objects.requireNonNull(constants.get(key), key);
    }

    private String[] $$(String key) {
        return (String[]) Objects.requireNonNull(constants.get(key), key);
    }

    public void build() throws JAXBException, IOException {
        System.out.println($("kolhoz"));
        System.out.println("keyCounter: " + keyCounter);
        System.out.println("input jar: " + jarFile);
        System.out.println("libs dir: " + libsDir);
        System.out.println("target dir: " + targetDir);
        new RaXmlGenerator(constants).generateConnectorXml();
        new ConnectorJ2eeXmlGenerator(constants).generateConnectorXml();
        new ApplicationXmlGenerator(constants).generateApplicationXml();
        new ApplicationJ2eeEngineXmlGenerator(constants).generateApplicationJ2eeEngineXml(deployReferenceList);
        buildSDAlib();
        buildRAR();
        buildSDArar();

        System.out.println("\uD83D\uDCE5 ready: " + sdaFile);
    }

    public void buildSDArar() throws IOException, JAXBException {
        InputStream isRar = Files.newInputStream(rarFile);
        OutputStream os = Files.newOutputStream(sdaFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/application-j2ee-engine.xml");
        zos.putNextEntry(zipEntry);
        String s = new ApplicationJ2eeEngineXmlGenerator(constants).generateApplicationJ2eeEngineXml(deployReferenceList);
        IOUtils.write(s, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/application.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(new ApplicationXmlGenerator(constants).generateApplicationXml(), zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        Manifest mf = new Manifest();
        Attributes atts = mf.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.put(Attributes.Name.IMPLEMENTATION_TITLE, $("raName"));
        atts.put(Attributes.Name.IMPLEMENTATION_VERSION, keyCounter);
        atts.put(Attributes.Name.SPECIFICATION_VENDOR, "SAP AG");
        atts.put(Attributes.Name.SPECIFICATION_TITLE, $("raName"));
        atts.put(Attributes.Name.SPECIFICATION_VERSION, "7.5.0");
        atts.put(Attributes.Name.IMPLEMENTATION_VENDOR, "SAP");
        mf.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/SAP_MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        mf = new Manifest();
        atts = mf.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
// keyvendor, keyname, keycounter обязательны
        atts.put(new Attributes.Name("keyvendor"), $("adapterVendor"));
        atts.put(new Attributes.Name("keyname"), $("raName"));
        atts.put(new Attributes.Name("keycounter"), keyCounter);
        atts.put(new Attributes.Name("keylocation"), "SAP AG");
        atts.put(new Attributes.Name("dependencylist"), sdaSapManifestDependencyList());
//      atts.put(new Attributes.Name("dependencies"), EchoAdapterConstants.dependencies);
        atts.put(new Attributes.Name("softwaretype"), "J2EE");
//      atts.put(new Attributes.Name("csncomponent"), "BC-XI-CON-DEMO");
        atts.put(new Attributes.Name("deployfile"), "j2ee-dd.xml");

        // componentelement и его атрибуты очень важны
        String componentelement = String.format(
                "<componentelement name=\"%s\" vendor=\"%s\" componenttype=\"DC\" subsystem=\"NO_SUBSYS\" location=\"SAP AG\" counter=\"%s\" scvendor=\"%s\" scname=\"%s\" deltaversion=\"F\" componentprovider=\"SAP AG\" servertype=\"P4\"/>",
                $("raName"), $("adapterVendor"), keyCounter, $("adapterSWCvendor"), $("adapterSWCname"));
        atts.put(new Attributes.Name("componentelement"), componentelement);
        mf.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/j2ee-dd.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(Komar.minimalSdaDdXml("J2EE"), zos);
        zos.closeEntry();

        if (Files.exists(appPropertiesFile)) {
            zipEntry = new ZipEntry("META-INF/sap.application.global.properties");
            zos.putNextEntry(zipEntry);
            IOUtils.copy(Files.newInputStream(appPropertiesFile), zos);
            zos.closeEntry();
        }

        zipEntry = new ZipEntry($("rarName"));
        zos.putNextEntry(zipEntry);
        IOUtils.copy(isRar, zos);
        zos.closeEntry();

        zos.close();
    }

    public String sdaSapManifestDependencyList() {
        StringBuilder sb = new StringBuilder();
        for (String s : $$("sdaDependencyList")) {
            String kv = s.split("\\|")[0];
            String kn = s.split("\\|")[1];
            sb.append(String.format("<dependency keyname=\"%s\" keyvendor=\"%s\"/>", kn, kv));
        }
        return sb.toString();
    }

    public Path buildRAR() throws IOException, JAXBException {
        OutputStream os = Files.newOutputStream(rarFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("AdapterMetadata/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("AdapterMetadata/" + $("adapterType") + ".xml");
        Path adapterMetadataFile = srcPath.resolve("main/resources/metadata/" + $("adapterType") + ".xml");
        if (!Files.isRegularFile(adapterMetadataFile)) {
            throw new NoSuchFileException("No such file: " + adapterMetadataFile.toAbsolutePath());
        }
        zos.putNextEntry(zipEntry);
        IOUtils.copy(Files.newInputStream(adapterMetadataFile), zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        Manifest manifest = new Manifest();
        Attributes atts = manifest.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.put(Attributes.Name.IMPLEMENTATION_TITLE, $("raName"));
        atts.put(Attributes.Name.IMPLEMENTATION_VERSION, keyCounter);
        atts.put(Attributes.Name.SPECIFICATION_VENDOR, "SAP AG");
        atts.put(Attributes.Name.SPECIFICATION_TITLE, "XPI Resource Adapter");
        atts.put(Attributes.Name.SPECIFICATION_VERSION, "7.1.0");
        atts.put(Attributes.Name.IMPLEMENTATION_VENDOR, "SAP");
        manifest.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/SAP_MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        manifest = new Manifest();
        atts = manifest.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.put(new Attributes.Name("keycounter"), keyCounter);
//        atts.put(new Attributes.Name("keyvendor"), "sap.com");
//        atts.put(new Attributes.Name("keylocation"), "SAP AG");
//        atts.put(new Attributes.Name("keyname"), EchoAdapterConstants.raName);
        atts.put(new Attributes.Name("deployfile"), "single-module-dd.xml");
        manifest.write(zos);
        zos.closeEntry();

        String connj2ee = new ConnectorJ2eeXmlGenerator(constants).generateConnectorXml();
        zipEntry = new ZipEntry("META-INF/connector-j2ee-engine.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(connj2ee, zos);
        zos.closeEntry();

        String raxml = new RaXmlGenerator(constants).generateConnectorXml();
        zipEntry = new ZipEntry("META-INF/ra.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(raxml, zos);
        zos.closeEntry();

//        zipEntry = new ZipEntry("META-INF/log-configuration.xml");
//        zos.putNextEntry(zipEntry);
//        String log = logConfiguration();
//        System.out.println(log);
//        IOUtils.write(log, zos);
//        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/single-module-dd.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(Komar.minimalSdaDdXml("single-module"), zos);
        zos.closeEntry();

        zipEntry = new ZipEntry($("jarName"));
        zos.putNextEntry(zipEntry);
        IOUtils.copy(Files.newInputStream(jarFile), zos);
        zipEntry.setComment("JAR");
        zos.closeEntry();
//
        zos.finish();
        zos.close();
        return rarFile;
    }

    // есть подозрение что не имеет смысла в 7.5
    String logConfiguration() throws JAXBException, IOException {
        logConfiguration.ObjectFactory cof = new logConfiguration.ObjectFactory();
        JAXBContext ctx = JAXBContext.newInstance(logConfiguration.LogConfiguration.class);
        LogConfiguration logConfiguration = cof.createLogConfiguration();

        logConfiguration.LogFormatter lf = new logConfiguration.LogFormatter();
        final String trc = "trc";
        lf.setName(trc);
        lf.setPattern("%26d %150l [%t] %10s: %m");
        lf.setType("TraceFormatter");
        logConfiguration.LogFormatters lfs = new logConfiguration.LogFormatters();
        logConfiguration.setLogFormatters(lfs);
        lfs.getLogFormatter().add(lf);

        logConfiguration.LogDestinations lds = new logConfiguration.LogDestinations();
        logConfiguration.setLogDestinations(lds);

        logConfiguration.LogDestination ld = new logConfiguration.LogDestination();
        lds.getLogDestination().add(ld);
        ld.setCount("5");
        ld.setEffectiveSeverity("DEBUG");
        ld.setLimit("2000000");
        ld.setName($("logNameTrc"));
        ld.setPattern($("logFilePattern"));
        ld.setType("FileLog");

        logConfiguration.FormatterRef fref = new logConfiguration.FormatterRef();
        fref.setName(trc);
        ld.getFormatterRefOrAnonymousFormatter().add(fref);

        logConfiguration.LogControllers lcs = new logConfiguration.LogControllers();
        logConfiguration.setLogControllers(lcs);

        logConfiguration.LogController lc = new logConfiguration.LogController();
        lcs.getLogController().add(lc);
        lc.setName($("raName"));
        lc.setEffectiveSeverity(ld.getEffectiveSeverity());
        logConfiguration.AssociatedDestinations ads = new logConfiguration.AssociatedDestinations();
        lc.setAssociatedDestinations(ads);
        logConfiguration.DestinationRef dref = new logConfiguration.DestinationRef();
        ads.getDestinationRefOrAnonymousDestination().add(dref);
        dref.setName($("logNameTrc"));
        dref.setAssociationType("LOG");

        Marshaller marshaller = ctx.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(logConfiguration, sw);
        sw.close();
        return sw.toString();
    }

    /**
     * SDA для используемых библиотек
     *
     * @throws IOException
     * @throws JAXBException
     */
    public void buildSDAlib() throws IOException, JAXBException {
        OutputStream os = Files.newOutputStream(sdaLibFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("lib/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("lib/private/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("server/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("lib/" + io);
        zos.putNextEntry(zipEntry);
        IOUtils.copy(Objects.requireNonNull(Files.newInputStream(libsDir.resolve(io))), zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("lib/" + lang3);
        zos.putNextEntry(zipEntry);
        IOUtils.copy(Objects.requireNonNull(Files.newInputStream(libsDir.resolve(lang3))), zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/javalib-dd.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(Komar.minimalSdaDdXml("library"), zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        Manifest mf = new Manifest();
        Attributes atts = mf.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.put(Attributes.Name.IMPLEMENTATION_TITLE, $("raNameLib"));
        atts.put(Attributes.Name.IMPLEMENTATION_VERSION, keyCounter);
//        atts.put(Attributes.Name.SPECIFICATION_VENDOR, "SAP AG");
//        atts.put(Attributes.Name.SPECIFICATION_TITLE, $("raName"));
//        atts.put(Attributes.Name.SPECIFICATION_VERSION, "7.5.0");
        atts.put(Attributes.Name.IMPLEMENTATION_VENDOR, "SAP");
        mf.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/SAP_MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        mf = new Manifest();
        atts = mf.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
// keyvendor, keyname, keycounter обязательны
        atts.put(new Attributes.Name("keyvendor"), $("adapterVendor"));
        atts.put(new Attributes.Name("keyname"), $("raNameLib"));
        atts.put(new Attributes.Name("keycounter"), keyCounter);
        atts.put(new Attributes.Name("keylocation"), "SAP AG");
//        atts.put(new Attributes.Name("dependencylist"), sdaSapManifestDependencyList());
        atts.put(new Attributes.Name("softwaretype"), "library");
        atts.put(new Attributes.Name("deployfile"), "javalib-dd.xml");

        // componentelement и его атрибуты очень важны
        String componentelement = String.format(
                "<componentelement name=\"%s\" vendor=\"%s\" componenttype=\"DC\" subsystem=\"NO_SUBSYS\" location=\"SAP AG\" counter=\"%s\" scvendor=\"%s\" scname=\"%s\" deltaversion=\"F\" componentprovider=\"SAP AG\" servertype=\"P4\"/>",
                $("raNameLib"), $("adapterVendor"), keyCounter, $("adapterSWCvendor"), $("adapterSWCname"));
        atts.put(new Attributes.Name("componentelement"), componentelement);
        mf.write(zos);
        zos.closeEntry();

        List<String> jars = new LinkedList<>();
        jars.add("lib/" + io);
        jars.add("lib/" + lang3);
        String providerXml = new ProviderXmlGenerator().generateProviderXml(
                $("raNameLib"), $("raNameLib"),  $("adapterVendor"), new LinkedList<>(), jars
        );
        zipEntry = new ZipEntry("server/provider.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(providerXml, zos);
        zos.closeEntry();

        zos.close();
    }

}

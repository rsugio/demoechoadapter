package tests;

import com.sap.engine.services.deploy.server.dpl_info.DeploymentInfo;
import com.sap.engine.services.deploy.server.dpl_info.module.Version;
import demoecho.EchoAdapterConstants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sap.engine.services.deploy.server.validate.jlin.AppJLinInfo;

// https://help.sap.com/docs/SAP_NETWEAVER_750/c591e2679e104fcdb8dc8e77771ff524/4a503995869c14d1e10000000a42189c.html?locale=en-US
// Developing JCA Resource Adapter for EIS
public class BuilderTest {
    private static final String keyCounter = EchoAdapterConstants.getImplementationVersion();
    private static final String singleModuleDdXml = "<?xml version=\"1.0\"?>\n<SDA><SoftwareType>single-module</SoftwareType><engine-deployment-descriptor version=\"3.0\"/></SDA>";
    private static Path rarFile = Paths.get("./build/" + EchoAdapterConstants.rarName);
    private static Path sdaFile = Paths.get("./build/" + EchoAdapterConstants.raName + ".sda");

    @Test
    public void buildTest() throws Exception {
        String ra = new ResourceAdapterGenerator().generateConnectorXml();
        String con = new ConnectorJ2EEGenerator().generateConnectorXml();
        // детально возиться с логом смысла нет - проверяем что строки упоминаются
        InputStream is = Objects.requireNonNull(Files.newInputStream(Paths.get("./src/main/resources/log-configuration.xml")));
        String log = IOUtils.toString(is, StandardCharsets.UTF_8);
        Assertions.assertTrue(log.contains(EchoAdapterConstants.raName));
        Assertions.assertTrue(log.contains(EchoAdapterConstants.logNameTrc));
        // AdapterMetaData - проверяем значение по умолчанию в JNDI
        is = Objects.requireNonNull(Files.newInputStream(Paths.get("./src/main/resources/metadata/Echo.xml")));
        String amd = IOUtils.toString(is, StandardCharsets.UTF_8);
        Assertions.assertTrue(amd.contains(EchoAdapterConstants.raName));
        buildRAR(ra, con, log, amd);

        is = Objects.requireNonNull(getClass().getResourceAsStream("/application-j2ee-engine.xml"));
        String appj2ee = IOUtils.toString(is, StandardCharsets.UTF_8);
        is = Objects.requireNonNull(getClass().getResourceAsStream("/application.xml"));
        String appxml = IOUtils.toString(is, StandardCharsets.UTF_8);
        buildSDA(appj2ee, appxml);
    }

    public void buildSDA(String appj2ee, String appxml) throws IOException {
        InputStream isRar = Files.newInputStream(rarFile);
        OutputStream os = Files.newOutputStream(sdaFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/application-j2ee-engine.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(appj2ee, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/application.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(appxml, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        Manifest rarMF = new Manifest();
        Attributes mainAttrs = rarMF.getMainAttributes();
        mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttrs.put(Attributes.Name.IMPLEMENTATION_TITLE, EchoAdapterConstants.raName);
        mainAttrs.put(Attributes.Name.IMPLEMENTATION_VERSION, keyCounter);
//        mainAttrs.put(new Attributes.Name("sap-changelistnumber"), "631060");
//        mainAttrs.put(new Attributes.Name("sap-perforceserver"), "perforce3007.wdf.sap.corp:3007");
        mainAttrs.put(Attributes.Name.SPECIFICATION_VENDOR, "SAP AG");
        mainAttrs.put(Attributes.Name.SPECIFICATION_TITLE, EchoAdapterConstants.raName);
// a       mainAttrs.put(Attributes.Name.IMPLEMENTATION_VENDOR_ID, "sap.com");
        mainAttrs.put(Attributes.Name.SPECIFICATION_VERSION, "7.5.0");
//        mainAttrs.put(new Attributes.Name("sourcelocation"), "perforce\\://perforce3007.wdf.sap.corp\\:3007/tc/xpi.adapters.con/NW750EXT_32_REL");
        mainAttrs.put(Attributes.Name.IMPLEMENTATION_VENDOR, "SAP");
        rarMF.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/SAP_MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        rarMF = new Manifest();
        mainAttrs = rarMF.getMainAttributes();
        mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
//        mainAttrs.put(new Attributes.Name("changelistnumber"), "631060");
//        mainAttrs.put(new Attributes.Name("projectname"), "xpi.adapters.con");
//        mainAttrs.put(new Attributes.Name("compress"), "true");
// keyvendor, keyname, keycounter обязательны
        mainAttrs.put(new Attributes.Name("keyvendor"), EchoAdapterConstants.adapterVendor);
        mainAttrs.put(new Attributes.Name("keyname"), EchoAdapterConstants.raName);
        mainAttrs.put(new Attributes.Name("keycounter"), keyCounter);
        mainAttrs.put(new Attributes.Name("keylocation"), "SAP AG");
//        mainAttrs.put(new Attributes.Name("docfile"), "build.xml");
// 4       mainAttrs.put(new Attributes.Name("Ext-SDM-SDA-Comp-Version"), "1");
//        mainAttrs.put(new Attributes.Name("JarSAP-Standalone-Version"), "20081017.1000");


       mainAttrs.put(new Attributes.Name("dependencylist"), EchoAdapterConstants.sdaSapManifestDependencyList());
// 6       mainAttrs.put(new Attributes.Name("SDM-SDA-Comp-Version"), "1");
// 2       mainAttrs.put(new Attributes.Name("dependencies"), EchoAdapterConstants.dependencies);
//        mainAttrs.put(new Attributes.Name("JarSL-Version"), "20100616.1800");
        mainAttrs.put(new Attributes.Name("softwaretype"), "J2EE");
// 1       mainAttrs.put(new Attributes.Name("csncomponent"), "BC-XI-CON-DEMO");
//        mainAttrs.put(new Attributes.Name("JarSAP-Version"), "20081017.1000");
        mainAttrs.put(new Attributes.Name("deployfile"), "j2ee-dd.xml");

        // componentelement и его атрибуты очень важны
        String componentelement = String.format("<componentelement name=\"%s\" " +
                        "vendor=\"%s\" componenttype=\"DC\" subsystem=\"NO_SUBSYS\" location=\"SAP AG\" counter=\"%s\" " +
                        "scvendor=\"%s\" scname=\"%s\" deltaversion=\"F\" componentprovider=\"SAP AG\" " +
                        "servertype=\"P4\"/>",
                EchoAdapterConstants.raName, EchoAdapterConstants.adapterVendor, keyCounter, EchoAdapterConstants.adapterSWCvendor, EchoAdapterConstants.adapterSWCname);
        mainAttrs.put(new Attributes.Name("componentelement"), componentelement);
//        mainAttrs.put(new Attributes.Name("JarSAPProcessing-Version"), "20081017.1000");
//        mainAttrs.put(new Attributes.Name("refactoringfile"), "empty");
        rarMF.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/j2ee-dd.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write("<?xml version=\"1.0\"?>\n<SDA><SoftwareType>J2EE</SoftwareType><engine-deployment-descriptor version=\"3.0\"/></SDA>", zos);
        zos.closeEntry();

//        zipEntry = new ZipEntry("/META-INF/buildinfo.xml");
//        zos.putNextEntry(zipEntry);
//        IOUtils.write("<BUILDINFO/>", zos);
//        zos.closeEntry();

//        zipEntry = new ZipEntry("/META-INF/sap.application.global.properties");
//        zos.putNextEntry(zipEntry);
//        IOUtils.write("", zos);
//        zos.closeEntry();

//        zipEntry = new ZipEntry("src.zip.index");
//        zos.putNextEntry(zipEntry);
//        IOUtils.write("", zos);
//        zos.closeEntry();

        zipEntry = new ZipEntry(EchoAdapterConstants.rarName);
        zos.putNextEntry(zipEntry);
        IOUtils.copy(isRar, zos);
        zos.closeEntry();

        zos.close();
    }

    public void buildRAR(String ra, String con, String log, String amd) throws IOException {
        System.out.println("\uD83D\uDCE5 Колхозная система сборки RAR");
        // идея - используя src/main/java/demo и константы оттуда, что используются
        // для сборки адаптера, также собрать необходимый *.rar с ресурсами
        // попутно проверяется и используется AdapterMetadata

        InputStream isJar = Objects.requireNonNull(Files.newInputStream(Paths.get("./build/libs/default.jar")));
        OutputStream os = Files.newOutputStream(rarFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("AdapterMetadata/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("AdapterMetadata/" + EchoAdapterConstants.adapterType + ".xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(amd, zos);
        zos.closeEntry();

//        zipEntry = new ZipEntry("META-INF/buildinfo.xml");
//        zos.putNextEntry(zipEntry);
//        IOUtils.write("<BUILDINFO/>", zos);
//        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/connector-j2ee-engine.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(con, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/log-configuration.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(log, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        Manifest rarMF = new Manifest();
        Attributes mainAttrs = rarMF.getMainAttributes();
        mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttrs.put(Attributes.Name.IMPLEMENTATION_TITLE, EchoAdapterConstants.raName);
        mainAttrs.put(Attributes.Name.IMPLEMENTATION_VERSION, keyCounter);
        mainAttrs.put(Attributes.Name.SPECIFICATION_VENDOR, "SAP AG");
        mainAttrs.put(Attributes.Name.SPECIFICATION_TITLE, "XPI Resource Adapter");
//        mainAttrs.put(Attributes.Name.IMPLEMENTATION_VENDOR_ID, "sap.com");
        mainAttrs.put(Attributes.Name.SPECIFICATION_VERSION, "7.1.0");
        mainAttrs.put(Attributes.Name.IMPLEMENTATION_VENDOR, "SAP");
        rarMF.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/SAP_MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        rarMF = new Manifest();
        mainAttrs = rarMF.getMainAttributes();
        mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
//        mainAttrs.put(new Attributes.Name("projectname"), "xpi.external");
//        mainAttrs.put(new Attributes.Name("compress"), "true");
//        mainAttrs.put(new Attributes.Name("keyvendor"), "sap.com");
//        mainAttrs.put(new Attributes.Name("docfile"), "buildinfo.xml");
//        mainAttrs.put(new Attributes.Name("Ext-SDM-SDA-Comp-Version"), "1");
//        mainAttrs.put(new Attributes.Name("JarSAP-Standalone-Version"), "20081017.1000");
        //mainAttrs.put("dependencylist"), "");
        mainAttrs.put(new Attributes.Name("keycounter"), keyCounter);
//        mainAttrs.put(new Attributes.Name("SDM-SDA-Comp-Version"), "1");
//        mainAttrs.put(new Attributes.Name("keylocation"), "SAP AG");
        //mainAttrs.put(new Attributes.Name("dependencies"), "");
// b        mainAttrs.put(new Attributes.Name("JarSL-Version"), "20100616.1800");
//        mainAttrs.put(new Attributes.Name("softwaretype"), "single-module");
//        mainAttrs.put(new Attributes.Name("csncomponent"), "BC-XI-CON-AFW");
//        mainAttrs.put(new Attributes.Name("JarSAP-Version"), "20081017.1000");
        mainAttrs.put(new Attributes.Name("deployfile"), "single-module-dd.xml");
//        mainAttrs.put(new Attributes.Name("keyname"), EchoAdapterConstants.raName);
        //mainAttrs.put(new Attributes.Name("componentelement"), "");
//        mainAttrs.put(new Attributes.Name("JarSAPProcessing-Version"), "20081017.1000");
//        mainAttrs.put(new Attributes.Name("refactoringfile"), "empty");
        rarMF.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/single-module-dd.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(singleModuleDdXml, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/ra.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(ra, zos);
        zos.closeEntry();

//        zipEntry = new ZipEntry("src.zip.index");
//        zos.putNextEntry(zipEntry);
//        zos.closeEntry();

        ZipEntry zipEntryJar = new ZipEntry(EchoAdapterConstants.jarName);
        zos.putNextEntry(zipEntryJar);
        IOUtils.copy(isJar, zos);
        zos.closeEntry();

        zos.finish();
        zos.close();
    }


    @Test
    public void jlin() throws Exception {
        DeploymentInfo di = new DeploymentInfo("sap.com/aaa.ear", Version.FIRST);
        String applicationXml = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/application.xml")), StandardCharsets.UTF_8);

        di.setApplicationXML(applicationXml);
        System.out.println(di.getVersion());
        System.out.println(di.getApplicationXML().length());
        AppJLinInfo appJLinInfo = null;
    }

}

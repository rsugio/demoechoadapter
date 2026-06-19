import io.rsug.komar.DeployReference;
import io.rsug.komar.Komar;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class SdaFromWar extends DefaultTask {
    @Input
    public abstract Property<@NotNull String> getDcName();

    @InputFile
    public abstract RegularFileProperty getPropertyXml();

    @InputFile
    @Optional
    public abstract RegularFileProperty getSapGlobalApplicationPropertiesFile();

    @InputFile
    public abstract RegularFileProperty getWarFile();

    @OutputFile
    public abstract RegularFileProperty getSdaFile();

    @TaskAction
    public void buildSDA() throws IOException {
        // Строит SDA поверх WAR
        Properties props = new Properties();
        props.loadFromXML(new FileInputStream(getPropertyXml().get().getAsFile()));

        String version = Objects.requireNonNull(props.getProperty("adapterVersion"));
        String vendorName = Objects.requireNonNull(props.getProperty("adapterVendor"));
        String vendorLocation = Objects.requireNonNull(props.getProperty("adapterVendorLocation"));
        String dcNameLib = Objects.requireNonNull(props.getProperty("dcNameLib"));
        String warName = getDcName().get() + ".war";

        String keyCounter = version + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddhhmmss"));

        File warFile = getWarFile().getAsFile().get();
        File sdaFile = getSdaFile().getAsFile().get();
        getLogger().lifecycle("SDA {} from RAR {}", sdaFile, warFile);
        if (!Files.isDirectory(sdaFile.toPath().getParent())) {
            Files.createDirectories(sdaFile.toPath().getParent());
        }

        List<DeployReference> deployReferenceList = new LinkedList<>();
        deployReferenceList.addAll(Dependencies.deployReferenceList);
        DeployReference dref = new DeployReference("weak", "library", vendorName, dcNameLib);
        deployReferenceList.add(dref);

        OutputStream os = new FileOutputStream(sdaFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/application-j2ee-engine.xml");
        zos.putNextEntry(zipEntry);
        String s = Komar.generateApplicationJ2eeEngineXml(deployReferenceList, warName, "WebContainer", vendorName);
        IOUtils.write(s, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/application.xml");
        zos.putNextEntry(zipEntry);
        s = Komar.generateApplicationXmlWar(
                Objects.requireNonNull(props.getProperty("kolhoz")),
                warName,
                Objects.requireNonNull(props.getProperty("webContextRoot"))
        );
        IOUtils.write(s, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        Manifest mf = new Manifest();
        Attributes atts = mf.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.put(Attributes.Name.IMPLEMENTATION_TITLE, getDcName().get());
        atts.put(Attributes.Name.IMPLEMENTATION_VERSION, keyCounter);
        atts.put(Attributes.Name.SPECIFICATION_VENDOR, vendorName);
        atts.put(Attributes.Name.SPECIFICATION_TITLE, getDcName().get());
        atts.put(Attributes.Name.IMPLEMENTATION_VENDOR, vendorName);
        mf.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/SAP_MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        mf = new Manifest();
        atts = mf.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
// keyvendor, keyname, keycounter обязательны
        atts.put(new Attributes.Name("keyvendor"), vendorName);
        atts.put(new Attributes.Name("keyname"), getDcName().get());
        atts.put(new Attributes.Name("keycounter"), keyCounter);
        atts.put(new Attributes.Name("keylocation"), vendorLocation);
        atts.put(new Attributes.Name("softwaretype"), "J2EE");
        atts.put(new Attributes.Name("deployfile"), "j2ee-dd.xml");
        atts.put(new Attributes.Name("csncomponent"), Objects.requireNonNull(props.getProperty("csncomponent")));

        String componentelement = Komar.componentElementDC(getDcName().get(), vendorName,
                vendorLocation, keyCounter, Objects.requireNonNull(props.getProperty("swcName")), vendorName);
        atts.put(new Attributes.Name("componentelement"), componentelement);
        mf.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/j2ee-dd.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(Komar.minimalSdaDdXml("J2EE"), zos);
        zos.closeEntry();

        if (getSapGlobalApplicationPropertiesFile().isPresent()) {
            zipEntry = new ZipEntry("META-INF/sap.application.global.properties");
            zos.putNextEntry(zipEntry);
            IOUtils.copy(new FileInputStream(getSapGlobalApplicationPropertiesFile().get().getAsFile()), zos);
            zos.closeEntry();
        }

        zipEntry = new ZipEntry(warName);
        zos.putNextEntry(zipEntry);
        IOUtils.copy(new FileInputStream(warFile), zos);
        zos.closeEntry();

        zos.close();
    }

}
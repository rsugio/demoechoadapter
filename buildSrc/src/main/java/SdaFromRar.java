import io.rsug.komar.DeployReference;
import io.rsug.komar.Komar;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
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

public abstract class SdaFromRar extends DefaultTask {
    @Input
    public abstract Property<@NotNull String> getDcName();

    @InputFile
    public abstract RegularFileProperty getPropertyXml();

    @InputFile
    public abstract RegularFileProperty getRarFile();

    @OutputFile
    public abstract RegularFileProperty getSdaFile();

    @TaskAction
    public void buildSDA() throws IOException {
        // Строит SDA поверх RAR
        Properties props = new Properties();
        props.loadFromXML(new FileInputStream(getPropertyXml().get().getAsFile()));

        String version = Objects.requireNonNull(props.getProperty("adapterVersion"));
        String vendorName = Objects.requireNonNull(props.getProperty("adapterVendor"));
        String vendorLocation = Objects.requireNonNull(props.getProperty("adapterVendorLocation"));
        String dcNameLib = Objects.requireNonNull(props.getProperty("dcNameLib"));
        //String dcNameRA = Objects.requireNonNull(props.getProperty("dcNameRA"));

        String keyCounter = version + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddhhmmss"));

        File rarFile = getRarFile().getAsFile().get();
        File sdaFile = getSdaFile().getAsFile().get();
        getLogger().lifecycle("SDA {} from RAR {}", sdaFile, rarFile);
        if (!Files.isDirectory(sdaFile.toPath().getParent())) {
            Files.createDirectories(sdaFile.toPath().getParent());
        }

        List<DeployReference> deployReferenceList = new LinkedList<>();
        deployReferenceList.addAll(Dependencies.deployReferenceList);
        deployReferenceList.add(new DeployReference("hard", "library", vendorName, dcNameLib));

        OutputStream os = new FileOutputStream(sdaFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/application-j2ee-engine.xml");
        zos.putNextEntry(zipEntry);
        String s = Komar.generateApplicationJ2eeEngineXml(deployReferenceList);
        IOUtils.write(s, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/application.xml");
        zos.putNextEntry(zipEntry);
        s = Komar.generateApplicationXml(
                Objects.requireNonNull(props.getProperty("kolhoz")),
                Objects.requireNonNull(getRarFile().get().getAsFile().getName()));
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
//        atts.put(new Attributes.Name("dependencylist"), sdaSapManifestDependencyList());
//      atts.put(new Attributes.Name("dependencies"), EchoAdapterConstants.dependencies);
        atts.put(new Attributes.Name("softwaretype"), "J2EE");
        atts.put(new Attributes.Name("deployfile"), "j2ee-dd.xml");

        String componentelement = Komar.componentElementDC(getDcName().get(), vendorName,
                vendorLocation, keyCounter, Objects.requireNonNull(props.getProperty("swcName")), vendorName);
        atts.put(new Attributes.Name("componentelement"), componentelement);
        mf.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/j2ee-dd.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(Komar.minimalSdaDdXml("J2EE"), zos);
        zos.closeEntry();

//        if (Files.exists(appPropertiesFile)) {
//            zipEntry = new ZipEntry("META-INF/sap.application.global.properties");
//            zos.putNextEntry(zipEntry);
//            IOUtils.copy(Files.newInputStream(appPropertiesFile), zos);
//            zos.closeEntry();
//        }

        zipEntry = new ZipEntry(getRarFile().get().getAsFile().getName());
        zos.putNextEntry(zipEntry);
        IOUtils.copy(new FileInputStream(rarFile), zos);
        zos.closeEntry();

        zos.close();
    }

}
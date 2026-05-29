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
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class RarFromJar extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getPropertyXml();

    @Input
    public abstract Property<@NotNull String> getDcName();

    @InputFile
    public abstract RegularFileProperty getJarFile();

    @OutputFile
    public abstract RegularFileProperty getRarFile();

    @TaskAction
    public void buildRAR() throws IOException {
        Properties props = new Properties();
        props.loadFromXML(new FileInputStream(getPropertyXml().get().getAsFile()));

        String version = Objects.requireNonNull(props.getProperty("adapterVersion"));
        String keyCounter = version + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddhhmmss"));

        File rarFile = getRarFile().getAsFile().get();
        getLogger().lifecycle("RA sda to build dc {} into {}", getDcName().get(), rarFile);
        if (!Files.isDirectory(rarFile.toPath().getParent())) {
            Files.createDirectories(rarFile.toPath().getParent());
        }

        OutputStream os = new FileOutputStream(rarFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        Manifest manifest = new Manifest();
        Attributes atts = manifest.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.put(Attributes.Name.IMPLEMENTATION_TITLE, getDcName().get());
        atts.put(Attributes.Name.IMPLEMENTATION_VERSION, keyCounter);
        atts.put(Attributes.Name.SPECIFICATION_VENDOR, Objects.requireNonNull(props.getProperty("adapterVendor")));
//        atts.put(Attributes.Name.SPECIFICATION_TITLE, "XPI Resource Adapter");
//        atts.put(Attributes.Name.SPECIFICATION_VERSION, "7.1.0");
        atts.put(Attributes.Name.IMPLEMENTATION_VENDOR, Objects.requireNonNull(props.getProperty("adapterVendor")));
        manifest.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/SAP_MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        manifest = new Manifest();
        atts = manifest.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.put(new Attributes.Name("keycounter"), keyCounter);
        atts.put(new Attributes.Name("deployfile"), "single-module-dd.xml");
        manifest.write(zos);
        zos.closeEntry();

        String connj2ee = Komar.generateConnectorJ2eeXmlGenerator(Objects.requireNonNull(props.getProperty("kolhoz")), Objects.requireNonNull(props.getProperty("dcNameRA")), Dependencies.deployReferenceList);
        zipEntry = new ZipEntry("META-INF/connector-j2ee-engine.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(connj2ee, zos);
        zos.closeEntry();

        String raxml = Komar.generateRaXml(Objects.requireNonNull(props.getProperty("adapterType")), Objects.requireNonNull(props.getProperty("adapterNamespace")), Objects.requireNonNull(props.getProperty("adapterVendor")), Objects.requireNonNull(props.getProperty("adapterVersion")), Objects.requireNonNull(props.getProperty("raDescription")), Objects.requireNonNull(props.getProperty("raEis")), Objects.requireNonNull(props.getProperty("raSPIManagedConnectionFactory")), Objects.requireNonNull(props.getProperty("raCCIConnectionFactory")), Objects.requireNonNull(props.getProperty("raCCIConnection")));
        zipEntry = new ZipEntry("META-INF/ra.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(raxml, zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/single-module-dd.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(Komar.minimalSdaDdXml("single-module"), zos);
        zos.closeEntry();

        zipEntry = new ZipEntry(getDcName().get() + ".jar");
        zos.putNextEntry(zipEntry);
        IOUtils.copy(new FileInputStream(getJarFile().get().getAsFile()), zos);
        zipEntry.setComment("JAR");
        zos.closeEntry();

        zos.close();
    }
}
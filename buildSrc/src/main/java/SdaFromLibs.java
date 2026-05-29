import io.rsug.komar.Komar;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
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

public abstract class SdaFromLibs extends DefaultTask {
    @Input
    public abstract Property<@NotNull String> getDcName();

    @InputFile
    public abstract RegularFileProperty getPropertyXml();

//    @Input
//    public abstract Property<@NotNull String> getVendorName();
//
//    @Input
//    @Optional
//    public abstract Property<@NotNull String> getVendorLocation();
//
//    @Input
//    public abstract Property<@NotNull String> getSwcName();

    @InputFiles
    @Optional
    public abstract ConfigurableFileCollection getProvidedLibs();

//    @InputFile
//    @Optional
//    public abstract RegularFileProperty getJarFile();

//    @InputFile
//    @Optional
//    public abstract RegularFileProperty getRarFile();

//    @InputFile
//    @Optional
//    public abstract RegularFileProperty getWarFile();

    @OutputFile
    public abstract RegularFileProperty getSdaFile();

    //        FileTree jarFiles = (FileTree) getProject().fileTree("libs").include("**/*.jar");
    @TaskAction
    public void buildSDA() throws IOException {
        // Строит SDA с библиотеками, прописывая экспорты
        Properties props = new Properties();
        props.loadFromXML(new FileInputStream(getPropertyXml().get().getAsFile()));

        String version = Objects.requireNonNull(props.getProperty("adapterVersion"));
        String vendorName = Objects.requireNonNull(props.getProperty("adapterVendor"));
        String vendorLocation = Objects.requireNonNull(props.getProperty("adapterVendorLocation"));

        String keyCounter = version + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddhhmmss"));

        File sdaFile = getSdaFile().getAsFile().get();
        getLogger().lifecycle("library sda to build dc {} into {}", getDcName().get(), sdaFile);
        if (!Files.isDirectory(sdaFile.toPath().getParent())) {
            Files.createDirectories(sdaFile.toPath().getParent());
        }

        OutputStream os = new FileOutputStream(sdaFile);
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

        List<String> jarNameList = new LinkedList<>();
        for (File jar : getProvidedLibs().getFiles()) {
            zipEntry = new ZipEntry("lib/" + jar.getName());
            jarNameList.add(zipEntry.getName());
            zos.putNextEntry(zipEntry);
            IOUtils.copy(new FileInputStream(jar), zos);
            zos.closeEntry();
        }

        String providerXml = Komar.generateProviderXml(getDcName().get(), getDcName().get(), vendorName, null, jarNameList);

        zipEntry = new ZipEntry("server/provider.xml");
        zos.putNextEntry(zipEntry);
        IOUtils.write(providerXml, zos);
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
        atts.put(Attributes.Name.IMPLEMENTATION_TITLE, getDcName().get());
        atts.put(Attributes.Name.IMPLEMENTATION_VERSION, keyCounter);
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
        atts.put(new Attributes.Name("softwaretype"), "library");
        atts.put(new Attributes.Name("deployfile"), "javalib-dd.xml");

        // componentelement и его атрибуты очень важны
        String componentelement = Komar.componentElementDC(getDcName().get(), vendorName,
                vendorLocation, keyCounter, Objects.requireNonNull(props.getProperty("swcName")), vendorName);
        atts.put(new Attributes.Name("componentelement"), componentelement);
        mf.write(zos);
        zos.closeEntry();

        zos.close();
    }

}
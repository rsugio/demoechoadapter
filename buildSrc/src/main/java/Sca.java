import io.rsug.komar.Komar;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class Sca extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getPropertyXml();

    @InputFiles
    public abstract ConfigurableFileCollection getSdaFiles();

    @OutputFile
    public abstract RegularFileProperty getScaFile();

    @TaskAction
    public void buildSCA() throws IOException {
        Properties props = new Properties();
        props.loadFromXML(new FileInputStream(getPropertyXml().get().getAsFile()));

        String version = Objects.requireNonNull(props.getProperty("adapterVersion"));
        String vendorName = Objects.requireNonNull(props.getProperty("adapterVendor"));
        String vendorLocation = Objects.requireNonNull(props.getProperty("adapterVendorLocation"));
        String swcName = Objects.requireNonNull(props.getProperty("swcName"));
        String cmsKeyCounter = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddhhmmss"));
        String keyCounter = version + "." + cmsKeyCounter;
        String release = "0." + version;
        String serviceLevel = "0";
        String patchLevel = "0";

        File scaFile = getScaFile().getAsFile().get();
        getLogger().lifecycle("*************** SCA {} ***************", scaFile);

        OutputStream os = new FileOutputStream(scaFile);
        ZipOutputStream zos = new ZipOutputStream(os, StandardCharsets.UTF_8);
        ZipEntry zipEntry;

        zipEntry = new ZipEntry("META-INF/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("DEPLOYARCHIVES/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        zipEntry = new ZipEntry("BUILDARCHIVES/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        StringBuilder pr_deployarchivedir = new StringBuilder();
        StringBuilder pr_originaldeployarchivedir = new StringBuilder();
        LinkedHashMap<String, Attributes> sapManifestEntries = new LinkedHashMap<>();
        for (File sda : getSdaFiles().getFiles()) {
            String n = "DEPLOYARCHIVES/" + sda.getName();
            zipEntry = new ZipEntry(n);
            zos.putNextEntry(zipEntry);
            IOUtils.copy(new FileInputStream(sda), zos);
            zos.closeEntry();
            if (!sapManifestEntries.isEmpty()) {
                pr_deployarchivedir.append(";");
                pr_originaldeployarchivedir.append(";");
            }
            pr_deployarchivedir.append(n);
            pr_originaldeployarchivedir.append(sda.getName());

            ZipInputStream zis = new ZipInputStream(new FileInputStream(sda), StandardCharsets.UTF_8);
            ZipEntry ze = zis.getNextEntry();
            while (!"META-INF/SAP_MANIFEST.MF".equals(ze.getName())) {
                ze = zis.getNextEntry();
            }
            if (ze.getName().equals("META-INF/SAP_MANIFEST.MF")) {
                Manifest sdaManifest = new Manifest(zis);
                sapManifestEntries.put(n + "/", sdaManifest.getMainAttributes());
            }
            zis.close();
        }

        zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        Manifest mf = new Manifest();
        Attributes atts = mf.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.write(zos);
        zos.closeEntry();

        zipEntry = new ZipEntry("META-INF/SAP_MANIFEST.MF");
        zos.putNextEntry(zipEntry);
        mf = new Manifest();
        atts = mf.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.put(new Attributes.Name("keyname"), swcName);
        atts.put(new Attributes.Name("keyvendor"), vendorName);
        atts.put(new Attributes.Name("keycounter"), keyCounter);
        atts.put(new Attributes.Name("keylocation"), vendorLocation);

        atts.put(new Attributes.Name("pr_type"), "SC");
        atts.put(new Attributes.Name("pr_release"), release);
        atts.put(new Attributes.Name("pr_servicelevel"), serviceLevel);
        atts.put(new Attributes.Name("pr_patchlevel"), patchLevel);
        atts.put(new Attributes.Name("pr_deployarchivedir"), pr_deployarchivedir.toString());
        atts.put(new Attributes.Name("pr_owner"), "Make");
        atts.put(new Attributes.Name("skipnwdideployment"), "true");
        atts.put(new Attributes.Name("pr_softwarecomponentvendor"), vendorName);
        atts.put(new Attributes.Name("pr_cmskeycounter"), cmsKeyCounter);
        atts.put(new Attributes.Name("pr_softwarecomponentname"), swcName);
        atts.put(new Attributes.Name("pr_originaldeployarchivedir"), pr_originaldeployarchivedir.toString());
        atts.put(new Attributes.Name("pr_deltaversion"), "F");
        atts.put(new Attributes.Name("pr_updateversion"), keyCounter);
        String s = Komar.componentElementSC(swcName, vendorName, vendorLocation, keyCounter, release, serviceLevel, patchLevel, keyCounter);
        atts.put(new Attributes.Name("componentelement"), s);
        for (Map.Entry<String, Attributes> en : sapManifestEntries.entrySet()) {
            Attributes ats = new Attributes();
            ats.put(new Attributes.Name("keyname"), en.getValue().getValue("keyname"));
            ats.put(new Attributes.Name("keycounter"), en.getValue().getValue("keycounter"));
            ats.put(new Attributes.Name("softwaretype"), en.getValue().getValue("softwaretype"));
            ats.put(new Attributes.Name("keylocation"), en.getValue().getValue("keylocation"));
            ats.put(new Attributes.Name("keyvendor"), en.getValue().getValue("keyvendor"));
            mf.getEntries().put(en.getKey(), ats);
        }
        mf.write(zos);
        zos.closeEntry();
        zos.close();
    }

}
package demoecho;

import adaptermetadata.*;
import io.rsug.komar.AdapterMetaData;
import io.rsug.komar.Komar;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Dependencies {
    public static final String kolhoz = "\uD83D\uDCE5 Колхозная, им.тов.Гредлова, система сборки RAR/SDA. Ибо нефиг.";
    public static String vendor = "rsug.io";



    //TODO устранить дублирование
    public static String adapterType = "Echo";
    public static String adapterVersion = "1";
    public final static String raShortName = "demo.echoadapter";
    public final static String dcNameRA = raShortName + ".ra";
    public final static String jndi = "deployedAdapters/" + dcNameRA + "/shareable/" + dcNameRA;

    static String adapterTypeMetaData() throws Exception {
        AdapterTypeMetaData my = AdapterMetaData.makeStub(adapterType, adapterVersion, "Echo adapter");
        Attribute adapterStatus = AdapterMetaData.adapterStatus();
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(adapterStatus);
        Attribute text64 = AdapterMetaData.text("text64", 64);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(text64);
        Outbound out = AdapterMetaData.outbound(my, "NoProtocol");
        ModuleConfig mc = new ModuleConfig();
        ModuleConfigItem mci = new ModuleConfigItem();
        mci.setKey("exit");
        mci.setName("JNDIName");
        mci.setValue(jndi);
        mc.getModuleConfigItem().add(mci);
        out.getMessageProtocol().get(0).getModuleProcessorAttributes().setModuleConfig(mc);

        AttributeReference ar = new AttributeReference();
        ar.setReferenceName(text64.getName());
        out.getGlobalChannelAttributes().getTab().getAttributeReferenceOrAttributeGroup().add(ar);
        my.setOutbound(out);
        return Komar.marshallAdapterTypeMetaData(my);
    }

    public static void main(String[] args) throws Exception {
        String s = adapterTypeMetaData();
        Path where = Paths.get("resource-adapter/src/main/resources/metadata/Echo.xml.generated");
        IOUtils.write(s, Files.newOutputStream(where), StandardCharsets.UTF_8);
    }
}

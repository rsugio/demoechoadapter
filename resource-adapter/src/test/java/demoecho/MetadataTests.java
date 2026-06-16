package demoecho;

import adaptermetadata.*;
import io.rsug.komar.AdapterMetaData;
import io.rsug.komar.Komar;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MetadataTests {
    static String adapterTypeMetaData() {
        AdapterTypeMetaData my = AdapterMetaData.makeStub(EchoAdapterConstants.adapterType, EchoAdapterConstants.adapterVersion, "Echo adapter");

        // ссылочные TabDefinition, только для текстов
        // далее их id будем использовать
        TabDefinition tdMain = new TabDefinition();
        tdMain.setId("main");
        tdMain.setGuiLabels(AdapterMetaData.getLabelsEN("main"));
        my.getTabDefinition().add(tdMain);

        TabDefinition tdFault = new TabDefinition();
        tdFault.setId("throwFault");
        tdFault.setGuiLabels(AdapterMetaData.getLabelsEN("throwFault"));
        my.getTabDefinition().add(tdFault);

        TabDefinition tdAdvanced = new TabDefinition();
        tdAdvanced.setId("advanced");
        tdAdvanced.setGuiLabels(AdapterMetaData.getLabelsEN("advanced"));
        my.getTabDefinition().add(tdAdvanced);

        // отдельные атрибуты
        Attribute adapterStatus = AdapterMetaData.fixedValuesString(EchoAdapterConstants.adapterStatus,
                "Adapter status", "required",
                EchoAdapterConstants.adapterStatusActive,
                EchoAdapterConstants.adapterStatusInactive);

        Attribute text64 = AdapterMetaData.text(EchoAdapterConstants.text64, "Text64", "optional", 64, "");
        //  фолты
        Attribute throwFault = AdapterMetaData.fixedValuesString(EchoAdapterConstants.throwFault,
                "throwFault", "required",
                EchoAdapterConstants.throwNever,
                EchoAdapterConstants.throwAlways,
                EchoAdapterConstants.throwDynamicConfKey,
                EchoAdapterConstants.throwXPath);
        Attribute faultDynConfNS = AdapterMetaData.text(EchoAdapterConstants.faultDynConfNS, "DC Key namespace", "optional", 128, null);
        Attribute faultDynConfName = AdapterMetaData.text(EchoAdapterConstants.faultDynConfName, "DC Key name", "optional", 128, null);
        Attribute faultDynConfOperator = AdapterMetaData.fixedValuesString(
                EchoAdapterConstants.faultDynConfOperator,
                EchoAdapterConstants.faultDynConfOperator,
                "required",
                EchoAdapterConstants.throwOperatorExist,
                EchoAdapterConstants.throwOperatorNexst,
                EchoAdapterConstants.throwOperatorEqual,
                EchoAdapterConstants.throwOperatorNequal);
        Attribute faultDynConfValue = AdapterMetaData.text(
                EchoAdapterConstants.faultDynConfValue,
                EchoAdapterConstants.faultDynConfValue, "optional", 128, null);

        Attribute faultMessageTypeNS = AdapterMetaData.text(EchoAdapterConstants.faultMessageTypeNS,
                EchoAdapterConstants.faultMessageTypeNS, "required", 128,
                "");
        Attribute faultMessageTypeName = AdapterMetaData.text(EchoAdapterConstants.faultMessageTypeName,
                EchoAdapterConstants.faultMessageTypeName,
                "required", 128, "");

        my.getAttributeOrAttributeTableOrDynamicAttributes().add(adapterStatus);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(text64);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(throwFault);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(faultDynConfNS);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(faultDynConfName);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(faultDynConfOperator);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(faultDynConfValue);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(faultMessageTypeNS);
        my.getAttributeOrAttributeTableOrDynamicAttributes().add(faultMessageTypeName);

        // Ссылки на атрибуты
        AttributeReference arAdapterStatus = new AttributeReference();
        arAdapterStatus.setReferenceName(adapterStatus.getName());
        AttributeReference arText64 = new AttributeReference();
        arText64.setReferenceName(text64.getName());

        AttributeReference arFault = new AttributeReference();
        arFault.setReferenceName(throwFault.getName());
        EditCondition editCondition = new EditCondition();
        editCondition.setAttributeName(throwFault.getName());
        editCondition.getAttributeValue().add(EchoAdapterConstants.throwDynamicConfKey);
        AttributeReference arFaultDynConfNS = new AttributeReference();
        arFaultDynConfNS.setReferenceName(faultDynConfNS.getName());
        arFaultDynConfNS.setEditCondition(editCondition);
        AttributeReference arFaultDynConfName = new AttributeReference();
        arFaultDynConfName.setReferenceName(faultDynConfName.getName());
        arFaultDynConfName.setEditCondition(editCondition);
        AttributeReference arFaultOperator = new AttributeReference();
        arFaultOperator.setReferenceName(faultDynConfOperator.getName());
        arFaultOperator.setEditCondition(editCondition);
        AttributeReference arFaultOperatorValue = new AttributeReference();
        arFaultOperatorValue.setReferenceName(faultDynConfValue.getName());
        AndCondition andCondition = new AndCondition();
        editCondition = new EditCondition();
        editCondition.setAttributeName(throwFault.getName());
        editCondition.getAttributeValue().add(EchoAdapterConstants.throwDynamicConfKey);
        andCondition.getEditConditionOrOrCondition().add(editCondition);
        editCondition = new EditCondition();
        editCondition.setAttributeName(faultDynConfOperator.getName());
        editCondition.getAttributeValue().add(EchoAdapterConstants.throwOperatorEqual);
        editCondition.getAttributeValue().add(EchoAdapterConstants.throwOperatorNequal);
        andCondition.getEditConditionOrOrCondition().add(editCondition);
        arFaultOperatorValue.setEditCondition(editCondition);

        AttributeReference arFaultMessageTypeNS = new AttributeReference();
        arFaultMessageTypeNS.setReferenceName(faultMessageTypeNS.getName());
        editCondition = new EditCondition();
        editCondition.setAttributeName(throwFault.getName());
        editCondition.getAttributeValue().add(EchoAdapterConstants.throwAlways);
        editCondition.getAttributeValue().add(EchoAdapterConstants.throwDynamicConfKey);
        editCondition.getAttributeValue().add(EchoAdapterConstants.throwXPath);
        arFaultMessageTypeNS.setEditCondition(editCondition);
        AttributeReference arFaultMessageTypeName = new AttributeReference();
        arFaultMessageTypeName.setReferenceName(faultMessageTypeName.getName());
        arFaultMessageTypeName.setEditCondition(editCondition);

        // Receiver / outbound
        Outbound out = AdapterMetaData.outbound(my, "NoProtocol");
        ModuleConfig mc = new ModuleConfig();
        ModuleConfigItem mci = new ModuleConfigItem();
        mci.setKey("exit");
        mci.setName("JNDIName");
        mci.setValue(EchoAdapterConstants.jndi);
        mc.getModuleConfigItem().add(mci);
        // начиняем полями
        Tab tabMain = new Tab();
        tabMain.setId(tdMain.getId());
        AttributeGroup agMain = new AttributeGroup();
        agMain.setName("main");
        agMain.setGuiLabels(AdapterMetaData.getLabelsEN("Sample attributes"));
        agMain.getAttributeReference().add(arText64);
        tabMain.getAttributeReferenceOrAttributeGroup().add(agMain);
        Tab tabFault = new Tab();
        tabFault.setId(tdFault.getId());
        AttributeGroup agFault = new AttributeGroup();
        agFault.setName("throwFault");
        agFault.setGuiLabels(AdapterMetaData.getLabelsEN("Fault behaviour"));
        agFault.getAttributeReference().add(arFault);
        agFault.getAttributeReference().add(arFaultDynConfNS);
        agFault.getAttributeReference().add(arFaultDynConfName);
        agFault.getAttributeReference().add(arFaultOperator);
        agFault.getAttributeReference().add(arFaultOperatorValue);
        agFault.getAttributeReference().add(arFaultMessageTypeNS);
        agFault.getAttributeReference().add(arFaultMessageTypeName);

        tabFault.getAttributeReferenceOrAttributeGroup().add(agFault);
        Tab tabAdvanced = new Tab();
        tabAdvanced.setId(tdAdvanced.getId());
        tabAdvanced.getAttributeReferenceOrAttributeGroup().add(arAdapterStatus);

        out.getMessageProtocol().get(0).getChannelAttributes().getTab().add(tabMain);
        out.getMessageProtocol().get(0).getChannelAttributes().getTab().add(tabFault);
        out.getGlobalChannelAttributes().setTab(tabAdvanced); // getTab().getAttributeReferenceOrAttributeGroup().add(arText64);
        out.getMessageProtocol().get(0).getModuleProcessorAttributes().setModuleConfig(mc);

        my.setOutbound(out);

        return Komar.marshallAdapterTypeMetaData(my);
    }


    @Test
    public void metadataGenerator() throws Exception {
        System.out.println("Начали генерировать метаданные одаптёра");
        String s = adapterTypeMetaData();
        Path where = Paths.get("src/main/resources/metadata/Echo_generated.xml");
        System.out.println("см " + where.toAbsolutePath());
        IOUtils.write(s, Files.newOutputStream(where), StandardCharsets.UTF_8);
    }
}

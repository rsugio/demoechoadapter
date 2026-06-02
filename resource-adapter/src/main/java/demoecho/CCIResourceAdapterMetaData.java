package demoecho;
import javax.resource.cci.ResourceAdapterMetaData;

public class CCIResourceAdapterMetaData implements ResourceAdapterMetaData {
    private static final XITrace TRACE = new XITrace(CCIResourceAdapterMetaData.class.getName());
    private String vendorName = EchoAdapterConstants.adapterVendor;
    private String adapterVersion = EchoAdapterConstants.adapterVersion;
    private String specVersion = "1.5";
    private String adapterName = EchoAdapterConstants.adapterType;
    private String description = EchoAdapterConstants.raDescription;

    public String getAdapterVersion() {
        return this.adapterVersion;
    }

    public String getSpecVersion() {
        return this.specVersion;
    }

    public String getAdapterName() {
        return this.adapterName;
    }

    public String getAdapterVendorName() {
        return this.vendorName;
    }

    public String getAdapterShortDescription() {
        return this.description;
    }

    public void setAdapterVersion(String version) {
        this.adapterVersion = version;
    }

    public void setSpecVersion(String version) {
        this.specVersion = version;
    }

    public void setAdapterName(String name) {
        this.adapterName = name;
    }

    public void setAdapterVendorName(String name) {
        this.vendorName = name;
    }

    public void setAdapterShortDescription(String description) {
        this.description = description;
    }

    public String[] getInteractionSpecsSupported() {
        String SIGNATURE = "CciConnection(SpiManagedConnection)";
        TRACE.entering(SIGNATURE);
        String[] str = new String[1];
        str[0] = new String("com.sap.aii.af.ra.ms.cci.XiInteractionSpec");
        TRACE.exiting(SIGNATURE);
        return new String[0];   //str was here
    }

    public boolean supportsExecuteWithInputAndOutputRecord() {
        return false; // true;
    }

    public boolean supportsExecuteWithInputRecordOnly() {
        return false; //true;
    }

    public boolean supportsLocalTransactionDemarcation() {
        return false;
    }
}

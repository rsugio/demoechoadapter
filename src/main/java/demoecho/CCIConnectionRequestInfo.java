package demoecho;
import javax.resource.spi.ConnectionRequestInfo;

public class CCIConnectionRequestInfo implements ConnectionRequestInfo {
    private static final XITrace TRACE = new XITrace(CCIConnectionRequestInfo.class.getName());
    private String channelId;

    public String getChannelId() {
        return this.channelId;
    }

    public CCIConnectionRequestInfo(String channelId) {
        String SIGNATURE = "CciConnectionRequestInfo(String channelId)";
        TRACE.entering("CciConnectionRequestInfo(String channelId)", new Object[]{ channelId});
        this.channelId = channelId;
        TRACE.exiting("CciConnectionRequestInfo(String channelId)");
    }

    public boolean equals(Object obj) {
        String SIGNATURE = "equals(Object obj)";
        TRACE.entering("equals(Object obj)", new Object[]{obj});
        boolean equal = false;
        if (obj instanceof CCIConnectionRequestInfo) {
            CCIConnectionRequestInfo other = (CCIConnectionRequestInfo) obj;
            equal = this.isEqual(this.channelId, other.channelId);
        }

        TRACE.exiting("equals(Object obj)");
        return equal;
    }

    private boolean isEqual(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o1.equals(o2);
        }
    }

    public int hashCode() {
        String SIGNATURE = "hashCode()";
        TRACE.entering("hashCode()");
        String result = this.channelId;
        TRACE.exiting("hashCode()");
        return result.hashCode();
    }
}


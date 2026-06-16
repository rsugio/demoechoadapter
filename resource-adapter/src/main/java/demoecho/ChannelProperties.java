package demoecho;

import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.Channel;

import java.util.Objects;

public class ChannelProperties {
    public String text64 = null, adapterStatus = null;
    public String throwFault = null;
    public String faultDynConfNS = null, faultDynConfName = null, faultDynConfOperator = null, faultDynConfValue = null;
    public String faultMessageTypeNS = null, faultMessageTypeName = null;
    public final StringBuilder log = new StringBuilder();
//    public static final String NoValueDefined = "__no-value__";

    void readChannelAttributes(Channel channel) {
        text64 = $(channel, EchoAdapterConstants.text64, null);
        adapterStatus = $(channel, EchoAdapterConstants.adapterStatus, EchoAdapterConstants.adapterStatusActive);
        throwFault = $(channel, EchoAdapterConstants.throwFault, EchoAdapterConstants.throwNever);
        faultDynConfNS = $(channel, EchoAdapterConstants.faultDynConfNS, null);
        faultDynConfName = $(channel, EchoAdapterConstants.faultDynConfName, null);
        faultDynConfOperator = $(channel, EchoAdapterConstants.faultDynConfOperator, EchoAdapterConstants.throwOperatorExist);
        faultDynConfValue = $(channel, EchoAdapterConstants.faultDynConfValue, null);
        faultMessageTypeNS = $(channel, EchoAdapterConstants.faultMessageTypeNS, null);
        faultMessageTypeName = $(channel, EchoAdapterConstants.faultMessageTypeName, null);
    }

    /**
     * Метаданные адаптера могут меняться в процессе разработки и какого-то значения может не быть
     * вообще в момент чтения - делаем заглушку
     *
     * @param channel
     * @param name
     * @param default_
     * @return
     */
    public String $(Channel channel, String name, String default_) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(name);
        try {
            return channel.getValueAsString(name);
        } catch (CPAException e) {
            log.append(e.getMessage()).append("\n");
            return default_;
        }
    }

    public String toString() {
        return String.format("text64: %s, adapterStatus: %s, " +
                        "throwFault: %s, " +
                        "faultDynConfNS: %s, faultDynConfName: %s, faultDynConfOperator: %s, " +
                        "faultDynConfValue: %s, faultMessageTypeNS: %s, faultMessageTypeName: %s",
                text64, adapterStatus, throwFault, faultDynConfNS, faultDynConfName, faultDynConfOperator, faultDynConfValue, faultMessageTypeNS, faultMessageTypeName);
    }

}

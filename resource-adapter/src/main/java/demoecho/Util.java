package demoecho;


import com.sap.aii.af.lib.util.StringUtil;
import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationNotPossibleException;
import com.sap.aii.af.service.administration.api.monitoring.ChannelState;
import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.TableData;
//import com.sap.aii.utilxi.sld.SldUtil;
import com.sap.tc.logging.Location;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

public class Util {
    private static final Location TRACE = Location.getLocation(Util.class);
    private static LocalizationCallback localizer = null;
    private static Locale locale = null;

    public static void initLocalizer(LocalizationCallback localizer, Locale locale) {
        Util.localizer = localizer;
        Util.locale = locale;
    }

    public static String localizeMessage(String code) {
        return localizeMessage(locale, code);
    }

    public static String localizeMessage(String code, Object... params) {
        return localizeMessage(locale, code, params);
    }

    public static String localizeMessage(Locale locale, String code) {
        try {
            return localizer.localizeString(code, locale);
        } catch (LocalizationNotPossibleException var3) {
            return code;
        }
    }

    public static String localizeMessage(Locale locale, String code, Object... params) {
        try {
            return StringUtil.formatMessage(localizer.localizeString(code, locale), params, locale);
        } catch (LocalizationNotPossibleException var4) {
            return code;
        }
    }

    public static String getErrorMessage(Throwable e) {
        while(e.getCause() != null) {
            e = e.getCause();
        }

        return e.toString();
    }

    public static Throwable getInitialError(Throwable e) {
        while(e.getCause() != null) {
            e = e.getCause();
        }

        return e;
    }

    public static String getEntity(String resourcePath) throws ODataAdapterException {
        String separator = "?";
        if (resourcePath != null && resourcePath.contains("?")) {
            StringTokenizer token = new StringTokenizer(resourcePath, separator);
            String entity = null;
            if (token.hasMoreTokens()) {
                entity = token.nextToken();
            }

            return entity;
        } else {
            throw new ODataAdapterException(ODataAdapterError.ODATA_RESOURCE_PATH_ERROR, new String[]{"Invalid value for resource path"});
        }
    }

    public static String getQuery(String resourcePath) throws ODataAdapterException {
        String separator = "?";
        if (resourcePath != null && resourcePath.contains("?")) {
            StringTokenizer token = new StringTokenizer(resourcePath, separator);

            String entity;
            for(entity = null; token.hasMoreTokens(); entity = token.nextToken()) {
            }

            return entity;
        } else {
            throw new ODataAdapterException(ODataAdapterError.ODATA_RESOURCE_PATH_ERROR, new String[]{"Invalid value for resource path"});
        }
    }

    public static Properties getChannelAdditionalOptions(Channel _channel) throws CPAException {
        String SIGNATURE = "getChannelAdditionalOptions()";
        TRACE.entering(SIGNATURE);
        Properties addOptions = new Properties();
        if (_channel.getValueAsBoolean("useAdditionalSettings")) {
            addOptions.setProperty("Party", _channel.getParty());
            addOptions.setProperty("Service", _channel.getService());
            addOptions.setProperty("Channel", _channel.getChannelName());
            TableData optionsTable = _channel.getValueAsTable("settingsTable");
            int rowNumber = optionsTable.getRowCount();

            for(int i = 0; i < rowNumber; ++i) {
                String paramKey = optionsTable.getRow(i).getFieldValue("settingsKey");
                String paramValue = optionsTable.getRow(i).getFieldValue("settingsValue");
                addOptions.setProperty(paramKey, paramValue);
                TRACE.infoT(SIGNATURE, "Additional options for Channel {0}: {1}={2}", new Object[]{_channel.getChannelName(), paramKey, paramValue});
            }
        }

        TRACE.exiting(SIGNATURE, addOptions);
        return addOptions;
    }

    public static boolean isBatchSupported(String operationType) {
        return operationType.equalsIgnoreCase("merge") || operationType.equalsIgnoreCase("post") || operationType.equalsIgnoreCase("put") || operationType.equalsIgnoreCase("read");
    }

    public static String getIdentity(Object odataChannel) {
        String SIGNATURE = "getIdentity(ODataChannel)";
        TRACE.entering("getIdentity(ODataChannel)", new Object[]{odataChannel});
        String componentName = "<NA>";
        Properties sysprops = System.getProperties();
        String sysName = sysprops.getProperty("SAPSYSTEMNAME");
        if (sysName != null) {
            sysName = sysName.toLowerCase(Locale.ENGLISH);
        }

        String dbHost = sysprops.getProperty("j2ee.dbhost");
//        dbHost = SldUtil.normalizeHostname(dbHost);
//        if (sysName != null && dbHost != null) {
//            componentName = "af." + sysName + "." + dbHost;
//        }
//
//        ChannelConfig config = odataChannel.getChannelConfig();
        String identity = "PI System: '" + componentName + "', Party: '"; // + config.getParty() + "', Service: '" + config.getService() + "', Channel: '" + config.getChannelName() + "', Adapter Type: '" + "OData" + "'";
        TRACE.exiting("getIdentity(ODataChannel)", identity);
        return identity;
    }

    public static class InternalChannelStatus {
        public ChannelState state;
        public String message;
        public Object[] messageParams;

        public InternalChannelStatus(ChannelState state, String message, Object... messageParams) {
            this.state = state;
            this.message = message;
            this.messageParams = messageParams;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (this.getClass() != obj.getClass()) {
                return false;
            } else {
                Util.InternalChannelStatus other = (Util.InternalChannelStatus)obj;
                if (this.state == null) {
                    if (other.state != null) {
                        return false;
                    }
                } else if (!this.state.equals(other.state)) {
                    return false;
                }

                return true;
            }
        }

        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = 31 * result + (this.state == null ? 0 : this.state.hashCode());
            return result;
        }
    }
}

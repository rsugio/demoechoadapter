package demoecho;

import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesAccess;
import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesChangeListener;
import com.sap.tc.logging.Location;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ApplicationConfiguration {
    private static final Location TRACE = Location.getLocation(ApplicationConfiguration.class);
    private static ApplicationPropertiesAccess propertiesAccess = null;
    private static Properties applicationProperties;
    private static PropertyConfiguration propertyListener = null;

    public static void init() throws NamingException {
        InitialContext context = new InitialContext();
        propertiesAccess = (ApplicationPropertiesAccess)context.lookup("ApplicationConfiguration");
        if (propertiesAccess != null) {
            applicationProperties = propertiesAccess.getApplicationProperties();
            propertyListener = new PropertyConfiguration();
            propertiesAccess.addApplicationPropertiesChangedListener(propertyListener);
        }

    }

    public static void stop() {
        String SIGNATURE = "stop()";
        TRACE.entering(SIGNATURE);
        if (propertiesAccess != null && propertyListener != null) {
            propertiesAccess.removeApplicationPropertiesChangedListener(propertyListener);
        }

        TRACE.exiting(SIGNATURE);
    }

    public static boolean isProxyEnabled() {
        Properties properties = getProperties();
        return properties == null ? false : Boolean.parseBoolean(properties.getProperty("odata.proxy"));
    }

    public static String getProxyHost() {
        Properties properties = getProperties();
        return properties == null ? null : properties.getProperty("odata.proxy.host");
    }

    public static String getProxyPort() {
        Properties properties = getProperties();
        return properties == null ? null : properties.getProperty("odata.proxy.port");
    }

    public static String getProxyUsername() {
        Properties properties = getProperties();
        return properties == null ? null : properties.getProperty("odata.proxy.username");
    }

    public static String getProxyPassword() {
        Properties properties = getProperties();
        return properties == null ? null : properties.getProperty("odata.proxy.password");
    }

    public static int getHTTPConnectionTimeout() {
        Properties properties = getProperties();
        return properties == null ? 5 : Integer.parseInt(properties.getProperty("odata.http.connection.timeout"));
    }

    public static int getHTTPSocketTimeout() {
        Properties properties = getProperties();
        return properties == null ? 5 : Integer.parseInt(properties.getProperty("odata.http.socket.timeout"));
    }

    public static int getMetadataRefreshInterval() {
        Properties properties = getProperties();
        return properties == null ? 30 : Integer.parseInt(properties.getProperty("odata.metadata.refreshInterval"));
    }

    public static int getMaxMessageSize() {
        Properties properties = getProperties();
        return properties == null ? 25 : Integer.parseInt(properties.getProperty("odata.maxMessageSize"));
    }

    public static String getMinSslVersion() {
        String SIG = "getMinSslVersion()";
        TRACE.entering("getMinSslVersion()");
        String result = "TLSv1.2";
        Properties properties = getProperties();
        if (properties != null) {
            result = properties.getProperty("odata.minSslVersion");
        }

        TRACE.exiting("getMinSslVersion()", result);
        return result;
    }

    public static boolean getOAuthCacheEnabled() {
        String SIG = "getOAuthCacheEnabled()";
        TRACE.entering("getOAuthCacheEnabled()");
        boolean result = true;
        Properties properties = getProperties();
        if (properties != null) {
            result = Boolean.parseBoolean(properties.getProperty("odata.oauth.cache.enabled"));
        }

        TRACE.exiting("getOAuthCacheEnabled()", result);
        return result;
    }

    public static boolean isSyncResponceDetailsEnabled() {
        String SIG = "isSyncResponceDetailsEnabled()";
        TRACE.entering("isSyncResponceDetailsEnabled()");
        boolean result = false;
        Properties properties = getProperties();
        if (properties != null) {
            result = Boolean.parseBoolean(properties.getProperty("odata.syncResponseDetails"));
        }

        TRACE.exiting("isSyncResponceDetailsEnabled()", result);
        return result;
    }

    public static boolean isHandleApplicationErrorEnabled() {
        String SIG = "getHandleApplicationError";
        TRACE.entering("getHandleApplicationError");
        boolean result = false;
        Properties properties = getProperties();
        if (properties != null) {
            result = Boolean.parseBoolean(properties.getProperty("odata.appErrorHandler.enabled"));
        }

        TRACE.exiting("getHandleApplicationError", result);
        return result;
    }

    public static Properties getProperties() {
        if (applicationProperties == null) {
            try {
                init();
            } catch (NamingException e) {
                throw new RuntimeException(Util.localizeMessage("APPCONFIG_FAIL", new Object[]{e.getMessage()}), e);
            }
        }

        return applicationProperties;
    }

    private static Properties updateProperties() {
        ClassLoader old_class_loader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(ApplicationConfiguration.class.getClassLoader());
            InitialContext context = new InitialContext();
            propertiesAccess = (ApplicationPropertiesAccess)context.lookup("ApplicationConfiguration");
            if (propertiesAccess != null) {
                applicationProperties = propertiesAccess.getApplicationProperties();
            }
        } catch (NamingException e) {
            throw new RuntimeException(Util.localizeMessage("APPCONFIG_FAIL", new Object[]{e.getMessage()}), e);
        } finally {
            Thread.currentThread().setContextClassLoader(old_class_loader);
        }

        return applicationProperties;
    }

    private static class PropertyConfiguration implements ApplicationPropertiesChangeListener {
        private PropertyConfiguration() {
        }

        public void propertiesChanged() {
            ApplicationConfiguration.updateProperties();
            //ODataHelpServiceRegistration.registerHelpService();
        }
    }
}


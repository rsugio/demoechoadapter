package demoecho;

import com.sap.tc.logging.Category;

public class XIAdapterCategories {
    public static final Category MY_ADAPTER_ROOT = Category.getCategory(Category.getRoot(), "Applications/ExchangeInfrastructure/Adapter/" + EchoAdapterConstants.adapterType);
    public static final Category CONFIG;
    public static final Category SERVER;
    public static final Category SERVER_HTTP;
    public static final Category SERVER_JNDI;
    public static final Category SERVER_JCA;
    public static final Category CONNECT;
    public static final Category CONNECT_EIS;
    public static final Category CONNECT_AF;

    static {
        CONFIG = Category.getCategory(MY_ADAPTER_ROOT, "Configuration");
        SERVER = Category.getCategory(MY_ADAPTER_ROOT, "Server");
        SERVER_HTTP = Category.getCategory(SERVER, "HTTP");
        SERVER_JNDI = Category.getCategory(SERVER, "Naming");
        SERVER_JCA = Category.getCategory(SERVER, "JCA");
        CONNECT = Category.getCategory(MY_ADAPTER_ROOT, "Connection");
        CONNECT_EIS = Category.getCategory(SERVER, "EIS");
        CONNECT_AF = Category.getCategory(SERVER, "Adapter Framework");
    }
}

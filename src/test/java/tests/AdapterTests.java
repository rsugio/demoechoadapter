package tests;

import demoecho.EchoAdapterConstants;
import demoecho.SPIManagedConnectionFactory;
import org.junit.jupiter.api.Test;

public class AdapterTests {
    @Test
    public void runtimeUsedMethods() throws Exception {
        // Вызов методов, которые не @Override и которые лишь в рантайме есть, их нельзя удалять
        SPIManagedConnectionFactory mcf = new SPIManagedConnectionFactory();
        mcf.setAdapterType(EchoAdapterConstants.adapterType);
        mcf.setAdapterNamespace(EchoAdapterConstants.adapterNamespace);
    }
}

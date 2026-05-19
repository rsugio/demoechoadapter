package build;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

//import org.objectweb.asm.*;

public class RarBuilder {
    final Path jarFile, targetDir;
    final String keyCounter;
    final Map<String,String> constants;

    public RarBuilder(String version, Map<String,String> constants, Path jarFile, Path targetDir) {
        this.jarFile = jarFile;
        this.targetDir = targetDir;
        this.constants = constants;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime localDateTime = LocalDateTime.now();
        keyCounter = String.format("%s.%s", version, localDateTime.format(dtf));

    }

    public void build() throws IOException, JAXBException {
        System.out.println("\uD83D\uDCE5 Колхозная система сборки RAR");
        System.out.println("keyCounter: " + keyCounter);
        System.out.println("InputJar: " + jarFile);
        System.out.println("targetDir: " + targetDir);
        String raxml = ResourceAdapterGenerator.generateConnectorXml();
        System.out.println("** ra.xml **\n" + raxml);

        //InputStream isJar = Objects.requireNonNull(Files.newInputStream(Paths.get("./build/libs/default.jar")));
    }

    public String ra() {

        return "//TODO";
    }
}

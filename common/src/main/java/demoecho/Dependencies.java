package demoecho;

import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Dependencies {
    public static final String kolhoz = "\uD83D\uDCE5 Колхозная, им.тов.Гредлова, система сборки RAR/SDA. Ибо нефиг.";
    public static String vendor = "rsug.io";

    //TODO устранить дублирование
    public static String adapterType = "Echo";
    public static String adapterVersion = "1";
    public final static String raShortName = "demo.echoadapter";
    public final static String dcNameRA = raShortName + ".ra";
    public final static String jndi = "deployedAdapters/" + dcNameRA + "/shareable/" + dcNameRA;


    public static void main(String[] args) throws Exception {
    }
}

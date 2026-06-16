package demoecho;

import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesAccess;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public class CentralFileLog {
    final Path path;
    static final Set<PosixFilePermission> perms664set = PosixFilePermissions.fromString("rw-rw-r--");
    static final FileAttribute<Set<PosixFilePermission>> fileAttribute664 = PosixFilePermissions.asFileAttribute(perms664set);
    static final Set<PosixFilePermission> perms775set = PosixFilePermissions.fromString("rwxrwxr-x");
    static final FileAttribute<Set<PosixFilePermission>> dirAttribute775 = PosixFilePermissions.asFileAttribute(perms775set);
    final Path _init_;

    public CentralFileLog(InitialContext ctx) throws IOException {
        Objects.requireNonNull(ctx);
        String path1 = null;
        try {
            path1 = centralFileLogDirectoryBySystemProfile(ctx);
        } catch (NamingException ignored) {
            //
        }
        String p = path1 != null ? path1 : "/var/tmp/" + EchoAdapterConstants.centralFileLogDirectorySuffix;
        path = Paths.get(p);
        _init_ = path.resolve("_init_.txt");
        Files.createDirectories(path, dirAttribute775);

        Files.deleteIfExists(_init_);
        Files.createFile(_init_, fileAttribute664);
        String s = EchoAdapterConstants.kolhoz + "\t" + LocalDateTime.now() + "\n";
        Files.write(_init_, s.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        log_init("проверка добавления -- должна быть видна предыдущая строка");
    }

    static String centralFileLogDirectoryBySystemProfile(InitialContext ctx) throws NamingException {
        String centralFileLogDirectory = null;
        ApplicationPropertiesAccess aprop = (ApplicationPropertiesAccess) ctx.lookup("ApplicationConfiguration");
        if (aprop != null) {
            Properties system = aprop.getSystemProfile();
            if (system != null) {
                // SYS_GLOBAL_DIR=/usr/sap/JXD/SYS/global
                // centralFileLogDirectory=$SYS_GLOBAL_DIR/xi_customer_logs/echoadapter
                centralFileLogDirectory = system.getProperty("SYS_GLOBAL_DIR") + EchoAdapterConstants.centralFileLogDirectorySuffix;
            }
        }
        return centralFileLogDirectory;
    }

    static void write(Path p, String s) {
        Objects.requireNonNull(p);
        Objects.requireNonNull(s);
        try {
            Files.write(p, s.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        } catch (IOException ignored) {
        }
    }

    void log_init(String s, Object ... args) {
        Objects.requireNonNull(s);
        s = LocalDateTime.now() + "\t" + String.format(s, args) + "\n";
        try {
            Files.write(_init_, s.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    void println(String file, String s, Object ... args) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(s);
        String msg = LocalDateTime.now() + "\t" + String.format(s, args) + "\n";
        try {
            Path p = path.resolve(file);
            if (!Files.isRegularFile(p)) {
                Files.createFile(p, fileAttribute664);
            }
            Files.write(p, msg.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

}

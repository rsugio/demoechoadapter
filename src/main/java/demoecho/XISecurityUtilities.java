
package demoecho;

import java.security.AccessController;
import java.security.MessageDigest;
import java.security.PrivilegedAction;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.SecurityException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

public class XISecurityUtilities {
    private static final XITrace TRACE = new XITrace(XISecurityUtilities.class.getName());

    public static PasswordCredential getPasswordCredential(final ManagedConnectionFactory mcf, final Subject subject, ConnectionRequestInfo info) throws ResourceException {
        String SIGNATURE = "getPasswordCredential(final ManagedConnectionFactory mcf, final Subject subject, ConnectionRequestInfo info)";
        TRACE.entering("getPasswordCredential(final ManagedConnectionFactory mcf, final Subject subject, ConnectionRequestInfo info)", new Object[]{mcf, subject, info});
        PasswordCredential credential = null;
        if (subject == null) {
            if (info == null) {
                credential = null;
            } else {
                CCIConnectionRequestInfo myinfo = (CCIConnectionRequestInfo)info;
                credential = new PasswordCredential("fake-user", "password".toCharArray());
                credential.setManagedConnectionFactory(mcf);
            }
        } else {
            credential = (PasswordCredential)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    for(PasswordCredential temp : subject.getPrivateCredentials(PasswordCredential.class)) {
                        if (temp.getManagedConnectionFactory().equals(mcf)) {
                            return temp;
                        }
                    }

                    return null;
                }
            });
            if (credential == null) {
                throw new SecurityException("No PasswordCredential found");
            }
        }

        TRACE.exiting("getPasswordCredential(final ManagedConnectionFactory mcf, final Subject subject, ConnectionRequestInfo info)");
        return credential;
    }

    public static boolean isEqual(String a, String b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    public static boolean isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b) {
        String SIGNATURE = "isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b)";
        TRACE.entering("isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b)", new Object[]{a, b});
        boolean equal = false;
        if (a == b) {
            equal = true;
        } else if (a == null && b != null) {
            equal = false;
        } else if (a != null && b == null) {
            equal = false;
        } else if (!isEqual(a.getUserName(), b.getUserName())) {
            equal = false;
        } else {
            String p1 = null;
            String p2 = null;
            if (a.getPassword() != null) {
                p1 = new String(a.getPassword());
            }

            if (b.getPassword() != null) {
                p2 = new String(b.getPassword());
            }

            equal = isEqual(p1, p2);
        }

        TRACE.exiting("isPasswordCredentialEqual(PasswordCredential a, PasswordCredential b)");
        return equal;
    }

    public static String digest(String s) {
        String SIGNATURE = "digest(String)";
        TRACE.entering("digest(String)", new Object[]{s});
        String digestString = null;

        String var3;
        try {
            if (s != null && s.length() != 0) {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.reset();
                digest.update(s.getBytes("UTF-8"));
                byte[] b = digest.digest();
                digestString = bytesToHexString(b);
                String var5 = digestString;
                return var5;
            }

            var3 = digestString;
        } catch (Exception e) {
            TRACE.catching("digest(String)", e);
            digestString = s;
            String b = s;
            return b;
        } finally {
            TRACE.exiting("digest(String)", digestString);
        }

        return var3;
    }

    public static String bytesToHexString(byte[] buf) {
        StringBuffer sb = new StringBuffer();

        for(byte b : buf) {
            sb.append(Integer.toHexString(b < 0 ? 256 + b : b));
        }

        return sb.toString();
    }
}


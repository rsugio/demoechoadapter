
package demoecho;

import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPALookupManager;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.TextPayload;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.tc.logging.Location;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;

public class ConvertCRLFfromToLF0 implements Module {
    static final long serialVersionUID = 7435850550539048631L;
    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final String CRLF = "\r\n";
    private static final String LF = "\n";

    public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException {
        String SIGNATURE = "process(ModuleContext moduleContext, ModuleData inputModuleData)";
        Location location = null;

        try {
            location = Location.getLocation(ConvertCRLFfromToLF0.class.getName());
        } catch (Exception t) {
            t.printStackTrace();
            ModuleException me = new ModuleException("Unable to create trace location", t);
            throw me;
        }

        location.entering(SIGNATURE, new Object[]{moduleContext, inputModuleData});
        Object obj = null;
        Message msg = null;

        try {
            obj = inputModuleData.getPrincipalData();
            msg = (Message)obj;
        } catch (Exception e) {
            this.locationCatching(SIGNATURE, e, location);
            ModuleException me = new ModuleException(e);
            location.throwing(SIGNATURE, me);
            throw me;
        }

        if (msg == null) {
            String errTxt = "Null as XI message received (PrincipalData in ModulData is null)";
            location.errorT(SIGNATURE, errTxt);
            ModuleException me = new ModuleException(errTxt);
            location.throwing(SIGNATURE, me);
            throw me;
        } else {
            String cid = null;
            String mode = null;
            Channel channel = null;

            try {
                mode = moduleContext.getContextData("mode");
                cid = moduleContext.getChannelID();
                CPALookupManager lm = CPAFactory.getInstance().getLookupManager();
                channel = (Channel)lm.getCPAObject(CPAObjectType.CHANNEL, cid);
                if (mode == null) {
                    location.debugT(SIGNATURE, "Mode parameter is not set. Switch to 'none' as default.");
                    mode = "none";
                }

                location.debugT(SIGNATURE, "Mode is set to {0}", new Object[]{mode});
            } catch (Exception e) {
                this.locationCatching(SIGNATURE, e, location);
                location.errorT(SIGNATURE, "Cannot read the module context and configuration data");
                ModuleException me = new ModuleException(e);
                location.throwing(SIGNATURE, me);
                throw me;
            }

            if (mode.compareToIgnoreCase("none") == 0) {
                location.debugT(SIGNATURE, "Bypass CRLF conversion since 'mode' parameter was set to 'none'.");
            } else if (mode.compareToIgnoreCase("CRLFtoNative") == 0 && "\r\n".equals(LINE_SEP)) {
                location.debugT(SIGNATURE, "Bypass CRLF conversion since 'mode' parameter was set to 'CRLFtoNative' and the native line separator is CRLF.");
            } else if (mode.compareToIgnoreCase("LFtoNative") == 0 && "\n".equals(LINE_SEP)) {
                location.debugT(SIGNATURE, "Bypass CRLF conversion since 'mode' parameter was set to 'LFtoNative' and the native line separator is LF.");
            } else {
                try {
                    XMLPayload xmlpayload = msg.getDocument();
                    if (xmlpayload != null) {
                        if (mode.compareToIgnoreCase("CRLFtoLF") == 0) {
                            xmlpayload.setContent(this.convertCRLFtoLF(xmlpayload.getContent(), location));
                        } else if (mode.compareToIgnoreCase("CRLFtoNative") == 0) {
                            xmlpayload.setContent(this.convertCRLFtoNative(xmlpayload.getContent(), location));
                        } else if (mode.compareToIgnoreCase("LFtoNative") == 0) {
                            xmlpayload.setContent(this.convertLFtoNative(xmlpayload.getContent(), location));
                        } else {
                            xmlpayload.setContent(this.convertLFtoCRLF(xmlpayload.getContent(), location));
                        }
                    }

                    Iterator iter = msg.getAttachmentIterator();
                    Payload payload = null;

                    while(iter.hasNext()) {
                        payload = (Payload)iter.next();
                        if (payload instanceof TextPayload) {
                            TextPayload textpayload = (TextPayload)payload;
                            if (mode.compareToIgnoreCase("CRLFtoLF") == 0) {
                                textpayload.setContent(this.convertCRLFtoLF(textpayload.getContent(), location));
                            } else if (mode.compareToIgnoreCase("CRLFtoNative") == 0) {
                                textpayload.setContent(this.convertCRLFtoNative(textpayload.getContent(), location));
                            } else if (mode.compareToIgnoreCase("LFtoNative") == 0) {
                                textpayload.setContent(this.convertLFtoNative(textpayload.getContent(), location));
                            } else {
                                textpayload.setContent(this.convertLFtoCRLF(textpayload.getContent(), location));
                            }
                        }
                    }

                    inputModuleData.setPrincipalData(msg);
                    location.debugT(SIGNATURE, "CRLF conversion finished sucessfully.");
                } catch (Exception e) {
                    this.locationCatching(SIGNATURE, e, location);
                    location.errorT(SIGNATURE, "Cannot convert one of the payloads. Reason: {0}", new Object[]{e.getMessage()});
                    ModuleException me = new ModuleException(e);
                    location.throwing(SIGNATURE, me);
                    throw me;
                }
            }

            location.exiting(SIGNATURE);
            return inputModuleData;
        }
    }

    private byte[] convertLFtoCRLF(byte[] src, Location location) {
        String SIGNATURE = "convertLFtoCRLF(byte[] src)";
        location.entering(SIGNATURE, new Object[]{src});
        byte[] buf = new byte[2 * src.length];
        int actualCount = 0;
        int maxCount = 0;

        for(int i = 0; i < src.length; ++i) {
            if (src[i] == 10) {
                buf[actualCount] = 13;
                buf[actualCount + 1] = 10;
                actualCount += 2;
            } else {
                buf[actualCount++] = src[i];
            }
        }

        byte[] dst = new byte[actualCount];
        System.arraycopy(buf, 0, dst, 0, actualCount);
        location.debugT(SIGNATURE, "Found {0} LFs that were replaced by CRLF", new Object[]{String.valueOf(actualCount - src.length)});
        location.exiting(SIGNATURE);
        return dst;
    }

    private byte[] convertCRLFtoLF(byte[] src, Location location) {
        String SIGNATURE = "convertCRLFtoLF(byte[] src)";
        location.entering(SIGNATURE, new Object[]{src});
        int srclen = src.length;
        int dstlen = 0;

        for(int i = 0; i < srclen; ++i) {
            if (src[i] != 10) {
                if (src[i] != 13) {
                    src[dstlen++] = src[i];
                } else {
                    src[dstlen++] = 10;
                }
            }
        }

        byte[] dst = new byte[dstlen];
        System.arraycopy(src, 0, dst, 0, dstlen);
        location.debugT(SIGNATURE, "Found {0} CRLFs that were replaced by LF", new Object[]{String.valueOf(srclen - dstlen)});
        location.exiting(SIGNATURE);
        return dst;
    }

    private byte[] convertLFtoNative(byte[] src, Location location) {
        String SIGNATURE = "convertLFtoNative(byte[] src)";
        location.entering(SIGNATURE, new Object[]{src});
        byte[] buf = new byte[2 * src.length];
        int actualCount = 0;
        int maxCount = 0;
        int replacedCount = 0;

        for(int i = 0; i < src.length; ++i) {
            if (src[i] != 10) {
                buf[actualCount++] = src[i];
            } else {
                for(int j = 0; j < LINE_SEP.length(); ++j) {
                    src[actualCount++] = (byte)LINE_SEP.charAt(j);
                }

                ++replacedCount;
            }
        }

        byte[] dst = new byte[actualCount];
        System.arraycopy(buf, 0, dst, 0, actualCount);
        location.debugT(SIGNATURE, "Found {0} LFs that were replaced by the native line ending", new Object[]{String.valueOf(replacedCount)});
        location.exiting(SIGNATURE);
        return dst;
    }

    private byte[] convertCRLFtoNative(byte[] src, Location location) {
        String SIGNATURE = "convertCRLFtoNative(byte[] src)";
        location.entering(SIGNATURE, new Object[]{src});
        int dstlen = 0;
        int srclen = src.length;
        int replacedCount = 0;

        for(int i = 0; i < srclen; ++i) {
            if (i + 1 < srclen && src[i] == 13 && src[i + 1] == 10) {
                for(int j = 0; j < LINE_SEP.length(); ++j) {
                    src[dstlen++] = (byte)LINE_SEP.charAt(j);
                }

                ++i;
                ++replacedCount;
            } else {
                src[dstlen++] = src[i];
            }
        }

        byte[] dst = new byte[dstlen];
        System.arraycopy(src, 0, dst, 0, dstlen);
        location.debugT(SIGNATURE, "Found {0} CRLFs that were replaced by the native line ending", new Object[]{String.valueOf(replacedCount)});
        location.exiting(SIGNATURE);
        return dst;
    }

    private void locationCatching(String signature, Throwable t, Location location) {
        if (location != null && location.beLogged(400)) {
            ByteArrayOutputStream oStream = new ByteArrayOutputStream(1024);
            PrintStream pStream = new PrintStream(oStream);
            t.printStackTrace(pStream);
            pStream.close();
            String stackTrace = oStream.toString();
            location.warningT(signature, "Catching {0}", new Object[]{stackTrace});
        }

    }
}


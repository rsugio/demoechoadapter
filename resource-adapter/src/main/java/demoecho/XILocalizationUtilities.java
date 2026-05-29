
package demoecho;

import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.ResourceBundleLocalizationCallback;

public class XILocalizationUtilities {
    private XILocalizationUtilities() {
    }

    public static LocalizationCallback getLocalizationCallback() {
        return new ResourceBundleLocalizationCallback(XILocalizationUtilities.class.getPackage().getName() + ".rb_JCAAdapter_ChannelMonitor", XILocalizationUtilities.class.getClassLoader());
    }
}

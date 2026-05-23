package demoecho;

public class ODataAdapterException extends Exception {
    private static final String ERROR_CODE_PREFIX = "ODEX_";
    private static final long serialVersionUID = 1L;
    private ODataAdapterError errorCode;
    private String errorMessage;

    public ODataAdapterException(ODataAdapterError errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode + ":" + com.sap.aii.adapter.odata.ra.xi.util.Util.localizeMessage("ODEX_" + errorCode.toString());
    }

    public ODataAdapterException(ODataAdapterError errorCode, String... params) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode + ":" + com.sap.aii.adapter.odata.ra.xi.util.Util.localizeMessage("ODEX_" + errorCode.toString(), params);
    }

    public ODataAdapterException(Exception ex, ODataAdapterError errorCode) {
        super(ex);
        this.errorCode = errorCode;
        this.errorMessage = errorCode + ":" + com.sap.aii.adapter.odata.ra.xi.util.Util.localizeMessage("ODEX_" + errorCode.toString());
    }

    public ODataAdapterException(Exception ex, ODataAdapterError errorCode, String... params) {
        super(ex);
        this.errorCode = errorCode;
        this.errorMessage = errorCode + ":" + Util.localizeMessage("ODEX_" + errorCode.toString(), params);
    }

    public ODataAdapterError getError() {
        return this.errorCode;
    }

    public String getMessage() {
        return this.errorMessage;
    }
}

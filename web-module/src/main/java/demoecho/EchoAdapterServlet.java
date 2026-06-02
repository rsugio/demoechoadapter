package demoecho;

import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesAccess;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class EchoAdapterServlet extends HttpServlet {
    ApplicationPropertiesAccess applicationConfiguration = null;
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            InitialContext ctx = new InitialContext();
            applicationConfiguration = (ApplicationPropertiesAccess) ctx.lookup("ApplicationConfiguration");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain; charset=utf-8");
        PrintWriter pw = new PrintWriter(resp.getOutputStream());
        pw.write("demoecho.EchoAdapterServlet.doGet()\n");
        if (applicationConfiguration==null) {
            pw.write("applicationConfiguration=null\n");
        } else {
            pw.write("applicationConfiguration.getSystemProfile(): " + applicationConfiguration.getSystemProfile() + "\n");
            pw.write("applicationConfiguration.getApplicationProperties(): " + applicationConfiguration.getApplicationProperties() + "\n");
        }
        pw.flush();
        pw.close();
    }
}
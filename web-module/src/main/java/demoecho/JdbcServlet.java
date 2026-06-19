package demoecho;

import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesAccess;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcServlet extends HttpServlet {
    ApplicationPropertiesAccess applicationConfiguration = null;
    Properties config = null;
    DataSource dataSource = null;
    Connection conn = null;
    Exception initException = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            InitialContext ctx = new InitialContext();
            applicationConfiguration = (ApplicationPropertiesAccess) ctx.lookup("ApplicationConfiguration");
            this.config = applicationConfiguration.getApplicationProperties();
            String dataSourceName = this.config.getProperty("jdbc.dataSourceName");
            if (dataSourceName != null && !dataSourceName.isEmpty()) {
                dataSource = (DataSource) ctx.lookup(dataSourceName);
                conn = dataSource.getConnection();
                return;
            }

            String jdbcSelector = this.config.getProperty("jdbc.selector");
            if (jdbcSelector == null || jdbcSelector.isEmpty()) {
                return;
            }
            String driverClassName = this.config.getProperty(jdbcSelector + ".driverClassName");
            String url = this.config.getProperty(jdbcSelector + ".url");
            String username = this.config.getProperty(jdbcSelector + ".username");
            String password = this.config.getProperty(jdbcSelector + ".password");
            if (driverClassName != null && url != null) {
                Class.forName(driverClassName);
                conn = DriverManager.getConnection(url, username, password);
            }
        } catch (NamingException | SQLException | ClassNotFoundException e) {
            initException = e;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (initException != null) {
            resp.sendError(500);
            PrintWriter pw = new PrintWriter(resp.getOutputStream());
            resp.setContentType("text/plain; charset=utf-8");
            pw.printf("Initialization error: %s", initException.getMessage());
            pw.close();
            return;
        }
        PrintWriter pw = new PrintWriter(resp.getOutputStream());
        resp.setContentType("text/plain; charset=utf-8");
        pw.println(JdbcServlet.class.getName());
        try {
            pw.println(conn);
        } catch (Exception e) {
            e.printStackTrace(pw);
        }
        pw.flush();
        pw.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    public void destroy() {
        try {
            if (conn != null) {
                conn.close();
            }
            if (dataSource != null && dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

}

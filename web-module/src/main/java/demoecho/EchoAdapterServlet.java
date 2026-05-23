package demoecho;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
//import com.sap.portal.pcm.system.ISystems;

public class EchoAdapterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain;charset=utf-8");
        PrintWriter pw = new PrintWriter(resp.getOutputStream());
        pw.write("demoecho.EchoAdapterServlet.doGet()\n");
        pw.flush();
        pw.close();
    }
}
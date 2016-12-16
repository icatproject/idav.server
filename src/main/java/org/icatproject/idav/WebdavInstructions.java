package org.icatproject.idav;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.icatproject.idav.manager.PropertyManager;

@WebServlet("/instructions")
public class WebdavInstructions extends HttpServlet {
    
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(WebdavInstructions.class);
    
    private PropertyManager properties;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        try (BufferedReader br = new BufferedReader(new FileReader("/home/glassfish/glassfish4/glassfish/domains/domain1/applications/IDAV-0.0.2-SNAPSHOT/instructions.html"))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
        }
    }
}

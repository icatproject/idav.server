package org.icatproject.idav;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/instructions")
public class WebdavInstructions extends HttpServlet {
    
    @Resource(lookup="java:app/AppName")
    private String appName;
    
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebdavInstructions.class);
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        String relativePath = "../applications/" + appName + "/instructions.html";
        
        try (BufferedReader br = new BufferedReader(new FileReader(relativePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
        }
    }
}

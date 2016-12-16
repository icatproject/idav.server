package org.icatproject.idav;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.icatproject.idav.manager.PropertyManager;

@WebServlet("/entities")
public class WebdavEntities extends HttpServlet {
    
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(WebdavInstructions.class);
    
    private PropertyManager properties;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        // Get the name of the image file with getFileName()
        Path path = Paths.get(request.getRequestURI());
        String entityName = path.getFileName().toString();
        
        // Get the absolute path of the image
        String filename = "/home/glassfish/glassfish4/glassfish/domains/domain1/applications/IDAV-0.0.2-SNAPSHOT/webdav_files_isis/" + entityName;
        
        String extension = "";

        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i+1);
        }
        
        // If the file is a bat file, we want it to automatically download rather than displaying in the browser.
        // We set the name of the file and set it to force download.
        if (extension.equals("bat")) {
            response.setContentType("application/force-download");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Content-Disposition","attachment; filename=" + entityName);
        }
        
        File file = new File(filename);
        response.setContentLength((int)file.length());
        FileInputStream in = new FileInputStream(file);
        OutputStream out = response.getOutputStream();

        // Copy the contents of the file to the output stream
        byte[] buf = new byte[4096];
        int count = 0;
        
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        
        out.close();
        in.close();
    }
}

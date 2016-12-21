package org.icatproject.idav;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icatproject.idav.manager.PropertyManager;

/**
 * 
 * A servlet to serve up the HTML page, images and bat file from the Glassfish applications
 * folder, customising them with server specific text if this option has been set in the 
 * properties file. 
 *  
 * The instructions have been done like this so that a separate context "/instructions"
 * can be defined within the webdav application context and any requests for the
 * instructions pages are then directed through this servlet. All other requests are
 * directed through the main "webdav" servlet.
 * 
 * Facilities requiring more custom instructions (particularly non-STFC facilities) will
 * need to either replace the instructions pages and images within the IDAV war file
 * or host them separately somewhere else.
 *
 */
@SuppressWarnings("serial")
@WebServlet("/instructions")
public class WebdavInstructions extends HttpServlet {
    
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebdavInstructions.class);

    @Resource(lookup="java:app/AppName")
    private String appName;

    private static Path APP_FOLDER_PATH;
    
    private static boolean INIT_DONE = false;

    private static PropertyManager propertyManager = new PropertyManager(Utils.PROPERTIES_FILENAME, Utils.HIERARCHY_FILENAME);
    
    private static Font DIALOG_BOLD_12_FONT = new Font(Font.DIALOG, Font.BOLD, 12);
    private static Font DIALOG_BOLD_16_FONT = new Font(Font.DIALOG, Font.BOLD, 16);

	@Override
	public void init() throws ServletException {
		LOG.info("WebdavInstructions init() called");
    	LOG.info("appName = " + appName);
		APP_FOLDER_PATH = FileSystems.getDefault().getPath("..", "applications", appName);
    	LOG.info("APP_FOLDER_PATH = " + APP_FOLDER_PATH);
	}
		
    /**
     * Make simple automatic customisations to the instructions on how to connect
     * to and use the webdav server. This consists of adding text specific to this
     * deployment of IDAV to the images displayed in the instructions and inserting
     * some deployment specific strings into the HTML page.
     * 
     * This method should only be called once upon the first request after the webapp
     * has been deployed. This creates the custom images and HTML page, after which 
     * they are available to all other requests.
     * 
     * This method (called via doGet) is used in preference to the servlet init
     * method because the server URL variables are not all available via the
     * servlet context but are available in the servlet request.
     * 
     * @param request
     */
    private void doCustomInit(HttpServletRequest request) {
    	LOG.info("doCustomInit() called");

    	String facilityName = propertyManager.getFacilityName();

    	String scheme = request.getScheme();
    	String serverName = request.getServerName();
    	// context will have a leading / (on Glassfish anyway!)
    	// remove it because it is easier to work with
    	String context = request.getContextPath();
    	if (context.startsWith("/")) {
    		context = context.substring(1);
    	}

    	LOG.info("facilityName = " + facilityName);
    	LOG.info("Scheme = " + scheme);
    	LOG.info("ServerName = " + serverName);
    	LOG.info("Context = " + context);

    	try {
    		// customise the images with server specific text
    		doMapNetworkDriveImage2(facilityName, scheme, serverName, context);
    		doMapNetworkDriveImage3(facilityName, scheme, serverName, context);
    		doMapNetworkDriveImage4(facilityName, scheme, serverName, context);
    	} catch (IOException e) {
    		LOG.error("Failed to create image for instructions: " + e.getMessage());
    	}
    	try {
    		// customise the HTML page with server specific text
    		doCreateInstructionsHtmlPage(facilityName, scheme, serverName, context);
    	} catch (IOException e) {
    		LOG.error("Failed to create instructions.html page: " + e.getMessage());
    	}
    }

	private void doCreateInstructionsHtmlPage(String facilityName, String scheme, String serverName, String context) throws IOException {
        String webdavUrl = scheme + "://" + serverName + "/" + context + "/";
		String filename = "instructions.example.html";
        String newFilename = filename.replace(".example.", ".");
        File newHtmlFile = new File(APP_FOLDER_PATH.toFile(), newFilename);
        File htmlFile = new File(APP_FOLDER_PATH.toFile(), filename);
		byte[] fileBytes = Files.readAllBytes(htmlFile.toPath());
		String fileString = new String(fileBytes);
		// leading \\ escapes the $ because replaceAll requires a regular expression 
		fileString = fileString.replaceAll("\\$facilityName", facilityName);
		fileString = fileString.replaceAll("\\$webdavUrl", webdavUrl);
		PrintWriter out = new PrintWriter(new File(APP_FOLDER_PATH.toFile(), newFilename));
		out.write(fileString);
		out.close();
        LOG.info("HTML file created: " + newHtmlFile);
	}
	
    private void doMapNetworkDriveImage2(String facilityName, String scheme, String serverName, String context) throws IOException {
		String filename = "map_network_drive_w7_2.png";
		String extension = filename.substring(filename.indexOf(".") + 1);
        String textToInsert = scheme + "://" + serverName + "/" + context + "/";
        BufferedImage image = ImageIO.read(new File(APP_FOLDER_PATH.toFile(), filename));
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.DARK_GRAY);
        graphics.setFont(DIALOG_BOLD_12_FONT);
        graphics.drawString(textToInsert, 140, 222);
        String newFilename = filename.replace(".", "_" + facilityName + "." );
        File newImageFile = new File(APP_FOLDER_PATH.toFile(), newFilename);
        ImageIO.write(image, extension, newImageFile);
        LOG.info("Image created: " + newImageFile);
	}

	private static void doMapNetworkDriveImage3(String facilityName, String scheme, String serverName, String context) throws IOException {
		String filename = "map_network_drive_w7_3.png";
		String extension = filename.substring(filename.indexOf(".") + 1);
        String url = scheme + "://" + serverName + "/" + context + "/";
        String text1 = "Attempting to connect to " + url + " ...";
        String text2 = "Connect to " + serverName;
        String text3 = "Connecting to " + serverName;
        BufferedImage image = ImageIO.read(new File(APP_FOLDER_PATH.toFile(), filename));
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.DARK_GRAY);
        graphics.setFont(DIALOG_BOLD_16_FONT);
        graphics.drawString(text2, 50, 295);
        graphics.setFont(DIALOG_BOLD_12_FONT);
        graphics.drawString(text1, 40, 110);
        graphics.drawString(url, 140, 220);
        graphics.drawString(text3, 51, 312);
        String newFilename = filename.replace(".", "_" + facilityName + "." );
        File newImageFile = new File(APP_FOLDER_PATH.toFile(), newFilename);
        ImageIO.write(image, extension, newImageFile);
        LOG.info("Image created: " + newImageFile);
	}

	private static void doMapNetworkDriveImage4(String facilityName, String scheme, String serverName, String context) throws IOException {
		String filename = "map_network_drive_w7_4.png";
		String extension = filename.substring(filename.indexOf(".") + 1);
		String driveString = context + " (\\\\" + serverName + "@SSL) (X:)";
        BufferedImage image = ImageIO.read(new File(APP_FOLDER_PATH.toFile(), filename));
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.GRAY);
        graphics.setFont(DIALOG_BOLD_12_FONT);
        graphics.drawString(driveString, 185, 39);
        graphics.drawString(driveString, 50, 402);
        graphics.drawString(facilityName, 430, 130);
        String newFilename = filename.replace(".", "_" + facilityName + "." );
        File newImageFile = new File(APP_FOLDER_PATH.toFile(), newFilename);
        ImageIO.write(image, extension, newImageFile);
        LOG.info("Image created: " + newImageFile);
	}

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	if (!INIT_DONE) {
    		if (propertyManager.getAutoCreateInstructions()) {
    			doCustomInit(request);
    		}
    		INIT_DONE = true;
    	}
    	
    	// get the filename from the end of the URI
    	// taking only what is after the last forward slash should prevent
    	// any attempts to request ../../.. etc.
    	String requestURI = request.getRequestURI();
    	String filename = requestURI.substring(requestURI.lastIndexOf("/")+1);
    	
        File file = new File(APP_FOLDER_PATH.toFile(), filename);

        if (!file.exists()) {
        	// return a 404
        	response.sendError(HttpServletResponse.SC_NOT_FOUND);
        	return;
        }
        
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
            response.setHeader("Content-Disposition","attachment; filename=" + filename);
        }
        
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

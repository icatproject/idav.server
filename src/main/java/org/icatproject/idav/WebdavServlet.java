/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.icatproject.idav;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;

import javax.servlet.ServletException;


/**
 * Servlet which provides support for WebDAV level 2.
 * 
 * the original class is org.apache.catalina.servlets.WebdavServlet by Remy
 * Maucherat, which was heavily changed
 * 
 * @author Remy Maucherat
 */

@SuppressWarnings("serial")
public class WebdavServlet extends WebDavServletBean {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(WebdavServlet.class);

    private Properties properties = new Properties();

    // KP - This method has been modified to read the values 
    // from a properties file instead of the web.xml file.
    @Override
    public void init() throws ServletException {

    	try {
    		properties.load(new FileInputStream(Utils.PROPERTIES_FILENAME));
    	} catch (IOException e) {
    		throw new ServletException("Unable to load properties from file: '" + Utils.PROPERTIES_FILENAME + "'", e);
    	}
    	
        // Parameters now from properties file instead of web.xml
        String clazzName = properties.getProperty("webdavImplementationClassName");
        if (clazzName == null || clazzName.equals("")) {
            throw new ServletException("Property 'webdavImplementationClassName' must be specified in " + Utils.PROPERTIES_FILENAME);
        }
        LOG.info("webdavImplementationClassName is: '" + clazzName + "'");
        
        IWebdavStore webdavStore = constructStore(clazzName);

        boolean lazyFolderCreationOnPut = false; 
        String lazyFolderCreationOnPutString = properties.getProperty("lazyFolderCreationOnPut");
        if ( lazyFolderCreationOnPutString != null && lazyFolderCreationOnPutString.equalsIgnoreCase("TRUE") ) {
        	lazyFolderCreationOnPut = true;
        }
        LOG.info("lazyFolderCreationOnPut is: '" + lazyFolderCreationOnPut + "'");
        
        String defaultIndexFile = properties.getProperty("defaultIndexFile");
        LOG.info("defaultIndexFile is: '" + defaultIndexFile + "'");
        String insteadOf404 = properties.getProperty("insteadOf404");
        LOG.info("insteadOf404 is: '" + insteadOf404 + "'");

        boolean setContentLengthHeaders = false;
        String setContentLengthHeadersString = properties.getProperty("setContentLengthHeaders");
        if ( setContentLengthHeadersString != null && setContentLengthHeadersString.equalsIgnoreCase("TRUE") ) {
        	setContentLengthHeaders = true;
        }
        LOG.info("setContentLengthHeaders is: '" + setContentLengthHeaders + "'");

        super.init(webdavStore, defaultIndexFile, insteadOf404,
        		setContentLengthHeaders, lazyFolderCreationOnPut);
        
        LOG.info("WebdavServlet init complete");
    }

    protected IWebdavStore constructStore(String clazzName) throws ServletException {
        IWebdavStore webdavStore;
        try {
            Class<?> clazz = WebdavServlet.class.getClassLoader().loadClass(
                    clazzName);

            Constructor<?> ctor = clazz
                    .getConstructor(new Class[] {});

            webdavStore = (IWebdavStore) ctor
                    .newInstance(new Object[] {});
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Unable to create store from clazzName '" + clazzName + "'", e);
        }
        return webdavStore;
    }

    // TODO - if this method is being kept it needs moving into the 
    // LocalFileSystemStore implementation because it is implementation specific.
//    private File getFileRoot() {
//        String rootPath = getInitParameter(ROOTPATH_PARAMETER);
//        if (rootPath == null) {
//            throw new WebdavException("missing parameter: "
//                    + ROOTPATH_PARAMETER);
//        }
//        if (rootPath.equals("*WAR-FILE-ROOT*")) {
//            String file = LocalFileSystemStore.class.getProtectionDomain()
//                    .getCodeSource().getLocation().getFile().replace('\\', '/');
//            if (file.charAt(0) == '/'
//                    && System.getProperty("os.name").indexOf("Windows") != -1) {
//                file = file.substring(1, file.length());
//            }
//
//            int ix = file.indexOf("/WEB-INF/");
//            if (ix != -1) {
//                rootPath = file.substring(0, ix).replace('/',
//                        File.separatorChar);
//            } else {
//                throw new WebdavException(
//                        "Could not determine root of war file. Can't extract from path '"
//                                + file + "' for this web container");
//            }
//        }
//        return new File(rootPath);
//    }

}

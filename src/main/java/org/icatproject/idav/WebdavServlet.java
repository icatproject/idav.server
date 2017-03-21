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

import java.lang.reflect.Constructor;
import javax.servlet.ServletException;
import org.icatproject.idav.manager.PropertyManager;

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
    private PropertyManager properties;

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(WebdavServlet.class);

    // KP - This method has been modified to read the values 
    // from a properties file instead of the web.xml file.
    @Override
    public void init() throws ServletException {
        properties = new PropertyManager(Utils.PROPERTIES_FILENAME, Utils.HIERARCHY_FILENAME);
        String clazzName = properties.getWebdavImplementationClassName();
        String defaultIndexFile = properties.getDefaultIndexFile();
        String insteadOf404 = properties.getInsteadOf404();
        Boolean setContentLengthHeaders = properties.getSetContentLengthHeaders();
        Boolean lazyFolderCreationOnPut = properties.getLazyFolderCreationOnPut();
        Boolean readOnly = properties.getReadOnly();
        
        IWebdavStore webdavStore = constructStore(clazzName);
        super.init(webdavStore, defaultIndexFile, insteadOf404,
        		setContentLengthHeaders, lazyFolderCreationOnPut, readOnly);
        
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
}

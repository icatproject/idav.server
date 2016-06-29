/*
 * $Header: /Users/ak/temp/cvs2svn/webdav-servlet/src/main/java/net/sf/webdav/IWebdavStore.java,v 1.1 2008-08-05 07:38:42 bauhardt Exp $
 * $Revision: 1.1 $
 * $Date: 2008-08-05 07:38:42 $
 *
 * ====================================================================
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.icatproject.iDav;

import java.io.InputStream;
//import java.security.Principal;

import java.util.List;

import org.icatproject.iDav.exceptions.WebdavException;

/**
 * Interface for simple implementation of any store for the WebdavServlet
 * <p>
 * based on the BasicWebdavStore from Oliver Zeigermann, that was part of the
 * Webdav Construcktion Kit from slide
 * 
 */
public interface IWebdavStore {

    /**
     * Indicates that a new request or transaction with this store involved has
     * been started. The request will be terminated by either 
     * {@link #commit(String authString)} or
     * {@link #rollback(String authString)}. If only non-read methods
     * have been called, the request will be terminated by a
     * {@link #commit(String authString)}. This method will be
     * called by (@link WebdavStoreAdapter} at the beginning of each request.
     * 
     * 
     * @param principal
     *      the principal that started this request or <code>null</code> if
     *      there is non available
     * 
     * @throws WebdavException
     */
    void begin();

    /**
     * Checks if authentication information passed in is valid. If not throws an
     * exception.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     */
    void checkAuthentication(String authString);

    /**
     * Indicates that all changes done inside this request shall be made
     * permanent and any transactions, connections and other temporary resources
     * shall be terminated.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * 
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void commit(String authString);

    /**
     * Indicates that all changes done inside this request shall be undone and
     * any transactions, connections and other temporary resources shall be
     * terminated.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * 
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void rollback(String authString);

    /**
     * Creates a folder at the position specified by <code>folderUri</code>.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param folderUri
     *      URI of the folder
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void createFolder(String authString, String folderUri);

    /**
     * Creates a content resource at the position specified by
     * <code>resourceUri</code>.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param resourceUri
     *      URI of the content resource
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void createResource(String authString, String resourceUri);

    /**
     * Gets the content of the resource specified by <code>resourceUri</code>.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param resourceUri
     *      URI of the content resource
     * @return input stream you can read the content of the resource from
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    InputStream getResourceContent(String authString, String resourceUri);

    /**
     * Sets / stores the content of the resource specified by
     * <code>resourceUri</code>.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param resourceUri
     *      URI of the resource where the content will be stored
     * @param content
     *      input stream from which the content will be read from
     * @param contentType
     *      content type of the resource or <code>null</code> if unknown
     * @param characterEncoding
     *      character encoding of the resource or <code>null</code> if unknown
     *      or not applicable
     * @return lenght of resource
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    long setResourceContent(String authString, String resourceUri,
            InputStream content, String contentType, String characterEncoding);

    /**
     * Gets the names of the children of the folder specified by
     * <code>folderUri</code>.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param folderUri
     *      URI of the folder
     * @return a (possibly empty) list of children, or <code>null</code> if the
     *  uri points to a file
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    String[] getChildrenNames(String authString, String folderUri);

    /**
     * Gets the length of the content resource specified by
     * <code>resourceUri</code>.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param resourceUri
     *      URI of the content resource
     * @return length of the resource in bytes, <code>-1</code> declares this
     *  value as invalid and asks the adapter to try to set it from the
     *  properties if possible
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    long getResourceLength(String authString, String resourceUri);

    /**
     * Removes the object specified by <code>uri</code>.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param uri
     *      URI of the object, i.e. content resource or folder
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void removeObject(String authString, String uri);

    /**
     * Gets the storedObject specified by <code>uri</code>
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param uri
     *      URI
     * @return StoredObject
     */
    StoredObject getStoredObject(String authString, String uri);
    
    
    /**
     * Determines whether the store supports a "direct move". 
     * This could be a simple rename of a directory or folder or a move
     * of a file or folder to another location within the directory structure.
     * The original implemementation of the LocalFileSystemStore does not 
     * support this and does a potentially expensive "copy and delete".
     * The IcatStore aims to improve on this and replace this functionality
     * with a move that is much more efficient.
     * 
     * @return whether the storage implementation supports "direct move"
     * 		or just the default "copy and delete" alternative
     */
    boolean supportsDirectMove();
    
    /**
     * Method to be implemented if the supportsDirectMove method returns true.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param sourceUri
     * 		The current URI of the object to be moved 
     * @param destinationUri
     * 		The URI to move it to
     */
    void doDirectMove(String authString, String sourceUri, String destinationUri);

    /**
     * Return any session information that is being held by the Store implementation.
     * The intention is that this is used as a debugging tool and output to the log file
     * or via a non-guessable URL.
     * 
     * Care should be taken not to turn this into a security hole so things like
     * session IDs should be truncated or similar.
     * 
     * @return the information as a list of Strings so that they can be output
     * 		using a variety of methods and with whatever separators the caller wants
     */
    List<String> getSessionInfo();
    
}

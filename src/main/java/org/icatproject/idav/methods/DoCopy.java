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
package org.icatproject.iDav.methods;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icatproject.iDav.IWebdavStore;
import org.icatproject.iDav.StoredObject;
import org.icatproject.iDav.WebdavStatus;
import org.icatproject.iDav.exceptions.AccessDeniedException;
import org.icatproject.iDav.exceptions.LockFailedException;
import org.icatproject.iDav.exceptions.ObjectAlreadyExistsException;
import org.icatproject.iDav.exceptions.ObjectNotFoundException;
import org.icatproject.iDav.exceptions.WebdavException;
//import net.sf.webdav.fromcatalina.RequestUtil;
import org.icatproject.iDav.locking.ResourceLocks;

public class DoCopy extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoCopy.class);

    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;
    private DoDelete _doDelete;
    private boolean _readOnly;

    public DoCopy(IWebdavStore store, ResourceLocks resourceLocks,
            DoDelete doDelete, boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _readOnly = readOnly;
    }

    public void execute(String authString, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        String path = getRelativePath(req);
        if (!_readOnly) {

            String tempLockOwner = "doCopy" + System.currentTimeMillis()
                    + req.toString();
            if (_resourceLocks.lock(authString, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    if (!copyResource(authString, req, resp))
                    	// TODO - KP 13/07/15 - why does this just return 
                    	// if the copy was NOT successful ??? 
                        return;
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_CONFLICT, req
                            .getRequestURI());
                } catch (ObjectNotFoundException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req
                            .getRequestURI());
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    _resourceLocks.unlockTemporaryLockedObjects(authString,
                            path, tempLockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }

        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }

    }

    /**
     * Copy a resource.
     * 
     * @param authString
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param req
     *      Servlet request
     * @param resp
     *      Servlet response
     * @return true if the copy is successful
     * @throws WebdavException
     *      if an error in the underlying store occurs
     * @throws IOException
     *      when an error occurs while sending the response
     * @throws LockFailedException
     */
    public boolean copyResource(String authString,
            HttpServletRequest req, HttpServletResponse resp)
            throws WebdavException, IOException, LockFailedException {

        // Parsing destination header
        String destinationPath = parseDestinationHeader(req, resp);

        if (destinationPath == null)
        	// TODO - KP 14/07/15 - do we need to return an error here?
            return false;

        String path = getRelativePath(req);

        if (path.equals(destinationPath)) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return false;
        }

        Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();
        String parentDestinationPath = getParentPath(getCleanPath(destinationPath));

        if (!checkLocks(authString, req, resp, _resourceLocks,
                parentDestinationPath)) {
            errorList.put(parentDestinationPath, WebdavStatus.SC_LOCKED);
            sendReport(req, resp, errorList);
            return false; // parentDestination is locked
        }

        if (!checkLocks(authString, req, resp, _resourceLocks, destinationPath)) {
            errorList.put(destinationPath, WebdavStatus.SC_LOCKED);
            sendReport(req, resp, errorList);
            return false; // destination is locked
        }

        // Parsing overwrite header

        boolean overwrite = true;
        String overwriteHeader = req.getHeader("Overwrite");

        if (overwriteHeader != null) {
            overwrite = overwriteHeader.equalsIgnoreCase("T");
        }

        // Overwriting the destination
        String lockOwner = "copyResource" + System.currentTimeMillis()
                + req.toString();

        if (_resourceLocks.lock(authString, destinationPath, lockOwner, false,
                0, TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject copySo, destinationSo = null;
            try {
                copySo = _store.getStoredObject(authString, path);
                // Retrieve the resources
                if (copySo == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return false;
                }

                if (copySo.isNullResource()) {
                    String methodsAllowed = DeterminableMethod
                            .determineMethodsAllowed(copySo);
                    resp.addHeader("Allow", methodsAllowed);
                    resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                    return false;
                }

                errorList = new Hashtable<String, Integer>();

                destinationSo = _store.getStoredObject(authString,
                        destinationPath);

                if (overwrite) {

                    // Delete destination resource, if it exists
                    if (destinationSo != null) {
                        _doDelete.deleteResource(authString, destinationPath,
                                errorList, req, resp);

                    } else {
                        resp.setStatus(WebdavStatus.SC_CREATED);
                    }
                } else {

                    // If the destination exists, then it's a conflict
                    if (destinationSo != null) {
                        resp.sendError(WebdavStatus.SC_PRECONDITION_FAILED);
                        return false;
                    } else {
                        resp.setStatus(WebdavStatus.SC_CREATED);
                    }

                }
                copy(authString, path, destinationPath, errorList, req, resp);

                if (!errorList.isEmpty()) {
                    sendReport(req, resp, errorList);
                    // TODO - KP 13/07/15 - why is there no "return false" here?
                    // (there is now but only because I put it there!)
                    // I am almost certain this is wrong particularly as DoMove
                    // does a copy and delete, and if the copy fails the source
                    // folder gets deleted because this method returns true
                    // meaning that data is lost !!!
                    LOG.debug("Failure in copyResource - sending errorList: " + errorList.toString());
                    return false;
                }

            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(authString,
                        destinationPath, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
        return true;

    }

    /**
     * copies the specified resource(s) to the specified destination.
     * preconditions must be handled by the caller. Standard status codes must
     * be handled by the caller. a multi status report in case of errors is
     * created here.
     * 
     * @param authString
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param sourcePath
     *      path from where to read
     * @param destinationPath
     *      path where to write
     * @param req
     *      HttpServletRequest
     * @param resp
     *      HttpServletResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     * @throws IOException
     */
    private void copy(String authString, String sourcePath,
            String destinationPath, Hashtable<String, Integer> errorList,
            HttpServletRequest req, HttpServletResponse resp)
            throws WebdavException, IOException {

        StoredObject sourceSo = _store.getStoredObject(authString, sourcePath);
        if (sourceSo.isResource()) {
        	// TODO - KP 14/07/15 - I think the createResource line below needs to be  
        	// commented out for the IcatStore implementation to work, otherwise it causes 
        	// a zero length file to be created which then needs deleting when the source 
        	// file is streamed in the following line. Currently this causes an error in
        	// the IDS as the dataset is locked whilst the source file is being read.
            //_store.createResource(transaction, destinationPath);
            long resourceLength = _store.setResourceContent(authString,
                    destinationPath, _store.getResourceContent(authString,
                            sourcePath), null, null);

            // TODO - KP 14/07/15 - I can't see what the following few lines do
            // in both implementations (LocalFileSystemStore and IcatStore) the
            // StoredObject is created on demand (not retrieved from cache) so
            // the resource length should never need updating like this
            if (resourceLength != -1) {
                StoredObject destinationSo = _store.getStoredObject(
                        authString, destinationPath);
                destinationSo.setResourceLength(resourceLength);
            }

        } else {

            if (sourceSo.isFolder()) {
                copyFolder(authString, sourcePath, destinationPath, errorList,
                        req, resp);
            } else {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            }
        }
    }

    /**
     * helper method of copy() recursively copies the FOLDER at source path to
     * destination path
     * 
     * @param authString
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param sourcePath
     *      where to read
     * @param destinationPath
     *      where to write
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      HttpServletRequest
     * @param resp
     *      HttpServletResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     */
    private void copyFolder(String authString, String sourcePath,
            String destinationPath, Hashtable<String, Integer> errorList,
            HttpServletRequest req, HttpServletResponse resp)
            throws WebdavException {

        _store.createFolder(authString, destinationPath);
        boolean infiniteDepth = true;
        String depth = req.getHeader("Depth");
        if (depth != null) {
            if (depth.equals("0")) {
                infiniteDepth = false;
            }
        }
        if (infiniteDepth) {
            String[] children = _store
                    .getChildrenNames(authString, sourcePath);
            children = children == null ? new String[] {} : children;

            StoredObject childSo;
            for (int i = children.length - 1; i >= 0; i--) {
                children[i] = "/" + children[i];
                try {
                    childSo = _store.getStoredObject(authString,
                            (sourcePath + children[i]));
                    if (childSo.isResource()) {
                    	// TODO - KP 14/07/15 - I think the createResource line below needs to be  
                    	// commented out for the IcatStore implementation to work, otherwise it causes 
                    	// a zero length file to be created which then needs deleting when the source 
                    	// file is streamed in the following line. Currently this causes an error in
                    	// the IDS as the dataset is locked whilst the source file is being read.
                        //_store.createResource(transaction, destinationPath + children[i]);
                        long resourceLength = _store.setResourceContent(
                                authString, destinationPath + children[i],
                                _store.getResourceContent(authString,
                                        sourcePath + children[i]), null, null);

                        if (resourceLength != -1) {
                            StoredObject destinationSo = _store
                                    .getStoredObject(authString,
                                            destinationPath + children[i]);
                            destinationSo.setResourceLength(resourceLength);
                        }

                    } else {
                        copyFolder(authString, sourcePath + children[i],
                                destinationPath + children[i], errorList, req,
                                resp);
                    }
                } catch (AccessDeniedException e) {
                    errorList.put(destinationPath + children[i], new Integer(
                            WebdavStatus.SC_FORBIDDEN));
                } catch (ObjectNotFoundException e) {
                    errorList.put(destinationPath + children[i], new Integer(
                            WebdavStatus.SC_NOT_FOUND));
                } catch (ObjectAlreadyExistsException e) {
                    errorList.put(destinationPath + children[i], new Integer(
                            WebdavStatus.SC_CONFLICT));
                } catch (WebdavException e) {
                    errorList.put(destinationPath + children[i], new Integer(
                            WebdavStatus.SC_INTERNAL_SERVER_ERROR));
                }
            }
        }
    }


}

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
package org.icatproject.idav.methods;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icatproject.idav.IWebdavStore;
import org.icatproject.idav.StoredObject;
import org.icatproject.idav.WebdavStatus;
import org.icatproject.idav.exceptions.AccessDeniedException;
import org.icatproject.idav.exceptions.LockFailedException;
import org.icatproject.idav.exceptions.ObjectAlreadyExistsException;
import org.icatproject.idav.exceptions.WebdavException;
import org.icatproject.idav.locking.ResourceLocks;

public class DoMove extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoMove.class);

    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;
    private DoDelete _doDelete;
    private DoCopy _doCopy;
    private boolean _readOnly;

    public DoMove(IWebdavStore store, ResourceLocks resourceLocks, 
    		DoDelete doDelete, DoCopy doCopy, boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _doCopy = doCopy;
        _readOnly = readOnly;
    }

    public void execute(String authString, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {

        if (!_readOnly) {
            LOG.trace("-- " + this.getClass().getName());

            String sourcePath = getRelativePath(req);
            LOG.trace("sourcePath: '" + sourcePath + "'");
            Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

            if (!checkLocks(authString, req, resp, _resourceLocks, sourcePath)) {
                errorList.put(sourcePath, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
                return;
            }

            // KP - replacing a direct read of the Destination header
            // (which returns a full URL starting http...)
            // with a call to parseDestinationHeader
            // (which returns just the relative path from the end)
            // in the same way as is done in DoCopy
            // I am pretty sure it was a bug not to use this in the first place
//            String destinationPath = req.getHeader("Destination");
            String destinationPath = parseDestinationHeader(req, resp);
            LOG.trace("destinationPath: '" + destinationPath + "'");
            
            
//            LOG.trace("sourcePath: '" + sourcePath + "'");
//            LOG.trace("destinationPath: '" + destinationPath + "'");
//            LOG.trace("req.getRequestURL() : '" + req.getRequestURL() + "'");
//            LOG.trace("req.getScheme()     : '" + req.getScheme() + "'");
//            LOG.trace("req.getServerName() : '" + req.getServerName() + "'");
//            LOG.trace("req.getServerPort() : '" + req.getServerPort() + "'");
//            LOG.trace("req.getContextPath(): '" + req.getContextPath() + "'");
//            logRequestAttributes(req);

        
            if (destinationPath == null) {
                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                return;
            }

            if (!checkLocks(authString, req, resp, _resourceLocks, destinationPath)) {
                errorList.put(destinationPath, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
                return;
            }

            String tempLockOwner = "doMove" + System.currentTimeMillis() + req.toString();

            if (_resourceLocks.lock(authString, sourcePath, tempLockOwner,
                    false, 0, TEMP_TIMEOUT, TEMPORARY)) {

	                try {
	                	if (_store.supportsDirectMove() && isSimpleRename(sourcePath, destinationPath)) {
	    	            	// call new method added to do a "direct move"
	                		LOG.trace("Doing direct move");
	    	            	moveResource(authString, req, resp);
	    	            } else {
	    	            	// use the original code to do a "copy and delete"
	                		LOG.trace("Doing copy and delete");
		                    if (_doCopy.copyResource(authString, req, resp)) {
		                    	LOG.trace("Copy succeeded - deleting source resource");
		                        errorList = new Hashtable<String, Integer>();
		                        _doDelete.deleteResource(authString, sourcePath,
		                                errorList, req, resp);
		                        if (!errorList.isEmpty()) {
		                            sendReport(req, resp, errorList);
		                        }
		                    } else {
		                    	LOG.warn("Copy failed - skipping deletion of source resource");
		                    }
	    	            }
	                } catch (AccessDeniedException e) {
	                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
	                } catch (ObjectAlreadyExistsException e) {
	                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req.getRequestURI());
	                } catch (WebdavException e) {
	                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
	                } finally {
	                    _resourceLocks.unlockTemporaryLockedObjects(authString,
	                            sourcePath, tempLockOwner);
	                }
            	
            } else {
            	// KP - I am pretty sure this error should report that the source
            	// is locked and not the destination so I have changed this
//	                errorList.put(req.getHeader("Destination"), WebdavStatus.SC_LOCKED);
                errorList.put(sourcePath, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }

    }
    
    /**
     * Move a resource.
     * 
     * This is a "direct move" rather than a "copy and delete" which
     * allows for very efficient renaming of files and folders particularly
     * when there is a fairly large sub-tree below the node being renamed.
     * 
     * This code almost an exact copy of that in DoCopy copyResource().
     * 
     * @param authString
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param req
     *      Servlet request
     * @param resp
     *      Servlet response
     * @return true if the move is successful
     * @throws WebdavException
     *      if an error in the underlying store occurs
     * @throws IOException
     *      when an error occurs while sending the response
     * @throws LockFailedException
     */
    public boolean moveResource(String authString,
            HttpServletRequest req, HttpServletResponse resp)
            throws WebdavException, IOException, LockFailedException {

        // Parsing destination header
        String destinationPath = parseDestinationHeader(req, resp);

        if (destinationPath == null)
            return false;

        String sourcePath = getRelativePath(req);

        if (sourcePath.equals(destinationPath)) {
        	// TODO - KP - is this the right thing to do or should be just
        	// return OK and do nothing? what does a filesystem expect?
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
        String lockOwner = "moveResource" + System.currentTimeMillis()
                + req.toString();

        if (_resourceLocks.lock(authString, destinationPath, lockOwner, false,
                0, TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject copySo, destinationSo = null;
            try {
                copySo = _store.getStoredObject(authString, sourcePath);
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
                
                try {
                	_store.doDirectMove(authString, sourcePath, destinationPath);
                } catch (WebdavException e) {
                	// TODO - can we be more specific than this?
                	errorList.put(e.getMessage(), WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                }
                
                if (!errorList.isEmpty()) {
                    sendReport(req, resp, errorList);
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
    
    private boolean isSimpleRename(String sourcePath, String destinationPath) {
    	String sourceStem = sourcePath.substring(0, sourcePath.lastIndexOf('/'));
    	String destStem = destinationPath.substring(0, destinationPath.lastIndexOf('/'));
    	if (sourceStem.equals(destStem)) {
    		return true;
    	}
    	return false;
    }

}

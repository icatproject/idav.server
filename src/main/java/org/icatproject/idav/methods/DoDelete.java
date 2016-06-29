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
import org.icatproject.iDav.locking.ResourceLocks;

public class DoDelete extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoDelete.class);

    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;
    private boolean _readOnly;

    public DoDelete(IWebdavStore store, ResourceLocks resourceLocks,
            boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void execute(String authString, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        if (!_readOnly) {
            String path = getRelativePath(req);
            String parentPath = getParentPath(getCleanPath(path));

            Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

            if (!checkLocks(authString, req, resp, _resourceLocks, parentPath)) {
                errorList.put(parentPath, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
                return; // parent is locked
            }

            if (!checkLocks(authString, req, resp, _resourceLocks, path)) {
                errorList.put(path, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
                return; // resource is locked
            }

            String tempLockOwner = "doDelete" + System.currentTimeMillis()
                    + req.toString();
            if (_resourceLocks.lock(authString, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    errorList = new Hashtable<String, Integer>();
                    deleteResource(authString, path, errorList, req, resp);
                    if (!errorList.isEmpty()) {
                        sendReport(req, resp, errorList);
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
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
     * deletes the recources at "path"
     * 
     * @param authString
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param path
     *      the folder to be deleted
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      HttpServletRequest
     * @param resp
     *      HttpServletResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     * @throws IOException
     *      when an error occurs while sending the response
     */
    public void deleteResource(String authString, String path,
            Hashtable<String, Integer> errorList, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, WebdavException {

        resp.setStatus(WebdavStatus.SC_NO_CONTENT);

        if (!_readOnly) {

            StoredObject so = _store.getStoredObject(authString, path);
            if (so != null) {

                if (so.isResource()) {
                    _store.removeObject(authString, path);
                } else {
                    if (so.isFolder()) {
                        deleteFolder(authString, path, errorList, req, resp);
                        _store.removeObject(authString, path);
                    } else {
                        resp.sendError(WebdavStatus.SC_NOT_FOUND);
                    }
                }
            } else {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            }
            so = null;

        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
     * 
     * helper method of deleteResource() deletes the folder and all of its
     * contents
     * 
     * @param authString
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param path
     *      the folder to be deleted
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      HttpServletRequest
     * @param resp
     *      HttpServletResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     */
    private void deleteFolder(String authString, String path,
            Hashtable<String, Integer> errorList, HttpServletRequest req,
            HttpServletResponse resp) throws WebdavException {

        String[] children = _store.getChildrenNames(authString, path);
        children = children == null ? new String[] {} : children;
        StoredObject so = null;
        for (int i = children.length - 1; i >= 0; i--) {
            children[i] = "/" + children[i];
            try {
                so = _store.getStoredObject(authString, path + children[i]);
                if (so.isResource()) {
                    _store.removeObject(authString, path + children[i]);

                } else {
                    deleteFolder(authString, path + children[i], errorList,
                            req, resp);

                    _store.removeObject(authString, path + children[i]);

                }
            } catch (AccessDeniedException e) {
                errorList.put(path + children[i], new Integer(
                        WebdavStatus.SC_FORBIDDEN));
            } catch (ObjectNotFoundException e) {
                errorList.put(path + children[i], new Integer(
                        WebdavStatus.SC_NOT_FOUND));
            } catch (WebdavException e) {
                errorList.put(path + children[i], new Integer(
                        WebdavStatus.SC_INTERNAL_SERVER_ERROR));
            }
        }
        so = null;

    }

}

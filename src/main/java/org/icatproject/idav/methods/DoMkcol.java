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
import org.icatproject.iDav.exceptions.WebdavException;
import org.icatproject.iDav.locking.IResourceLocks;
import org.icatproject.iDav.locking.LockedObject;

public class DoMkcol extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoMkcol.class);

    private IWebdavStore _store;
    private IResourceLocks _resourceLocks;
    private boolean _readOnly;

    public DoMkcol(IWebdavStore store, IResourceLocks resourceLocks,
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
                // TODO remove
                LOG
                        .trace("MkCol on locked resource (parentPath) not executable!"
                                + "\n Sending SC_FORBIDDEN (403) error response!");

                resp.sendError(WebdavStatus.SC_FORBIDDEN);
                return;
            }

            String tempLockOwner = "doMkcol" + System.currentTimeMillis()
                    + req.toString();

            if (_resourceLocks.lock(authString, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                StoredObject parentSo, so = null;
                try {
                    parentSo = _store.getStoredObject(authString, parentPath);
					if (parentSo == null) {
						// parent not exists
						resp.sendError(WebdavStatus.SC_CONFLICT);
						return;
					}
					if (parentPath != null && parentSo.isFolder()) {
                        so = _store.getStoredObject(authString, path);
                        if (so == null) {
                            _store.createFolder(authString, path);
                            resp.setStatus(WebdavStatus.SC_CREATED);
                        } else {
                            // object already exists
                            if (so.isNullResource()) {

                                LockedObject nullResourceLo = _resourceLocks
                                        .getLockedObjectByPath(authString,
                                                path);
                                if (nullResourceLo == null) {
                                    resp
                                            .sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                    return;
                                }
                                String nullResourceLockToken = nullResourceLo
                                        .getID();
                                String[] lockTokens = getLockIdFromIfHeader(req);
                                String lockToken = null;
                                if (lockTokens != null)
                                    lockToken = lockTokens[0];
                                else {
                                    resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                                    return;
                                }
                                if (lockToken.equals(nullResourceLockToken)) {
                                    so.setNullResource(false);
                                    so.setFolder(true);

                                    String[] nullResourceLockOwners = nullResourceLo
                                            .getOwner();
                                    String owner = null;
                                    if (nullResourceLockOwners != null)
                                        owner = nullResourceLockOwners[0];

                                    if (_resourceLocks.unlock(authString,
                                            lockToken, owner)) {
                                        resp.setStatus(WebdavStatus.SC_CREATED);
                                    } else {
                                        resp
                                                .sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                    }

                                } else {
                                    // TODO remove
                                    LOG
                                            .trace("MkCol on lock-null-resource with wrong lock-token!"
                                                    + "\n Sending multistatus error report!");

                                    errorList.put(path, WebdavStatus.SC_LOCKED);
                                    sendReport(req, resp, errorList);
                                }

                            } else {
                                String methodsAllowed = DeterminableMethod
                                        .determineMethodsAllowed(so);
                                resp.addHeader("Allow", methodsAllowed);
                                resp
                                        .sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                            }
                        }

					} else if (parentPath != null && parentSo.isResource()) {
                        // TODO remove
                        LOG
                                .trace("MkCol on resource is not executable"
                                        + "\n Sending SC_METHOD_NOT_ALLOWED (405) error response!");

                        String methodsAllowed = DeterminableMethod
                                .determineMethodsAllowed(parentSo);
                        resp.addHeader("Allow", methodsAllowed);
                        resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);

                    } else {
                        resp.sendError(WebdavStatus.SC_FORBIDDEN);
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
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

}

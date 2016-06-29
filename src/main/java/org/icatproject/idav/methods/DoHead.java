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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.icatproject.idav.methods;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icatproject.idav.IMimeTyper;
import org.icatproject.idav.StoredObject;
import org.icatproject.idav.WebdavStatus;
import org.icatproject.idav.IWebdavStore;
import org.icatproject.idav.exceptions.AccessDeniedException;
import org.icatproject.idav.exceptions.LockFailedException;
import org.icatproject.idav.exceptions.ObjectAlreadyExistsException;
import org.icatproject.idav.exceptions.WebdavException;
import org.icatproject.idav.locking.ResourceLocks;

public class DoHead extends AbstractMethod {

    protected String _dftIndexFile;
    protected IWebdavStore _store;
    protected String _insteadOf404;
    protected ResourceLocks _resourceLocks;
    protected IMimeTyper _mimeTyper;
    protected boolean _contentLength;

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoHead.class);

    public DoHead(IWebdavStore store, String dftIndexFile, String insteadOf404,
            ResourceLocks resourceLocks, IMimeTyper mimeTyper,
            boolean contentLengthHeader) {
        _store = store;
        _dftIndexFile = dftIndexFile;
        _insteadOf404 = insteadOf404;
        _resourceLocks = resourceLocks;
        _mimeTyper = mimeTyper;
        _contentLength = contentLengthHeader;
    }

    public void execute(String authString, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {

        // determines if the uri exists.

        boolean bUriExists = false;

        String path = getRelativePath(req);
        LOG.trace("-- " + this.getClass().getName());

        if ( path.equals("/$howResourceLock$/") ) {
        	LOG.info("Showing ResourceLocks");
        	showResourceLocks(resp);
        	return;
        }
        
        if ( path.equals("/$howSessionInfo$/") ) {
        	LOG.info("Showing Session Info");
        	showSessionInfo(resp);
        	return;
        }

        StoredObject so = _store.getStoredObject(authString, path);
        if (so == null) {
            if (this._insteadOf404 != null && !_insteadOf404.trim().equals("")) {
                path = this._insteadOf404;
                so = _store.getStoredObject(authString, this._insteadOf404);
            }
        } else
            bUriExists = true;

        if (so != null) {
            if (so.isFolder()) {
                if (_dftIndexFile != null && !_dftIndexFile.trim().equals("")) {
                    resp.sendRedirect(resp.encodeRedirectURL(req
                            .getRequestURI()
                            + this._dftIndexFile));
                    return;
                }
            } else if (so.isNullResource()) {
                String methodsAllowed = DeterminableMethod
                        .determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                return;
            }

            // TODO - (KP) shouldn't the lock owner be doHead ???
            String tempLockOwner = "doGet" + System.currentTimeMillis()
                    + req.toString();

            if (_resourceLocks.lock(authString, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                try {

                    String eTagMatch = req.getHeader("If-None-Match");
                    if (eTagMatch != null) {
                        if (eTagMatch.equals(getETag(so))) {
                            resp.setStatus(WebdavStatus.SC_NOT_MODIFIED);
                            return;
                        }
                    }

                    if (so.isResource()) {
                        // path points to a file but ends with / or \
                        if (path.endsWith("/") || (path.endsWith("\\"))) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                    req.getRequestURI());
                        } else {

                            // setting headers
                            long lastModified = so.getLastModified().getTime();
                            resp.setDateHeader("last-modified", lastModified);

                            String eTag = getETag(so);
                            resp.addHeader("ETag", eTag);

                            long resourceLength = so.getResourceLength();

                            if (_contentLength) {
                                if (resourceLength > 0) {
                                    if (resourceLength <= Integer.MAX_VALUE) {
                                    	// KP - I think this is good for up to 2GB
                                        resp.setContentLength((int) resourceLength);
                                    	LOG.trace("Set content-length using setContentLength method");
                                    } else {
                                        resp.setHeader("content-length", "" + resourceLength);
                                        // is "content-length" the right header?
                                        // is long a valid format?
                                    	LOG.trace("Set content-length manually using setHeader method");
                                    }
                                }
                            }

                            String mimeType = _mimeTyper.getMimeType(path);
                            if (mimeType != null) {
                                resp.setContentType(mimeType);
                            } else {
                                int lastSlash = path.replace('\\', '/')
                                        .lastIndexOf('/');
                                int lastDot = path.indexOf(".", lastSlash);
                                if (lastDot == -1) {
                                    resp.setContentType("text/html");
                                }
                            }

                            doBody(authString, resp, path);
                        }
                    } else {
                        folderBody(authString, path, resp, req);
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
            folderBody(authString, path, resp, req);
        }

        if (!bUriExists)
            resp.setStatus(WebdavStatus.SC_NOT_FOUND);

    }

    private void showResourceLocks(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        OutputStream out = resp.getOutputStream();
        out.write("<html><body><pre>".getBytes());
//        out.write("ResourceLocks contents will go here".getBytes());
        out.write(_resourceLocks.toString().getBytes());       
        out.write("</pre></body></html>".getBytes());
	}

    private void showSessionInfo(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        OutputStream out = resp.getOutputStream();
        out.write("<html><body><pre>".getBytes());
        for (String outputLine : _store.getSessionInfo()) {
            out.write(outputLine.getBytes());
            out.write("\n".getBytes());       
        }
        out.write("</pre></body></html>".getBytes());
	}
    
	protected void folderBody(String authString, String path,
            HttpServletResponse resp, HttpServletRequest req)
            throws IOException {
        // no body for HEAD
    }

    protected void doBody(String authString, HttpServletResponse resp,
            String path) throws IOException {
        // no body for HEAD
    }
}

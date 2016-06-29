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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icatproject.iDav.StoredObject;
import org.icatproject.iDav.WebdavStatus;
import org.icatproject.iDav.IWebdavStore;
import org.icatproject.iDav.exceptions.AccessDeniedException;
import org.icatproject.iDav.exceptions.LockFailedException;
import org.icatproject.iDav.exceptions.WebdavException;
import org.icatproject.iDav.locking.ResourceLocks;

public class DoOptions extends DeterminableMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoOptions.class);

    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;

    public DoOptions(IWebdavStore store, ResourceLocks resLocks) {
        _store = store;
        _resourceLocks = resLocks;
    }

    public void execute(String authString, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {

        LOG.trace("-- " + this.getClass().getName());

        String tempLockOwner = "doOptions" + System.currentTimeMillis()
                + req.toString();
        String path = getRelativePath(req);
        if (_resourceLocks.lock(authString, path, tempLockOwner, false, 0,
                TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject so = null;
            try {
                resp.addHeader("DAV", "1, 2");

                so = _store.getStoredObject(authString, path);
                String methodsAllowed = determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.addHeader("MS-Author-Via", "DAV");
            } catch (AccessDeniedException e) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            } catch (WebdavException e) {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(authString, path,
                        tempLockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

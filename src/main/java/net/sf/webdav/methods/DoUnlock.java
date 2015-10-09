package net.sf.webdav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.StoredObject;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.exceptions.LockFailedException;
import net.sf.webdav.locking.IResourceLocks;
import net.sf.webdav.locking.LockedObject;

public class DoUnlock extends DeterminableMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoUnlock.class);

    private IWebdavStore _store;
    private IResourceLocks _resourceLocks;
    private boolean _readOnly;

    public DoUnlock(IWebdavStore store, IResourceLocks resourceLocks,
            boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void execute(String authString, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        } else {

            String path = getRelativePath(req);
            String tempLockOwner = "doUnlock" + System.currentTimeMillis()
                    + req.toString();
            try {
                if (_resourceLocks.lock(authString, path, tempLockOwner,
                        false, 0, TEMP_TIMEOUT, TEMPORARY)) {

                    String lockId = getLockIdFromLockTokenHeader(req);
                    LockedObject lo;
                    if (lockId != null
                            && ((lo = _resourceLocks.getLockedObjectByID(
                                    authString, lockId)) != null)) {

                        String[] owners = lo.getOwner();
                        String owner = null;
                        if (lo.isShared()) {
                            // more than one owner is possible
                            if (owners != null) {
                                for (int i = 0; i < owners.length; i++) {
                                    // remove owner from LockedObject
                                    lo.removeLockedObjectOwner(owners[i]);
                                }
                            }
                        } else {
                            // exclusive, only one lock owner
                            if (owners != null)
                                owner = owners[0];
                            else
                                owner = null;
                        }

                        if (_resourceLocks.unlock(authString, lockId, owner)) {
                            StoredObject so = _store.getStoredObject(
                                    authString, path);
                            if (so.isNullResource()) {
                                _store.removeObject(authString, path);
                            }

                            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
                        } else {
                            LOG.trace("DoUnlock failure at " + lo.getPath());
                            resp.sendError(WebdavStatus.SC_METHOD_FAILURE);
                        }

                    } else {
                        resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                    }
                }
            } catch (LockFailedException e) {
                e.printStackTrace();
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(authString, path,
                        tempLockOwner);
            }
        }
    }

}

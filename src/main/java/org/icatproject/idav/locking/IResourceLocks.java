package org.icatproject.idav.locking;

import org.icatproject.idav.exceptions.LockFailedException;

public interface IResourceLocks {

    /**
     * Tries to lock the resource at "path".
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param path
     *      what resource to lock
     * @param owner
     *      the owner of the lock
     * @param exclusive
     *      if the lock should be exclusive (or shared)
     * @param depth
     *      depth
     * @param timeout
     *      Lock Duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException
     */
    boolean lock(String authString, String path, String owner,
            boolean exclusive, int depth, int timeout, boolean temporary)
            throws LockFailedException;

    /**
     * Unlocks all resources at "path" (and all subfolders if existing)<p/> that
     * have the same owner.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param id
     *      id to the resource to unlock
     * @param owner
     *      who wants to unlock
     */
    boolean unlock(String authString, String id, String owner);

    /**
     * Unlocks all resources at "path" (and all subfolders if existing)<p/> that
     * have the same owner.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param path
     *      what resource to unlock
     * @param owner
     *      who wants to unlock
     */
    void unlockTemporaryLockedObjects(String authString, String path,
            String owner);

    /**
     * Deletes LockedObjects, where timeout has reached.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param temporary
     *      Check timeout on temporary or real locks
     */
    void checkTimeouts(String authString, boolean temporary);

    /**
     * Tries to lock the resource at "path" exclusively.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param path
     *      what resource to lock
     * @param owner
     *      the owner of the lock
     * @param depth
     *      depth
     * @param timeout
     *      Lock Duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException
     */
    boolean exclusiveLock(String authString, String path, String owner,
            int depth, int timeout) throws LockFailedException;

    /**
     * Tries to lock the resource at "path" shared.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param path
     *      what resource to lock
     * @param owner
     *      the owner of the lock
     * @param depth
     *      depth
     * @param timeout
     *      Lock Duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException
     */
    boolean sharedLock(String authString, String path, String owner,
            int depth, int timeout) throws LockFailedException;

    /**
     * Gets the LockedObject corresponding to specified id.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param id
     *      LockToken to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getLockedObjectByID(String authString, String id);

    /**
     * Gets the LockedObject on specified path.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param path
     *      Path to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getLockedObjectByPath(String authString, String path);

    /**
     * Gets the LockedObject corresponding to specified id (locktoken).
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param id
     *      LockToken to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getTempLockedObjectByID(String authString, String id);

    /**
     * Gets the LockedObject on specified path.
     * 
     * @param authString
     *      the base64 encoded Authorization string exactly as sent
     *      in the HTTP header from the client
     * @param path
     *      Path to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getTempLockedObjectByPath(String authString, String path);

}

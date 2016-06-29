/*
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
 *
 */
package org.icatproject.iDav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.icatproject.iDav.exceptions.UnauthenticatedException;
import org.icatproject.iDav.exceptions.WebdavException;

/**
 * Reference Implementation of WebdavStore
 * 
 * @author joa
 * @author re
 */
public class LocalFileSystemStore implements IWebdavStore {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(LocalFileSystemStore.class);

    private static int BUF_SIZE = 65536;

    private File _root = null;

    public LocalFileSystemStore(File root) {
        _root = root;
    }

    public void begin() throws WebdavException {
        LOG.trace("LocalFileSystemStore.begin()");
        if (!_root.exists()) {
            if (!_root.mkdirs()) {
                throw new WebdavException("root path: "
                        + _root.getAbsolutePath()
                        + " does not exist and could not be created");
            }
        }
    }

    public void checkAuthentication(String authString)
            throws UnauthenticatedException {
        // do nothing
        LOG.trace("LocalFileSystemStore.checkAuthentication()");
    }

    public void commit(String authString) throws WebdavException {
        // do nothing
        LOG.trace("LocalFileSystemStore.commit()");
    }

    public void rollback(String authString) throws WebdavException {
        // do nothing
        LOG.trace("LocalFileSystemStore.rollback()");

    }

    public void createFolder(String authString, String uri)
            throws WebdavException {
        LOG.trace("LocalFileSystemStore.createFolder(" + uri + ")");
        File file = new File(_root, uri);
        if (!file.mkdir())
            throw new WebdavException("cannot create folder: " + uri);
    }

    public void createResource(String authString, String uri)
            throws WebdavException {
        LOG.trace("LocalFileSystemStore.createResource(" + uri + ")");
        File file = new File(_root, uri);
        try {
            if (!file.createNewFile())
                throw new WebdavException("cannot create file: " + uri);
        } catch (IOException e) {
            LOG
                    .error("LocalFileSystemStore.createResource(" + uri
                            + ") failed");
            throw new WebdavException(e);
        }
    }

    public long setResourceContent(String authString, String uri,
            InputStream is, String contentType, String characterEncoding)
            throws WebdavException {

        LOG.trace("LocalFileSystemStore.setResourceContent(" + uri + ")");
        File file = new File(_root, uri);
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(
                    file), BUF_SIZE);
            try {
                int read;
                byte[] copyBuffer = new byte[BUF_SIZE];

                while ((read = is.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    os.write(copyBuffer, 0, read);
                }
            } finally {
                try {
                    is.close();
                } finally {
                    os.close();
                }
            }
        } catch (IOException e) {
            LOG.error("LocalFileSystemStore.setResourceContent(" + uri
                    + ") failed");
            throw new WebdavException(e);
        }
        long length = -1;

        try {
            length = file.length();
        } catch (SecurityException e) {
            LOG.error("LocalFileSystemStore.setResourceContent(" + uri
                    + ") failed" + "\nCan't get file.length");
        }

        return length;
    }

    public String[] getChildrenNames(String authString, String uri)
            throws WebdavException {
        LOG.trace("LocalFileSystemStore.getChildrenNames(" + uri + ")");
        File file = new File(_root, uri);
        String[] childrenNames = null;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            List<String> childList = new ArrayList<String>();
            String name = null;
            for (int i = 0; i < children.length; i++) {
                name = children[i].getName();
                childList.add(name);
                LOG.trace("Child " + i + ": " + name);
            }
            childrenNames = new String[childList.size()];
            childrenNames = (String[]) childList.toArray(childrenNames);
        }
        return childrenNames;
    }

    public void removeObject(String authString, String uri)
            throws WebdavException {
        File file = new File(_root, uri);
        boolean success = file.delete();
        LOG.trace("LocalFileSystemStore.removeObject(" + uri + ")=" + success);
        if (!success) {
            throw new WebdavException("cannot delete object: " + uri);
        }

    }

    public InputStream getResourceContent(String authString, String uri)
            throws WebdavException {
        LOG.trace("LocalFileSystemStore.getResourceContent(" + uri + ")");
        File file = new File(_root, uri);

        InputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
        } catch (IOException e) {
            LOG.error("LocalFileSystemStore.getResourceContent(" + uri
                    + ") failed");
            throw new WebdavException(e);
        }
        return in;
    }

    public long getResourceLength(String authString, String uri)
            throws WebdavException {
        LOG.trace("LocalFileSystemStore.getResourceLength(" + uri + ")");
        File file = new File(_root, uri);
        return file.length();
    }

    public StoredObject getStoredObject(String authString, String uri) {

        LOG.trace("LocalFileSystemStore.getStoredObject(" + uri + ")");
        StoredObject so = null;

        File file = new File(_root, uri);
        if (file.exists()) {
            so = new StoredObject();
            so.setFolder(file.isDirectory());
            so.setLastModified(new Date(file.lastModified()));
            so.setCreationDate(new Date(file.lastModified()));
            so.setResourceLength(getResourceLength(authString, uri));
        }

        return so;
    }

	@Override
	public boolean supportsDirectMove() {
		// this original implementation only supports "copy and delete"
		return false;
	}

	@Override
	public void doDirectMove(String authString, String sourceUri, String destinationUri)
            throws WebdavException {
		String message = "LocalFileSystemStore does not support direct moves";
		LOG.error(message);
		throw new WebdavException(message);
	}

	@Override
    public List<String> getSessionInfo() {
		String message = "Not implemented for " + this.getClass().getSimpleName();
		List<String> sessionInfo = new ArrayList<String>();
		sessionInfo.add(message);
		return sessionInfo;
	}

}

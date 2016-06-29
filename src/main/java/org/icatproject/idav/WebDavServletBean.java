package org.icatproject.iDav;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icatproject.iDav.exceptions.AccessDeniedException;
import org.icatproject.iDav.exceptions.UnauthenticatedException;
import org.icatproject.iDav.exceptions.WebdavException;
import org.icatproject.iDav.fromcatalina.MD5Encoder;
import org.icatproject.iDav.locking.ResourceLocks;
import org.icatproject.iDav.methods.DoCopy;
import org.icatproject.iDav.methods.DoDelete;
import org.icatproject.iDav.methods.DoGet;
import org.icatproject.iDav.methods.DoHead;
import org.icatproject.iDav.methods.DoLock;
import org.icatproject.iDav.methods.DoMkcol;
import org.icatproject.iDav.methods.DoMove;
import org.icatproject.iDav.methods.DoNotImplemented;
import org.icatproject.iDav.methods.DoOptions;
import org.icatproject.iDav.methods.DoPropfind;
import org.icatproject.iDav.methods.DoProppatch;
import org.icatproject.iDav.methods.DoPut;
import org.icatproject.iDav.methods.DoUnlock;

@SuppressWarnings("serial")
public class WebDavServletBean extends HttpServlet {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(WebDavServletBean.class);

    /**
     * MD5 message digest provider.
     */
    protected static MessageDigest MD5_HELPER;

    /**
     * The MD5 helper object for this class.
     */
    protected static final MD5Encoder MD5_ENCODER = new MD5Encoder();

    private static final boolean READ_ONLY = false;
    private ResourceLocks _resLocks;
    private IWebdavStore _store;
    private String _dftIndexFile;
    private String _insteadOf404;
    private boolean _setContentLengthHeaders;
    private boolean _lazyFolderCreationOnPut;
    
    private IMimeTyper _mimeTyper;
    
//    private HashMap<String, IMethodExecutor> _methodMap = new HashMap<String, IMethodExecutor>();

    public WebDavServletBean() {
        _resLocks = new ResourceLocks();

        try {
            MD5_HELPER = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }
    }

    public void init(IWebdavStore store, String dftIndexFile,
            String insteadOf404, boolean setContentLengthHeaders,
            boolean lazyFolderCreationOnPut) throws ServletException {

        _store = store;
        _dftIndexFile = dftIndexFile;
        _insteadOf404 = insteadOf404;
        _setContentLengthHeaders = setContentLengthHeaders;
        _lazyFolderCreationOnPut = lazyFolderCreationOnPut;

        _mimeTyper = new IMimeTyper() {
            public String getMimeType(String path) {
                return getServletContext().getMimeType(path);
            }
        };

        // KP 28/09/15 - replaced this section with the method getMethodExecutor
        // because I found that there are instance variables in DoLock, DoPropfind
        // and DoPut that are not thread safe due to the fact that there is only
        // one instance of each method executor class.
        // The _depth variable in DoProppatch showed this up by causing folders to
        // appear empty because of simultaneous calls to DoPropfind overwriting
        // the value of the _depth variable.
//        register("GET", new DoGet(store, dftIndexFile, insteadOf404, _resLocks,
//                mimeTyper, setContentLengthHeaders));
//        register("HEAD", new DoHead(store, dftIndexFile, insteadOf404,
//                _resLocks, mimeTyper, setContentLengthHeaders));
//        DoDelete doDelete = (DoDelete) register("DELETE", new DoDelete(store,
//                _resLocks, READ_ONLY));
//        DoCopy doCopy = (DoCopy) register("COPY", new DoCopy(store, _resLocks,
//                doDelete, READ_ONLY));
//        register("LOCK", new DoLock(store, _resLocks, READ_ONLY));
//        register("UNLOCK", new DoUnlock(store, _resLocks, READ_ONLY));
//        register("MOVE", new DoMove(store, _resLocks, doDelete, doCopy, READ_ONLY));
//        register("MKCOL", new DoMkcol(store, _resLocks, READ_ONLY));
//        register("OPTIONS", new DoOptions(store, _resLocks));
//        register("PUT", new DoPut(store, _resLocks, READ_ONLY,
//                lazyFolderCreationOnPut));
//        register("PROPFIND", new DoPropfind(store, _resLocks, mimeTyper));
//        register("PROPPATCH", new DoProppatch(store, _resLocks, READ_ONLY));
//        register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
        
        LOG.info("WebDavServletBean init complete");
    }

//    private IMethodExecutor register(String methodName, IMethodExecutor method) {
//        _methodMap.put(methodName, method);
//        return method;
//    }

    // create a new instance of the IMethodExecutor classes each time they are
    // required to avoid threading problems with instance variables that
    // existed in DoLock, DoPropfind and DoPut when a single instance of
    // each class was being shared by all threads
    private IMethodExecutor getMethodExecutor(String methodName) {
		switch (methodName) {
			case "GET":
				return new DoGet(_store, _dftIndexFile, _insteadOf404, _resLocks,
						_mimeTyper, _setContentLengthHeaders);
			case "HEAD": 
				return new DoHead(_store, _dftIndexFile, _insteadOf404,
		                _resLocks, _mimeTyper, _setContentLengthHeaders);
			case "DELETE":
				return new DoDelete(_store, _resLocks, READ_ONLY);
			case "COPY":
				DoDelete doDelete1 = (DoDelete)getMethodExecutor("DELETE");
				return new DoCopy(_store, _resLocks, doDelete1, READ_ONLY);
			case "LOCK":
				return new DoLock(_store, _resLocks, READ_ONLY);
			case "UNLOCK":
				return new DoUnlock(_store, _resLocks, READ_ONLY);
			case "MOVE":
				DoDelete doDelete2 = (DoDelete)getMethodExecutor("DELETE");
				DoCopy doCopy = (DoCopy)getMethodExecutor("COPY");
				return new DoMove(_store, _resLocks, doDelete2, doCopy, READ_ONLY);
			case "MKCOL":
				return new DoMkcol(_store, _resLocks, READ_ONLY);
			case "OPTIONS":
				return new DoOptions(_store, _resLocks);
			case "PUT":
				return new DoPut(_store, _resLocks, READ_ONLY, _lazyFolderCreationOnPut);
			case "PROPFIND":
				return new DoPropfind(_store, _resLocks, _mimeTyper);
			case "PROPPATCH":
				return new DoProppatch(_store, _resLocks, READ_ONLY);
			default :
				return new DoNotImplemented(READ_ONLY);
		}
    }
    
    
    /**
     * Handles the special WebDAV methods.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String methodName = req.getMethod();
        boolean needRollback = false;
    	String authString = null;

        if (LOG.isTraceEnabled())
            debugRequest(methodName, req);

        try {
        	
        	// request login details if they have not been sent in the Authorization header
        	String authHeaderString = req.getHeader("Authorization");
        	if (authHeaderString == null) {
        		throw new UnauthenticatedException("Login required - no Authorization header present");
        	} else {
        		// header should be something like "Basic YWRtaW46YWRtaW4="
        		// where the second string is a base64 encoded username:password
        		// first split the header on whitespace
        		String[] authHeaderParts = authHeaderString.split("\\s+");
        		if (authHeaderParts.length != 2) {
        			throw new UnauthenticatedException("Unexpected format of Authorization header: " + authHeaderString);
        		}
        		authString = authHeaderParts[1];
        	}

        	_store.begin();
            needRollback = true;
            
            _store.checkAuthentication(authString);
            
            resp.setStatus(WebdavStatus.SC_OK);

            try {
            	// KP 28/09/15 - updated how this is done (see comments above)
//                IMethodExecutor methodExecutor = (IMethodExecutor) _methodMap
//                        .get(methodName);
//                if (methodExecutor == null) {
//                    methodExecutor = (IMethodExecutor) _methodMap
//                            .get("*NO*IMPL*");
//                }
            	IMethodExecutor methodExecutor = (IMethodExecutor)getMethodExecutor(methodName);
                methodExecutor.execute(authString, req, resp);

                _store.commit(authString);
                needRollback = false;
            } catch (IOException e) {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                LOG.error("IOException: " + sw.toString());
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                _store.rollback(authString);
                throw new ServletException(e);
            }

        } catch (UnauthenticatedException e) {
        	LOG.debug("Caught UnauthenticatedException: " + e.getMessage() + " - sending WWW-Authenticate header");
        	resp.setHeader("WWW-Authenticate", "Basic realm=\"ICAT Webdav Server\"");
            resp.sendError(WebdavStatus.SC_UNAUTHORIZED);
        } catch (AccessDeniedException e) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } catch (WebdavException e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.error("WebdavException: " + sw.toString());
            throw new ServletException(e);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.error("Exception: " + sw.toString());
        } finally {
            if (needRollback)
                _store.rollback(authString);
        }
        
        LOG.trace("WebdavServlet finished request: methodName = " + methodName + ", path: " + req.getRequestURI());

    }

    private void debugRequest(String methodName, HttpServletRequest req) {
        LOG.trace("-----------");
        LOG.trace("WebdavServlet request: methodName = " + methodName + ", path: " + req.getRequestURI());
        LOG.trace("-----------");
//        Enumeration<?> e = req.getHeaderNames();
//        while (e.hasMoreElements()) {
//            String s = (String) e.nextElement();
//            LOG.trace("header: " + s + " " + req.getHeader(s));
//        }
//        e = req.getAttributeNames();
//        while (e.hasMoreElements()) {
//            String s = (String) e.nextElement();
//            LOG.trace("attribute: " + s + " " + req.getAttribute(s));
//        }
//        e = req.getParameterNames();
//        while (e.hasMoreElements()) {
//            String s = (String) e.nextElement();
//            LOG.trace("parameter: " + s + " " + req.getParameter(s));
//        }
//        LOG.trace("-----------");
    }

}

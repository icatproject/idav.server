package org.icatproject.iDav.methods;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;

import org.icatproject.iDav.IWebdavStore;
import org.icatproject.iDav.StoredObject;
import org.icatproject.iDav.WebdavStatus;
import org.icatproject.iDav.exceptions.LockFailedException;
import org.icatproject.iDav.fromcatalina.XMLHelper;
import org.icatproject.iDav.fromcatalina.XMLWriter;
import org.icatproject.iDav.locking.ResourceLocks;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class DoProppatch extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoProppatch.class);

//    private boolean _readOnly;
    private IWebdavStore _store;
//    private ResourceLocks _resourceLocks;

    public DoProppatch(IWebdavStore store, ResourceLocks resLocks,
            boolean readOnly) {
//        _readOnly = readOnly;
        _store = store;
//        _resourceLocks = resLocks;
    }

    public void execute(String authString, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        // KP 24/07/15 - I am having problems with this method when copying a file
        // into a folder path containing special characters (mostly £ and &)
        // it is returning a Multi-status 207 containing a 423 Locked status for 
        // the destination file. The file is locked because Windows Explorer locks
        // it a couple of operations previously and with "normal" file paths (no
        // special chars) Windows appears to ignore this and carry on but with
        // special chars the copy is prevented and a random error 
        // "Error 0x80070057: The parameter is incorrect" shown to the user
        // As the todo comment slightly further down this method indicates that
        // this method is not actually doing anything anyway then for now
        // I am going to comment out the lock checking and let the method
        // return a multi-status 207 saying that everything is OK
        
        // just returning here doesn't work so a 200 OK with content length of 0 
        // is obviously not what Windows Explorer wants to receive!
//        return;


//        if (_readOnly) {
//            resp.sendError(WebdavStatus.SC_FORBIDDEN);
//            return;
//        }
//
        String path = getRelativePath(req);
//        String parentPath = getParentPath(getCleanPath(path));
//
//        Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();
//
//        if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
//            errorList.put(parentPath, WebdavStatus.SC_LOCKED);
//            sendReport(req, resp, errorList);
//            return; // parent is locked
//        }
//
//        if (!checkLocks(transaction, req, resp, _resourceLocks, path)) {
//            errorList.put(path, WebdavStatus.SC_LOCKED);
//            sendReport(req, resp, errorList);
//            return; // resource is locked
//        }
//
//        // TODO for now, PROPPATCH just sends a valid response, stating that
//        // everything is fine, but doesn't do anything.
//
//        // Retrieve the resources
//        String tempLockOwner = "doProppatch" + System.currentTimeMillis()
//                + req.toString();
//
//        if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
//                TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject so = null;
//            LockedObject lo = null;
            try {
                so = _store.getStoredObject(authString, path);
//                lo = _resourceLocks.getLockedObjectByPath(transaction,
//                        getCleanPath(path));
//
//                if (so == null) {
//                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
//                    return;
//                    // we do not to continue since there is no root
//                    // resource
//                }
//
//                if (so.isNullResource()) {
//                    String methodsAllowed = DeterminableMethod
//                            .determineMethodsAllowed(so);
//                    resp.addHeader("Allow", methodsAllowed);
//                    resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
//                    return;
//                }
//
//                if (lo != null && lo.isExclusive()) {
//                    // Object on specified path is LOCKED
//                    errorList = new Hashtable<String, Integer>();
//                    errorList.put(path, new Integer(WebdavStatus.SC_LOCKED));
//                    sendReport(req, resp, errorList);
//                    return;
//                }

                List<String> toset = null;
                List<String> toremove = null;
                List<String> tochange = new Vector<String>();
                // contains all properties from
                // toset and toremove

                path = getCleanPath(getRelativePath(req));

                Node tosetNode = null;
                Node toremoveNode = null;

                if (req.getContentLength() != 0) {
                    DocumentBuilder documentBuilder = getDocumentBuilder();
                    try {
                        Document document = documentBuilder
                                .parse(new InputSource(req.getInputStream()));
                        // Get the root element of the document
                        Element rootElement = document.getDocumentElement();

                        tosetNode = XMLHelper.findSubElement(XMLHelper
                                .findSubElement(rootElement, "set"), "prop");
                        toremoveNode = XMLHelper.findSubElement(XMLHelper
                                .findSubElement(rootElement, "remove"), "prop");
                    } catch (Exception e) {
                        resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                        return;
                    }
                } else {
                    // no content: error
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                    return;
                }

                HashMap<String, String> namespaces = new HashMap<String, String>();
                namespaces.put("DAV:", "D");

                if (tosetNode != null) {
                    toset = XMLHelper.getPropertiesFromXML(tosetNode);
                    tochange.addAll(toset);
                }

                if (toremoveNode != null) {
                    toremove = XMLHelper.getPropertiesFromXML(toremoveNode);
                    tochange.addAll(toremove);
                }

                resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
                resp.setContentType("text/xml; charset=UTF-8");

                // Create multistatus object
                XMLWriter generatedXML = new XMLWriter(resp.getWriter(),
                        namespaces);
                generatedXML.writeXMLHeader();
                generatedXML
                        .writeElement("DAV::multistatus", XMLWriter.OPENING);

                generatedXML.writeElement("DAV::response", XMLWriter.OPENING);
                String status = new String("HTTP/1.1 " + WebdavStatus.SC_OK
                        + " " + WebdavStatus.getStatusText(WebdavStatus.SC_OK));

                // Generating href element
                generatedXML.writeElement("DAV::href", XMLWriter.OPENING);

                String href = req.getContextPath();
                if ((href.endsWith("/")) && (path.startsWith("/")))
                    href += path.substring(1);
                else
                    href += path;
                if ((so.isFolder()) && (!href.endsWith("/")))
                    href += "/";

                generatedXML.writeText(rewriteUrl(href));

                generatedXML.writeElement("DAV::href", XMLWriter.CLOSING);

                for (Iterator<String> iter = tochange.iterator(); iter
                        .hasNext();) {
                    String property = (String) iter.next();

                    generatedXML.writeElement("DAV::propstat",
                            XMLWriter.OPENING);

                    generatedXML.writeElement("DAV::prop", XMLWriter.OPENING);
                    generatedXML.writeElement(property, XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::prop", XMLWriter.CLOSING);

                    generatedXML.writeElement("DAV::status", XMLWriter.OPENING);
                    generatedXML.writeText(status);
                    generatedXML.writeElement("DAV::status", XMLWriter.CLOSING);

                    generatedXML.writeElement("DAV::propstat",
                            XMLWriter.CLOSING);
                }

                generatedXML.writeElement("DAV::response", XMLWriter.CLOSING);

                generatedXML
                        .writeElement("DAV::multistatus", XMLWriter.CLOSING);

                generatedXML.sendData();
                
                
//            } catch (AccessDeniedException e) {
//                resp.sendError(WebdavStatus.SC_FORBIDDEN);
//            } catch (WebdavException e) {
//                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            } catch (ServletException e) {
            	// KP 28/07/15 - changed the exception handling here
            	// as it was just printing the stack trace previously
            	LOG.error("Caught: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            	resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }/* finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, path,
                        tempLockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }*/
    }
}

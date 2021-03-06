package org.icatproject.idav;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.icatproject.idav.exceptions.AccessDeniedException;
import org.icatproject.idav.exceptions.UnauthenticatedException;
import org.icatproject.idav.exceptions.WebdavException;

import org.apache.commons.lang3.StringUtils;
import org.icatproject.Datafile;
import org.icatproject.DatafileFormat;
import org.icatproject.Dataset;
import org.icatproject.DatasetType;
import org.icatproject.EntityBaseBean;
import org.icatproject.Facility;
import org.icatproject.ICAT;
import org.icatproject.ICATService;
import org.icatproject.IcatExceptionType;
import org.icatproject.IcatException_Exception;
import org.icatproject.Instrument;
import org.icatproject.Investigation;
import org.icatproject.InvestigationInstrument;
import org.icatproject.InvestigationType;
import org.icatproject.Login.Credentials;
import org.icatproject.Login.Credentials.Entry;
import org.icatproject.idav.manager.PropertyManager;
import org.icatproject.ids.client.DataSelection;
import org.icatproject.ids.client.IdsClient;
import org.icatproject.ids.client.IdsClient.Flag;
import org.icatproject.ids.client.IdsException;
import org.icatproject.ids.client.InsufficientPrivilegesException;

/**
 * ICAT Implementation of IWebdavStore
 *
 * @author kphipps
 */
public class IcatStore implements IWebdavStore {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IcatStore.class);

    private ICAT icatEP = null;

    private IdsClient idsClient = null;

    // This is fixed for now assuming levels are Facility - Inv - Dataset - Datafile
    // but will need to be configurable when FacilityCycles and/or Instruments are included
    private static final int DATAFILE_LEVEL = 4;

    private static final String CURLY_BRACES = "{}";
    private static final String FORWARD_SLASH_OR_SLASHES = "/+";
    private static final String FOLDER = "FOLDER";
    private static final String TEMP_FILE_CONTENTS = "Temp file used by ICAT Webdav - please ignore";
    private static final String EMPTY_STRING = "";
    
    private String userId = null;

    // Two session maps used by all instances of IcatStore
    private static Map<String, String> authStringToIcatSessionIdMap = new HashMap<>();
    private static Map<String, Date> icatSessionIdToTimeoutDate = new HashMap<>();
    
    // Lock to be used whenever access is being made to either of the session maps
    private static Object sessionMapsLock = new Object();

    // Hierarchy specified by the user.
    private static List<IcatEntity> hierarchy;

    // ICAT Utility class for mapping entities
    private static IcatMapper icatMapper;

    // Property manager for parsing and exposing properties.
    private PropertyManager properties;

    // Cached objects (frequently used and never changed)
    private Instrument instrument;
    private InvestigationType investigationType;
    private DatasetType datasetType;
    private DatafileFormat datafileFormat;

    private enum DatafileSearchType {
        NONE, EQUALS, LIKE
    }

    // Initialisation problems here should be thrown to the servlet init method
    // thus preventing the servlet from initialising 
    public IcatStore() throws Exception {
        try {
            // Load the properties
            properties = new PropertyManager(Utils.PROPERTIES_FILENAME, Utils.HIERARCHY_FILENAME);
            hierarchy = properties.getHierarchy();
            icatMapper = new IcatMapper();

            // Currently assuming that ICAT and IDS are on the same machine
            URL icatAndIdsServerURL = new URL(properties.getIcatUrl());
            URL idsURL = new URL(properties.getIdsUrl());
            URL icatUrl = new URL(icatAndIdsServerURL, "/ICATService/ICAT?wsdl");

            ICATService icatService = new ICATService(icatUrl, new QName("http://icatproject.org", "ICATService"));
            icatEP = icatService.getICATPort();
            idsClient = new IdsClient(idsURL);
            
            LOG.info("IcatStore constructor complete");
            
        } catch (MalformedURLException e) {
            LOG.error("Problem with URL initialising IcatStore: " + e);
            throw e;
        }
    }

    @Override
    public void begin() throws WebdavException {
        LOG.trace("IcatStore.begin()");
    }

    // Every call to the webdav servlet calls this method after an 
    // Authentication header has been extracted from the request
    @Override
    public void checkAuthentication(String authString) throws UnauthenticatedException {
        LOG.trace("IcatStore.checkAuthentication()");
        String icatSessionId = getIcatSessionId(authString);
        String username = Utils.getUsernamePasswordFromAuthString(authString).getUsername();
        LOG.trace("User '" + username + "' is using ICAT session ID " + icatSessionId);
    }

    private String getIcatSessionId(String authString) throws UnauthenticatedException {
        // From testing, over 99% of the time this method completes in less than one millisecond.
        // This is because most of the time there is a valid session ID in the map and it
        // is returned very quickly.
        // The method takes longer when a new ICAT login or session refresh is required
        // but these operations are also very quick and infrequent (every 2 hours)
        // so for now this is a small price to pay for making the session maps "safe".
        LOG.debug("Getting session ID");
        Date methodStartDate = new Date();
        synchronized (sessionMapsLock) {
            Date now = new Date();
            String sessionId = authStringToIcatSessionIdMap.get(authString);
            Date sessionTimeoutDate = icatSessionIdToTimeoutDate.get(sessionId);
            String username = Utils.getUsernamePasswordFromAuthString(authString).getUsername();
            if (sessionId == null || now.after(sessionTimeoutDate)) {
                if (sessionId == null) {
                    LOG.warn("No session ID found in authStringToIcatSessionIdMap for user '" + username + "'. Doing ICAT login.");
                } else {
                    LOG.warn("Expired session ID found in authStringToIcatSessionIdMap for user '" + username + "'. Doing ICAT login.");
                    // Clean up the session maps
                    authStringToIcatSessionIdMap.remove(authString);
                    icatSessionIdToTimeoutDate.remove(sessionId);
                }
                doIcatLogin(authString);
                // If the login was successful then the session ID will now be in the map
                // (otherwise there would have been an UnauthenticatedException)
                sessionId = authStringToIcatSessionIdMap.get(authString);
            } else {
                // If we are within the specified number of minutes of the session timeout then refresh it
                Date sessionRefreshDate = new Date(sessionTimeoutDate.getTime() - (properties.getSessionRefreshMarginMins() * 60 * 1000));
                if (now.after(sessionRefreshDate)) {
                    double remainingMinutes = 0.0;
                    // Save nowMs before calling getRemainingMinutes so that the sessionTimeoutDate
                    // is before rather than after the actual timeout date
                    long nowMs = System.currentTimeMillis();
                    try {
                        LOG.debug("Refreshing ICAT session ID for user '" + username + "'");
                        icatEP.refresh(sessionId);
                        remainingMinutes = icatEP.getRemainingMinutes(sessionId);
                    } catch (IcatException_Exception e) {
                        String message = "Error refreshing or getting remaining minutes for ICAT session: " + sessionId;
                        LOG.error(message, e);
                        throw new UnauthenticatedException(message, e);
                    }
                    Date newSessionTimeoutDate = new Date(nowMs + (long) (remainingMinutes * 60.0 * 1000.0));
                    // Update this entry in the session timeout map
                    icatSessionIdToTimeoutDate.put(sessionId, newSessionTimeoutDate);
                }
            }
            LOG.trace("getIcatSessionId took " + (System.currentTimeMillis() - methodStartDate.getTime()) + " ms");
            return sessionId;
        }
    }

    private void doIcatLogin(String authString) throws UnauthenticatedException {
        LOG.debug("Logging into ICAT");
        // Note that calls from getIcatSessionId() will not be blocked because they are in the same thread
        // but calls from doIcatSearch(), for example, will be blocked - which is what we want
        synchronized (sessionMapsLock) {
            // Do an ICAT login and store the session ID in the map
            UsernamePassword usernamePassword = Utils.getUsernamePasswordFromAuthString(authString);
            String username = usernamePassword.getUsername();

            Credentials credentials = new Credentials();
            List<Entry> entries = credentials.getEntry();
            Entry entry;

            entry = new Entry();
            entry.setKey("username");
            entry.setValue(username);
            entries.add(entry);
            
            com.stfc.useroffice.webservice.UserOfficeWebService_Service service = new com.stfc.useroffice.webservice.UserOfficeWebService_Service();
            com.stfc.useroffice.webservice.UserOfficeWebService port = service.getUserOfficeWebServicePort();
            String fedId = username;
            userId = "uows/" + port.getUserIdFromFedId(fedId);

            entry = new Entry();
            entry.setKey("password");
            entry.setValue(usernamePassword.getPassword());
            entries.add(entry);

            String icatSessionId = null;
            for (String icatAuthenticator : properties.getIcatAuthenticators()) {
                try {
                    icatSessionId = icatEP.login(icatAuthenticator, credentials);
                    // successful login using this authenticator so break here
                    LOG.info("Successful ICAT login for '" + username
                            + "' using the authenticator '" + icatAuthenticator
                            + "'");
                    break;
                } catch (IcatException_Exception e) {
                    String message = "Error logging in to ICAT for '"
                            + username + " using the authenticator '"
                            + icatAuthenticator + "' : " + e.getMessage();
                    LOG.error(message);
                }
            }
            if (icatSessionId == null) {
                String message = "Unable to do ICAT login for '" + username
                        + "' using any of the authenticators";
                LOG.error(message);
                throw new UnauthenticatedException(message);
            }

            double remainingMinutes = 0.0;
            // save nowMs before calling getRemainingMinutes so that the sessionTimeoutDate
            // is before rather than after the actual timeout date
            long nowMs = System.currentTimeMillis();
            try {
                remainingMinutes = icatEP.getRemainingMinutes(icatSessionId);
            } catch (IcatException_Exception e) {
                String message = "Error getting remaining minutes for ICAT session: " + icatSessionId;
                LOG.error(message, e);
                throw new UnauthenticatedException(message, e);
            }
            Date sessionTimeoutDate = new Date(nowMs + (long) (remainingMinutes * 60.0 * 1000.0));

            String previousSessionId = authStringToIcatSessionIdMap.put(authString, icatSessionId);
            if (previousSessionId != null) {
                // clean up the session map
                icatSessionIdToTimeoutDate.remove(previousSessionId);
            }
            icatSessionIdToTimeoutDate.put(icatSessionId, sessionTimeoutDate);
            LOG.debug("icatSessionId created for user: " + usernamePassword.getUsername() + " timeout: " + sessionTimeoutDate);
        }
    }

    @Override
    public List<String> getSessionInfo() {
        LOG.debug("Getting session info");
        // This method is most likely to cause ConcurrentModificationExceptions
        // and because it is only called extremely infrequently, it seems
        // sensible to make it as safe as possible
        synchronized (sessionMapsLock) {
            List<String> sessionInfo = new ArrayList<>();
            sessionInfo.add("Contents of authStringToIcatSessionIdMap:");
            for (String authString : authStringToIcatSessionIdMap.keySet()) {
                String username = Utils.getUsernamePasswordFromAuthString(authString).getUsername();
                String sessionId = authStringToIcatSessionIdMap.get(authString);
                sessionInfo.add(username + " : " + Utils.getStartAndEndOfSessionId(sessionId));
            }
            sessionInfo.add("");
            sessionInfo.add("Contents of icatSessionIdToTimeoutDate:");
            for (String sessionId : icatSessionIdToTimeoutDate.keySet()) {
                Date sessionTimeoutDate = icatSessionIdToTimeoutDate.get(sessionId);
                sessionInfo.add(Utils.getStartAndEndOfSessionId(sessionId) + " : " + sessionTimeoutDate);
            }
            return sessionInfo;
        }
    }

    @Override
    public void commit(String authString) throws WebdavException {
        // DO NOTHING
        LOG.trace("IcatStore.commit()");
    }

    @Override
    public void rollback(String authString) throws WebdavException {
        /// DO NOTHING
        LOG.trace("IcatStore.rollback()");
    }

    /**
     * Specific query to deal with isis calibration investigations where the
     * name is CAL_ALF_12/01/21 12:22:22 This method will take the / and turn
     * them into -
     *
     * @param investigationName Name to be checked and parsed.
     * @param toOriginal if to original then replace - with / else the other way
     * around
     * @return either the original or a parsed investigation name.
     */
    private String processCalibrationInvestigation(String investigationName) {
        
        LOG.info("Checking if investigation " + investigationName + " is a calibration");
        String isCalibration;
        
        if (investigationName.length() >= 4) {
            isCalibration = investigationName.substring(0, 3);
        }
        else {
            isCalibration = investigationName;
        }
        
        if (isCalibration.equals("CAL")) {
            LOG.info(investigationName + " is a calibration");
            int count = 0;
            StringBuilder builder = new StringBuilder(investigationName);
            if (!investigationName.contains(":")) {
                for (int i = 0; i < investigationName.length(); i++) {
                    if (investigationName.charAt(i) == '-') {
                        if (count >= 2 && count < 4) {
                            builder.setCharAt(i, ':');
                        }
                        count ++;
                    }
                }
                investigationName = builder.toString();
            } 
        }
        else {
            LOG.info(investigationName + " is not a calibration");
        }
        return investigationName;
    }

    @Override
    public long getResourceLength(String authString, String uri) throws WebdavException {
        // this method appears never to be called so presumably does not need implementing
        LOG.trace("IcatStore.getResourceLength(" + uri + ") - NOT IMPLEMENTED YET");
        throw new WebdavException("getResourceLength not implemented yet for IcatStore");
    }
    
    @Override
    public String[] getChildrenNames(String authString, String uri) throws WebdavException {
        LOG.trace("IcatStore.getChildrenNames(" + uri + ")");
        String[] uriParts = getUriParts(uri);
        int length = uriParts.length;

        // Deal with going from Root level being 0 and next levels being 1 more then they should.
        if (length > 0) {
            if (length > 1) {
                // Add two to the length since we are skipping both cycle and instrument layers
                if ("MY DATA".equalsIgnoreCase(uriParts[1])) {
                    length += 2;
                }
            }
            length -= 1;
        }
        
        LOG.debug("Length = " + length);
        String icatQuery = null;
        int datafilesLevelDepth = getDatafilesLevelDepth(uri);
        LOG.debug("datafilesLevelDepth = " + datafilesLevelDepth);
        
        IcatEntity selectedEntity = hierarchy.get(length);

        HashMap<String, String> icatEntityValues = getIcatEntityValues(uri);
        
        LOG.info("Have selected: " + selectedEntity.getEntity()); 

        if (selectedEntity.getEntity().equals("FacilityCycle")) {
            icatEntityValues = getFacilityCycleDates(authString, icatEntityValues);
        }
       
        LOG.debug("Creating new query");
        icatQuery = icatMapper.createQuery(hierarchy, icatEntityValues, length, true, userId);
        
        List<Object> results = doIcatSearch(authString, icatQuery);
        
        if (uri.equals("/") && results.size() == 1) {
            results.add("My Data");
            
        }
        LOG.info("Found " + results.size() + " results");

        // Need to combine columns if the entity is investigation and combine value isn't empty.
        // This means that we attach the visit ID onto the end of the investigation
        if (selectedEntity.getEntity().equals("Investigation") && !selectedEntity.getColumnCombineValue().equals("")) {
            String combineValue = selectedEntity.getColumnCombineValue();
            String[] resultsStringArray = new String[results.size()];
            int pointer = 0;
            for (Object temp : results) {
                Investigation investigation = (Investigation) temp;

                String result = processCalibrationInvestigation(investigation.getName()) + " " + combineValue + " " + investigation.getVisitId();

                resultsStringArray[pointer] = result;
                pointer++;
            }
            return resultsStringArray;
        }
        String[] resultsStringArray = new String[results.size()];
        resultsStringArray = results.toArray(resultsStringArray);
        
        return resultsStringArray;
    }
    
    @Override
    public StoredObject getStoredObject(String authString, String uri) throws WebdavException {
        LOG.trace("IcatStore.getStoredObject(" + uri + ")");
        
        boolean isMyData = false;
        String icatQuery = "";
        
        String[] uriParts = getUriParts(uri);
        int length = uriParts.length;
        LOG.debug("Length = " + length);
        
        // Here, we have to take 2 from the total length. GetStoredObject will get sent a path such as:
        // root/ISIS/SANS2D/. We have to compensate for the root level anyway, so take away one for that.
        // In this example, we are searching for instrument objects (SANS2D in this case). In order
        // to select the 'instrument layer' in the hierarchy, we need to actually select layer 1:
        //     0          1             2              3           4         5
        // (Facility, Instrument, FacilityCycle, Investigation, Dataset, Datafile)
        // In the other two methods, we only had to go down one layer as the path would have only been:
        // /ISIS which would have an actual length of 2. Taking away one would still allow us to query
        // on the instrument layer. Since we have an extra level on top when looking for a specific 
        // stored object, we have to take an extra 1 away.
        if (length > 1) {
            // Add two to the length since we are skipping both cycle and instrument layers
            if ("MY DATA".equalsIgnoreCase(uriParts[1])) {
                isMyData = true;
                length += 2;
            }
            length -= 2;
        }
        
        if(uriParts.length > 0) {
            String lastPart = uriParts[uriParts.length - 1];
            List<String> ignoredFiles = properties.getIgnoredFiles();
            
            // Check for files that should be ignored
            // these are typically those that operating systems and installed plugins for git, svn etc.
            // continually look for such as desktop.ini, folder.gif, .git
            // and which we don't want to waste time running ICAT queries looking for them
            if (ignoredFiles.contains(lastPart)) {
                return null;
            }
        }
        
        IcatEntityNames icatEntityNames = getIcatEntityNames(uri);
        IcatEntity selectedMember = hierarchy.get(length);
        HashMap<String, String> icatEntityValues = getIcatEntityValues(uri);

        if (selectedMember.getEntity().equals("Datafile")) {
            LOG.debug("Searching for a datafile...");
            if (isMyData) {
                icatQuery = icatMapper.createQuery(hierarchy, icatEntityValues, length, false, userId);
            }
            else {
                icatQuery = "SELECT datafile from Datafile datafile" + createWhereClause(icatEntityNames, DatafileSearchType.EQUALS);
            }
            
            LOG.debug("icatQuery = [" + icatQuery + "]");

            List<Object> results = doIcatSearch(authString, icatQuery);
            LOG.debug("Found " + results.size() + " results");

            if (results.size() == 1) {
                LOG.debug("We have found the datafile!");
                // we have found the datafile
                Datafile df = (Datafile) results.get(0);
                if (df.getDescription() != null && df.getDescription().equals(FOLDER)) {
                    // these are the virtual "FOLDER" datafiles created by IDAV 
                    LOG.debug("Found virtual folder (via description) for uri: '" + uri + "'. Datafile: " + Utils.getDatafileAsShortString(df));
                    // return a StoredObject with create and modified date set to now
                    Date now = new Date();
                    return createFolderStoredObject(now, now);
                } else {
                    LOG.debug("Found datafile for uri: '" + uri + "': " + Utils.getDatafileAsShortString(df));
                    StoredObject so = new StoredObject();
                    so.setFolder(false);
                    so.setLastModified(df.getModTime().toGregorianCalendar().getTime());
                    so.setCreationDate(df.getCreateTime().toGregorianCalendar().getTime());
                    so.setResourceLength(df.getFileSize());
                    return so;
                }
            }
        } else {
            LOG.info("Creating a new query");
            List<Object> results;
            // If we are entering the MyData folder then we need to create a fake result
            // which will imitate a Facility object which we can then use to create the
            // folder in WebDav.
            if (uri.equals("/My Data")) {
                results = new ArrayList<>();
                Facility facility = new Facility();
                // Set only the attributes that are necessary
                facility.setName("My Data");
                facility.setModTime(getXMLGregorianCalendarNow());
                facility.setCreateTime(getXMLGregorianCalendarNow());
                EntityBaseBean bean = (EntityBaseBean) facility;
                return createFolderStoredObject(bean.getCreateTime(), bean.getModTime());
            } else {
                icatQuery = icatMapper.createQuery(hierarchy, icatEntityValues, length, false, userId);

                LOG.debug("icatQuery = [" + icatQuery + "]");
                results = doIcatSearch(authString, icatQuery);
            }
            
            if (results.size() == 1) {
                LOG.info("Found " + results.size() + " results in ICAT");
                EntityBaseBean bean = (EntityBaseBean) results.get(0);
                return createFolderStoredObject(bean.getCreateTime(), bean.getModTime());
            }  
        }
        // if we have not found a match then a uri must have been
        // provided which doesn't map to an object (or virtual folder) in ICAT
        // return null such that a suitable response is generated by the calling method
        LOG.trace("IcatStore.getStoredObject(" + uri + ") - returning null");
        return null;
    }
    
    public XMLGregorianCalendar getXMLGregorianCalendarNow() {
        try {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
            return now;
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }
    
    @Override
    public InputStream getResourceContent(String authString, String uri)
            throws WebdavException {
        LOG.trace("IcatStore.getResourceContent(" + uri + ")");
        
        String[] uriParts = getUriParts(uri);
        boolean isMyData = false;
        int length = uriParts.length;

        // Deal with going from Root level being 0 and next levels being 1 more then they should.
        if (length > 0) {
            // Add two to the length since we are skipping both cycle and instrument layers
            if ("MY DATA".equalsIgnoreCase(uriParts[1])) {
                isMyData = true;
            }
            else {
                length -= 1;
            }
        }
        
        LOG.debug("Length = " + length);

        int datafilesLevelDepth = getDatafilesLevelDepth(uri);
        if (datafilesLevelDepth > -1) {
            String icatQuery = "";
            if (isMyData) {
                HashMap<String, String> icatEntityValues = getIcatEntityValues(uri);
                icatQuery = icatMapper.createQuery(hierarchy, icatEntityValues, length, false, userId);
            }
            else {
                // we need to be at this level for it to be a datafile
                IcatEntityNames icatEntityNames = getIcatEntityNames(uri);
                // search for an exact datafile match (not a "folder") in this dataset, investigation and facility
                icatQuery = "SELECT datafile from Datafile datafile" + createWhereClause(icatEntityNames, DatafileSearchType.EQUALS);
            }
            
            LOG.debug("icatQuery = [" + icatQuery + "]");
            try {
                List<Object> results = doIcatSearch(authString, icatQuery);
                if (results.size() == 1) {
                    // we have found the datafile
                    Datafile df = (Datafile) results.get(0);
                    LOG.debug("Found datafile: " + Utils.getDatafileAsShortString(df));
                    DataSelection dataSelection = new DataSelection();
                    dataSelection.addDatafile(df.getId());
                    return idsClient.getData(getIcatSessionId(authString), dataSelection, Flag.NONE, 0L);
                }
            } catch (IdsException e) {
                LOG.debug("Error getting file from IDS", e);
                throw new WebdavException("Error getting file from IDS", e);
            }
        }
        throw new WebdavException("getResourceContent not implemented yet for this type of object in IcatStore");
    }
    
    // A convenience method to be able to pass dates directly from ICAT objects
    private static StoredObject createFolderStoredObject(XMLGregorianCalendar createDateXML, XMLGregorianCalendar modifiedDateXML) {
        return createFolderStoredObject(createDateXML.toGregorianCalendar().getTime(), modifiedDateXML.toGregorianCalendar().getTime());
    }

    private static StoredObject createFolderStoredObject(Date createDate, Date modifiedDate) {
        StoredObject so = new StoredObject();
        so.setFolder(true);
        so.setLastModified(modifiedDate);
        so.setCreationDate(createDate);
        so.setResourceLength(0L);
        return so;
    }

    /**
     * Retrieves the current facility cycle dates so these values can be used
     *
     * @param authString
     * @param currentIcatValues
     * @return
     */
    private HashMap<String, String> getFacilityCycleDates(String authString, HashMap<String, String> currentIcatValues) {
        return currentIcatValues;
    }

    private static String createWhereClause(IcatEntityNames icatEntityNames, DatafileSearchType datafileSearchType, boolean includeTopLevel, boolean replaceDashes) {
        String whereClause = null;
        if (icatEntityNames.getDatafileName() != null) {
            // DatafileSearchType needs to be a value other than NONE
            whereClause = " WHERE datafile.dataset.investigation.facility.name='{}' AND datafile.dataset.investigation.name='{}'";
            whereClause += " AND datafile.dataset.investigation.visitId='{}' AND datafile.dataset.name='{}'";
            if (datafileSearchType == DatafileSearchType.EQUALS) {
                whereClause += " AND datafile.name='{}'";
            } else if (datafileSearchType == DatafileSearchType.LIKE) {
                // TODO - modify this query to escape any % chars in datafile names once this is fixed in ICAT
                whereClause += " AND datafile.name like '{}/%'";
            } else {
                // TODO - this should not happen - throw an exception?
            }
            if (!includeTopLevel) {
                whereClause = whereClause.replaceAll("datafile.", "");
            }
        } else if (icatEntityNames.getDatasetName() != null) {
            whereClause = " WHERE datafile.dataset.investigation.facility.name='{}' AND datafile.dataset.investigation.title='{}'";
            whereClause += " AND datafile.dataset.name='{}'";
            if (!includeTopLevel) {
                whereClause = whereClause.replaceAll("datafile.", "");
            }
        } else if (icatEntityNames.getInvestigationName() != null) {
            whereClause = " WHERE dataset.investigation.facility.name='{}' AND dataset.investigation.title='{}'";
            if (!includeTopLevel) {
                whereClause = whereClause.replaceAll("dataset.", "");
            }
        } else if (icatEntityNames.getFacilityName() != null) {
            whereClause = " WHERE investigation.facility.name='{}'";
            if (!includeTopLevel) {
                whereClause = whereClause.replaceAll("investigation.", "");
            }
        } else {
            // TODO - throw an exception if we get this far because they are all null
        }
        // facility should never be null
        // StringUtils.replaceOnce is used instead of String.replaceFirst because it does literal string replacement
        // replaceFirst uses regular expressions which causes problems with characters like $
        
        whereClause = StringUtils.replaceOnce(whereClause, CURLY_BRACES, Utils.escapeStringForIcatQuery(icatEntityNames.getFacilityName()));
        if (icatEntityNames.getInvestigationName() != null) {
            String[] investigationAndVisit =  getInvestigationAndVisit(Utils.escapeStringForIcatQuery(icatEntityNames.getInvestigationName()));
            String investigationName = investigationAndVisit[0];
            String visitName = investigationAndVisit[1];
            whereClause = StringUtils.replaceOnce(whereClause, CURLY_BRACES, investigationName);
            whereClause = StringUtils.replaceOnce(whereClause, CURLY_BRACES, visitName);
        }
        
        if (icatEntityNames.getDatasetName() != null) {
            whereClause = StringUtils.replaceOnce(whereClause, CURLY_BRACES, Utils.escapeStringForIcatQuery(icatEntityNames.getDatasetName()));
        }
        if (icatEntityNames.getDatafileName() != null) {
            whereClause = StringUtils.replaceOnce(whereClause, CURLY_BRACES, Utils.escapeStringForIcatQuery(icatEntityNames.getDatafileName()));
        }
        return whereClause;
    }

    private static String createWhereClause(IcatEntityNames icatEntityNames, DatafileSearchType datafileSearchType) {
        return createWhereClause(icatEntityNames, datafileSearchType, true, false);
    }

    private static String createWhereClause(IcatEntityNames icatEntityNames, boolean includeTopLevel) {
        return createWhereClause(icatEntityNames, DatafileSearchType.NONE, includeTopLevel, false);
    }

    // As there is a possibility of us trying this first with an expired ICAT session ID
    // we might need to catch the ICAT session exception, re-login and try again.
    // Potentially all other ICAT calls need this functionality added as well but seeing as
    // this is by far the most frequently called this is the most important place to have it.
    private List<Object> doIcatSearch(String authString, String icatQuery) throws WebdavException {
        LOG.debug("Searching the ICAT");
        List<Object> results = null;
        try {
            results = icatEP.search(getIcatSessionId(authString), icatQuery);
        } catch (IcatException_Exception e1) {
            if (e1.getFaultInfo().getType() == IcatExceptionType.SESSION) {
                String username = Utils.getUsernamePasswordFromAuthString(authString).getUsername();
                LOG.debug("ICAT session exception for user: " + username + " - will re-login and try again");
                try {
                    doIcatLogin(authString);
                    results = icatEP.search(getIcatSessionId(authString), icatQuery);
                } catch (IcatException_Exception e2) {
                    String message = "Error executing second ICAT query: [" + icatQuery + "]";
                    LOG.debug(message, e2);
                    throw new WebdavException(message, e2);
                }
            } else {
                String message = "Error executing first ICAT query: [" + icatQuery + "]";
                LOG.debug(message, e1);
                throw new WebdavException(message, e1);
            }
        }
        return results;
    }

    @Override
    public void createFolder(String authString, String uri) throws WebdavException {
        LOG.trace("IcatStore.createFolder(" + uri + ")");
        IcatEntityNames icatEntityNames = getIcatEntityNames(uri);
        String[] uriParts = getUriParts(uri);
        try {
            if (uriParts.length == 0 || uriParts.length == 1) {
                // the uri is probably either "/" or the empty string
                String message = "Unable to create folder from uri: '" + uri + "'";
                LOG.debug(message);
                throw new WebdavException(message);
            } else if (uriParts.length == 2) {
                // create a Facility
                Facility fac = new Facility();
                fac.setName(icatEntityNames.getFacilityName());
                long facId = icatEP.create(getIcatSessionId(authString), fac);
                LOG.debug("Created Facility with name '" + fac.getName() + "', id=" + facId);
            } else if (uriParts.length == 3) {
                // create an Investigation
                Facility fac = null;
                String icatQuery = "SELECT fac from Facility fac where fac.name='" + Utils.escapeStringForIcatQuery(icatEntityNames.getFacilityName()) + "'";
                LOG.debug("icatQuery = [" + icatQuery + "]");
                List<Object> facilities = doIcatSearch(authString, icatQuery);
                if (facilities.size() == 1) {
                    fac = (Facility) facilities.get(0);
                } else {
                    LOG.error(facilities.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
                    throw new WebdavException("Error creating facility folder for uri (" + uri + ")");
                }
                InvestigationType invType = getInvestigationType(authString);
                Investigation inv = new Investigation();
                inv.setFacility(fac);
                
                // The investigation name needs to be unique (ICAT name plus visit ID unique constraint)
                // and because on Windows a folder is always created with the name "New Folder"
                // we need to make it unique. WebDAV works with Investigation titles so this value
                // is never seen by the user anyway.
                inv.setName(icatEntityNames.getInvestigationName() + " " + System.currentTimeMillis());
                inv.setTitle(icatEntityNames.getInvestigationName()); // title MUST be set
                inv.setVisitId("1"); // visitId MUST be set
                inv.setType(invType);
                inv.setId(icatEP.create(getIcatSessionId(authString), inv));
                LOG.debug("Created Investigation with name '" + inv.getName() + "', id=" + inv.getId());

                // TopCAT needs the investigation to be linked to an instrument
                // for it to be displayed in the "Browse All Data" tab
                Instrument instrument = getInstrument(authString);
                InvestigationInstrument ii = new InvestigationInstrument();
                ii.setInvestigation(inv);
                ii.setInstrument(instrument);
                long invInstId = icatEP.create(getIcatSessionId(authString), ii);
                LOG.debug("Created InvestigationInstrument with id=" + invInstId);

            } else if (uriParts.length == 4) {
                // Create a Dataset
                Investigation inv = null;
                String icatQuery = "SELECT inv from Investigation inv where inv.facility.name='"
                        + Utils.escapeStringForIcatQuery(icatEntityNames.getFacilityName())
                        + "' and inv.title='"
                        + Utils.escapeStringForIcatQuery(icatEntityNames.getInvestigationName()) + "'";
                LOG.debug("icatQuery = [" + icatQuery + "]");
                List<Object> results = doIcatSearch(authString, icatQuery);
                if (results.size() == 1) {
                    inv = (Investigation) results.get(0);
                } else {
                    LOG.error(results.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
                    throw new WebdavException("Error creating investigation folder for uri (" + uri + ")");
                }
                DatasetType dsType = getDatasetType(authString);
                Dataset ds = new Dataset();
                ds.setInvestigation(inv);
                ds.setName(icatEntityNames.getDatasetName());
                ds.setType(dsType);
                long dsId = icatEP.create(getIcatSessionId(authString), ds);
                LOG.debug("Created Dataset with name '" + ds.getName() + "', id=" + dsId);
            } else {
                // if there are more parts then it must be a virtual folder
                LOG.debug("Creating virtual folder for uri '" + uri + "'");
                // create a virtual folder by creating a Datafile with the description set to FOLDER
                Dataset dataset = getDataset(authString, icatEntityNames);
                DatafileFormat datafileFormat = getDatafileFormat(authString);
                String dfName = icatEntityNames.getDatafileName();
                InputStream inputStream = new ByteArrayInputStream(TEMP_FILE_CONTENTS.getBytes());
                long dfId = idsClient.put(getIcatSessionId(authString), inputStream, dfName, dataset.getId(), datafileFormat.getId(), FOLDER);
                LOG.debug("Created Datafile with name '" + dfName + "', id=" + dfId);
            }
        } catch (IcatException_Exception e) {
            String message = "Error creating folder for uri (" + uri + ")";
            LOG.error(message, e);
            throw new WebdavException("Error creating folder for uri (" + uri + ")", e);
        } catch (IdsException e) {
            String message = "Error creating virtual folder with IDS for uri (" + uri + ")";
            LOG.error(message, e);
            throw new WebdavException(message, e);
        }
    }

    private DatafileFormat getDatafileFormat(String authString) throws WebdavException {
        if (datafileFormat == null) {
            // TODO - DatafileFormat also has a mandatory field version - how to deal with more than one version?
            LOG.info("Getting datafileFormat from ICAT");
            String icatQuery = "SELECT datafileFormat from DatafileFormat datafileFormat WHERE datafileFormat.name='" + properties.getDatafileFormatName() + "'";
            LOG.debug("icatQuery = [" + icatQuery + "]");
            List<Object> datafileFormats = doIcatSearch(authString, icatQuery);
            if (datafileFormats.size() == 1) {
                datafileFormat = (DatafileFormat) datafileFormats.get(0);
            } else {
                LOG.error(datafileFormats.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
                throw new WebdavException("Error getting DatafileFormat");
            }
        }
        return datafileFormat;
    }

    private DatasetType getDatasetType(String authString) throws WebdavException {
        if (datasetType == null) {
            LOG.info("Getting datasetType from ICAT");
            String icatQuery = "SELECT dsType from DatasetType dsType WHERE dsType.name='" + properties.getDatasetTypeName() + "'";
            LOG.debug("icatQuery = [" + icatQuery + "]");
            List<Object> dsTypes = doIcatSearch(authString, icatQuery);
            if (dsTypes.size() == 1) {
                datasetType = (DatasetType) dsTypes.get(0);
            } else {
                LOG.error(dsTypes.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
                throw new WebdavException("Error getting DatasetType");
            }
        }
        return datasetType;
    }

    private InvestigationType getInvestigationType(String authString) throws WebdavException {
        if (investigationType == null) {
            LOG.info("Getting investigationType from ICAT");
            String icatQuery = "SELECT invType from InvestigationType invType WHERE invType.name='" + properties.getInvestigationTypeName() + "'";
            LOG.debug("icatQuery = [" + icatQuery + "]");
            List<Object> invTypes = doIcatSearch(authString, icatQuery);
            if (invTypes.size() == 1) {
                investigationType = (InvestigationType) invTypes.get(0);
            } else {
                LOG.error(invTypes.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
                throw new WebdavException("Error getting InvestigationType");
            }
        }
        return investigationType;
    }

    private Instrument getInstrument(String authString) throws WebdavException {
        LOG.debug("Getting intrument data");
        if (instrument == null) {
            LOG.info("Getting instrument from ICAT");
            String icatQuery = "SELECT inst from Instrument inst WHERE inst.name='" + properties.getInstrumentName() + "'";
            LOG.debug("icatQuery = [" + icatQuery + "]");
            List<Object> instruments = doIcatSearch(authString, icatQuery);
            if (instruments.size() == 1) {
                instrument = (Instrument) instruments.get(0);
            } else {
                LOG.error(instruments.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
                throw new WebdavException("Error getting Instrument");
            }
        }
        return instrument;
    }

    // NOTE: getFacility, getInvestigation and getDataset need the INCLUDE 1 
    // for when a rename is being done and the icatEP.update() method is being called
    private Facility getFacility(String authString, IcatEntityNames icatEntityNames) throws WebdavException {
        LOG.debug("Getting facility data");
        Facility fac = null;
        String icatQuery = "SELECT facility from Facility facility" + createWhereClause(icatEntityNames, false) + " INCLUDE 1";
        LOG.debug("icatQuery = [" + icatQuery + "]");
        List<Object> results = doIcatSearch(authString, icatQuery);
        if (results.size() == 1) {
            fac = (Facility) results.get(0);
        } else {
            LOG.error(results.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
            throw new WebdavException("Error getting Facility from icatEntityNames: " + icatEntityNames.toString());
        }
        return fac;
    }

    private Investigation getInvestigation(String authString, IcatEntityNames icatEntityNames) throws WebdavException {
        LOG.debug("Getting investigation data");
        Investigation inv = null;
        String icatQuery = "SELECT investigation from Investigation investigation" + createWhereClause(icatEntityNames, false) + " INCLUDE 1";
        LOG.debug("icatQuery = [" + icatQuery + "]");
        List<Object> results = doIcatSearch(authString, icatQuery);
        if (results.size() == 1) {
            inv = (Investigation) results.get(0);
        } else {
            LOG.error(results.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
            throw new WebdavException("Error getting Investigation from icatEntityNames: " + icatEntityNames.toString());
        }
        return inv;
    }

    private Dataset getDataset(String authString, IcatEntityNames icatEntityNames) throws WebdavException {
        LOG.debug("Getting dataset data");
        Dataset dataset = null;
        String icatQuery = "SELECT dataset from Dataset dataset" + createWhereClause(icatEntityNames, false) + " INCLUDE 1";
        LOG.debug("icatQuery = [" + icatQuery + "]");
        List<Object> results = doIcatSearch(authString, icatQuery);
        if (results.size() == 1) {
            dataset = (Dataset) results.get(0);
        } else {
            LOG.error(results.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
            throw new WebdavException("Error getting Dataset from icatEntityNames: " + icatEntityNames.toString());
        }
        return dataset;
    }

    private Datafile getDatafile(String authString, IcatEntityNames icatEntityNames) throws WebdavException {
        LOG.debug("Getting datafile data");
        Datafile datafile = null;
        String icatQuery = "SELECT datafile from Datafile datafile" + createWhereClause(icatEntityNames, DatafileSearchType.EQUALS);
        LOG.debug("icatQuery = [" + icatQuery + "]");
        List<Object> results = doIcatSearch(authString, icatQuery);
        if (results.size() == 1) {
            datafile = (Datafile) results.get(0);
        } else {
            // this may be OK - we may just be checking that the file does not exist
            LOG.warn(results.size() + " results returned from icatQuery '" + icatQuery + "' - expected 1");
            throw new WebdavException("Error getting Datafile from icatEntityNames: " + icatEntityNames.toString());
        }
        return datafile;
    }

    private List<Datafile> getDatafileAndChildren(String authString, IcatEntityNames icatEntityNames) throws WebdavException {
        LOG.debug("Getting the datafile and children");
        // TODO - modify this query to escape any % chars in datafile names once this is fixed in ICAT
        String icatQuery = "SELECT df from Datafile df "
                + "WHERE df.dataset.investigation.facility.name='" + Utils.escapeStringForIcatQuery(icatEntityNames.getFacilityName())
                + "' AND df.dataset.investigation.title='" + Utils.escapeStringForIcatQuery(icatEntityNames.getInvestigationName())
                + "' AND df.dataset.name='" + Utils.escapeStringForIcatQuery(icatEntityNames.getDatasetName())
                + "' AND (df.name='" + Utils.escapeStringForIcatQuery(icatEntityNames.getDatafileName())
                + "' OR df.name like '" + Utils.escapeStringForIcatQuery(icatEntityNames.getDatafileName()) + "/%') INCLUDE 1";
        LOG.debug("icatQuery = [" + icatQuery + "]");
        List<Object> results = doIcatSearch(authString, icatQuery);
        if (results.isEmpty()) {
            LOG.error("No results returned from icatQuery '" + icatQuery + "' - expected at least 1");
            String message = "Error getting Datafile and Children from icatEntityNames: " + icatEntityNames.toString();
            LOG.error(message);
            throw new WebdavException(message);
        } else if (results.size() == 1) {
            LOG.debug("Found just the Datafile (no children)");
        } else {
            LOG.debug("Found the Datafile plus " + (results.size() - 1) + " children");
        }
        List<Datafile> datafileList = new ArrayList<Datafile>();
        for (Object dfObj : results) {
            datafileList.add((Datafile) dfObj);
        }
        return datafileList;
    }

    @Override
    public void createResource(String authString, String uri)
            throws WebdavException {
        LOG.trace("IcatStore.createResource(" + uri + ")");
        // NOTE: resources can only be created at Datafile level
        // above that only createFolder should be being called (for Facility, Investigation and Dataset)
        IcatEntityNames icatEntityNames = getIcatEntityNames(uri);
        if (icatEntityNames.getDatafileName() == null) {
            String message = "Error creating resource for uri (" + uri + ")";
            LOG.error(message);
            throw new WebdavException(message);
        } else {
            DatafileFormat datafileFormat = getDatafileFormat(authString);
            Dataset dataset = getDataset(authString, icatEntityNames);
            // just create an empty file - setResourceContent will be called afterwards
            InputStream inputStream = new ByteArrayInputStream(EMPTY_STRING.getBytes());
            String dfName = icatEntityNames.getDatafileName();
            long dfId;
            try {
                dfId = idsClient.put(getIcatSessionId(authString), inputStream, dfName, dataset.getId(), datafileFormat.getId(), null);
                LOG.debug("Created datafile with name '" + dfName + "', id=" + dfId);
            } catch (IdsException e) {
                String message = "Error creating resource from uri (" + uri + ")";
                LOG.error(message, e);
                throw new WebdavException(message, e);
            }
        }
    }

    @Override
    public long setResourceContent(String authString, String uri,
            InputStream is, String contentType, String characterEncoding)
            throws WebdavException {
        LOG.trace("IcatStore.setResourceContent(" + uri + ")");
        // NOTE: resources can only be created at Datafile level
        // above that only createFolder should be being called (for Facility, Investigation and Dataset)
        IcatEntityNames icatEntityNames = getIcatEntityNames(uri);
        if (icatEntityNames.getDatafileName() == null) {
            String message = "Error setting resource content for uri (" + uri + ")";
            LOG.error(message);
            throw new WebdavException(message);
        } else {
            // check whether the datafile already exists
            Datafile existingDatafile = null;
            try {
                existingDatafile = getDatafile(authString, icatEntityNames);
            } catch (WebdavException e) {
                // this could be that no datafile was found which is OK
                LOG.info("No existing datafile found - this is probably OK");
            }
            if (existingDatafile != null) {
                DataSelection dataSelection = new DataSelection();
                dataSelection.addDatafile(existingDatafile.getId());
                try {
                    idsClient.delete(getIcatSessionId(authString), dataSelection);
                    LOG.debug("Deleted existing Datafile with id: " + existingDatafile.getId());
                } catch (IdsException e) {
                    String message = "Error deleting existing Datafile: " + Utils.getDatafileAsShortString(existingDatafile);
                    LOG.error(message, e);
                    throw new WebdavException(message, e);
                }
            }
            DatafileFormat datafileFormat = getDatafileFormat(authString);
            Dataset dataset = getDataset(authString, icatEntityNames);
            String dfName = icatEntityNames.getDatafileName();
            long dfId;
            try {
                dfId = idsClient.put(getIcatSessionId(authString), is, dfName, dataset.getId(), datafileFormat.getId(), null);
            } catch (IdsException e) {
                String message = "Error setting resource content for uri (" + uri + ")";
                LOG.error(message, e);
                throw new WebdavException(message, e);
            }
            LOG.debug("Created datafile with name '" + dfName + "', id=" + dfId);
            // getting the actual resource length may not be trivial
            // so try just returning -1 for now
            return -1;
        }
    }

    @Override
    public void removeObject(String authString, String uri)
            throws WebdavException {
        LOG.trace("IcatStore.removeObject(" + uri + ")");
        // NOTE: objects can only be removed at Datafile level currently
        // TODO - add recursive behaviour to delete from Facility, Investigation, Dataset or virtual directory
        // or should I not implement this behaviour to prevent accidental deletions???
        String[] uriParts = getUriParts(uri);
        IcatEntityNames icatEntityNames = getIcatEntityNames(uri);
        try {
            if (uriParts.length == 0 || uriParts.length == 1) {
                // uri must either be "/" for 0 or "" for 1 - this should not be possible
                String message = "Error removing object for uri (" + uri + ")";
                LOG.error(message);
                throw new WebdavException(message);
            } else if (uriParts.length == 2) {
                // delete the Facility
                Facility fac = getFacility(authString, icatEntityNames);
                icatEP.delete(getIcatSessionId(authString), fac);
                LOG.debug("Deleted Facility: name='" + fac.getName() + "', id=" + fac.getId());
            } else if (uriParts.length == 3) {
                // delete the Investigation (and InvestigationInstrument)
                Investigation inv = getInvestigation(authString, icatEntityNames);
                icatEP.delete(getIcatSessionId(authString), inv);
                LOG.debug("Deleted Investigation: name='" + inv.getName()
                        + "', id=" + inv.getId()
                        + ", title='" + inv.getTitle() + "'");
            } else if (uriParts.length == 4) {
                // delete the Dataset
                Dataset ds = getDataset(authString, icatEntityNames);
                icatEP.delete(getIcatSessionId(authString), ds);
                LOG.debug("Deleted Dataset: name='" + ds.getName() + "', id=" + ds.getId());
            } else {
                Datafile df = getDatafile(authString, icatEntityNames);
                DataSelection dataSelection = new DataSelection();
                dataSelection.addDatafile(df.getId());
                try {
                    idsClient.delete(getIcatSessionId(authString), dataSelection);
                    LOG.debug("Deleted Datafile: " + Utils.getDatafileAsShortString(df));
                } catch (InsufficientPrivilegesException e) {
                    String message = "Error deleting Datafile: "
                            + Utils.getDatafileAsShortString(df)
                            + " : IDS InsufficientPrivilegesException";
                    LOG.error(message, e);
                    throw new AccessDeniedException(message, e);
                } catch (IdsException e) {
                    String message = "Error deleting Datafile: " + Utils.getDatafileAsShortString(df);
                    LOG.error(message, e);
                    throw new WebdavException(message, e);
                }
            }
        } catch (IcatException_Exception e) {
            String message = "Error removing object for uri (" + uri + ")";
            if (e.getFaultInfo().getType() == IcatExceptionType.INSUFFICIENT_PRIVILEGES) {
                LOG.error(message + " : ICAT INSUFFICIENT_PRIVILEGES", e);
                throw new AccessDeniedException(message, e);
            } else {
                LOG.error(message, e);
                throw new WebdavException(message, e);
            }
        }
    }

    private HashMap<String, String> getIcatEntityValues(String uri) {
        LOG.debug("Getting icat entity values");

        HashMap<String, String> icatEntityValues = new HashMap<>();

        String[] uriParts = getUriParts(uri);

        int currentPosition = 0;

        for (String uriPoint : uriParts) {
            //Ignore initial root level.
            if (!"".equals(uriPoint)) {
                String entity = hierarchy.get(currentPosition).getEntity();

                switch (entity) {
                    case "Facility":
                        icatEntityValues.put("Facility", uriPoint);
                        break;
                    case "FacilityCycle":
                        icatEntityValues.put("FacilityCycle", uriPoint);
                        break;
                    case "Instrument":
                        icatEntityValues.put("Instrument", uriPoint);
                        break;
                    case "Investigation":
                        String processNamed = processCalibrationInvestigation(uriPoint);
                        icatEntityValues.put("Investigation", processNamed);
                        break;
                    case "Dataset":
                        icatEntityValues.put("Dataset", uriPoint);
                        break;
                    case "Datafile":
                        icatEntityValues.put("Datafile", uriPoint);
                }

                currentPosition += 1;
            }
        }
        return icatEntityValues;
    }

    private static IcatEntityNames getIcatEntityNames(String uri) {
        LOG.debug("Getting icat entity names");
        
        IcatEntityNames icatEntityNames = new IcatEntityNames();
        String[] uriParts = getUriParts(uri);
        
        int currentPosition = 0;

        for (String uriPoint : uriParts) {
            //Ignore initial root level.
            if (!"".equals(uriPoint)) {
                String entity = hierarchy.get(currentPosition).getEntity();
                switch (entity) {
                    case "Facility":
                        icatEntityNames.setFacilityName(uriPoint);
                        break;
                    case "FacilityCycle":
                        icatEntityNames.setCycleName(uriPoint);
                        break;
                    case "Instrument":
                        icatEntityNames.setInstrumentName(uriPoint);
                        break;
                    case "Investigation":
                        icatEntityNames.setInvestigationName(uriPoint);
                        break;
                    case "Dataset":
                        icatEntityNames.setDatasetName(uriPoint);
                    case "Datafile":
                        icatEntityNames.setDatafileName(uriPoint);
                }
                currentPosition += 1;
            }
        }
        return icatEntityNames;
    }

    private static int getDatafilesLevelDepth(String uri) {
        LOG.debug("Getting datafiles level depth");
        
        int datafilesLevelDepth = -1;
        String[] uriParts = getUriParts(uri);
        if (uriParts.length >= DATAFILE_LEVEL) {
            datafilesLevelDepth = uriParts.length - DATAFILE_LEVEL;
        }
        return datafilesLevelDepth;
    }

    private static String[] getUriParts(String uri) {
        LOG.debug("Getting URI parts for " + uri);
        // when called with "/" this results in a zero length
        // array being returned
        // when called with "" (empty string) this results in an array
        // of length one with the only element being an empty string
        // when called with "/Facility" this results in an
        // array with 2 parts
        return uri.split(FORWARD_SLASH_OR_SLASHES);
    }
    
    // A simple method that can be used to construct a URI from the parts (perhaps after modifying one of the parts)
    private static String reconstructUri(String [] uriParts) {
        String uriReturn = "";
        for (String part : uriParts) {
            uriReturn = uriReturn + part + "/";
        }
        uriReturn = uriReturn.substring(0, uriReturn.length() - 1);
        return uriReturn;
    }
    
    // A method to separate out the visitId and the investigation name from the the string that ICATEntityNames returns.
    public static String[] getInvestigationAndVisit(String combinedString) {
        String[] investigationAndVisit = new String[2];
        // Get the index of the actual visit number
        String visitNumber = "";
        String investigationName = "";
        int visitIndex = combinedString.indexOf("visitId") + 9;
        
        if (visitIndex != -1)
        {
            // This will get the whole visit number, regardless of how many digits in the visitId.
            visitNumber = combinedString.substring(visitIndex, combinedString.length());
            
            // The investigationName is whatever is left of the string.
            investigationName = combinedString.substring(0, visitIndex - 11);
            
            // Make sure that the investigation name contains the forwards slashes we need for a correct query.
            if (investigationName.startsWith("CAL")) {
                StringBuilder builder = new StringBuilder(investigationName);
                int count = 0;
                // Need to make sure to add the colons back when doing the query as well
                if (!investigationName.contains(":")) {
                    for (int i = 0; i < investigationName.length(); i++) {
                        if (investigationName.charAt(i) == '-') {
                            if (count >= 2 && count < 4) {
                                builder.setCharAt(i, ':');
                            }
                            count ++;
                        }
                    }
                    investigationName = builder.toString();
                }
            }
        }
        investigationAndVisit[0] = investigationName;
        investigationAndVisit[1] = visitNumber;
        
        return investigationAndVisit;
    }
 
    @Override
    public boolean supportsDirectMove() {
        // this supports the more efficient "direct move"
        return true;
    }

    @Override
    public void doDirectMove(String authString, String sourceUri, String destinationUri) throws WebdavException {
        LOG.trace("IcatStore.doDirectMove(sourceUri:'" + sourceUri + "', destinationUri:'" + destinationUri + "')");
        String[] uriParts = getUriParts(sourceUri);
        IcatEntityNames sourceIcatEntityNames = getIcatEntityNames(sourceUri);
        IcatEntityNames destIcatEntityNames = getIcatEntityNames(destinationUri);
        try {
            if (uriParts.length == 0 || uriParts.length == 1) {
                // uri must either be "/" for 0 or "" for 1 - this should not be possible
                String message = "Error moving object from sourceUri:'" + sourceUri + "' to destinationUri:'" + destinationUri + "'";
                LOG.error(message);
                throw new WebdavException(message);
            } else if (uriParts.length == 2) {
                // rename the Facility
                Facility fac = getFacility(authString, sourceIcatEntityNames);
                fac.setName(destIcatEntityNames.getFacilityName());
                icatEP.update(getIcatSessionId(authString), fac);
                LOG.debug("Renamed Facility: name='" + fac.getName() + "', id=" + fac.getId());
            } else if (uriParts.length == 3) {
                // rename the Investigation
                Investigation inv = getInvestigation(authString, sourceIcatEntityNames);
                inv.setTitle(destIcatEntityNames.getInvestigationName());
                icatEP.update(getIcatSessionId(authString), inv);
                LOG.debug("Renamed Investigation: title='" + inv.getTitle() + "', id=" + inv.getId());
            } else if (uriParts.length == 4) {
                // rename the Dataset
                Dataset ds = getDataset(authString, sourceIcatEntityNames);
                ds.setName(destIcatEntityNames.getDatasetName());
                icatEP.update(getIcatSessionId(authString), ds);
                LOG.debug("Renamed Dataset: name='" + ds.getName() + "', id=" + ds.getId());
            } else {
                List<Datafile> datafileList = getDatafileAndChildren(authString, sourceIcatEntityNames);
                for (Datafile df : datafileList) {
                    LOG.debug("Updating datafile: " + Utils.getDatafileAsShortString(df));
                    // use Apache StringUtils to do the string replacement here
                    // because we just want a literal replacement of one string with another
                    // String.replaceFirst works with regular expressions so can have undesired effects
                    // for example with the $ char which is a legal character in file names
                    df.setName(StringUtils.replaceOnce(df.getName(), sourceIcatEntityNames.getDatafileName(), destIcatEntityNames.getDatafileName()));
                    icatEP.update(getIcatSessionId(authString), df);
                }
                LOG.debug("Listing updated datafiles:");
                for (Datafile df : datafileList) {
                    // extra space to make it line up with the output above
                    LOG.debug("Updated datafile:  " + Utils.getDatafileAsShortString(df));
                }
            }
        } catch (IcatException_Exception e) {
            String message = "Error moving object from sourceUri:'" + sourceUri + "' to destinationUri:'" + destinationUri + "'";
            LOG.error(message, e);
            throw new WebdavException(message, e);
        }
    }

}

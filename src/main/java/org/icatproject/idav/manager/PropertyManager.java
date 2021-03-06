/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.idav.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.icatproject.idav.IcatEntity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyManager {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyManager.class);
    
    private List<String> icatAuthenticators;
    private int sessionRefreshMarginMins;
    private boolean autoCreateInstructions = false;
    private boolean lazyFolderCreationOnPut;
    private boolean setContentLengthHeaders;
    private boolean readOnly;
    private String icatUrl;
    private String idsUrl;
    private String facilityName;
    private String instrumentName;
    private String investigationTypeName;
    private String datasetTypeName;
    private String datafileFormatName;
    private String webdavImplementationClassName;
    private String defaultIndexFile;
    private String insteadOf404;
    
    private List<String> ignoredFiles;
    private List<IcatEntity> hierarchy;

    public PropertyManager(String propertyFile, String hierarchyFile) {
        collectProperties(propertyFile);
        parseHierarchyFile(hierarchyFile);
    }

    /**
     * Constructor that reads the property files and assigns the values to the
     * variables.
     *
     * @param propertyFile Name of the properties file
     */
    private void collectProperties(String propertyFile) {
        LOG.info("Reading properties from " + propertyFile);
        File f = new File(propertyFile);
        Properties props = null;
        try {
            props = new Properties();
            props.load(new FileInputStream(f));
        } catch (Exception e) {
            String msg = "Unable to read property file " + f.getAbsolutePath() + "  "
                    + e.getMessage();
            LOG.error(msg);
            throw new IllegalStateException(msg);

        }

        icatUrl = props.getProperty("icat.url").trim();
        idsUrl = props.getProperty("ids.url").trim();
        
        String icatAuthenticatorsString = props.getProperty("icat.authenticators");
        icatAuthenticators = Arrays.asList(icatAuthenticatorsString.split("\\s+"));

        String sessionRefreshMarginMinsString = props.getProperty("sessionRefreshMarginMins");
        sessionRefreshMarginMins = Integer.parseInt(sessionRefreshMarginMinsString);
        
        String ignoredFilesString = props.getProperty("ignoredFiles");
        ignoredFiles = Arrays.asList(ignoredFilesString.split("\\s+"));
        
        autoCreateInstructions = "TRUE".equalsIgnoreCase(props.getProperty("autoCreateInstructions"));       
        facilityName = props.getProperty("facilityName");
        instrumentName = props.getProperty("instrumentName");
        investigationTypeName = props.getProperty("investigationTypeName");
        datasetTypeName = props.getProperty("datasetTypeName");
        datafileFormatName = props.getProperty("datafileFormatName");
        
        webdavImplementationClassName = props.getProperty("webdavImplementationClassName");
        defaultIndexFile = props.getProperty("defaultIndexFile");
        insteadOf404 = props.getProperty("insteadOf404");
         
        lazyFolderCreationOnPut = "TRUE".equalsIgnoreCase(props.getProperty("lazyFolderCreationOnPut"));
        setContentLengthHeaders = "TRUE".equalsIgnoreCase(props.getProperty("setContentLengthHeaders"));
        readOnly = "TRUE".equalsIgnoreCase(props.getProperty("readOnly"));

        LOG.info("ICAT url set as: " + icatUrl);
        LOG.info("ICAT authenticators are: " + icatAuthenticators);
        LOG.info("Ignored files are: " + ignoredFiles);
        LOG.info("Session refresh minutes are: " + sessionRefreshMarginMins);
        LOG.info("Facility name is: '" + facilityName + "'");
        LOG.info("Instrument name is: '" + instrumentName + "'");
        LOG.info("Investigation type name is: '" + investigationTypeName + "'");
        LOG.info("Dataset type name is: '" + datasetTypeName + "'");
        LOG.info("Datafile format name is: '" + datafileFormatName + "'");
        LOG.info("Auto create instructions is: " + autoCreateInstructions);
        LOG.info("webdavImplementationClassName set as: " + webdavImplementationClassName);
        LOG.info("defaultIndexFile set as: " + defaultIndexFile);
        LOG.info("insteadOf404 set as: " + insteadOf404);
        LOG.info("lazyFolderCreationOnPut set as: " + lazyFolderCreationOnPut);
        LOG.info("setContentLengthHeaders set as: " + setContentLengthHeaders);
        LOG.info("Read Only set as: " + readOnly);
        
        LOG.info("Finished collecting properties.");
    }
    
    /**
     * Parses the hierarchy JSON file and places it in an ArrayList containing Members.
     * @param fileName name of the file to parse.
     */
    private void parseHierarchyFile(String fileName){
        
        hierarchy = new ArrayList<>();
        try {
            JSONParser parser = new JSONParser();
            JSONArray resultArray = (JSONArray) parser.parse(new FileReader(fileName));

            for (Object hierarchyPoint : resultArray) {
                JSONObject entity = (JSONObject) hierarchyPoint;
                IcatEntity member  = new IcatEntity(entity.get("entity").toString(),entity.get("attribute").toString());
                //Only add for specific case
                if("Investigation".equals(member.getEntity())){
                    member.setColumnCombineValue(entity.get("columnCombineValue").toString());
                }
                hierarchy.add(member);        
            }
        } catch (IOException | ParseException e) {
            LOG.error("Issue parsing idav.structure: " + e.getMessage());
        }
    }

    public List<IcatEntity> getHierarchy() {
        return hierarchy;
    }

    public String getIcatUrl() {
        return icatUrl;
    }

    public String getIdsUrl() {
        return idsUrl;
    }

    public List<String> getIcatAuthenticators() {
        return icatAuthenticators;
    }
    
    public List<String> getIgnoredFiles() {
        return ignoredFiles;
    }

    public int getSessionRefreshMarginMins() {
        return sessionRefreshMarginMins;
    }

    public boolean getAutoCreateInstructions() {
        return autoCreateInstructions;
    }
    
    public String getFacilityName() {
        return facilityName;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public String getInvestigationTypeName() {
        return investigationTypeName;
    }

    public String getDatasetTypeName() {
        return datasetTypeName;
    }

    public String getDatafileFormatName() {
        return datafileFormatName;
    }
    
    public String getWebdavImplementationClassName() {
        return webdavImplementationClassName;
    }
    
    public String getDefaultIndexFile() {
        return defaultIndexFile;
    }
    
    public String getInsteadOf404() {
        return insteadOf404;
    }
    
    public boolean getLazyFolderCreationOnPut() {
        return lazyFolderCreationOnPut;
    }
    
    public boolean getSetContentLengthHeaders() {
        return setContentLengthHeaders;
    }
    
    public boolean getReadOnly() {
        return readOnly;
    }
}

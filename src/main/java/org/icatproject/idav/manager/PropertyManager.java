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

    private String icatUrl;
    private String idsUrl;
    private List<String> icatAuthenticators;
    private int sessionRefreshMarginMins;
    private String instrumentName;
    private String investigationTypeName;
    private String datasetTypeName;
    private String datafileFormatName;
    private List<String> ignoredFiles;
    
    private ArrayList<IcatEntity> hierarchy;

    public PropertyManager(String propertyFile, String hierarchyFile) {
        collectProperties(propertyFile);
        hierarchy = parseHierarchyFile(hierarchyFile);
    }

    /**
     * Constructor that reads the property files and assigns the values to the
     * variables.
     *
     * @param propertyFile Name of the properties file
     */
    public void collectProperties(String propertyFile) {
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
        
        instrumentName = props.getProperty("instrumentName");
        investigationTypeName = props.getProperty("investigationTypeName");
        datasetTypeName = props.getProperty("datasetTypeName");
        datafileFormatName = props.getProperty("datafileFormatName");

        LOG.info("ICAT url set as: " + icatUrl);
        LOG.info("ICAT authenticators are: " + icatAuthenticators);
        LOG.info("Ignored files are: " + ignoredFiles);
        LOG.info("Session refresh minutes are: " + sessionRefreshMarginMins);
        LOG.info("Instrument name is: '" + instrumentName + "'");
        LOG.info("Investigation type name is: '" + investigationTypeName + "'");
        LOG.info("Dataset type name is: '" + datasetTypeName + "'");
        LOG.info("Datafile format name is: '" + datafileFormatName + "'");

        LOG.info("Finished collecting properties.");

    }
    
    /**
     * Passes the hierarchy JSON file and places it in an ArrayList containing Members.
     * @param fileName name of the file to parse.
     * @return an ArrayList of IcatEntity objects in the order required.
     */
    public ArrayList<IcatEntity> parseHierarchyFile(String fileName){
        
        ArrayList<IcatEntity> temp = new ArrayList<>();
        
        try {
            JSONParser parser = new JSONParser();
            JSONArray resultArray = (JSONArray) parser.parse(new FileReader(fileName));
            
           
            
            for(Object hierarchyPoint:resultArray){
               
                JSONObject entity = (JSONObject) hierarchyPoint;
                
                IcatEntity member  = new IcatEntity(entity.get("entity").toString(),entity.get("attribute").toString());
                //Only add for specific case
                if("Investigation".equals(member.getEntity())){
                    member.setColumnCombineValue(entity.get("columnCombineValue").toString());
                }
                
                temp.add(member);        
                
            
            }
        } catch (IOException | ParseException ex) {
            LOG.error("Issue parsing idav.structure "+ex);
        }
        
        return temp;
    }

    public ArrayList<IcatEntity> getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(ArrayList<IcatEntity> hierarchy) {
        this.hierarchy = hierarchy;
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
}

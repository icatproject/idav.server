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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
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
    private String[] hierarchy;

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
    public void collectProperties(String propertyFile) {
        LOG.info("Reading properties.");
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
        
        instrumentName = props.getProperty("instrumentName");
        investigationTypeName = props.getProperty("investigationTypeName");
        datasetTypeName = props.getProperty("datasetTypeName");
        datafileFormatName = props.getProperty("datafileFormatName");

        LOG.info("ICAT url set as: " + icatUrl);
        LOG.info("ICAT authenticators are: " + icatAuthenticators);
        LOG.info("Session refresh minutes are: " + sessionRefreshMarginMins);
        LOG.info("Instrument name is: '" + instrumentName + "'");
        LOG.info("Investigation type name is: '" + investigationTypeName + "'");
        LOG.info("Dataset type name is: '" + datasetTypeName + "'");
        LOG.info("Datafile format name is: '" + datafileFormatName + "'");

        LOG.info("Finished collecting properties.");

    }
    
    public void parseHierarchyFile(String fileName){
        try {
            JSONParser parser = new JSONParser();
            JSONArray resultArray = (JSONArray) parser.parse(new FileReader(fileName));
            
            for(Object hierarchyPoint:resultArray){
                
                JSONObject entity = (JSONObject) hierarchyPoint;
            
            }
        } catch (IOException | ParseException ex) {
            java.util.logging.Logger.getLogger(PropertyManager.class.getName()).log(Level.SEVERE, null, ex);
        }
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

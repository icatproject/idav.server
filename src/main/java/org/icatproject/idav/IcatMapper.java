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
package org.icatproject.idav;

import java.util.HashMap;
import java.util.List;
import org.icatproject.idav.IcatStore;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author yqa41233
 */
public class IcatMapper {
    
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IcatMapper.class);
    
    private static final String GENERAL_MAPPING = " JOIN datafile.dataset dataset JOIN dataset.investigation investigation ";
    private static final String INSTRUMENT_INVESTIGATION = " JOIN investigation.investigationInstruments investigationInstrument JOIN investigationInstrument.instrument instrument ";

    public static HashMap<String, String> icatMap = new HashMap<>();

    static {
        // Datafile Mapping
        icatMap.put("Dataset-Datafile", " JOIN datafile.dataset dataset, dataset.investigation investigation, investigation.facility facility ");

        // Investigation Mapping
        icatMap.put("Investigation-Dataset", " JOIN dataset.investigation investigation ");
        icatMap.put("Investigation-Datafile", GENERAL_MAPPING);
        icatMap.put("Investigation-Instrument", " JOIN instrument.investigationInstruments investigationInstrument JOIN investigationInstrument.investigation investigation ");

        // Facility Mapping
        icatMap.put("Facility-Investigation", " JOIN investigation.facility facility ");
        icatMap.put("Facility-Instrument", " JOIN instrument.facility facility ");
        icatMap.put("Facility-FacilityCycle", " JOIN facilityCycle.facility facility ");

        // Instrument Mapping
        icatMap.put("Instrument-Investigation", INSTRUMENT_INVESTIGATION);
        icatMap.put("Instrument-Dataset", " JOIN dataset.investigation investigation " + INSTRUMENT_INVESTIGATION);
        icatMap.put("Instrument-Datafile", GENERAL_MAPPING + INSTRUMENT_INVESTIGATION);
        icatMap.put("Instrument-FacilityCycle", " JOIN facilityCycle.facility facility JOIN facility.instruments instrument ");

        // FacilityCycle mapping
        icatMap.put("FacilityCycle-Instrument", " JOIN instrument.facility facility JOIN facility.facilityCycles facilityCycle ");
        icatMap.put("FacilityCycle-Investigation", " JOIN investigation.facility facility JOIN facility.facilityCycles facilityCycle ");

        // FacilityCycle
        icatMap.put("Instrument-FacilityCycle-Investigation", " , investigation.facility as facility , facility.instruments as instrument , facility.facilityCycles as facilityCycle , investigation.investigationInstruments as investigationInstrumentPivot , investigationInstrumentPivot.instrument as investigationInstrument  ");
    }

    private String createWhere(List<IcatEntity> hierarchy, HashMap<String, String> icatEntityValues, int currentPosition, boolean child) {
        LOG.info("Creating the WHERE part of the query");

        String where = " WHERE ";

        IcatEntity parentEntity = hierarchy.get(currentPosition - 1);
        IcatEntity childEntity = hierarchy.get(currentPosition);

        String childName = childEntity.getEntity();
        String parentName = parentEntity.getEntity();
        
        LOG.info("Child name = " + childName);
        LOG.info("Parent name = " + parentName);
        
        if (!child) {
            LOG.info("Not a child!");
            String childWhereValue = "";
            String childValue = icatEntityValues.get(childEntity.getEntity());

            // Handle the possible combined columns
            if (childName.equals("Investigation")) {
                // If combined columns split the name and visit id  
                if (childValue.contains(childEntity.getColumnCombineValue())) {
                    String[] investigationValues = childValue.split(childEntity.getColumnCombineValue());
                    childWhereValue = " investigation.name='" + investigationValues[0].trim() + "' AND investigation.visitId='" + investigationValues[1].trim() + "'";
                }
            } else if (childName.equals("Dataset")) {
                if (parentName.equals("Investigation")) {
                    String parentValue = icatEntityValues.get(parentName);
                    // If combined columns split the name and visit id  
                    if (parentValue.contains(parentEntity.getColumnCombineValue())) {
                        String[] investigationValues = parentValue.split(parentEntity.getColumnCombineValue());
                        childWhereValue = " investigation.name='" + investigationValues[0].trim() + "' AND investigation.visitId='" + investigationValues[1].trim() + "' AND ";
                    }
                }
                childWhereValue += StringUtils.uncapitalize(childEntity.getEntity()) + "." + childEntity.getAttribute() + "='" + childValue + "'";
            } else if (childName.equals("Instrument")) {
            	// TODO - KP 22/12/16 - why is fullName specified here when we have specified
            	// Instrument should use the attribute 'name' in the idav.structure file?
                childWhereValue = " instrument.fullName='" + childValue + "'";
            }
            else {
                childWhereValue = StringUtils.uncapitalize(childEntity.getEntity()) + "." + childEntity.getAttribute() + "='" + childValue + "'";
            }
            where += childWhereValue;
        } 
        
        // If parent is FacilityCycle want to do between startDate and endDate of child entity.
        else if (parentEntity.getEntity().equals("FacilityCycle")) {
            IcatEntity grandParentEntity = hierarchy.get(currentPosition - 2);
            String parentWhereValue = StringUtils.uncapitalize(grandParentEntity.getEntity()) + "." + grandParentEntity.getAttribute() + "='" + icatEntityValues.get(grandParentEntity.getEntity()) + "' AND facilityCycle.name='" + icatEntityValues.get(parentEntity.getEntity()) + "' AND investigationInstrument.name= instrument.name AND  " + StringUtils.uncapitalize(childEntity.getEntity()) + ".startDate BETWEEN facilityCycle.startDate AND facilityCycle.endDate ";
            where += parentWhereValue;
        } else {
            String parentWhereValue = "";
            String parentValue = icatEntityValues.get(parentEntity.getEntity());

            if (parentEntity.getEntity().equals("Investigation") && !parentEntity.getColumnCombineValue().equals("")) {
                String[] investigationValues = parentValue.split(parentEntity.getColumnCombineValue());
                parentWhereValue = " investigation.name='" + investigationValues[0].trim() + "' AND investigation.visitId='" + investigationValues[1].trim() + "'";
            } else if (childName.equals("Datafile")) {
                // Get all of the values needed
                String investigation = icatEntityValues.get("Investigation");
                String[] investigationAndVisit =  IcatStore.getInvestigationAndVisit(Utils.escapeStringForIcatQuery(investigation));
                investigation = investigationAndVisit[0];
                String visitId = investigationAndVisit[1];
                String dataset = icatEntityValues.get("Dataset");
                String facility = icatEntityValues.get("Facility");
                
                parentWhereValue = " dataset.name='" + dataset + "' AND investigation.name='" + investigation + "' AND investigation.visitId='" + 
                                   visitId + "' AND facility.name='" + facility + "'";
            } else {
                parentWhereValue = StringUtils.uncapitalize(parentEntity.getEntity()) + "." + parentEntity.getAttribute() + "='" + parentValue + "'";
            }
            where += parentWhereValue;
        }
        
        LOG.info("WHERE = [" + where + "]");
        
        return where;
    }

    private String createJoin(List<IcatEntity> hierarchy, int currentPosition, boolean child) {
        LOG.info("Creating the JOIN part of the query");

        String join = "";
        String key = "";

        IcatEntity childEntity = hierarchy.get(currentPosition);
        IcatEntity parentEntity = hierarchy.get(currentPosition - 1);

        if (parentEntity.getEntity().equals("FacilityCycle")) {
            IcatEntity grandParentEntity = hierarchy.get(currentPosition - 2);

            key = grandParentEntity.getEntity() + "-" + parentEntity.getEntity() + "-" + childEntity.getEntity();
        } else {
            key = parentEntity.getEntity() + "-" + childEntity.getEntity();
        }
        join = icatMap.get(key);
        
        LOG.info("JOIN = [" + join + "]");
        
        return join;
    }

    public String createQuery(List<IcatEntity> hierarchy, HashMap<String, String> icatEntityValues, int currentPosition, boolean child, String userId) {
        // If we are in the MyData folder, we need to call the other mapper class to deal with the specific
        // queries that we need to do in this folder.
        if (icatEntityValues.get("Facility") != null && icatEntityValues.get("Facility").equals("My Data")) {
            LOG.info("Calling MyDataMapper to create the query");
            MyDataMapper mapper = new MyDataMapper();
            return mapper.createQuery(hierarchy, icatEntityValues, currentPosition, child, userId);
        }
        LOG.debug("Creating SELECT part of query");
        
        LOG.info ("Current position = " + currentPosition);
        
        IcatEntity entity = hierarchy.get(currentPosition);
        
        LOG.info("Entity = " + entity.getEntity());
        LOG.info("Boolean child = " + child);
        
        String finalQuery = "";
        String select = "SELECT ";

        String entityName = entity.getEntity();

        // Child is if we need to get all children else we only need the specific entity.
        if (child) {
            // If combined columns required to hold uniqueness then need to combine visitId and Name
            if (entityName.equals("Investigation") && !entity.getColumnCombineValue().equals("")) {
                select = "SELECT investigation FROM Investigation investigation ";
            } else if (entityName.equals("Instrument")) {
            	// TODO - KP 22/12/16 - why is fullName specified here when we have specified
            	// Instrument should use the attribute 'name' in the idav.structure file?
                select = "SELECT instrument.fullName FROM Instrument instrument";
            } else if (entityName.equals("Datafile")) {
                select = "SELECT datafile.name FROM Datafile datafile";
            }
            else {
                select += StringUtils.uncapitalize(entityName) + "." + entity.getAttribute() + " FROM " + entity.getEntity() + " " + StringUtils.uncapitalize(entityName);
            }
        } else {
            select += StringUtils.uncapitalize(entityName) + " FROM " + entity.getEntity() + " " + StringUtils.uncapitalize(entityName);
        }

        finalQuery += select;
        LOG.info("SELECT = [" + select + "]");

        // Only do Join and where if past root.
        if (currentPosition > 0) {
            // Only need a join if getting all the children or a Dataset or Datafile.
            if (child || entityName.equals("Dataset") || entityName.equals("Datafile")) {
                finalQuery += createJoin(hierarchy, currentPosition, child);
            }

            finalQuery += createWhere(hierarchy, icatEntityValues, currentPosition, child);
        }
        
        LOG.info("Full query: " + finalQuery);
        
        return finalQuery;
    }

}

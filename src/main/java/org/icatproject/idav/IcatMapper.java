/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.idav;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author yqa41233
 */
public class IcatMapper {
    
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IcatMapper.class);
    
    private static final String GENERAL_MAPPING = " JOIN datafile.dataset dataset JOIN dataset.investigation investigation ";
    private static final String INSTRUMENT_INVESTIGATION = " JOIN investigation.investigationInstruments investigationInstrument JOIN investigationInstrument.instrument instrument ";

    public static HashMap<String, String> icatMap;

    static {
        HashMap<String, String> tempMap = new HashMap();
        //Datafile Mapping
        tempMap.put("Dataset-Datafile", " JOIN datafile.dataset dataset ");

        //Investigation Mapping
        tempMap.put("Investigation-Dataset", " JOIN dataset.investigation investigation ");
        tempMap.put("Investigation-Datafile", GENERAL_MAPPING);
        tempMap.put("Investigation-Instrument", " JOIN instrument.investigationInstruments investigationInstrument JOIN investigationInstrument.investigation investigation ");

        //Facility Mapping
        tempMap.put("Facility-Investigation", " JOIN investigation.facility facility ");
        tempMap.put("Facility-Instrument", " JOIN instrument.facility facility ");
        tempMap.put("Facility-FacilityCycle", " JOIN facilityCycle.facility facility ");

        //Instrument Mapping
        tempMap.put("Instrument-Investigation", INSTRUMENT_INVESTIGATION);
        tempMap.put("Instrument-Dataset", " JOIN dataset.investigation investigation " + INSTRUMENT_INVESTIGATION);
        tempMap.put("Instrument-Datafile", GENERAL_MAPPING + INSTRUMENT_INVESTIGATION);
        tempMap.put("Instrument-FacilityCycle", " JOIN facilityCycle.facility facility JOIN facility.instruments instrument ");

        //Facility Cycle mapping
        tempMap.put("FacilityCycle-Instrument", " JOIN instrument.facility facility JOIN facility.facilityCycles facilityCycle ");
        tempMap.put("FacilityCycle-Investigation", " JOIN investigation.facility facility JOIN facility.facilityCycles facilityCycle ");

        //Facility Cycle
        tempMap.put("Instrument-FacilityCycle-Investigation", " , investigation.facility as facility , facility.instruments as instrument , facility.facilityCycles as facilityCycle , investigation.investigationInstruments as investigationInstrumentPivot , investigationInstrumentPivot.instrument as investigationInstrument  ");
        icatMap = tempMap;
    }

    public String createWhere(ArrayList<IcatEntity> hierarchy, HashMap<String, String> icatEntityValues, int currentPosition, boolean child) {

        String where = " WHERE ";

        IcatEntity parentEntity = hierarchy.get(currentPosition - 1);
        IcatEntity childEntity = hierarchy.get(currentPosition);

        String childName = childEntity.getEntity();
        String parentName = parentEntity.getEntity();
        
        LOG.info("Child name = " + childName);
        LOG.info("Parent name = " + parentName);
        
        if (!child) {
            String childWhereValue = "";
            String childValue = icatEntityValues.get(childEntity.getEntity());

            //Handle the possible combined columns
            if (childName.equals("Investigation")) {
                //If combined columns split the name and visit id  
                if (childValue.contains(childEntity.getColumnCombineValue())) {
                    String[] investigationValues = childValue.split(childEntity.getColumnCombineValue());
                    childWhereValue = " investigation.name='" + investigationValues[0].trim() + "' AND investigation.visitId='" + investigationValues[1].trim() + "'";
                }

            } else if (childName.equals("Dataset") || childName.equals("Datafile")) {
                if (parentName.equals("Investigation")) {
                    String parentValue = icatEntityValues.get(parentName);
                    //If combined columns split the name and visit id  
                    if (parentValue.contains(parentEntity.getColumnCombineValue())) {
                        String[] investigationValues = parentValue.split(parentEntity.getColumnCombineValue());
                        childWhereValue = " investigation.name='" + investigationValues[0].trim() + "' AND investigation.visitId='" + investigationValues[1].trim() + "' AND ";
                    }
                }

                childWhereValue += StringUtils.uncapitalize(childEntity.getEntity()) + "." + childEntity.getAttribute() + "='" + childValue + "'";

            } else {
                childWhereValue = StringUtils.uncapitalize(childEntity.getEntity()) + "." + childEntity.getAttribute() + "='" + childValue + "'";
            }

            where += childWhereValue;

        } 
        
        //If parent is FacilityCycle want to do between startDate and endDate of child entity.
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
            } else {
                parentWhereValue = StringUtils.uncapitalize(parentEntity.getEntity()) + "." + parentEntity.getAttribute() + "='" + parentValue + "'";
            }

            where += parentWhereValue;
        }
        return where;
    }

    public String createJoin(ArrayList<IcatEntity> hierarchy, int currentPosition, boolean child) {

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

        return join;
    }

    public String createQuery(ArrayList<IcatEntity> hierarchy, HashMap<String, String> icatEntityValues, int currentPosition, boolean child) {
        LOG.debug("Creating select part of query");
        IcatEntity entity = hierarchy.get(currentPosition);

        String finalQuery = "";
        String select = "SELECT ";

        String entityName = entity.getEntity();

        //Child is if we need to get all children else we only need the specific entity.
        if (child) {
            //If combined columns required to hold uniqueness then need to combine visitId and Name
            if (entityName.equals("Investigation") && !entity.getColumnCombineValue().equals("")) {
                select = "SELECT investigation FROM Investigation investigation ";
            }
            // Only use this if trying to omit cycles that have no investigations
            /* else if (entityName.equals("FacilityCycle")) {
                String instrumentName = icatEntityValues.get("Instrument");
                select = "SELECT facilityCycle.name FROM FacilityCycle facilityCycle JOIN facilityCycle.facility facility " +
                         "WHERE (SELECT COUNT (investigation) FROM Investigation investigation , investigation.investigationInstruments as investigationInstrumentPivot , " +
                         "investigationInstrumentPivot.instrument as instrument WHERE instrument.name='" + instrumentName + "' AND " +
                         "investigation.startDate BETWEEN " +
                         "facilityCycle.startDate AND facilityCycle.endDate) > 0";
                return select;
            
            }*/
            else {
                select += StringUtils.uncapitalize(entityName) + "." + entity.getAttribute() + " FROM " + entity.getEntity() + " " + StringUtils.uncapitalize(entityName);
            }
        } else {
            select += StringUtils.uncapitalize(entityName) + " FROM " + entity.getEntity() + " " + StringUtils.uncapitalize(entityName);
        }

        finalQuery += select;

        //Only do Join and where if past root.
        if (currentPosition > 0) {
            //Only need a join if getting all the children or a Dataset or Datafile.
            if (child || entityName.equals("Dataset") || entityName.equals("Datafile")) {
                finalQuery += createJoin(hierarchy, currentPosition, child);
            }

            finalQuery += createWhere(hierarchy, icatEntityValues, currentPosition, child);
        }
        return finalQuery;
    }

}

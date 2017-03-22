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
import org.apache.commons.lang3.StringUtils;
import org.icatproject.idav.IcatStore;

/**
 *
 * @author tip22963
 */
public class MyDataMapper {
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MyDataMapper.class);
    
    public String createQuery(List<IcatEntity> hierarchy, HashMap<String, String> icatEntityValues, int currentPosition, boolean child, String userId) {
        LOG.debug("Creating SELECT part of query");

        IcatEntity entity = hierarchy.get(currentPosition);
        String entityName = entity.getEntity();
        LOG.error(entityName);
        LOG.error(Integer.toString(currentPosition));
        LOG.error("Facility = " + icatEntityValues.get("Facility"));
        LOG.error("Instrument = " + icatEntityValues.get("Instrument"));
        LOG.error("Cycle = " + icatEntityValues.get("FacilityCycle"));
        LOG.error("Investigation = " + icatEntityValues.get("Investigation"));
        LOG.error("Dataset = " + icatEntityValues.get("Dataset"));
        LOG.error("Datafile = " + icatEntityValues.get("Datafile"));
              
        String query = "";
        
        if (child) {
            if (entityName.equals("Investigation")) {
            query = "SELECT investigation.investigation FROM InvestigationUser " +
                    "investigation WHERE investigation.user.name = '" + userId + "'";
            } else if (entityName.equals("Dataset")) {
                String investigation = icatEntityValues.get("Instrument");
                String[] investigationAndVisit =  IcatStore.getInvestigationAndVisit(Utils.escapeStringForIcatQuery(investigation));
                investigation = investigationAndVisit[0];
                String visitId = investigationAndVisit[1];
                query = "SELECT dataset.name FROM Dataset dataset JOIN dataset.investigation " + 
                        "investigation WHERE investigation.name = '"+ investigation + "' AND " + 
                        "investigation.visitId = '" + visitId + "'";
            } else if (entityName.equals("Datafile")) {
                String investigation = icatEntityValues.get("Instrument");
                String dataset = icatEntityValues.get("FacilityCycle");
                String[] investigationAndVisit =  IcatStore.getInvestigationAndVisit(Utils.escapeStringForIcatQuery(investigation));
                investigation = investigationAndVisit[0];
                String visitId = investigationAndVisit[1];
                query = "SELECT datafile.name FROM Datafile datafile JOIN datafile.dataset dataset " + 
                        "JOIN dataset.investigation investigation WHERE investigation.name = '" + investigation + 
                        "' AND dataset.name = '" + dataset + "' AND investigation.visitId = '" + visitId + "'";
            }
        } else {
            if (entityName.equals("FacilityCycle") || entityName.equals("Investigation")) {
            query = "SELECT investigation.investigation FROM InvestigationUser " +
                    "investigation WHERE investigation.user.name = '" + userId + "'";
            } else if (entityName.equals("Dataset")) {
                String investigation = icatEntityValues.get("Instrument");
                String[] investigationAndVisit =  IcatStore.getInvestigationAndVisit(Utils.escapeStringForIcatQuery(investigation));
                investigation = investigationAndVisit[0];
                String visitId = investigationAndVisit[1];
                query = "SELECT dataset FROM Dataset dataset JOIN dataset.investigation " + 
                        "investigation WHERE investigation.name = '"+ investigation + "' AND " + 
                        "investigation.visitId = '" + visitId + "'";
            } else if (entityName.equals("Datafile")) {
                
            }
        }
        
        
        LOG.error("Created the following query: " + query);
        return query;
    }

}

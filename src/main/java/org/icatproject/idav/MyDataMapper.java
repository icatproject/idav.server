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
        IcatEntity entity = hierarchy.get(currentPosition);
        String entityName = entity.getEntity();
              
        String query = "", investigation = "", visitId = "";
        
        // Since we have skipped 2 layers out, the mappings become skewed and will always be
        // 2 steps out of place. This means that when we want data about the investigation,
        // it will actually be stored in the instrument value. This could be changed so that
        // the mapping is actually correct, but this would also require a lot of if statements.
        // However, to make this more of a versatile method, we should be able to determine 
        // the mappings more dynamically than this. - TODO
        if (icatEntityValues.get("Instrument") == null) {
            investigation = icatEntityValues.get("Instrument");
            String[] investigationAndVisit =  IcatStore.getInvestigationAndVisit(Utils.escapeStringForIcatQuery(investigation));
            investigation = investigationAndVisit[0];
            visitId = investigationAndVisit[1];
        }
        
        if (child) {
            if (entityName.equals("Investigation")) {
            query = "SELECT investigation.investigation FROM InvestigationUser " +
                    "investigation WHERE investigation.user.name = '" + userId + "'";
            } else if (entityName.equals("Dataset")) {
                query = "SELECT dataset.name FROM Dataset dataset JOIN dataset.investigation " + 
                        "investigation WHERE investigation.name = '"+ investigation + "' AND " + 
                        "investigation.visitId = '" + visitId + "'";
            } else if (entityName.equals("Datafile")) {
                // In the same vein as above, we get the dataset from two layers back which is
                // the facility cycle.
                String dataset = icatEntityValues.get("FacilityCycle");
                query = "SELECT datafile.name FROM Datafile datafile JOIN datafile.dataset dataset " + 
                        "JOIN dataset.investigation investigation WHERE investigation.name = '" + investigation + 
                        "' AND dataset.name = '" + dataset + "' AND investigation.visitId = '" + visitId + "'";
            }
        } else {
            if (entityName.equals("FacilityCycle") || entityName.equals("Investigation")) {
                query = "SELECT investigation FROM Investigation investigation, investigation.investigationUsers as " + 
                        "investigationUserPivot, investigationUserPivot.user as investigationUser WHERE " + 
                        "investigationUser.name = '" + userId + "' AND investigation.name = '" + investigation +
                        "' AND investigation.visitId = '" + visitId + "'";
            } else if (entityName.equals("Dataset")) {
                query = "SELECT dataset FROM Dataset dataset JOIN dataset.investigation " + 
                        "investigation WHERE investigation.name = '"+ investigation + "' AND " + 
                        "investigation.visitId = '" + visitId + "'";
            } else if (entityName.equals("Datafile")) {
                String dataset = icatEntityValues.get("FacilityCycle");
                // Again, datafile is stored in investigation
                String datafile = icatEntityValues.get("Investigation");
                query = "SELECT datafile FROM Datafile datafile JOIN datafile.dataset dataset " + 
                        "JOIN dataset.investigation investigation WHERE investigation.name = '" + investigation + 
                        "' AND dataset.name = '" + dataset + "' AND investigation.visitId = '" + visitId + "'" + 
                        "AND datafile.name = '" + datafile + "'";
            }
        }
        return query;
    }

}

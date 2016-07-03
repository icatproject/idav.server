/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.idav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author yqa41233
 */
public class IcatMapper {
    
    
   
    
    private static final String GENERAL_MAPPING = " JOIN datafile.dataset dataset JOIN dataset.investigation investigation "; 
    private static final String INSTRUMENT_INVESTIGATION = " JOIN investigation.investigationInstruments investigationInstrument JOIN investigationInstrument.instrument instrument ";
    
    public static HashMap<String,String> icatMap;
    static {
        HashMap<String,String> tempMap = new HashMap();
         //Datafile Mapping
        tempMap.put("Dataset-Datafile", " JOIN datafile.dataset dataset ");
        
        //Investigation Mapping
        tempMap.put("Investigation-Dataset"," JOIN dataset.investigation investigation ");
        tempMap.put("Investigation-Datafile",GENERAL_MAPPING);
        tempMap.put("Investigation-Instrument", " JOIN instrument.investigationInstruments investigationInstrument JOIN investigationInstrument.investigation investigation "); 
               
        
        //Facility Mapping
        tempMap.put("Facility-Investigation"," JOIN investigation.facility facility ");
        tempMap.put("Facility-Instrument"," JOIN instrument.facility facility ");
        
        //Instrument Mapping
        tempMap.put("Instrument-Investigation",INSTRUMENT_INVESTIGATION);
        tempMap.put("Instrument-Dataset"," JOIN dataset.investigation investigation " + INSTRUMENT_INVESTIGATION);
        tempMap.put("Instrument-Datafile", GENERAL_MAPPING+INSTRUMENT_INVESTIGATION);
        
        icatMap = tempMap;
        
    }
          
        
        
    public void IcatMapper(){
        //Creates mapping between entities. Joins are in reverse as we are traversing down with identification info from behind.
        icatMap = new HashMap<>();
             
        
    }
    
    public String createWhere(ArrayList<IcatEntity> hierarchy, HashMap<String,String> icatEntityValues, int currentPosition, boolean child){  
        
        String where = " WHERE ";
        
        IcatEntity parentEntity = hierarchy.get(currentPosition-1);         
        
        if(!child){
            IcatEntity childEntity = hierarchy.get(currentPosition);
            String childWhereValue = childEntity.getEntity().toLowerCase()+"."+childEntity.getAttribute()+"='"+icatEntityValues.get(childEntity.getEntity())+"'";
            where+= childWhereValue +" AND ";
        }
        
        String parentWhereValue =  parentEntity.getEntity().toLowerCase()+"."+parentEntity.getAttribute()+"='"+icatEntityValues.get(parentEntity.getEntity())+"'"; 
        where+=parentWhereValue;
        
        return where;
        
    }
    
    public String createJoin(ArrayList<IcatEntity> hierarchy, int currentPosition){      
        
        IcatEntity childEntity = hierarchy.get(currentPosition);
        IcatEntity parentEntity = hierarchy.get(currentPosition-1);     
         
        String key = parentEntity.getEntity()+"-"+childEntity.getEntity();
            
        String join = icatMap.get(key);
        
        return join;
    }
    
    
    public String createQuery(ArrayList<IcatEntity> hierarchy, HashMap<String,String> icatEntityValues, int currentPosition, boolean child){
        IcatEntity entity = hierarchy.get(currentPosition); 
        
        String finalQuery = "";
        String select = "SELECT ";
        
        if(child){
            select += entity.getEntity().toLowerCase() +"." +entity.getAttribute()+" FROM "+entity.getEntity()+" "+ entity.getEntity().toLowerCase();
        }
        else{
            select += entity.getEntity().toLowerCase() +" FROM "+entity.getEntity()+" "+ entity.getEntity().toLowerCase();
        }
        
        
        
        finalQuery+= select; 
        
        //Only do Join and where if past root.
        if(currentPosition>0){
            finalQuery+= createJoin(hierarchy,currentPosition);
            finalQuery+= createWhere(hierarchy,icatEntityValues,currentPosition,child);
        }
        
        
        
        return finalQuery;
    }
    
}

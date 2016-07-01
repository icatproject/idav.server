/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.idav;

import java.util.HashMap;

/**
 *
 * @author yqa41233
 */
public class IcatMapper {
    
    
    public static HashMap<String,String> icatMap;
        
        
    public void IcatMapper(){
        icatMap = new HashMap<>();
        //Datafile Mapping
        icatMap.put("Dataset-Datafile", "");
        
        //Investigation Mapping
        icatMap.put("Investigation-Dataset","");
        icatMap.put("Investigation-Datafile","");
        icatMap.put("Investigation-Instrument","");
        
        //Facility Mapping
        icatMap.put("Facility-Investigation","");
        icatMap.put("Facility-Instrument","");
        
        //Instrument Mapping
        icatMap.put("Instrument-Investigation","");
        icatMap.put("Instrument-Dataset","");
        icatMap.put("Instrument-Datafile","");      
        
    }
    
    public String createWhereQuery(){
        
    }
    
}

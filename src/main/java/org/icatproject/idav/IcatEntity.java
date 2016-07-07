/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.idav;


public class IcatEntity {
    
    private String entity;
    private String attribute; 
    private String columnCombineValue;

    public IcatEntity(String entity, String attribute) {
        this.entity = entity;
        this.attribute = attribute;
    }

    public String getColumnCombineValue() {
        return columnCombineValue;
    }

    public void setColumnCombineValue(String columnCombineValue) {
        this.columnCombineValue = columnCombineValue;
    }
     
                
    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }
    
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.idav;


public class Member {
    
    private String entity;
    private String attribute; 

    public Member(String entity, String attribute) {
        this.entity = entity;
        this.attribute = attribute;
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

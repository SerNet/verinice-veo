/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 *
 * @author Daniel Murygin
 */
@Entity
public class Property implements Serializable {
    
    @Id
    @Column(length = 36)
    private String uuid;
    
    @Column(name="group_index", nullable = false)
    private int index = 0;
    
    @Column(nullable = false)
    private String id;
    
    @Column(length = 255)
    private String label;
    
    @Lob
    private String text;
    
    private long number;
    
    private Date date;
    
    public Property() {
        if (this.uuid == null) {
            UUID randomUUID = java.util.UUID.randomUUID();
            uuid = randomUUID.toString();
        }
    }

    public String getUuid() {
        return uuid;
    }

    public int getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getText() {
        return text;
    }

    public long getNumber() {
        return number;
    }

    public Date getDate() {
        return date;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
}

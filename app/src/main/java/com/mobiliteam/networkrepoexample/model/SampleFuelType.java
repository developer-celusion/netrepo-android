package com.mobiliteam.networkrepoexample.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by admin on 14/04/18.
 */

public class SampleFuelType {

    @SerializedName("ID")
    private int id;

    @SerializedName("Name")
    private String name;

    @SerializedName("Description")
    private String description;

    @SerializedName("Code")
    private String code;

    @SerializedName("CreatedOn")
    private Date createdOn;

    @SerializedName("CreatedBy")
    private String createdBy;

    @SerializedName("ModifiedOn")
    private Date modifiedOn;

    @SerializedName("ModifiedBy")
    private String modifiedBy;

    @SerializedName("IsDeleted")
    private boolean isDeleted;

    public SampleFuelType() {

    }

    public SampleFuelType(String name) {
        this.id = 0;
        this.name = name;
        this.description = name;
        this.code = name;
        this.createdBy = name;
        this.modifiedBy = name;
        this.isDeleted = false;
        this.createdOn = new Date();
        this.modifiedOn = new Date();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModifiedOn() {
        return modifiedOn;
    }

    public void setModifiedOn(Date modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}

package com.mobiliteam.networkrepo;

/**
 * Created by admin on 14/04/18.
 */

public class APIHeaderField {

    private String key;
    private String value;

    public APIHeaderField(String key,String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

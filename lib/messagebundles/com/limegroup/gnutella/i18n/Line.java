package com.limegroup.gnutella.i18n;

/**
 * Describes a line in a properties file.
 */
class Line {
    
    private final String wholeLine;
    private final String key;
    private final String value;
    
    Line(String data) {
        if(data == null)
            throw new NullPointerException("null data");
            
        wholeLine = data;
        if(data.startsWith("#") || data.trim().equals("")) {
            key = null;
            value = null;
        } else {
            int eq = data.indexOf("=");
            if(eq == -1)
                throw new IllegalArgumentException("can't decode line: " + data);
            key = data.substring(0, eq);
            if(eq == data.length() || eq == data.length()-1)
                value = "";
            else
                value = data.substring(eq+1);
        }
    }
    
    boolean isComment() {
        return key == null;
    }
    
    String getLine() {
        return wholeLine;
    }
    
    String getKey() {
        return key;
    }
    
    String getValue() {
        return value;
    }
}
    
    
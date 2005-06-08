package com.limegroup.gnutella.i18n;
/**
 * Describes a line in a properties file.
 */
class Line {
    private final String wholeLine;
    private final String key;
    private final String value;
    
    /**
     * TODO: does not handle all comment lines properly!
     * TODO: does not separate key=value pairs properly!
     * TODO: does not decode continuation lines properly (continuation lines
     * should be already joined in a upper layer before passing data line here.)
     * @param data a data line to parse and store.
     */
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
    
    /**
     * @return true if this is a not a key=value line.
     */
    boolean isComment() {
        return key == null;
    }
   
    /**
     * @return this whole text line (comment or key=value).
     */
    String getLine() {
        return wholeLine;
    }
    
    /**
     * @return this line key, or null if isComment() is true.
     */
    String getKey() {
        return key;
    }
   
    /**
     * @return this line value, or null if isComment() is true.
     */
    String getValue() {
        return value;
    }
}
 
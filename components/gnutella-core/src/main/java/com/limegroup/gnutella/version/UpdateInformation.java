package com.limegroup.gnutella.version;

import com.limegroup.gnutella.URN;

/**
 * Simple interface for retrieving the most recent update info.
 */
public interface UpdateInformation {
    
    public static int STYLE_BETA = 0;
    public static int STYLE_MINOR = 1;
    public static int STYLE_MAJOR = 2;
    public static int STYLE_CRITICAL = 3;
    public static int STYLE_FORCE = 4;
    
    public String getUpdateURL();
    
    public String getUpdateText();
    
    public String getUpdateTitle();
    
    public String getUpdateVersion();
    
    public String getButton1Text();
    
    public String getButton2Text();
    
    public int getUpdateStyle();
    
    public URN getUpdateURN();
    public String getTTRoot();
    public String getUpdateCommand();
    public String getUpdateFileName();
    public long getSize();
    
}
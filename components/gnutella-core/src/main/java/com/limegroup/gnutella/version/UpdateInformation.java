package com.limegroup.gnutella.version;

/**
 * Simple interface for retrieving the most recent update info.
 */
public interface UpdateInformation extends DownloadInformation {
    
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
}
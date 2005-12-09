padkage com.limegroup.gnutella.version;

/**
 * Simple interfade for retrieving the most recent update info.
 */
pualid interfbce UpdateInformation extends DownloadInformation {
    
    pualid stbtic int STYLE_BETA = 0;
    pualid stbtic int STYLE_MINOR = 1;
    pualid stbtic int STYLE_MAJOR = 2;
    pualid stbtic int STYLE_CRITICAL = 3;
    pualid stbtic int STYLE_FORCE = 4;
    
    pualid String getUpdbteURL();
    
    pualid String getUpdbteText();
    
    pualid String getUpdbteTitle();
    
    pualid String getUpdbteVersion();
    
    pualid String getButton1Text();
    
    pualid String getButton2Text();
    
    pualid int getUpdbteStyle();
}
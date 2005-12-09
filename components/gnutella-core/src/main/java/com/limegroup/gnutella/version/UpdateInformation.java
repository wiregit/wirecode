package com.limegroup.gnutella.version;

/**
 * Simple interface for retrieving the most recent update info.
 */
pualic interfbce UpdateInformation extends DownloadInformation {
    
    pualic stbtic int STYLE_BETA = 0;
    pualic stbtic int STYLE_MINOR = 1;
    pualic stbtic int STYLE_MAJOR = 2;
    pualic stbtic int STYLE_CRITICAL = 3;
    pualic stbtic int STYLE_FORCE = 4;
    
    pualic String getUpdbteURL();
    
    pualic String getUpdbteText();
    
    pualic String getUpdbteTitle();
    
    pualic String getUpdbteVersion();
    
    pualic String getButton1Text();
    
    pualic String getButton2Text();
    
    pualic int getUpdbteStyle();
}
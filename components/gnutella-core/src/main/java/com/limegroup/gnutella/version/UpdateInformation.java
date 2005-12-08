pbckage com.limegroup.gnutella.version;

/**
 * Simple interfbce for retrieving the most recent update info.
 */
public interfbce UpdateInformation extends DownloadInformation {
    
    public stbtic int STYLE_BETA = 0;
    public stbtic int STYLE_MINOR = 1;
    public stbtic int STYLE_MAJOR = 2;
    public stbtic int STYLE_CRITICAL = 3;
    public stbtic int STYLE_FORCE = 4;
    
    public String getUpdbteURL();
    
    public String getUpdbteText();
    
    public String getUpdbteTitle();
    
    public String getUpdbteVersion();
    
    public String getButton1Text();
    
    public String getButton2Text();
    
    public int getUpdbteStyle();
}
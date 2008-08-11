package org.limewire.ui.swing.search.sponsored;

public class SponsoredResult {

    public static enum LinkTarget {STORE, EXTERNAL}

    private String text;

    private LinkTarget target;

    private String title;

    private String navUrl;

    private String visibleUrl;

    /**
     * @param title title of the ad
     * @param text main text of the ad
     * @param visibleUrl url shown in the ad
     * @param navUrl the url navigated to when clicked
     * @param target LinkTarget.INTERNAL to open within LimeWire or
     *        LinkTarget.EXTERNAL to open in a native browser
     */
    public SponsoredResult(String title, String text, String visibleUrl, String navUrl, LinkTarget target) {
        this.title = title;
        this.text = text;
        this.visibleUrl = visibleUrl;
        this.navUrl = navUrl;
        this.target = target;
    }

    public String getTitle() {
        return title;
    }
    
    public String getVisibleUrl() {
        return visibleUrl;
    }
    
    public String getUrl(){
        return navUrl;
    }

    public LinkTarget getTarget() {
        return target;
    }

    public String getText() {
        return text;
    }
}

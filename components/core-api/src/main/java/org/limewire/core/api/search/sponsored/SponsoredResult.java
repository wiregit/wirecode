package org.limewire.core.api.search.sponsored;


public interface SponsoredResult {

    public abstract String getTitle();

    public abstract String getVisibleUrl();

    public abstract String getUrl();

    public abstract SponsoredResultTarget getTarget();

    public abstract String getText();

}
package com.limegroup.gnutella.licenses;

public class PublishedCCLicense implements RDFLicense, EmbeddableLicense {

    private final String holder, year, title, url, description;
    private final int type;
    public PublishedCCLicense(String holder, String title, 
            String year, String url, String description, int type) {
        this.holder = holder;
        this.year = year;
        this.title = title;
        this.url = url;
        this.description = description;
        this.type = type;
    }
    public String getRDFRepresentation() {
        return null;
    }
    public String getEmbeddableString() {
        return null;
    }
}

package com.limegroup.gnutella.xml;

public interface LimeXMLReplyCollectionFactory {

    public LimeXMLReplyCollection createLimeXMLReplyCollection(
            String URI, String path);

    public LimeXMLReplyCollection createLimeXMLReplyCollection(
            String URI);

}

package com.limegroup.gnutella.settings;

import org.limewire.setting.StringSetting;

/**
 * Settings for The LimeWire Store&#8482; song search results. This is used by
 * {@link RemoteStringBasicSpecialResultsDatabaseImpl} to store the search
 * index.
 */
public final class ThirdPartySearchResultsSettings extends LimeProps {
    private ThirdPartySearchResultsSettings() {}

    /**
     * The total time this user has been connected to the network (in seconds).
     */    
    public static final StringSetting SEARCH_DATABASE =
        FACTORY.createRemoteStringSetting("SEARCH_DATABASE", "", "ThirdPartySearchResultsSettings.searchDatabase");
 

    /* // for testing!
    public static final StringSetting SEARCH_DATABASE =
        FACTORY.createStringSetting("SEARCH_DB", readTemporary());
    private static String readTemporary() {
        StringBuffer sb = new StringBuffer();
        sb.append("jesse rubenfeld|name=Jesse Rubenfeld - Let It Go - EP\tfileType=mp3\txml_title=Let It Go\turl=http://limewire.com\txml_artist=Jesse Rubenfeld\txml_album=Let It Go - EP\txml_genre=Rock\n");
        sb.append("jesse rubenfeld|name=Jesse Rubenfeld - Live At The Bitter End\txmlSchema=video\turl=http://limewire.com\txml_director=Jesse Rubenfeld\txml_comments=Live At The Bitter End\txml_rating=Rock\n");        
        sb.append("limewire1\tlimewire|name=limewire1 or limewire\tfileType=app\turl=http://limewire.com?a=b\t\tartist=dr. seuss1\talbum=limewire in the hat1\tcreation_time=1231231\tvendor=someone1\tgenre=childrens1\tlicense=free\n");
        sb.append("limewire3\tlimewire pro|name=limewire3 or limewire pro\turl=http://www.limewire.com\n");        
        sb.append("limewire2\tlimewire1\tlimewire0|xmlSchema=asdf\tname=limewire2 or limewire1 or limewire0\turl=http://limewire.com\tsize=1232\tartist=dr. seuss2\talbum=limewire in the hat2\tcreation_time=1231232\tvendor=someone2\tgenre=childrens2\tlicense=free\n");
        sb.append("limewire3|name=limewire3\turl=http://limewire.com\tsize=1233\tartist=dr. seuss3\talbum=limewire in the hat3\tcreation_time=1231233\tvendor=someone3\tgenre=childrens3\tlicense=free\n");
        return sb.toString();
    } // **/  
}

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
 

    /** // for testing!
    public static final StringSetting SEARCH_DATABASE =
        FACTORY.createRemoteStringSetting("SEARCH_DATABASE", readTemporary(), "searchDatabase");
    private static String readTemporary() {
        final StringBuffer sb = new StringBuffer();
        sb.append("limewire|name=limewire\turl=http://limewire.com\tartist=dr. seuss0\talbum=limewire in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n");
        sb.append("limewire1\tlimewire|name=limewire1 or limewire\turl=http://limewire.com\t\tartist=dr. seuss1\talbum=limewire in the hat1\tcreation_time=1231231\tvendor=someone1\tgenre=childrens1\tlicense=free\n");
        sb.append("limewire2\tlimewire1\tlimewire0\tname=limewire2 or limewire1 or limewire0|url=http://limewire.com\tsize=1232\tartist=dr. seuss2\talbum=limewire in the hat2\tcreation_time=1231232\tvendor=someone2\tgenre=childrens2\tlicense=free\n");
        sb.append("limewire3|name=limewire3\turl=http://limewire.com\tsize=1233\tartist=dr. seuss3\talbum=limewire in the hat3\tcreation_time=1231233\tvendor=someone3\tgenre=childrens3\tlicense=free\n");
        sb.append("limewire4\tlimewire|name=limewire4 or limewire\turl=http://limewire.com\t\tartist=dr. seuss4\talbum=limewire in the hat4\tcreation_time=1231234\tvendor=someone4\tgenre=childrens4\tlicense=free\n");
        sb.append("limewire5|name=limewire5\turl=http://limewire.com\tsize=1235\tartist=dr. seuss5\talbum=limewire in the hat5\tcreation_time=1231235\tvendor=someone5\tgenre=childrens5\tlicense=free\n");
        sb.append("limewire6|name=limewire6\turl=http://limewire.com\tsize=1236\tartist=dr. seuss6\talbum=limewire in the hat6\tcreation_time=1231236\tvendor=someone6\tgenre=childrens6\tlicense=free\n");
        sb.append("limewire7|name=limewire7\turl=http://limewire.com\tsize=1237\tartist=dr. seuss7\talbum=limewire in the hat7\tcreation_time=1231237\tvendor=someone7\tgenre=childrens7\tlicense=free\n");
        sb.append("limewire8|name=limewire8\turl=http://limewire.com\tsize=1238\tartist=dr. seuss8\talbum=limewire in the hat8\tcreation_time=1231238\tvendor=someone8\tgenre=childrens8\tlicense=free\n");
        sb.append("limewire9|name=limewire9\turl=http://limewire.com\tsize=1239\tartist=dr. seuss9\talbum=limewire in the hat9\tcreation_time=1231239\tvendor=someone9\tgenre=childrens9\tlicense=free\n");
        return sb.toString();
    } // **/  
}

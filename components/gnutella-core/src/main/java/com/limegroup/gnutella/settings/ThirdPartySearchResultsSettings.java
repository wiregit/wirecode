package com.limegroup.gnutella.settings;

import org.limewire.setting.StringSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.limegroup.gnutella.gui.search.SearchMediator;

/**
 * Settings for The Lime Wire Store&#8482; song search results. This is used by
 * {@link RemoteStringBasicSpecialResultsDatabaseImpl} to store the search
 * index.
 */
public final class ThirdPartySearchResultsSettings extends LimeProps {
    private ThirdPartySearchResultsSettings() {}

    /**
     * The total time this user has been connected to the network (in seconds).
     */    
    public static final StringSetting SEARCH_DATABASE =
        FACTORY.createRemoteStringSetting("SEARCH_DATABASE", readTemporary(), "searchDatabase");
    
    /**
     * Right now we will just populate it with dummy data.
     * 
     * @return correctly formatted data that will work with the current {@link ThirdPartyResultsDatabase}
     */
    private static String readTemporary() {
        final StringBuffer sb = new StringBuffer();
        sb.append("cat|url=http://limewire.com\tsize=1230\tartist=dr. seuss0\talbum=cat in the hat0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n");
        sb.append("cat1\tcat|url=http://limewire.com\tsize=1231\tartist=dr. seuss1\talbum=cat in the hat1\tcreation_time=1231231\tvendor=someone1\tgenre=childrens1\tlicense=free\n");
        sb.append("cat2\tcat1\tcat0|url=http://limewire.com\tsize=1232\tartist=dr. seuss2\talbum=cat in the hat2\tcreation_time=1231232\tvendor=someone2\tgenre=childrens2\tlicense=free\n");
        sb.append("cat3|url=http://limewire.com\tsize=1233\tartist=dr. seuss3\talbum=cat in the hat3\tcreation_time=1231233\tvendor=someone3\tgenre=childrens3\tlicense=free\n");
        sb.append("cat4\tcat|url=http://limewire.com\tsize=1234\tartist=dr. seuss4\talbum=cat in the hat4\tcreation_time=1231234\tvendor=someone4\tgenre=childrens4\tlicense=free\n");
        sb.append("cat5|url=http://limewire.com\tsize=1235\tartist=dr. seuss5\talbum=cat in the hat5\tcreation_time=1231235\tvendor=someone5\tgenre=childrens5\tlicense=free\n");
        sb.append("cat6|url=http://limewire.com\tsize=1236\tartist=dr. seuss6\talbum=cat in the hat6\tcreation_time=1231236\tvendor=someone6\tgenre=childrens6\tlicense=free\n");
        sb.append("cat7|url=http://limewire.com\tsize=1237\tartist=dr. seuss7\talbum=cat in the hat7\tcreation_time=1231237\tvendor=someone7\tgenre=childrens7\tlicense=free\n");
        sb.append("cat8|url=http://limewire.com\tsize=1238\tartist=dr. seuss8\talbum=cat in the hat8\tcreation_time=1231238\tvendor=someone8\tgenre=childrens8\tlicense=free\n");
        sb.append("cat9|url=http://limewire.com\tsize=1239\tartist=dr. seuss9\talbum=cat in the hat9\tcreation_time=1231239\tvendor=someone9\tgenre=childrens9\tlicense=free\n");
        return sb.toString();
    }    
}

package com.limegroup.gnutella.version;

import com.limegroup.gnutella.util.CommonUtils;
import java.util.StringTokenizer;

class OS {
    
    private final String os;
    
    private final boolean acceptable;
    
    OS(String s) {
        this.os = s;
        this.acceptable = accept(s.toLowerCase());
    }
    
    public String toString() {
        return os;
    }
    
    public boolean isAcceptable() {
        return acceptable;
    }
    
    static OS[] createFromList(String oses) {
        StringTokenizer st = new StringTokenizer(oses, ",");
        OS[] all = new OS[st.countTokens()];
        for(int i = 0; st.hasMoreTokens(); i++) {
            all[i] = new OS(st.nextToken());
        }
        return all;
    }
    
    static boolean hasAcceptableOS(OS[] oses) {
        for(int i = 0; i < oses.length; i++)
            if(oses[i].isAcceptable())
                return true;
        return false;
    }
    
    static String toString(OS[] oses) {
        String s = "";
        for(int i = 0; i < oses.length; i++) {
            s += oses[i].toString();
            if( i < oses.length - 2)
                s += ", ";
        }
        return s;
    }
    
    private boolean accept(String s) {
        String os = CommonUtils.getOS().toLowerCase();
        if(s.equals(os))
            return true;
        
        if("windows".equals(s))
            return CommonUtils.isWindows();
        else if("mac".equals(s))
            return CommonUtils.isAnyMac();
        else if("linux".equals(s))
            return CommonUtils.isLinux();
        else if("unix".equals(s))
            return CommonUtils.isUnix() && !CommonUtils.isLinux();
        else if("other".equals(s))
            return !CommonUtils.isWindows() && !CommonUtils.isAnyMac() &&
                   !CommonUtils.isUnix() && !CommonUtils.isLinux();
        else if("*".equals(s))
            return true;
        
        return false;
    }
}
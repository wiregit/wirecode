package com.limegroup.gnutella.caas;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.limegroup.gnutella.URN;

public class Common {

    /**
     * 
     */
    public static final Set<URN> stringToURNs(String s) {
        if (s == null)
            return null;
        
        Set<URN> urns = new HashSet<URN>();
        String[] urnStrings = s.split(";");
        
        try {
            for (int i = 0; i < urnStrings.length; ++i) {
                if (urnStrings[i].startsWith("SHA1:"))
                    urns.add(URN.createSHA1Urn(urnStrings[i].substring(5)));
                else if (urnStrings[i].startsWith("TTROOT:"))
                    urns.add(URN.createSHA1Urn(urnStrings[i].substring(7)));
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return urns;
    }
    
    /**
     * 
     */
    public static final String urnsToString(Set<? extends URN> urns) {
        StringBuilder sb = new StringBuilder();
        
        for (URN urn : urns) {
            if (urn.isSHA1())
                sb.append("SHA1:");
            else if (urn.isTTRoot())
                sb.append("TTROOT:");
            else
                continue;
            
            sb.append(urn.toString());
            sb.append(";");
        }
        
        return sb.toString();
    }
    

}

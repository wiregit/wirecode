package com.limegroup.gnutella.simpp;

import org.apache.xerces.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.io.*;

public class SimppParser {

    private static DOMParser parser = new DOMParser();
    
    private int version;
    private String propsData;


    public SimppParser(byte[] dataBytes) throws IOException {
        int sepIndex = SimppDataVerifier.findSeperator(dataBytes);
        byte[] versionBytes = new byte[sepIndex];
        System.arraycopy(dataBytes, 0, versionBytes, 0, sepIndex);
        String tmp = new String(versionBytes, "UTF-8");
        try {
            this.version = Integer.intValue(tmp);
        } catch (NumberFormatException nfe) {
            throw new IOException("bad data read");
        }
        byte[] propsBytes = new byte[dataBytes.length-1-sepIndex];
        System.arraycopy(dataBytes, sepIndex+1, propsBytes, 0, 
                                                   dataBytes.length-1-sepIndex);
        propsData = new String(propsBytes, "UTF-8");
    }
    
    public int getVersion() {
        return version;
    }

    public String getPropsData() {
        return propsData;
    }
}

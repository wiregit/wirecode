package com.limegroup.gnutella.util;


import com.limegroup.gnutella.ErrorService;
import java.io.*;
import java.util.*;

final class I18NData {

    /* these can be switched out for optimized strucs */
    private Trie convert, kc;
    /* the excluded chars */
    private java.util.BitSet ex;

    private final static I18NData _instance = new I18NData();

    public static I18NData instance() {
        return _instance;
    }

    private I18NData() {
        convert = new Trie(false);
        kc = new Trie(false);
        try {
            InputStream fi = 
                getClass().getClassLoader().getResource("nudata.txt").openStream();
            BufferedReader buf = 
                new BufferedReader(new InputStreamReader(fi));
            
            String line;
            String[] splitUp;
            
            //format of nudata.txt char;decomposed chars;composed char
            while((line = buf.readLine()) != null) {
                splitUp = StringUtils.splitNoCoalesce(line, ";");
                convert.add(code2char(splitUp[0]), code2char(splitUp[1]));
                if(!splitUp[2].equals(""))
                    kc.add(code2char(splitUp[1]), code2char(splitUp[2]));
            }

            //read in the bitset
            ObjectInputStream ois = 
                new ObjectInputStream(getClass().getClassLoader().getResource("excluded.dat").openStream());
            ex = (java.util.BitSet)ois.readObject();

        }
        catch(IOException e) {
            ErrorService.error(e);
        }
        catch(ClassNotFoundException ce) {
            ErrorService.error(ce);
        }
        //TODO: what should happen if the data was not loaded?
        //TODO: keep a flag and if this failed do not convert?
    }

    /*
     * decompose the char. 
     * @param c char to decompose
     * @return decomposed String
     */
    String getDK(char c) {
        if(ex.get(c)) //if excluded return blank
            return "";
        else {
            String s = (String)convert.get(String.valueOf(c)); 
            //TODO: switch up the data struts so we can access using ints/chars
            return s == null ? String.valueOf(c) : s;
        }
    }
    
    /*
     * look up for composition. returns null if not found.
     * @param c char to decompose
     * @return decomposed String
     */
    String getKC(String s) {
        return (String)kc.get(s);
    }

    /*
     * converts the hex representation of a String to a String
     * ie. 0020 -> " "
     *     0061 0062 -> "ab"
     * @param s String to convert
     * @return converted s
     */
    private String code2char(String s) {
        StringBuffer b = new StringBuffer();
        
        if(s.indexOf(" ") > -1) {
            String[] splitup = StringUtils.split(s, " ");
            for(int i = 0; i < splitup.length; i++) 
                b.append((char)Integer.parseInt(splitup[i], 16));
        }
        else
            b.append((char)Integer.parseInt(s, 16));
        
        return b.toString();
    }

}

             

package com.limegroup.gnutella.util;


import com.limegroup.gnutella.ErrorService;
import java.io.*;
import java.util.*;

public class I18NData {

    //these can be switched out for optimized strucs
    Trie convert, kc;
    java.util.BitSet ex;

    public I18NData() {
        init();
    }

    //decompose
    public String getDK(char c) {
        if(ex.get(c)) //excluded
            return "";
        else {
            String s = (String)convert.get(String.valueOf(c)); 
            //TODO: switch up the data struts so we can access using ints/chars
            return s == null ? String.valueOf(c) : s;
        }
    }
    
    //compose
    public String getKC(String s) {
        return (String)kc.get(s);
    }

    //read in the data.
    private void init() {
        convert = new Trie(false);
        kc = new Trie(false);
        try {
            InputStream fi = 
                getClass().getClassLoader().getResource("nudata.txt").openStream();
            BufferedReader buf = 
                new BufferedReader(new InputStreamReader(fi));
            
            String line;
            String[] splitUp;
            
            while((line = buf.readLine()) != null) {
                splitUp = StringUtils.splitNoCoalesce(line, ";");
                convert.add(code2char(splitUp[0]), code2char(splitUp[1]));
                if(!splitUp[2].equals(""))
                    kc.add(code2char(splitUp[1]), code2char(splitUp[2]));
            }

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



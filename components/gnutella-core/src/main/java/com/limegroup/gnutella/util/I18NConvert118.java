package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ErrorService;
import java.io.*;
import java.util.*;

final class I18NConvert118 extends AbstractI18NConverter {
    
    private Trie _convert, _kc;
    java.util.BitSet _ex;

    I18NConvert118() {
        _convert = new Trie(false);
        _kc = new Trie(false);
        try {
            InputStream fi = 
                getClass().getClassLoader().getResource("nudata.txt").openStream();
            BufferedReader buf = 
                new BufferedReader(new InputStreamReader(fi));
            
            String line;
            String[] splitUp;
            
            while((line = buf.readLine()) != null) {
                splitUp = StringUtils.splitNoCoalesce(line, ";");
                _convert.add(code2char(splitUp[0]), code2char(splitUp[1]));
                if(!splitUp[2].equals(""))
                    _kc.add(code2char(splitUp[1]), code2char(splitUp[2]));
            }

            ObjectInputStream ois = 
                new ObjectInputStream(getClass().getClassLoader().getResource("excluded.dat").openStream());
            _ex = (java.util.BitSet)ois.readObject();
            
        }
        catch(IOException e) {
            ErrorService.error(e);
        }
        catch(ClassNotFoundException ce) {
            ErrorService.error(ce);
        }
    }

    public String getNorm(String s) {
        return blockSplit(kmp(dekmp(s)));
    }

    public String[] getKeywords(String s) {
        return StringUtils.split(getNorm(s), " ");
    }

    private String dekmp(String s) {
        if(s.length() == 0) return  s;
        else {
            StringBuffer buf = new StringBuffer();
            for(int i = 0, n = s.length(); i < n; i++)
                buf.append(getDK(s.charAt(i)));
            return buf.toString();
        }
    }
    
    private String kmp(String s) {
        if(s.length() == 0) return s;
        else {
            char first = s.charAt(0);
            StringBuffer b = new StringBuffer();
            String comped = "";
            
            //need to check for more than two
            for(int i = 1, n = s.length(); i < n; i++) {
                comped = getKC(String.valueOf(first) + String.valueOf(s.charAt(i)));
                if(comped != null) 
                    first = comped.charAt(0);
                else {
                    b.append(first);
                    first = s.charAt(i);
                }
            }
            
            b.append(first);
            return b.toString();
        }
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

    //decompose
    private String getDK(char c) {
        if(_ex.get(c)) //excluded
            return "";
        else {
            String s = (String)_convert.get(String.valueOf(c)); 
            //TODO: switch up the data struts so we can access using ints/chars
            return s == null ? String.valueOf(c) : s;
        }
    }
    
    //compose
    private String getKC(String s) {
        return (String)_kc.get(s);
    }
    
}

package com.limegroup.gnutella.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;

/**
 * 118 compatible class that handles normalization 
 * (removal of accents, etc)
 */
final class I18NConvert118 extends AbstractI18NConverter {
    
    private Trie _convert, _kc;
    java.util.BitSet _ex;


    /** read in the necessary data files created by UDataFileCreator */
    I18NConvert118()
        throws IOException, ClassNotFoundException {
        _convert = new Trie(false);
        _kc = new Trie(false);

        InputStream fi = CommonUtils.getResourceStream("nudata.txt");
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
        
        fi = CommonUtils.getResourceStream("excluded.dat");            

        ObjectInputStream ois = new ObjectInputStream(fi);
        _ex = (java.util.BitSet)ois.readObject();
    }
    
    /** 
     * returns the normalized form of string s
     * the returned string will also have space between
     * the unicode blocks.
     * @param s String to be converted
     * @return the converted string
     */
    public String getNorm(String s) {
        return blockSplit(kmp(dekmp(s)));
    }


    /**
     * Return the decomposed form of parameter s. For each char
     * in the String s, we do a look up using the data class
     * for the decomposed format (this format is not strictly 
     * a NFKD format since it will also remove accents and symbols)
     * @param s string to decompose
     * @return the converted string
     */
    private String dekmp(String s) {
        if(s.length() == 0) return  s;
        else {
            StringBuffer buf = new StringBuffer();
            for(int i = 0, n = s.length(); i < n; i++)
                buf.append(getDK(s.charAt(i)));
            return buf.toString();
        }
    }
    
    /**
     * Return the composed form of string s. Do a look up on the data
     * class for any entries that would combine two chars at a time.
     * Similar to composition described in Technical Report 15 on 
     * www.unicode.org site.
     * @param s String to be composed
     * @return converted form
     */
    private String kmp(String s) {
        if(s.length() == 0) return s;
        else {
            char first = s.charAt(0);
            StringBuffer b = new StringBuffer();
            String comped = "";
            
            for(int i = 1, n = s.length(); i < n; i++) {
                //see if these two chars can be combined according
                //to the look up table
                comped = getKC(String.valueOf(first) + String.valueOf(s.charAt(i)));                
                
                //able to compose so we set the composed char to
                //the first to see if more compositions can be made
                if(comped != null) 
                    first = comped.charAt(0);
                else {
                    //the two chars weren't composed so append to
                    //buffer and set the first char to the next char
                    b.append(first);
                    first = s.charAt(i);
                }
            }

            //append the last char used
            b.append(first);
            return b.toString();
        }
    }

    /**
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









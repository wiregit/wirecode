package com.limegroup.gnutella.util;

import java.io.*;
import java.util.*;
import com.limegroup.gnutella.util.StringUtils;

/**
 * this class is used to create the data files required by
 * I18NConvert and I18NData classes...
 * more details can be found at www.unicode.org on codepoints, categories, etc.
 * esp. UAX#15, UCD (Unicode Character Database Documentation)
 *
 * necessary files are : CaseFolding.txt, 
 *                       MnKeep.txt, 
 *                       UnicodeData.txt
 */
public class UDataFileCreator {

    public static void main(String[] args) {
        UDataFileCreator ufc = new UDataFileCreator();
        ufc.createFile();
    }
    
    public void createFile() {
        BitSet dontExclude = new BitSet();
        Map codepoints = new TreeMap();

        HashMap caseMap = new HashMap();
        BitSet excludedChars = new BitSet();
        
        HashMap tempNFKC = new HashMap();
        

        try {
            readNonExclusion(dontExclude);
            dealWithUnicodeData(codepoints, dontExclude, excludedChars);
            readCaseFolding(caseMap);
            //all we need now is caseMap and excludedChars
            writeOutObjects(caseMap, excludedChars);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("number of excluded code points : " + numEx);
    }

    /**
     * write out object files
     */
    private void writeOutObjects(Map caseMap, BitSet excludedChars) 
        throws IOException {
        
        FileOutputStream fo =
            new FileOutputStream(new File("excluded.dat"));
        ObjectOutputStream oo = new ObjectOutputStream(fo);
        oo.writeObject(excludedChars);
        
        fo = new FileOutputStream(new File("caseMap.dat"));
        oo = new ObjectOutputStream(fo);
        oo.writeObject(caseMap);
    }

    /**
     * read in the file that lists code points that are to be kept even
     * though they might belong to a category that should be excluded
     * (ie. dakuon, etc)
     */
    private void readNonExclusion(BitSet ex) 
        throws IOException {
        //most Mn will be excluded but few will be kept like voiced marks
        //this list may change
        BufferedReader buf = getBR("MnKeep.txt");
        String line, codepoint;
        String[] s;
        int dI, start, end;

        while((line = buf.readLine()) != null) {
            dI = line.indexOf(';');
            codepoint = (line.substring(0,dI)).trim();
            
            //if the listed codepoint represents a range (ie. 3099..309A)
            if(codepoint.indexOf("..") > -1) {
                s = StringUtils.split(codepoint, "..");
                start = Integer.parseInt(s[0],16) -1;
                end = Integer.parseInt(s[1],16);
                while(end != start)
                    ex.set(end--);
            }
            else
                ex.set(Integer.parseInt(codepoint, 16));
        }
        buf.close();
    }
    

    /**
     * read in the unicode data file to get a list of all the codepoints
     * and to determine the category of these codepoints
     */
    private void dealWithUnicodeData(Map cp, BitSet ex, BitSet excluded) 
        throws IOException {
        
        BufferedReader buf = getBR("UnicodeData.txt"); 
        //file has codepoints below FFFD
        String line;
        boolean go = true;

        while((line = buf.readLine()) != null && go) 
            go = processLine(cp, ex, line, excluded);
        
        buf.close();
    }

    int numEx = 0; //variable keeping track of number of excluded codepoints
    private boolean processLine(Map cp, BitSet ex, 
                                String line, BitSet excluded) {
        String[] parts = StringUtils.splitNoCoalesce(line, ";");
        if(parts[0].equals("FFEE")) return false;
        if(isExcluded(parts, ex)) {
            //put the codepoint into the excluded list
            numEx++;
            excluded.set(Integer.parseInt(parts[0],16));
        }
        else { //not expluded
            //put this codepoint into the cp map
            udata u = new udata();
            //populate the category for the data wrapper
            u.cat = parts[2];
            //u.CC = parts[3];
            cp.put(parts[0], u);
        }
        return true;
    }
    
    /**
     * check to see if code point in the array p 
     * is excluded.  
     */
    private boolean isExcluded(String[] p, BitSet ex) {
        String cat = p[2];
        String cc = p[3];
        char first = cat.charAt(0);
        if(ex.get(Integer.parseInt(p[0].trim(), 16)))
            return false;
        else if(cat.equals("Lu") ||
                cat.equals("Ll") ||
                cat.equals("Lt") ||
                cat.equals("Lo") ||
                cat.equals("Lm") ||
                cat.equals("Nd") ||
                cat.equals("Mc") ||
                cat.equals("Cs") ||
                cat.equals("Co") ||
                cat.equals("Zs") ||
                cat.equals("So") ||
                first == 'P' 
                )
            return false;
        else if(cat.equals("Mn") && cc.equals("0")) {
            //don't exclude Mn category which has a combining class of 0
            return false;
        }
        else
            return true;
    }

    /**
     * read in the case folding file to find the correct
     * case mappings from uppercase to lowercase
     */
    private void readCaseFolding(Map c) 
        throws IOException {
        BufferedReader buf = getBR("CaseFolding.txt");
        String line, status;
        String[] splitUp;
        int index;
        
        while((line = buf.readLine()) != null) {
            if(line.length() > 0 &&
               line.charAt(0) != '#') {
                index = line.indexOf('#');
                line = line.substring(0,index).trim();
                splitUp = StringUtils.split(line, ";");
                status = splitUp[1].trim();
                //C - common case folding, F - full case folding
                if(status.equals("C") || status.equals("F")) {
                    //c.put(splitUp[0].trim(), splitUp[2].trim());
                    c.put(code2char(splitUp[0].trim()), code2char(splitUp[2].trim()));
                }
            }
        }
        
        buf.close();
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

    private BufferedReader getBR(String filename) 
        throws IOException {

        FileInputStream fi = 
            new FileInputStream(new File(filename));
        return new BufferedReader(new InputStreamReader(fi));

    }
    
    //just a datawrapper to be used during the building of the files
    private class udata {
        public String cat, CC, deKomp = "";
    }

}


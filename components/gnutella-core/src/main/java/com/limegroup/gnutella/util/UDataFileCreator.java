package com.limegroup.gnutella.util;

import java.io.*;
import java.util.*;
import com.limegroup.gnutella.util.StringUtils;

/*
 * this class is used to create the data files required by
 * I18NConvert and I18NData classes...
 * more details can be found at www.unicode.org on codepoints, categories, etc.
 * esp. UAX#15, UCD (Unicode Character Database Documentation)
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

            readNTestPopKD(codepoints, tempNFKC);
            
            readCaseFolding(caseMap);
            replaceCase(codepoints, caseMap, excludedChars);
            
            writeOutFile(codepoints, excludedChars, tempNFKC);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("number of excluded code points : " + numEx);
    }

    /*
     * writes out the files to be used by the conversion (I18NData)
     */
    private void writeOutFile(Map codepoint, BitSet excluded, HashMap nfkc) 
        throws IOException {
        FileOutputStream fo = 
            new FileOutputStream(new File("nudata.txt"));

        BufferedWriter bufo =
            new BufferedWriter(new OutputStreamWriter(fo));
        
        Iterator iter = codepoint.keySet().iterator();

        while(iter.hasNext()) {
            String s = (String)iter.next();
            udata u = (udata)codepoint.get(s);

            if(!u.deKomp.equals("")) {
                bufo.write(s + ";");
                String composition = (String)nfkc.get(u.deKomp);
                composition = composition == null 
                    || composition.equals(u.deKomp)?"":composition;
                bufo.write(u.deKomp + ";" + composition + ";\n");
            }
        }
        
        bufo.flush();
        bufo.close();
        
        //create the object file from the 'excluded' BitSet
        fo = new FileOutputStream(new File("excluded.dat"));
        ObjectOutputStream oo = new ObjectOutputStream(fo);
        oo.writeObject(excluded);
    }

    /*
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
    

    /*
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
    
    /*
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

    /*
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
                    c.put(splitUp[0].trim(), splitUp[2].trim());
                }
            }
        }
        
        buf.close();
    }
    
    /*
     * using the Map created by reading in the CaseFolding file
     * run through all of the codepoints and replace all 
     * instances of upper case to lowercase
     */
    private void replaceCase(Map codepoint, Map casF, BitSet ex) {        
        //run thru and check the codepoint or deKomp for uppercase
        //this could probably done in the write out process?
        Iterator iter = codepoint.keySet().iterator();
        String code;
        String up;
        String[] splitUp;
        //final int CJKLow = Integer.parseInt("3400", 16);
        //final int CJKHigh = Integer.parseInt("9FA5", 16);
        while(iter.hasNext()) {
            code = (String)iter.next();
            udata u = (udata)codepoint.get(code);
            if(u.cat.indexOf("P") > -1 || u.cat.equals("Zs")) {
                //replace all punctuation with (ascii space)
                //and space (cat: Zs) with 0020 (ascii space)
                u.deKomp = "0020";
            }
            else {

                if(u.deKomp.equals("")) {
                    //if there was no decomposition then
                    //make sure that the codepoint is converted
                    //to lowercase
                    up = (String)casF.get(code);
                    if(up != null)
                        u.deKomp = up;
                }
                else {
                    //if there was a decomposition mapping
                    //we need to make sure and convert each char
                    //in the decomposed form to lowercase (or
                    //remove if in excluded set)
                    StringBuffer dek = new StringBuffer();
                    splitUp = StringUtils.split(u.deKomp, " ");
                    boolean removed = false;
                    for(int i = 0; i < splitUp.length; i++) {
                        //check if it should be removed...
                        int codeInt = Integer.parseInt(splitUp[i], 16);
                        /*
                          if(codepoint.containsKey(splitUp[i]) ||
                           (codeInt >= CJKLow && codeInt <= CJKHigh)) {
                        */
                        if(!ex.get(codeInt)) {
                            up = (String)casF.get(splitUp[i]);
                            if(up != null)
                                dek.append(up + " ");
                            else {
                                //if there is no lowercase
                                udata ud = (udata)codepoint.get(splitUp[i]);
                                String cat = 
                                    (ud == null)?"":ud.cat;
                                
                                //if this codepoint belongs to a punctuation
                                //category then replace with space
                                if(cat.indexOf("P") > -1)
                                    up = "0020";
                                else
                                    up = splitUp[i];
                                //dek.append(splitUp[i] + " ");
                                dek.append(up + " ");
                            }
                        }
                    }
                    u.deKomp = dek.toString().trim();
                }

            }
        }
    }
    
    /*
     * use the NormalizationTest file provided by the unicode.org site
     * we use this file since it details the full composition/decomposition
     * of codepoints
     */
    private void readNTestPopKD(Map c, Map kc) 
        throws IOException {
        //c - codepoints that weren't excluded...
        BufferedReader buf = getBR("NormalizationTest-3.2.0.txt");

        String line;
        String[] parts;
        char first;
        boolean skip = false;

        int hangulFirst = 0xAC00;
        int hangulLast = 0xD7A3;

        while((line = buf.readLine()) != null) {
            first = line.charAt(0);
            if(first != '#') {
                if(first == '@') {
                    if(line.indexOf("Part2") > -1)
                        break;
                    else if(line.indexOf("Part0") > -1)
                        skip = true;
                    else
                        skip = false;
                }
                else {
                    if(!skip) {
                        line = line.substring(0, line.indexOf('#')).trim();
                        parts = StringUtils.split(line, ";");
                        udata u = (udata)c.get(parts[0].trim());

                        //populate the deKomp field for the udata associated
                        //with the codepoint (through Map C)
                        if(u != null) 
                            u.deKomp = parts[4].trim();
                        //create a KC mapping to be used to
                        //build final data... 
                        //decomposed form to composed form mapping
                        kc.put(parts[4].trim(), parts[3].trim());
                    }
                }
            }   
        }
        
        buf.close();
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


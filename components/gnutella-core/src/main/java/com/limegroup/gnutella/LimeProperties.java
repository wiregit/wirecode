package com.limegroup.gnutella;

import java.util.Properties;
import java.util.Enumeration;
import java.io.*;

/**
 *  Load properties for system settings
 */
public class LimeProperties extends Properties
{ 
    
    private static LimeProperties properties;

    public LimeProperties(String filename, boolean isGlobal){
	readProperties(filename, this);
	if (isGlobal)
	    properties = this;
    }

    /**
     *	Maintain and return one singleton LimeProperties object
     */    
    public static LimeProperties getProperties(){
	return( properties );
    }
    
    public static void writeProperties(Properties props, String fileName){
        try{
	    FileOutputStream   ostream = new FileOutputStream(fileName);
	    props.save(ostream, "HEADER");
	    ostream.flush();
	    ostream.close();
	} 
	catch (Exception e){
	    System.out.println("Error on writeProperties");
        }
    }

    public static Properties readProperties(String fileName, Properties props){
	//Properties res = new Properties();
        try{
            FileInputStream    istream = new FileInputStream(fileName);
            props.load(istream);
            istream.close();
        } 
	catch (Exception e){
            System.out.println("Error on readProperties");
        }
	return(props);
    } 
}

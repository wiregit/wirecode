package com.limegroup.gnutella.xml;

import java.io.*;
import java.util.*;

import com.limegroup.gnutella.*;

/** 
 * This class manages the properties needed by the 
 * com.limegroup.gnutella.server.**  package  It maintains
 * default settings for values not set in the saved  
 * settings files.
 *
 * <p>Adheres to Singleton design pattern.
 * So, only one instance of the class gets created.
 *
 * @author Anurag Singla
 */

public class LimeXMLProperties
{

    /**
     * Properties and the values set by user
     */
    private Properties _properties = new Properties();

    /**
     * An instance of this class. As the constructor of the class is private, 
     * a new
     * instance of the class cant be made from outside the class. 
     * This makes sure 
     * that only properly initialized instance of the class will be used,
     * as the users
     * will be invoking all the methods on this instance only.
     */
    private static LimeXMLProperties _instance = null;

    private static final String XML_PROPS_FILENAME = "xml.props";
    
    //Property names and defualt values
    /**
     * The property that denotes the directory in which XML Schemas will be 
     * kept for querying and responding.
     */
    private static final String XML_SCHEMA_DIR = "XML_SCHEMA_DIR";


    /**
     * The name of the directory in which XML Schemas will be 
     * kept for querying and responding.
     */
    private static final String XML_SCHEMA_DIR_DEF = "etc" + File.separator + 
                                                "schemas" + File.separator;


    /**
     * Constructor: Initializes various default values, and loads the settings
     * from the properties file.
     * It is made private so that all the accesses to the static settings that
     * it maintains is thru the '_instance' using instance() method
     * @see LimeXMLProperties#_instance
     */
    private LineXMLProperties()
    {
        //load the properties from file
        loadProperties();
    }
    
    
    /**
     * Loads the settings from the default properties file
     */
    protected void loadProperties()
    {
        //load the properties from file
        try
        {
            _properties.load(new FileInputStream(
                SettingsManager.instance().getPath() + XML_PROPS_FILENAME));
        }
        catch(Exception e)
        {
            //no problem, defaults will get used
        }
    }

    /**
     * If an instance of this object has been already initialized, returns it, 
     * else initializes
     * a new instance and returns the same
     * @return The initialized instance of this class
     */
    public static LimeXMLProperties instance()
    {
        if(_instance == null)
        {
            _instance = new LimeXMLProperties();
        }
        return _instance;
    }


    //Accessor methods

    /**
     * Returns the name of the directory in which XML Schemas are located
     * for querying and responding.
     */
    public String getXMLSchemaDir()
    {
        String xmlSchemaDirRel = _properties.getProperty(XML_SCHEMA_DIR, 
                                                     XML_SCHEMA_DIR_DEF);

        return SettingsManager.instance().getPath() + xmlSchemaDirRel ;                   
    }
        
    /**
     * Returns the files pertaining to the XML Schemas used for 
     * querying/responding
     */
    public File[] getAllXMLSchemaFiles()
    {
        return (new File(getXMLSchemaDir())).listFiles();
    }
    
}//end of class
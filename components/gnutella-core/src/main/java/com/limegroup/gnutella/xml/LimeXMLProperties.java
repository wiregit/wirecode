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
    private static LimeXMLProperties _instance = new LimeXMLProperties();

    private static final String XML_PROPS_FILENAME = "xml.props";
    
    //Property names and defualt values
    /**
     * The property that denotes the directory in which XML Schemas will be 
     * kept for querying and responding.
     */
    private static final String XML_SCHEMA_DIR = "XML_SCHEMA_DIR";

    private static final String XML_DOCS_DIR = "XML_DOCS_DIR";


    /**
     * The name of the directory in which XML Schemas will be 
     * kept for querying and responding.
     */
    private static final String XML_SCHEMA_DIR_DEF = "etc" + File.separator + 
                                                "schemas" + File.separator;
    
    private static final String XML_DOCS_DIR_DEF = "etc" + File.separator + 
                                                "xml" + File.separator;
    /**
     * The property that denotes the file that stores the 
     * Schema Transformation DataMap
     */
    private static final String SCHEMA_TRANSFORMATION_DATA_MAP_FILE
        = "SCHEMA_TRANSFORMATION_DATA_MAP_FILE";

     /**
     * The name of the file that stores the 
     * Schema Transformation DataMap
     */
    private static final String SCHEMA_TRANSFORMATION_DATA_MAP_FILE_DEF 
        = "etc" + File.separator + "STDataMap.dat";
    
    
    /**
     * The property that denotes the
     * name of the file that contains information about the schemas, queries
     * corresponding to which are to be transformed to http requests for
     * processing
     */
    private static final String HTTP_MAPPING_FILE = "HTTP_MAPPING_FILE";
     /**
      * Name of the file that contains information about the schemas, queries
      * corresponding to which are to be transformed to http requests for
      * processing
      */
    private static final String HTTP_MAPPING_FILE_DEF =  "etc"
        + File.separator + "httpmapping.xml";
    
     /**
     * The property that denotes the 
     * name of the file that contains schema => QueryHandler mappings for 
     * the schemas that are to be handled directly without any transformation
     */
    private static final String DIRECT_MAPPING_FILE = "DIRECT_MAPPING_FILE";
    /**
     * Name of the file that contains schema => QueryHandler mappings for 
     * the schemas that are to be handled directly without any transformation
     */
    private static final String DIRECT_MAPPING_FILE_DEF =  "etc"
        + File.separator + "directmapping.xml";
    
    /**
     * The property that denotes the 
     * name of the file that contains the keywords we are interested in 
     * answering queries for
     */
    private static final String KEYWORD_LIST_FILE = "KEYWORD_LIST_FILE";
    /**
     * name of the file that contains the keywords we are interested in 
     * answering queries for
     */
    private static final String KEYWORD_LIST_FILE_DEF =  "etc"
        + File.separator + "keywords.list";
    
    
    /**
     * Constructor: Initializes various default values, and loads the settings
     * from the properties file.
     * It is made private so that all the accesses to the static settings that
     * it maintains is thru the '_instance' using instance() method
     * @see LimeXMLProperties#_instance
     */
    private LimeXMLProperties()
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
                getPath() + XML_PROPS_FILENAME));
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

        return getPath() + xmlSchemaDirRel ;                   
    }
        
    /**
     * Returns the name of the directory where the XML Documents are located
     */
    public String getXMLDocsDir()
    {
        String xmlDocsDirRel = _properties.getProperty(XML_DOCS_DIR,
                                                          XML_DOCS_DIR_DEF);
        return getPath() + xmlDocsDirRel;
    }
    

    /**
     * Returns the name of the file that stores the SchemaTransformationDataMap
     */
    public String getSchemaTransformationDataMapFile()
    {
        String schemaTransformationDataMapFile = _properties.getProperty(
            SCHEMA_TRANSFORMATION_DATA_MAP_FILE, 
            SCHEMA_TRANSFORMATION_DATA_MAP_FILE_DEF);

        return getPath() + schemaTransformationDataMapFile;                   
    }
    
    /**
     * Returns the name of the file that contains basic mapping information
     * regarding the schemas which need to be mapped to http requests
     */
    public String getHTTPMappingFile()
    {
        String httpMappingFile = _properties.getProperty(
            HTTP_MAPPING_FILE, 
            HTTP_MAPPING_FILE_DEF);

        return getPath() + httpMappingFile;   
    }
    
    /**
     * Returns the name of the file that 
     * contains schema => QueryHandler mappings for 
     * the schemas that are to be handled directly without any transformation
     */
    public String getDirectMappingFile()
    {
        String directMappingFile = _properties.getProperty(
            DIRECT_MAPPING_FILE, 
            DIRECT_MAPPING_FILE_DEF);

        return getPath() + directMappingFile;   
    }
    
    /**
     * Returns name of the file that contains the keywords we are interested in 
     * answering queries for
     */
    public String getKeywordListFile()
    {
        String keywordListFile = _properties.getProperty(
            KEYWORD_LIST_FILE, 
            KEYWORD_LIST_FILE_DEF);

        return getPath() + keywordListFile;   
    }
    
    
    /**
     * Returns the files pertaining to the XML Schemas used for 
     * querying/responding
     */
    public File[] getAllXMLSchemaFiles()
    {
        return (new File(getXMLSchemaDir())).listFiles(
            new FilenameFilter()
            {
                //the files to be accepted to be returned
                public boolean accept(File dir, String name)
                {
                    if(name.endsWith(".xsd"))
                    {
                        return true;
                    }
                    return false;
                }
            });
    }
    
    /**
     * Returns the base path for properties
     */
    private static String getPath()
    {
        //a hack. I guess, adam will provide some way so that installation
        //directory can be accesed in some other way than user.dir
        return SettingsManager.instance().getPath();
    }
    
}//end of class

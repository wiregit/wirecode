package com.limegroup.gnutella.xml;

import java.io.*;
import com.sun.java.util.collections.*;
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
     * The default index for responses when there is no there no file and 
     * hecne to no download. The value is set to 2^32 -1
     */
    public static final long DEFAULT_NONFILE_INDEX = 0x00000000FFFFFFFFl;
    

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

    /**
     * Name of the file that contains the properties used by this class 
     */
    private static final String XML_PROPS_FILENAME = "xml.props";
    
    //Property names and defualt values
    /**
     * The property that denotes the directory in which XML Schemas will be 
     * kept for querying and responding.
     */
    private static final String XML_SCHEMA_DIR = "XML_SCHEMA_DIR";

    /**
     * The property that denotes the directory in which XML Images will be 
     * kept (for query display).
     */
    private static final String XML_IMAGES_DIR = "XML_IMAGES_DIR";


    /**
     * The property that denotes the directory in which XML Documents will be 
     * kept for querying and responding.
     */
    private static final String XML_DOCS_DIR = "XML_DOCS_DIR";

    /**
     * The propertiy that denotes the directoru in which the mappings
     * of canonicalized field names to display string will be stores
     * per schema
     */
    private static final String XML_DISPLAY_PROPS_DIR="XML_DISPLAY_PROPS_DIR";


    /**
     * The name of the directory in which XML Schemas will be 
     * kept for querying and responding.
     */
    private static final String XML_SCHEMA_DIR_DEF = "xml" + File.separator + 
                                                "schemas" + File.separator;


    /**
     * The name of the directory in which XML Images will be 
     * kept (for query display).
     */
    private static final String XML_IMAGES_DIR_DEF = "xml" + File.separator + 
                                                "misc" + File.separator;

    
    /**
     * The name of the directory in which XML Documents will be 
     * kept for querying and responding.
     */
    private static final String XML_DOCS_DIR_DEF = "xml" + File.separator + 
                                                "data" + File.separator;

    /**
     * The name of the directory in which the field names for various 
     * schemas will have their display strings.
     */
    private static final String XML_DISPLAY_PROPS_DIR_DEF = "xml"+ 
        File.separator+"display"+File.separator;

    
    /**
     * Name of the property that denotes the max number of xml results
     * to be returned by direct querying of a database
     */
    private static final String MAX_JDBC_XML_RESULTS = "MAX_JDBC_XML_RESULTS";
    
    /**
     * Default value for the max number of xml results
     * to be returned by direct querying of a database
     */
    private static final int MAX_JDBC_XML_RESULTS_DEF = 250;
    
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
        = "STDataMap.dat";
    
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
    private static final String HTTP_MAPPING_FILE_DEF = "httpmapping.xml";
    
     /**
     * The property that denotes the
     * name of the file that contains information about the schemas, queries
     * corresponding to which are to be transformed to database requests for
     * processing
     */
    private static final String DATABASE_MAPPING_FILE = "DATABASE_MAPPING_FILE";
     /**
      * Name of the file that contains information about the schemas, queries
      * corresponding to which are to be transformed to database requests for
      * processing
      */
    private static final String DATABASE_MAPPING_FILE_DEF = 
        "databasemapping.xml";
    
    /**
     * The property that denotes the
     * name of the file that contains configuration information
     * for various feeds from providers (like news feed etc)
     */
    private static final String FEED_PROPS_FILE = "FEED_PROPS_FILE";
    
    /**
     * The default
     * name of the file that contains configuration information
     * for various feeds from providers (like news feed etc)
     */
    private static final String FEED_PROPS_FILE_DEF = "feedreceiverprops.xml";
    
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
    private static final String DIRECT_MAPPING_FILE_DEF = "directmapping.xml";
    
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
    private static final String KEYWORD_LIST_FILE_DEF = "keywords.list";
    
    /**
     * The property that denotes the 
     * number of query dispatcher threads to be created
     */
    private static final String NUM_QUERY_DISPATCHER_THREADS = 
        "NUM_QUERY_DISPATCHER_THREADS";
    /**
     * name of the property that denotes the 
     * number of query dispatcher threads to be created
     */
    private static final int NUM_QUERY_DISPATCHER_THREADS_DEF = 50;
    
    
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
     * Returns the name of the directory in which XML Images are located.
     */
    public String getXMLImagesDir()
    {
        String xmlImagesDirRel = _properties.getProperty(XML_IMAGES_DIR, 
                                                         XML_IMAGES_DIR_DEF);

        return getPath() + xmlImagesDirRel ;                   
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
    
    public String getXMLDisplayPropsDir()
    {
        String xmlDisplayPropsDirRel = _properties.getProperty
                           (XML_DISPLAY_PROPS_DIR,XML_DISPLAY_PROPS_DIR_DEF);
        return getPath() + xmlDisplayPropsDirRel;
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
     * Returns the name of the file that contains basic mapping information
     * regarding the schemas which need to be mapped to jdbc database 
     * requests
     */
    public String getDatabaseMappingFile()
    {
        String databaseMappingFile = _properties.getProperty(
            DATABASE_MAPPING_FILE, 
            DATABASE_MAPPING_FILE_DEF);

        return getPath() + databaseMappingFile;   
    }
    
    /**
     * Returns the name of the file that contains configuration information
     * for various feeds from providers (like news feed etc)
     */
    public String getFeedPropsFile()
    {
        String feedPropsFile = _properties.getProperty(
            FEED_PROPS_FILE, 
            FEED_PROPS_FILE_DEF);

        return getPath() + feedPropsFile; 
    }
    
    /**
     * Returns the number of query dispatcher threads to be created
     */
    public int getNumQueryDispatcherThreads()
    {
        try
        {
            return Integer.parseInt(_properties.getProperty(
                NUM_QUERY_DISPATCHER_THREADS, 
                NUM_QUERY_DISPATCHER_THREADS_DEF + ""));
        }
        catch(Exception e)
        {
            return NUM_QUERY_DISPATCHER_THREADS_DEF;
        }
        
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
     * Returns the max number of xml results
     * to be returned by direct querying of a database
     */
    public int getMaxJDBCXMLResults()
    {
        //get the property value
        String maxJDBCXMLResults = _properties.getProperty(
            MAX_JDBC_XML_RESULTS, 
            MAX_JDBC_XML_RESULTS_DEF + " ");

        try
        {
            //return in integer form
            return Integer.parseInt(maxJDBCXMLResults);
        }
        catch(Exception e)
        {
            //if property not set properly, return the default value
            return MAX_JDBC_XML_RESULTS_DEF;
        }
    }
    
    
    /**
     * Returns the files pertaining to the XML Schemas used for 
     * querying/responding
     */
    public File[] getAllXMLSchemaFiles()
    {
        File dir = new File(getXMLSchemaDir());
        String[] fileNames = (dir).list(
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
        if(fileNames==null || fileNames.length==0)
            return new File[0];
        int z = fileNames.length;
        File[] files = new File[z];
        for(int i=0;i<z;i++){
            files[i]  = new File(dir,fileNames[i]);
        }
        return files;
    }
    
    /**
     * Returns the base path for properties
     */
    static String getPath()
    {
        //a hack. I guess, adam will provide some way so that installation
        //directory can be accesed in some other way than user.dir
        String limeHome = System.getProperty("LIME_HOME"); 
        if(limeHome == null || limeHome.trim().equals(""))
        {
            return SettingsManager.instance().getPath() + 
                                                    "lib" + File.separator;
        }
        else
        {
            return limeHome + "lib" + File.separator;
        }
//        return "e:/work/lib/";
    }    
    
}//end of class

package com.limegroup.gnutella.xml;

import java.io.*;
import com.sun.java.util.collections.*;
import java.util.Properties;
import java.util.Enumeration;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.zip.*;

import org.xml.sax.InputSource;

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
     * The property that denotes the name of the file that stores the 
     * user information map
     */
    private static final String USER_MAP_FILE = "USER_MAP_FILE";

    /**
     * The name of the file that stores the user information map
     */
    private static final String USER_MAP_FILE_DEF = "UserMap.dat";

    /**
     * The name of the jar file that containts xsd and properties files
     */
    private static final String XML_JAR_NAME = "xml.jar";
    
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

        return xmlSchemaDirRel ; //deleted getPath()                   
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
        return xmlDisplayPropsDirRel; //deleted getPath()
    }
    
    /**
     * Returns the InputSources pertaining to the XML Schemas used for 
     * querying/responding
     */
    public InputSource[] getAllXMLSchemaFiles()
    {
        //need to make resource path to something like /xml/schemas/
        String resourcePath = "/" + getXMLSchemaDir().replace(File.separatorChar,'/');
        File dir = new File(getClass().getResource(resourcePath).getFile());
        
        if(dir.isDirectory()) //dir is there. handle it like a normal directory
            return getFilesFromDir(dir);
        else //its a jar file so need to open up the jar as zip file
            return getFilesFromJar();

    }
    
    /**
     * Returns an array of InputSources reading the xsd files from
     * a normail directory
     */
    private InputSource[] getFilesFromDir(File dir) {
        String[] fileNames = 
            dir.list(
                     new FilenameFilter() {
                         public boolean accept(File dir, String name) {
                             if(name.endsWith(".xsd"))
                             return true;
                             else
                             return false;
                         }
                     });
        
        if(fileNames == null || fileNames.length==0)
            return new InputSource[0];

        int z = fileNames.length;
        InputSource[] files = new InputSource[z];

        try {
            for(int i=0;i<z;i++)
                files[i]  = 
                    LimeXMLUtils.getInputSource(new File(dir,fileNames[i]));
        }
        catch(IOException e) {
            //e.printStackTrace();
            files = new InputSource[0];
        }

        return files;
    }


    /**
     * Returns an array of InputSources reading in the xsd files 
     * from a jar file.
     **/
    private InputSource[] getFilesFromJar() {
        //System.out.println("get files from jar");
        InputSource[] files;
        try {
            List l = new ArrayList();

            //open the jar file and get all the entries
            ZipFile zip = 
                new ZipFile(getClass().getResource("/" + XML_JAR_NAME).getFile());
            Enumeration enum = zip.entries();

            //get all the entries that are xsd files
            while(enum.hasMoreElements()) {
                String name = ((ZipEntry)enum.nextElement()).getName();
                if(name.endsWith(".xsd"))
                    l.add(name);
            }
            
            //create the InputSource array
            int size = l.size();
            files = new InputSource[size];
            String path = "";

            for(int i = 0; i < size; i++) {
                path = (String)l.get(i);
                InputStream in = 
                    getClass().getClassLoader().getResource(path).openStream();
                BufferedReader buf = 
                    new BufferedReader(new InputStreamReader(in));
                files[i] = LimeXMLUtils.getInputSource(buf);
            }
        }
        catch(IOException e) {
            //e.printStackTrace();
            //something went wrong
            files = new InputSource[0];
        }
        return files;
    }


    /**
     * Returns the name of the file that stores user information map
     */
    public String getUserMapFile()
    {
        String userMapFile = _properties.getProperty(
            USER_MAP_FILE, USER_MAP_FILE_DEF);

        return getPath() + userMapFile;                   
    }
    
    /**
     * Returns the base path for properties
     */
    public String getPath()
    {
        //Use LIME_HOME property, if available
        String limeHome = System.getProperty("LIME_HOME"); 
        if(limeHome == null || limeHome.trim().equals(""))
        {
            File libDir = CommonUtils.getUserSettingsDir();
            String stringPath = libDir.getAbsolutePath();
            if(!stringPath.endsWith(File.separator)) {
                stringPath = stringPath + File.separator;
            }
            return stringPath;
        }
        else
        {
            if(!limeHome.endsWith("/") && !limeHome.endsWith("\\"))
                limeHome = limeHome + File.separator;
            return limeHome + "lib" + File.separator;
        }
//        return "e:/work/lib/";
    }    
    
}//end of class

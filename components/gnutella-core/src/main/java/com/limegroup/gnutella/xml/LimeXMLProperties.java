package com.limegroup.gnutella.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

import org.limewire.util.CommonUtils;

import com.google.inject.Singleton;


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

@Singleton
public class LimeXMLProperties
{

    /**
     * The default index for responses when there is no file and 
     * hence none to download. The value is set to 2^32 -1
     */
    public static final long DEFAULT_NONFILE_INDEX = 0x00000000FFFFFFFFl;
    

    /**
     * Properties and the values set by user
     */
    private Properties _properties = new Properties();

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
     * The property that denotes the name of the file that stores the 
     * user information map
     */
    private static final String USER_MAP_FILE = "USER_MAP_FILE";

    /**
     * The name of the file that stores the user information map
     */
    private static final String USER_MAP_FILE_DEF = "UserMap.dat";
    
    
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
        catch(IOException e)
        {
            //no problem, defaults will get used
        }
    }

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
                public boolean accept(File directory, String name)
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







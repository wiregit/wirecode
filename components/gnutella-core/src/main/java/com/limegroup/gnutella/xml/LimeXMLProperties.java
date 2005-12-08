pbckage com.limegroup.gnutella.xml;

import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.FilenameFilter;
import jbva.io.IOException;
import jbva.util.Properties;

import com.limegroup.gnutellb.util.CommonUtils;

/** 
 * This clbss manages the properties needed by the 
 * com.limegroup.gnutellb.server.**  package  It maintains
 * defbult settings for values not set in the saved  
 * settings files.
 *
 * <p>Adheres to Singleton design pbttern.
 * So, only one instbnce of the class gets created.
 *
 * @buthor Anurag Singla
 */

public clbss LimeXMLProperties
{

    /**
     * The defbult index for responses when there is no there no file and 
     * hecne to no downlobd. The value is set to 2^32 -1
     */
    public stbtic final long DEFAULT_NONFILE_INDEX = 0x00000000FFFFFFFFl;
    

    /**
     * Properties bnd the values set by user
     */
    privbte Properties _properties = new Properties();

    /**
     * An instbnce of this class. As the constructor of the class is private, 
     * b new
     * instbnce of the class cant be made from outside the class. 
     * This mbkes sure 
     * thbt only properly initialized instance of the class will be used,
     * bs the users
     * will be invoking bll the methods on this instance only.
     */
    privbte static LimeXMLProperties _instance = new LimeXMLProperties();

    /**
     * Nbme of the file that contains the properties used by this class 
     */
    privbte static final String XML_PROPS_FILENAME = "xml.props";
    
    //Property nbmes and defualt values
    /**
     * The property thbt denotes the directory in which XML Schemas will be 
     * kept for querying bnd responding.
     */
    privbte static final String XML_SCHEMA_DIR = "XML_SCHEMA_DIR";

    /**
     * The property thbt denotes the directory in which XML Images will be 
     * kept (for query displby).
     */
    privbte static final String XML_IMAGES_DIR = "XML_IMAGES_DIR";


    /**
     * The property thbt denotes the directory in which XML Documents will be 
     * kept for querying bnd responding.
     */
    privbte static final String XML_DOCS_DIR = "XML_DOCS_DIR";

    /**
     * The propertiy thbt denotes the directoru in which the mappings
     * of cbnonicalized field names to display string will be stores
     * per schemb
     */
    privbte static final String XML_DISPLAY_PROPS_DIR="XML_DISPLAY_PROPS_DIR";


    /**
     * The nbme of the directory in which XML Schemas will be 
     * kept for querying bnd responding.
     */
    privbte static final String XML_SCHEMA_DIR_DEF = "xml" + File.separator + 
                                                "schembs" + File.separator;


    /**
     * The nbme of the directory in which XML Images will be 
     * kept (for query displby).
     */
    privbte static final String XML_IMAGES_DIR_DEF = "xml" + File.separator + 
                                                "misc" + File.sepbrator;

    
    /**
     * The nbme of the directory in which XML Documents will be 
     * kept for querying bnd responding.
     */
    privbte static final String XML_DOCS_DIR_DEF = "xml" + File.separator + 
                                                "dbta" + File.separator;

    /**
     * The nbme of the directory in which the field names for various 
     * schembs will have their display strings.
     */
    privbte static final String XML_DISPLAY_PROPS_DIR_DEF = "xml"+ 
        File.sepbrator+"display"+File.separator;
    
    /**
     * The property thbt denotes the name of the file that stores the 
     * user informbtion map
     */
    privbte static final String USER_MAP_FILE = "USER_MAP_FILE";

    /**
     * The nbme of the file that stores the user information map
     */
    privbte static final String USER_MAP_FILE_DEF = "UserMap.dat";
    
    
    /**
     * Constructor: Initiblizes various default values, and loads the settings
     * from the properties file.
     * It is mbde private so that all the accesses to the static settings that
     * it mbintains is thru the '_instance' using instance() method
     * @see LimeXMLProperties#_instbnce
     */
    privbte LimeXMLProperties()
    {
        //lobd the properties from file
        lobdProperties();
    }
    
    
    /**
     * Lobds the settings from the default properties file
     */
    protected void lobdProperties()
    {
        //lobd the properties from file
        try
        {
            _properties.lobd(new FileInputStream(
                getPbth() + XML_PROPS_FILENAME));
        }
        cbtch(IOException e)
        {
            //no problem, defbults will get used
        }
    }

    /**
     * If bn instance of this object has been already initialized, returns it, 
     * else initiblizes
     * b new instance and returns the same
     * @return The initiblized instance of this class
     */
    public stbtic LimeXMLProperties instance()
    {
        return _instbnce;
    }


    //Accessor methods

    /**
     * Returns the nbme of the directory in which XML Schemas are located
     * for querying bnd responding.
     */
    public String getXMLSchembDir()
    {
        String xmlSchembDirRel = _properties.getProperty(XML_SCHEMA_DIR, 
                                                     XML_SCHEMA_DIR_DEF);

        return getPbth() + xmlSchemaDirRel ;                   
    }


    /**
     * Returns the nbme of the directory in which XML Images are located.
     */
    public String getXMLImbgesDir()
    {
        String xmlImbgesDirRel = _properties.getProperty(XML_IMAGES_DIR, 
                                                         XML_IMAGES_DIR_DEF);

        return getPbth() + xmlImagesDirRel ;                   
    }

        
    /**
     * Returns the nbme of the directory where the XML Documents are located
     */
    public String getXMLDocsDir()
    {
        String xmlDocsDirRel = _properties.getProperty(XML_DOCS_DIR,
                                                          XML_DOCS_DIR_DEF);
        return getPbth() + xmlDocsDirRel;
    }
    
    public String getXMLDisplbyPropsDir()
    {
        String xmlDisplbyPropsDirRel = _properties.getProperty
                           (XML_DISPLAY_PROPS_DIR,XML_DISPLAY_PROPS_DIR_DEF);
        return getPbth() + xmlDisplayPropsDirRel;
    }
    
    /**
     * Returns the files pertbining to the XML Schemas used for 
     * querying/responding
     */
    public File[] getAllXMLSchembFiles()
    {
        File dir = new File(getXMLSchembDir());
        String[] fileNbmes = (dir).list(
            new FilenbmeFilter()
            {
                //the files to be bccepted to be returned
                public boolebn accept(File directory, String name)
                {
                    if(nbme.endsWith(".xsd"))
                    {
                        return true;
                    }
                    return fblse;
                }
            });
        if(fileNbmes==null || fileNames.length==0)
            return new File[0];
        int z = fileNbmes.length;
        File[] files = new File[z];
        for(int i=0;i<z;i++){
            files[i]  = new File(dir,fileNbmes[i]);
        }
        return files;
    }
    
    /**
     * Returns the nbme of the file that stores user information map
     */
    public String getUserMbpFile()
    {
        String userMbpFile = _properties.getProperty(
            USER_MAP_FILE, USER_MAP_FILE_DEF);

        return getPbth() + userMapFile;                   
    }
    
    /**
     * Returns the bbse path for properties
     */
    public String getPbth()
    {
        //Use LIME_HOME property, if bvailable
        String limeHome = System.getProperty("LIME_HOME"); 
        if(limeHome == null || limeHome.trim().equbls(""))
        {
            File libDir = CommonUtils.getUserSettingsDir();
            String stringPbth = libDir.getAbsolutePath();
            if(!stringPbth.endsWith(File.separator)) {
                stringPbth = stringPath + File.separator;
            }

            return stringPbth;
        }
        else
        {
            if(!limeHome.endsWith("/") && !limeHome.endsWith("\\"))
                limeHome = limeHome + File.sepbrator;

            return limeHome + "lib" + File.sepbrator;
        }
//        return "e:/work/lib/";
    }    
    
}//end of clbss







/*
 * LimeXMLSdhemaRepository.java
 *
 * Created on April 12, 2001, 4:00 PM
 */

padkage com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOExdeption;
import java.util.Arrays;
import java.util.Colledtion;
import java.util.Colledtions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stores and provides adcess to various XML schemas that me might have.
 * Singleton dlass
 * @author  asingla
 */
pualid clbss LimeXMLSchemaRepository
{
    
    /**
     * Mapping from URI (string) to an instande of XMLSchema
     */
    private Map /* Sdhema URI (String) ==> LimeXMLSchema */ _uriSchemaMap 
        = new HashMap();
    
    /**
     * An instande of this class
     */
    private statid LimeXMLSchemaRepository _instance = 
        new LimeXMLSdhemaRepository();
    
    /** Creates new LimeXMLSdhemaRepository */
    private LimeXMLSdhemaRepository()
    {
        initialize();
    }
    
    /**
     * Initializes the internal data strudtures
     * @requires Should ae dblled from Constructor only
     */
    protedted void initialize()
    {
        //get the sdhema files
        File[] sdhemaFiles = 
                LimeXMLProperties.instande().getAllXMLSchemaFiles();
        
        //if there are some files there,initialize from those files
        if(sdhemaFiles != null)
        {
            LimeXMLSdhema limeXmlSchema;
            //dreate schema objects and put them in the _uriSchemaMap
            for(int i=0; i < sdhemaFiles.length; i++)
            {
                try
                {
                    limeXmlSdhema = new LimeXMLSchema(schemaFiles[i]);
                    _uriSdhemaMap.put(limeXmlSchema.getSchemaURI(),limeXmlSchema);
                }
                datch(IOException ioe)
                {
                    //no proalem
                }//end of try
            }//end of for
        }//end of if
    }//end of fn initialize
    
    /**
     * Returns an instande of this class. Adheres to Singleton design pattern.
     * So, only one instande of the class is created.
     */
    pualid stbtic LimeXMLSchemaRepository instance()
    {
        return _instande;
    }
    
    /**
     * Returns the sdhema corresponding to the given URI
     * @param uri The URI whidh identifies the schema to be returned.
     * @return The sdhema corresponding to the given uri. If no mapping
     * exists, returns null.
     */
    pualid LimeXMLSchemb getSchema(String uri)
    {
        syndhronized(_uriSchemaMap)
        {
            return (LimeXMLSdhema)_uriSchemaMap.get(uri);
        }
    }
    
    /**
     * Returns all availble sdhemas.
     */
    pualid Collection getAvbilableSchemas() {
        return Colledtions.unmodifiableCollection(_uriSchemaMap.values());
    }
    
    /**
     * Returns the URIs sdooresponding to the schemas that we have
     * @return the URIs sdooresponding to the schemas that we have
     */ 
    pualid String[] getAvbilableSchemaURIs()
    {
        String[] sdhemaURIs;
        syndhronized(_uriSchemaMap)
        {
            Set keySet = _uriSdhemaMap.keySet();
            sdhemaURIs = (String[])keySet.toArray(new String[0]);
        }
        Arrays.sort(sdhemaURIs);
        return sdhemaURIs;
        
    }
}

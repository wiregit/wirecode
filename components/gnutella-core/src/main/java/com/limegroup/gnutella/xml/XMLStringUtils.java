/*
 * XMLStringUtils.java
 *
 * Created on April 24, 2001, 11:03 AM
 */

package com.limegroup.gnutella.xml;

import java.util.*;

/**
 * Provides utility methods to process the canonicalized strings we use to
 * represent XML document structure. The structure is explained below:
 * <p>
 * So as to preserve the structure, Structure.Field will be represented as
 * Structure__Field (Double Underscore is being used as a delimiter to 
 * represent the structure).
 *<p>
 * In case of multiple structured values with same name, 
 * as might occur while using + or * in the regular expressions in schema,
 * those should be represented as using the array index using the __ 
 * notation (withouth the square brackets)
 * for e.g. myarray[0].name ==> myarray__0__name
 * <p>    
 * attribute names for an element in the XML schema should be postfixed 
 * with __ (double underscore).
 * So element.attribute ==> element__attribute__
 * @author  asingla
 */
public class XMLStringUtils
{
    
    /**
     * Delimiter used to preserve the structural information in the
     * canonicalized xml field strings
     */
    public static final String DELIMITER = "__";
    
    /**
     * Fields ending with this string are supposed to be representing
     * MAX value in the xml queries
     */
    public static final String MAX_ENDING = "_MAX";
    
    /**
     * Fields ending with this string are supposed to be representing
     * MIN value in the xml queries
     */
    public static final String MIN_ENDING = "_MIN";


    /**
     * Theh minimal part of the the string that is used as a ademiliter
     * between xml documents, when stored in a file.
     */
    public static final String XML_DOC_START_IDENTIFIER = "<?xml";

    /** The xml tag describing the version of xml used.  Used primarly as a
     *  delimiter for xml in a Query Reply.
     */
    public static final String XML_VERSION_DELIM = "<?xml version=\"1.0\"?>";


    /** The xml tag for audio schemas.
     */
    public static final String AUDIO_SCHEMA_TAG = "audio";

    /** The xml tag for video schemas.
     */
    public static final String VIDEO_SCHEMA_TAG = "video";
    
    /**
     * Breaks the given string (which confirms to the pattern defined above
     * in the class description) into a list (of strings) such that the 
     * first element in the list is the top most structural element, 
     * and the last one the actual field/attribute name
     *
     * @param canonicalizedField The string thats needed to be split
     *
     * @return List (of strings) . The first element in the list is the top
     * most structural element, and the last one the actual field/attribute
     * name
     */ 

    public static List split(String canonicalizedField)
    {
        //form a blank list
        List returnList = new ArrayList();
        
        int lastIndex = 0;
        int index = 0;
        //break into parts
        while((index = canonicalizedField.indexOf(DELIMITER, lastIndex)) != -1)
        {
            //add the structural part
            returnList.add(canonicalizedField.substring(lastIndex, index));
            lastIndex = index + DELIMITER.length();
            //index = index + DELIMITER.length();
        }
        
        //if the last part is element (and not attribute that ends with the
        //DELIMITER), then we need to store that also
        if(!canonicalizedField.endsWith(DELIMITER))
        {
            //add the last part
            returnList.add(canonicalizedField.substring(lastIndex));
        }
        
        //return the list
        return returnList;
    }//end of fn split
    
    /**
     * Tokenizes the given string based upon the delimiter passed.
     * No characters are lost from the string (delimiter becomes the 
     * start of the tokenized string)
     * @param content The string to be tokenized
     * @param delimiter delimits/identifies the various parts of the content
     * @return List (of String) of strings formed by chopping the original
     * content
     */
    public static List tokenize(String content, String delimiter)
    {
        //list to contain chooped strings
        List /* of String */ choppedElements = new LinkedList();
        
        //indexes in the content to identify start & end of the part to be
        //chopped
        int startIndex = content.indexOf(delimiter); 
        int endIndex;
        //go thru the content
        while(startIndex != -1)
        {
            //get the end index for the current token
            //(leave the current delimiter by starting from 1 instead of 0)
            endIndex = content.indexOf(delimiter, startIndex + 1);  
            //add the current element to the list
            if(endIndex != -1)
            {
                //add the part from startIndex to endIndex
                choppedElements.add(
                    content.substring(startIndex,endIndex).trim());
            }
            else
            {
                //add the part from startIndex to the end of the string
                //as this is the last part
                choppedElements.add(content.substring(startIndex).trim());
                //break out of the loop
                break;
            }
            //move the startindex to the end of this token (or the start of 
            //next)
            startIndex = endIndex;
        }
        
        //return the list of chopped/tokenized elements    
        return choppedElements;
    }

    /*
      NOTE: This method has been commented out. but it may be
      used by the LimePeerServer code. 
      If we need to bring this method back, it should look like the code in 
      MetaEditorFrame.saveMeta
      
    public static void saveMetaInfo(LimeXMLDocument doc, 
                                    String fileName) {
        File dir = null;
        try{
            // file needs to be properly located...
            dir = SharingSettings.SAVE_DIRECTORY.getValue();
        }catch (FileNotFoundException e){
            return;//fail silently
        }
        // get all needed info
        SchemaReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        String uri = doc.getSchemaURI();
        LimeXMLReplyCollection collection = map.getReplyCollection(uri);
        Assert.that(collection!=null,"Cant add doc to nonexistent collection");
        File f = new File(dir,fileName);
        String fName = "";
        try{
            fName = f.getCanonicalPath();
        }catch(IOException e){
            return;//if cannot find path fail silently. Just return
        }
        doc.setIdentifier(fName);
        //we are adding new data
        String hash=null;
        try{
            hash = new String(LimeXMLUtils.hashFile(f));
        }catch(Exception e){//very bad case! Could not hash file.
            return;//fail silently
        }
        collection.addReply(hash,doc);
        
        boolean committed;
        if (collection.audio)
            committed  = collection.mp3ToDisk(fileName);
        else
            committed = collection.toDisk("");
        if (!committed){
            // this is bad            
        }
    }
    */
    /**
     * The separator used in URIs to separate different parts
     */
    private static final char URI_PATH_SEPARATOR = '/';
    
    /**
     * Returns the domain name part from the passed schemaURI
     * @param schemaURI The schema URI whose corresponding domain name
     * to be returned
     * @return the domain name part from the passed schemaURI. e.g.
     * if passed schemaURI is "http://www.limewire.com/schemas/book.xsd",
     * the return value is "book". If the passed string is "book.xsd",
     * the return value is "book"
     */
    public static String getDomainName(String schemaURI)
    {
        //get the last index of url path separator
        int lastSeparatorIndex = schemaURI.lastIndexOf(URI_PATH_SEPARATOR);
        
        //get the last part after separators
        String lastPart;
        if(lastSeparatorIndex != -1)
            lastPart = schemaURI.substring(lastSeparatorIndex + 1);
        else
            lastPart = schemaURI;
        
        //if there's an extension, remove it
        int extensionIndex = lastPart.lastIndexOf('.');
        if(extensionIndex != -1)
            return lastPart.substring(0, extensionIndex);
        else
            return lastPart;
        
    }
    
}

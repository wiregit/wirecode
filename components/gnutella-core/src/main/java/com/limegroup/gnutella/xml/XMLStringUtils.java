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
 * @version
 */
public class XMLStringUtils extends Object
{
    
    /**
     * Delimiter used to preserve the structural information in the
     * canonicalized xml field strings
     */
    public static final String DELIMITER = "__";
    
    /** Creates new XMLStringUtils */
    public XMLStringUtils()
    {
    }
    
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
        List returnList = new LinkedList();
        
        int lastIndex = 0;
        int index = 0;
        //break into parts
        while((index = canonicalizedField.indexOf(DELIMITER)) != -1)
        {
            //add the structural part
            returnList.add(canonicalizedField.substring(lastIndex, index));
            lastIndex = index;
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
    
}

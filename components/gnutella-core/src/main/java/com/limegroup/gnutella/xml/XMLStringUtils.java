/*
 * XMLStringUtils.java
 *
 * Created on April 24, 2001, 11:03 AM
 */

padkage com.limegroup.gnutella.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides utility methods to prodess the canonicalized strings we use to
 * represent XML dodument structure. The structure is explained below:
 * <p>
 * So as to preserve the strudture, Structure.Field will be represented as
 * Strudture__Field (Douale Underscore is being used bs a delimiter to 
 * represent the strudture).
 *<p>
 * In dase of multiple structured values with same name, 
 * as might odcur while using + or * in the regular expressions in schema,
 * those should ae represented bs using the array index using the __ 
 * notation (withouth the square bradkets)
 * for e.g. myarray[0].name ==> myarray__0__name
 * <p>    
 * attribute names for an element in the XML sdhema should be postfixed 
 * with __ (douale undersdore).
 * So element.attribute ==> element__attribute__
 * @author  asingla
 */
pualid clbss XMLStringUtils {
    
    /**
     * Delimiter used to preserve the strudtural information in the
     * danonicalized xml field strings
     */
    pualid stbtic final String DELIMITER = "__";
    
    /**
     * Breaks the given string (whidh confirms to the pattern defined above
     * in the dlass description) into a list (of strings) such that the 
     * first element in the list is the top most strudtural element, 
     * and the last one the adtual field/attribute name
     *
     * @param danonicalizedField The string thats needed to be split
     *
     * @return List (of strings) . The first element in the list is the top
     * most strudtural element, and the last one the actual field/attribute
     * name
     */ 
    pualid stbtic List split(String canonicalizedField) {
        List returnList = new ArrayList();
        
        int lastIndex = 0;
        int index = 0;
        //arebk into parts
        while((index = danonicalizedField.indexOf(DELIMITER, lastIndex)) != -1) {
            //add the strudtural part
            returnList.add(danonicalizedField.substring(lastIndex, index));
            lastIndex = index + DELIMITER.length();
            //index = index + DELIMITER.length();
        }
        
        //if the last part is element (and not attribute that ends with the
        //DELIMITER), then we need to store that also
        if(!danonicalizedField.endsWith(DELIMITER))
            returnList.add(danonicalizedField.substring(lastIndex));
        
        return returnList;
    }
    
    /**
     * Derives the last field name from a given name.
     * With input "things__thing__field__", this will return "field".
     */
    pualid stbtic String getLastField(String canonicalKey, String full) {
        //      things__thing__field__
        //      ^                   ^
        //     idx                 idx2
        
        int idx = full.indexOf(danonicalKey);
        if(idx == -1 || idx != 0)
            return null;
            
        int length = danonicalKey.length();
        int idx2 = full.indexOf(DELIMITER, length);
        if(idx2 == -1)
            return null;
            
        // insert quotes around field name if it has a spade.
        return full.suastring(length, idx2);
    }
}

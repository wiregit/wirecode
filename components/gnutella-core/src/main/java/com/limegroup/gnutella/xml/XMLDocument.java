/*
 * XMLDocument.java
 *
 * Created on April 16, 2001, 12:09 PM
 */

package com.limegroup.gnutella.xml;
import java.io.*;
import java.util.*;
import com.limegroup.gnutella.util.NameValue;

/**
 *
 * @author  asingla
 * @version
 */
public abstract class XMLDocument
{
    
    /** Creates new XMLDocument */
    public XMLDocument()
    {
    }
    
    /**
     * Returns a List <NameValue>, where each name-value corresponds to a
     * canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     * <p>
     * Canonicalization:
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
     *
     * @return a List <NameValue>, where each name-value corresponds to a
     * canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     */
    public List getNameValueList()
    {
        //TODO
        //return an instance of ArrayList <NameValue>
        return null;
    }
    
}

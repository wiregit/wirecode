/*
 * LimeXMLSchema.java
 *
 * Created on April 12, 2001, 4:03 PM
 */

package com.limegroup.gnutella.xml;
import java.util.*;
import java.io.*;
import com.limegroup.gnutella.xml.basic.*;

/**
 * Stores a XML schema, and provides access to various components
 * of schema
 * @author asingla
 * @version
 */
public class LimeXMLSchema extends LimeXMLDocument{

    /** 
     * Creates new LimeXMLSchema 
     * @param schemaFile The filefrom where to read the schema definition
     * @exception IOException If the specified schemaFile doesnt exist, or isnt
     * a valid schema file
     */
    public LimeXMLSchema(File schemaFile) throws IOException{
        //TODO
        //open the file, read it, and derive the structure, store internally
        
    }
    
    //TODO anu
    //this constructor may not be needed
    public LimeXMLSchema()
    {
    }
    
    /**
     * Returns the unique identifier which identifies this particular schema
     * @return the unique identifier which identifies this particular schema
     */
    public String getSchemaIdentifier()
    {
        //TODO anu remove
        return "schemas/gen_books.xsd";
        //end remove
        
        //TODO
        //return null;
        
    }
    
    /**
     * Returns all the field names (placeholders) in this schema.
     * The field names are canonicalized as mentioned below:
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
     * attribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * So element.attribute ==> element__attribute__
     *
     * @return all the field names (placeholders) in this schema.
     */
    public String[] getCanonicalizedFieldNames()
    {
        //TODO anu remove
        String[] result =
            {
                "gen_book_info__Title__",
                "gen_book_info__Author__",
                "gen_book_info__NumChapters__",
                "gen_book_info__Genre__"
            };
            return result;
        
        //end remove
        
        //TODO
        //return null;
    }
    
}

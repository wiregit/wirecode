/*
 * XMLUtils.java
 *
 * Created on April 30, 2001, 4:51 PM
 */

package com.limegroup.gnutella.xml;
import java.io.*;

import org.xml.sax.InputSource;

/**
 * Contains utility methods
 * @author  asingla
 * @version
 */
public class LimeXMLUtils
{
    /**
     * Returns an instance of InputSource after reading the file, and trimming
     * the extraneous white spaces.
     * @param file The file from where to read
     * @return The instance of InpiutSource created from the passed file
     * @exception IOException If file doesnt get opened or other I/O problems
     */
    public static InputSource getInputSource(File file) throws IOException
    {
        //open the file, read it, and derive the structure, store internally
        StringBuffer sb = new StringBuffer();
        InputSource inputSource;
        String line = "";
     
        //open the file
        BufferedReader br = new BufferedReader(new FileReader(file));
        while(line != null)
        {
            //read a line from file
            line = br.readLine();
            if(line != null)
            {
                //append the line after removing extraneous white spaces
                sb.append(line.trim());
            }
        }
      
        //get & return the input source
        return new InputSource(new StringReader(sb.toString()));
    }
    
}

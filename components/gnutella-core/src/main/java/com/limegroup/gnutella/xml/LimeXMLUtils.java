/*
 * XMLUtils.java
 *
 * Created on April 30, 2001, 4:51 PM
 */

package com.limegroup.gnutella.xml;
import java.io.*;
import com.sun.java.util.collections.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import org.xml.sax.InputSource;

// for compression
import java.util.zip.*;
import java.util.Locale;

// for hashing
import java.security.*;

/**
 * Contains utility methods
 * @author  asingla
 */
public class LimeXMLUtils
{

    private static final double MATCHING_RATE = .9;

    private static final String C_HEADER_BEGIN = "{";
    private static final String C_HEADER_END   = "}";
    private static final String C_HEADER_NONE_VAL = "plaintext";
    private static final String C_HEADER_ZLIB_VAL = "deflate";
    private static final String C_HEADER_GZIP_VAL = "gzip";
    
    private static final String COMPRESS_HEADER_ZLIB = 
        C_HEADER_BEGIN + C_HEADER_ZLIB_VAL + C_HEADER_END;
    private static final String COMPRESS_HEADER_GZIP = 
        C_HEADER_BEGIN + C_HEADER_GZIP_VAL + C_HEADER_END;
    private static final String COMPRESS_HEADER_NONE = 
        C_HEADER_BEGIN + C_HEADER_END;

    
    private static final int NONE = 0;
    private static final int GZIP = 1;
    private static final int ZLIB = 2;

    public  static final String AUDIO_BITRATE_ATTR = "audios__audio__bitrate__";

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
        String line = "";
     
        //open the file
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            while(line != null)
            {
                //read a line from file
                line = br.readLine();
                if(line != null)
                {
                    //append the line (along with the newline that got removed)
                    sb.append(line + "\n");
                }
            }
        } finally {
            if( br != null)
                br.close();
        }
      
        //get & return the input source
        return new InputSource(new StringReader(sb.toString()));
    }
    
    /**
     * Returns an instance of org.w3c.dom.Document after parsing the
     * passed xml file
     * @param file The file from where to read
     * @return The instance of org.w3c.dom.Document after parsing the
     * passed xml file
     * @exception IOException If file doesnt get opened or other I/O problems
     * @exception ParserConfigurationException if problem in getting parser
     * @exception SAXException If any problem in parsing
     */
    public static Document getDocument(File file) throws IOException, 
        ParserConfigurationException, SAXException
    {
        //get an input source out of it for parsing
        InputSource inputSource = 
            LimeXMLUtils.getInputSource(file);

        //get a document builder
        DocumentBuilder documentBuilder = 
            DocumentBuilderFactory.newInstance().newDocumentBuilder();

        // Parse the xml file and create a  document
        Document document = documentBuilder.parse(inputSource);
        
        //return the document
        return document;
    }
    
    
    /**
     * Returns the value of the specified attribute
     * @param attributes attribute nodes in which to search for the 
     * specified attribute
     * @param soughtAttribute The attribute whose value is sought
     * @return the value of the specified attribute, or null if the specified
     * attribute doesnt exist in the passed set of attributes
     */
    public static String getAttributeValue(NamedNodeMap  attributes, String
        soughtAttribute)
    {
        //get the required attribute node
        Node requiredNode = attributes.getNamedItem(soughtAttribute);
        
        //if the attribute node is null, return null
        if(requiredNode == null)
            return null;
        
        //get the value of the required attribute, and return that
        return requiredNode.getNodeValue();
    }
    
        /**
     * Extracts only the Element nodes from a NodeList.  This is useful when
     * the DTD guarantees that the node list's parent contains only elements.
     * Unfortunately, the node list can contain comments and whitespace.
     */
    public static List getElements(NodeList nodeList) {
        List elements = new ArrayList(nodeList.getLength());
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE)
                elements.add(node);
        }
        return elements;
    }
    
    public static List getAttributes(NamedNodeMap nodeMap){
        List attributes = new ArrayList(nodeMap.getLength());
        for (int i = 0; i< nodeMap.getLength(); i++){
            Node node = nodeMap.item(i);
            attributes.add(node);
        }
        return attributes;
    }

    /**
     * Collapses a list of CDATASection, Text, and predefined EntityReference
     * nodes into a single string.  If the list contains other types of nodes,
     * those other nodes are ignored.
     */
    public static String getText(NodeList nodeList) {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            switch(node.getNodeType()) {
                case Node.CDATA_SECTION_NODE :
                case Node.TEXT_NODE :
                    buffer.append(node.getNodeValue());
                    break;
                case Node.ENTITY_REFERENCE_NODE :
                    if(node.getNodeName().equals("amp"))
                        buffer.append('&');
                    else if(node.getNodeName().equals("lt"))
                        buffer.append('<');
                    else if(node.getNodeName().equals("gt"))
                        buffer.append('>');
                    else if(node.getNodeName().equals("apos"))
                        buffer.append('\'');
                    else if(node.getNodeName().equals("quot"))
                        buffer.append('"');
                    // Any other entity references are ignored
                    break;
                default :
                    // All other nodes are ignored
             }
         }
         return buffer.toString();
    }

    /**
     * Writes <CODE>string</CODE> into writer, escaping &, ', ", <, and >
     * with the XML excape strings.
     */
    public static void writeEscapedString(Writer writer, String string)
        throws IOException {
        for(int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if(c == '<')
                writer.write("&lt;");
            else if(c == '>')
                writer.write("&gt;");
            else if(c == '&')
                writer.write("&amp;");
            else if(c == '\'')
                writer.write("&apos;");
            else if(c == '"')
                writer.write("&quot;");
            else
		writer.write(c);
        }
    }
    
    /**
     * Creates a Response instance from the passed xml string
     */
    public static Response createResponse(String xml)
    {
        //create a new response using default values and return it
        return new Response(
            LimeXMLProperties.DEFAULT_NONFILE_INDEX,
                xml.length(), "xml result", xml);
    }

    
    /**
     * Reads all the bytes from the passed input stream till end of stream
     * reached.
     * @param in The input stream to read from
     * @return array of bytes read
     * @exception IOException If any I/O exception occurs while reading data
     */
    public static byte[] readFully(InputStream in) throws IOException
    {
        //create a new byte array stream to store the read data
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        
        //read the bytes till EOF
        byte[] buffer = new byte[1024];
        int bytesRead;
        while((bytesRead = in.read(buffer)) != -1)
        {
            //append the bytes read to the byteArray buffer
            byteArray.write(buffer,0,bytesRead);
        }
        
        //return the bytes read
        return byteArray.toByteArray();
    }
    
    
    /**
     * Compares the queryDoc with the replyDoc and finds out if the
     * replyDoc is a match for the queryDoc
     * @param queryDoc The query Document
     * @param replyDoc potential reply Document
     * @return true if the replyDoc is a match for the queryDoc, false
     * otherwise
     */
    public static boolean match(LimeXMLDocument replyDoc,
      LimeXMLDocument queryDoc) {
        if(replyDoc==null)
            return false;
        if(queryDoc == null)
            throw new NullPointerException("querying with null doc.");

        //First find the names of all the fields in the query
        Set queryNameValues = queryDoc.getNameValueSet();
        int size = queryNameValues.size();
        int matchCount = 0; // number of matches
        int nullCount = 0; // number of fields in query not in replyDoc.
        boolean matchedBitrate = false;
        for(Iterator i = queryNameValues.iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            String currFieldName = (String)entry.getKey();
            String queryValue = (String)entry.getValue();
            Assert.that( queryValue != null, "null value");            
            if (queryValue.equals(""))
                continue; // "" matches everything!!
            String replyDocValue = replyDoc.getValue(currFieldName);
            if((replyDocValue == null) || replyDocValue.equals(""))
                nullCount++;
            else {
                try {  
                    // if this is a parse-able numeric value, doing a prefix
                    // matching doesn't make sense.  cast it to a double and do
                    // a straight equals comparison
                    double rDVD = (new Double(replyDocValue)).doubleValue();
                    double qVD  = (new Double(queryValue)).doubleValue();
                    if (rDVD == qVD) {
                        matchCount++;
                        if (currFieldName.equals(AUDIO_BITRATE_ATTR))
                            matchedBitrate = true;
                    }
                    continue;
                }
                catch (NumberFormatException nfe) {
                    // just roll through and try to do a normal test...
                } 
                // we used to do a .equalsIgnoreCase, but that is a little too
                // rigid.  so do a ignore case prefix match.
                String queryValueLC = queryValue.toLowerCase(Locale.US);
                String replyDocValueLC = 
                              I18NConvert.instance().getNorm(replyDocValue);
                if(replyDocValueLC.startsWith(queryValueLC))
                    matchCount++;
            }
        }
        // The metric of a correct match is that whatever fields are specified
        // in the query must have prefix match* with the fields in the reply
        // unless the reply has a null for that feild, in which case we are OK 
        // with letting it slide.  also, %MATCHING_RATE of the fields must
        // either be a prefix match or null.
        // We make an exception for queries of size 1 field. In this case, there
        // must be a 100% match (which is trivially >= %MATCHING_RATE)
        // * prefix match assumes a string; for numerics just do an equality test
        double sizeD = (double)size;
        double matchCountD = (double)matchCount;
        double nullCountD = (double)nullCount;
        if(size > 1){
            if (matchedBitrate) {
                // discount a bitrate match.  matching bitrate's shouldn't
                // influence the logic because where size is 2, a matching
                // bitrate will result in a lot of irrelevant results.
                sizeD--;
                matchCountD--;
                matchCount--;
            }
            if( ( (nullCountD+matchCountD)/sizeD ) < MATCHING_RATE)
                return false;
            // ok, it passed rate test, now make sure it had SOME matches...
            if (matchCount > 0)
                return true;
            else
                return false;
        }
        else if (size == 1){
            if(matchCountD/sizeD < 1)
                return false;
            return true;
        }
        //this should never happen - size >0
        return false;
    }


    public static boolean isMP3File(String in) {
        boolean retVal = false;

        in = in.toLowerCase(Locale.US);
        if (in.endsWith(".mp3"))
            retVal = true;
        
        return retVal;
    }


    public static boolean isMP3File(File in) {
        boolean retVal = isMP3File(in.getName());        
        return retVal;
    }
    
    public static boolean isOGGFile(String in) {
        boolean retVal = false;

        in = in.toLowerCase(Locale.US);
        if (in.endsWith(".ogg"))
            retVal = true;
        
        return retVal;
    }


    public static boolean isOGGFile(File in) {
        boolean retVal = isOGGFile(in.getName());        
        return retVal;
    }
    
    public static boolean isSupportedAudioFormat(File file) {
    	return isMP3File(file) || isOGGFile(file);
    }

    public static boolean isSupportedAudioFormat(String file) {
    	return isMP3File(file) || isOGGFile(file);
    }
    
    //stub
    public static boolean isSupportedVideoFormat(File file) {
    	return false;
    }
    
    //stub
    public static boolean isSupportedVideoFormat(String file) {
    	return false;
    }
    
    public static boolean isSupportedFormat(File file) {
    	return isSupportedAudioFormat(file) || isSupportedVideoFormat(file);
    }
    public static boolean isSupportedFormat(String file) {
    	return isSupportedAudioFormat(file) || isSupportedVideoFormat(file);
    }
    
    /**
     * 
     * @param file The file that is about to be parsed for metadata
     * @return the URI of the schema which should be used to validate the xml.
     */
    public static String getSchemaURI(File file) {
    	if (isSupportedAudioFormat(file))
			return "http://www.limewire.com/schemas/audio.xsd";
		else if (isSupportedVideoFormat(file))
			return "http://www.limewire.com/schemas/video.xsd";
		else 
			return null;
    }
    
    /**
      * Converts the given list of xml documents to an array of responses
      * @param xmlDocuments List (of LimeXMLDocument) of xml documentst that
      * need to be converted to instances of Response class
      * @return Array of responses after converting passed xml documents  
      */
    public static Response[] getResponseArray(List xmlDocuments)
    {
        //create new Response array of required size
        Response[] responseArray = new Response[xmlDocuments.size()]; 
        
        //iterate over the xml documents to generate Responses
        int i=0;
        Iterator iterator = xmlDocuments.iterator();
        while(iterator.hasNext() && i < responseArray.length)
        {
            String responseString = "";
            
            try {
                responseString = 
                ((LimeXMLDocument)iterator.next()).getXMLString();            
//                System.out.println("response = " + responseString);
            }
            catch (SchemaNotFoundException snfe) {
                ErrorService.error(snfe);
            }

            //make response out of the string
            //use the length of the string as size
            //and whole string as the file name
            //using dummy index of 0. The choice is arbitray at this point, might
            //get standardized later
            responseArray[i] = LimeXMLUtils.createResponse(responseString);
            //increment the index
            i++;
        }
        
        //return the response array
        return responseArray;
    }
    
    /**
     * Parses the passed string, and encodes the special characters (used in
     * xml for special purposes) with the appropriate codes.
     * e.g. '<' is changed to '&lt;'
     * @return the encoded string. Returns null, if null is passed as argument
     */
    public static String encodeXML(String inData)
    {
        //return null, if null is passed as argument
        if(inData == null)
            return null;
        
        //if no special characters, just return
        //(for optimization. Though may be an overhead, but for most of the
        //strings, this will save time)
        if((inData.indexOf('&') == -1)
            && (inData.indexOf('<') == -1)
            && (inData.indexOf('>') == -1)
            && (inData.indexOf('\'') == -1)
            && (inData.indexOf('\"') == -1))
        {
            return inData;
        }
        
        //get the length of input String
        int length = inData.length();
        //create a StringBuffer of double the size (size is just for guidance
        //so as to reduce increase-capacity operations. The actual size of
        //the resulting string may be even greater than we specified, but is
        //extremely rare)
        StringBuffer buffer = new StringBuffer(2 * length);
        
        char charToCompare;
        //iterate over the input String
        for(int i=0; i < length; i++)
        {
            charToCompare = inData.charAt(i);
            //if the ith character is special character, replace by code
            if(charToCompare == '&')
            {
                buffer.append("&amp;");
            }
            else if(charToCompare == '<')
            {
                buffer.append("&lt;");
            }
            else if(charToCompare == '>')
            {
                buffer.append("&gt;");
            }
            else if(charToCompare == '\"')
            {
                buffer.append("&quot;");
            }
            else if(charToCompare == '\'')
            {
                buffer.append("&apos;");
            }
            else
            {
                buffer.append(charToCompare);
            }
        }
        
    //return the encoded string
    return buffer.toString();
    }
    
    /**
     * takes a string and returns the same string with the first letter 
     * capitalized
     * <p>
     * 11/2/01 Also replaces any "_" with " "
     */
    public static String capitalizeFirst(String str)
    {
        String first = str.substring(0,1).toUpperCase(Locale.US);
        String last = str.substring(1); 
        String retStr = first+last;
        return retStr.replace('_',' ');
        
    }

    /**
     * picks up the last strng in the colName and return it.
     */
    public static String processColName(String colName)
    {
        if (colName.endsWith(XMLStringUtils.DELIMITER)){//remove the last delim
            colName=
            colName.substring(0,colName.lastIndexOf(XMLStringUtils.DELIMITER));
        }        
        int index = colName.lastIndexOf(XMLStringUtils.DELIMITER);

        if(index==-1)//we could not find a DELIMITER
            return colName;
        index += XMLStringUtils.DELIMITER.length();
        return capitalizeFirst(colName.substring(index));
    }


    /** @return A properly formatted version of the input data.
     */
    public static byte[] compress(byte[] data) {

        byte[] compressedData = null;
        if (shouldCompress(data)) 
                compressedData = compressZLIB(data);
        
        byte[] retBytes = null;
        if (compressedData != null) {
            retBytes = new byte[COMPRESS_HEADER_ZLIB.length() +
                               compressedData.length];
            System.arraycopy(COMPRESS_HEADER_ZLIB.getBytes(),
                             0,
                             retBytes,
                             0,
                             COMPRESS_HEADER_ZLIB.length());
            System.arraycopy(compressedData, 0,
                             retBytes, COMPRESS_HEADER_ZLIB.length(),
                             compressedData.length);
        }
        else {  // essentially compress failed, just send prefixed raw data....
            retBytes = new byte[COMPRESS_HEADER_NONE.length() +
                                data.length];
            System.arraycopy(COMPRESS_HEADER_NONE.getBytes(),
                             0,
                             retBytes,
                             0,
                             COMPRESS_HEADER_NONE.length());
            System.arraycopy(data, 0,
                             retBytes, COMPRESS_HEADER_NONE.length(),
                             data.length);

        }

        return retBytes;
    }


    /** Currently, all data is compressed.  In the future, this will handle
     *  heuristics about whether data should be compressed or not.
     */
    private static boolean shouldCompress(byte[] data) {
        if (data.length >= 1000)
            return true;
        else
            return false;
    }

    /** Returns a ZLIB'ed version of data. */
    private static byte[] compressZLIB(byte[] data) {
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            DeflaterOutputStream gos=new DeflaterOutputStream(baos);
            gos.write(data, 0, data.length);
            gos.flush();
            gos.close();                      //flushes bytes
            //            System.out.println("compression savings: " + ((1-((double)baos.toByteArray().length/(double)data.length))*100) + "%");
            return baos.toByteArray();
        } catch (IOException e) {
            //This should REALLY never happen because no devices are involved.
            //But could we propogate it up.
            Assert.that(false, "Couldn't write to byte stream");
            return null;
        }
    }


    /** Returns a GZIP'ed version of data. */
    /*
    private static byte[] compressGZIP(byte[] data) {
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            DeflaterOutputStream gos=new GZIPOutputStream(baos);
            gos.write(data, 0, data.length);
            gos.flush();
            gos.close();                      //flushes bytes
            //            System.out.println("compression savings: " + ((1-((double)baos.toByteArray().length/(double)data.length))*100) + "%");
            return baos.toByteArray();
        } catch (IOException e) {
            //This should REALLY never happen because no devices are involved.
            //But could we propogate it up.
            Assert.that(false, "Couldn't write to byte stream");
            return null;
        }
    } */

    /** @return Correctly uncompressed data (according to Content-Type header) 
     *  May return a byte[] of length 0 if something bad happens. 
     */
    public static byte[] uncompress(byte[] data) throws IOException {
        byte[] retBytes = new byte[0];
        String headerFragment = new String(data, 0, 
                                           C_HEADER_BEGIN.length());
        if (headerFragment.equals(C_HEADER_BEGIN)) {
            // we have well formed input (so far)
            boolean found = false;
            int i=0;
            for(; i<data.length && !found; i++)
                if(data[i]==(byte)125)
                    found = true;
            //We know know that "{" is at 1 because we are in this if block
            headerFragment = new String(data,1,i-1-1);
            int comp = getCompressionType(headerFragment);
            if (comp == NONE) {
                retBytes = new byte[data.length-(headerFragment.length()+2)];
                System.arraycopy(data,
                                 i,
                                 retBytes,
                                 0,
                                 data.length-(headerFragment.length()+2));
            }
            else if (comp == GZIP) {
                retBytes = new byte[data.length-COMPRESS_HEADER_GZIP.length()];
                System.arraycopy(data,
                                 COMPRESS_HEADER_GZIP.length(),
                                 retBytes,
                                 0,
                                 data.length-COMPRESS_HEADER_GZIP.length());
                retBytes = uncompressGZIP(retBytes);                
            }
            else if (comp == ZLIB) {
                retBytes = new byte[data.length-COMPRESS_HEADER_ZLIB.length()];
                System.arraycopy(data,
                                 COMPRESS_HEADER_ZLIB.length(),
                                 retBytes,
                                 0,
                                 data.length-COMPRESS_HEADER_ZLIB.length());
                retBytes = uncompressZLIB(retBytes);                
            }
            else
                ; // uncompressible XML, just drop it on the floor....
        }
        else
            return data;  // the Content-Type header is optional, assumes PT
        return retBytes;
    }

    private static int getCompressionType(String header) {
        String s = header.trim();
        if(s.equals("") || s.equalsIgnoreCase(C_HEADER_NONE_VAL))
            return NONE;
        else if(s.equalsIgnoreCase(C_HEADER_GZIP_VAL))
            return GZIP;
        else if(s.equalsIgnoreCase(C_HEADER_ZLIB_VAL))
            return ZLIB;
        else
            return -1;
        
    }
    

    /** Returns the uncompressed version of the given ZLIB'ed bytes.  Throws
     *  IOException if the data is corrupt. */
    private static byte[] uncompressGZIP(byte[] data) throws IOException {
        ByteArrayInputStream bais=new ByteArrayInputStream(data);
        InflaterInputStream gis=new GZIPInputStream(bais);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        while (true) {
            int b=gis.read();
            if (b==-1)
                break;
            baos.write(b);
        }
        return baos.toByteArray();
    }

        

    /** Returns the uncompressed version of the given ZLIB'ed bytes.  Throws
     *  IOException if the data is corrupt. */
    private static byte[] uncompressZLIB(byte[] data) throws IOException {
        ByteArrayInputStream bais=new ByteArrayInputStream(data);
        InflaterInputStream gis=new InflaterInputStream(bais);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        while (true) {
            int b=gis.read();
            if (b==-1)
                break;
            baos.write(b);
        }
        return baos.toByteArray();
    }


    private static final int NUM_BYTES_TO_HASH = 100;
    private static final int NUM_TOTAL_HASH    = NUM_BYTES_TO_HASH*3;
    private static void clearHashBytes(byte[] hashBytes) {
        for (int i = 0; i < NUM_BYTES_TO_HASH; i++)
            hashBytes[i] = (byte)0;
    }

    /**
     * Hashes the file using bits and pieces of the file.
     * 
     * @return The SHA hash bytes of hte input bytes.
     * @throws IOException if hashing failed for any reason.
     */
    public static byte[] hashFile(File toHash) throws IOException {
        byte[] retBytes = null;
        FileInputStream fis = null;
        byte[] hashBytes = new byte[NUM_BYTES_TO_HASH];
        
        try {        

            // setup
            fis = new FileInputStream(toHash);
            MessageDigest md = null;
           
            try {
                md = MessageDigest.getInstance("SHA");
            } catch(NoSuchAlgorithmException nsae) {
                Assert.that(false, "no sha algorithm.");
            }

            long fileLength = toHash.length();            
            if (fileLength < NUM_TOTAL_HASH) {
                int numRead = 0;
                do {
                    clearHashBytes(hashBytes);
                    numRead = fis.read(hashBytes);
                    md.update(hashBytes);
                    // if the file changed underneath me, throw away...
                    if (toHash.length() != fileLength)
                        throw new IOException("invalid length");
                } while (numRead == NUM_BYTES_TO_HASH);
            }
            else { // need to do some mathy stuff.......

                long thirds = fileLength / (long) 3;

                // beginning input....
                clearHashBytes(hashBytes);
                fis.read(hashBytes);
                md.update(hashBytes);

                // if the file changed underneath me, throw away...
                if (toHash.length() != fileLength)
                    throw new IOException("invalid length");

                // middle input...
                clearHashBytes(hashBytes);
                fis.skip(thirds - NUM_BYTES_TO_HASH);
                fis.read(hashBytes);
                md.update(hashBytes);

                // if the file changed underneath me, throw away...
                if (toHash.length() != fileLength)
                    throw new IOException("invalid length");
                
                // ending input....
                clearHashBytes(hashBytes);
                fis.skip(toHash.length() - 
                         (thirds + NUM_BYTES_TO_HASH) -
                         NUM_BYTES_TO_HASH);
                fis.read(hashBytes);
                md.update(hashBytes);

                // if the file changed underneath me, throw away...
                if (toHash.length() != fileLength)
                    throw new IOException("invalid length");

            }
                
            retBytes = md.digest();
        } finally {
            if (fis != null)
                fis.close();
        }
        return retBytes;
    }

    /**
     * Does an approximate check on the passed string for xml validity
     * @param inputStr The string to be tested
     * @return true, if the passed can be determined to be non-valid XML,
     * false otherwise.
     * Note that this method does only approximate evaluation. It guarantees
     * only that if true is returned, then the passed string was not a valid
     * xml string. The converse is not true.
     */
    public static boolean notValidXML(String inputStr)
    {
        //return true, if the passed string doesnt start with valid XML header
        if(inputStr == null || inputStr.trim().equals("")
            || !inputStr.startsWith(XMLStringUtils.XML_VERSION_DELIM))
            return true;
        
        //return false if we are not able to classify the passed string as 
        //non-valid xml
        return false;
    }
    
    /*
      public static void main(String argv[]) throws Exception {
      // TEST FOR hashFile(File)
        for (int i = 0; i < argv.length; i++) {
        File currFile = new File(argv[i]);
        byte[] hash = LimeXMLUtils.hashFile(currFile);
        Assert.that(MessageDigest.isEqual(hash,
        LimeXMLUtils.hashFile(currFile)));
        for (int j = 0; j < hash.length; j++)
        System.out.print(hash[j]);
        System.out.println("");
        }
        }
    */
    /*
    public static void main(String argv[]) {
        String dataSource = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\" ><audio album=\"Steve&apos;s ALbum\" artist=\"S. Cho / A. Kim\" bitrate=\"156\" comments=\"Live Concert 10/26/97\" genre=\"Chamber Music\" index=\"0\" title=\"Schumann Fantasiestucke - I.m\" year=\"2001\"/></audios>";
        byte[] dataBytes = dataSource.getBytes();
        byte[] dataCompressedBytes = compress(dataSource.getBytes());
        Assert.that(dataCompressedBytes.length<dataBytes.length);
        try {
            // pure byte tests...
            byte[] dataUncompressedBytes = uncompress(dataCompressedBytes);
            Assert.that(Arrays.equals(dataBytes, dataUncompressedBytes),
                        "Compress/uncompress loop failed 1");

        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false, "Bad GZIP data.");
        }
    }
    */

}

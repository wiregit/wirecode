package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.SAXException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.Response;


public final class LimeXMLDocumentHelper{

	/**
	 * Private constructor to ensure that this class can never be
	 * instantiated.
	 */
	private LimeXMLDocumentHelper() {}

    /**
     * TO be used when a Query Reply comes with a chunk of meta-data
     * we want to get LimeXMLDocuments out of it
     */
    public static List getDocuments(String aggregatedXML, 
                                    int totalResponseCount) {
        
        if(aggregatedXML==null || aggregatedXML.equals(""))
            return Collections.EMPTY_LIST;
        
        List results = new ArrayList();
        
        Iterator xmlDocumentsIterator = XMLParsingUtils.split(aggregatedXML).iterator();
        while(xmlDocumentsIterator.hasNext()) {
            String xmlDocument = (String)xmlDocumentsIterator.next();
            
            XMLParsingUtils.Result parsingResult;
            try {
                parsingResult = XMLParsingUtils.parse(xmlDocument);
            } catch (SAXException sax) {
                continue;// bad xml, ignore
            } catch (IOException bad) {
                break; // abort
            }
            
            String indexKey = parsingResult.canonicalKeyPrefix + "index__";
            LimeXMLDocument[] documents = new LimeXMLDocument[totalResponseCount];
            
            Iterator mapsIterator = parsingResult.canonicalAttributeMaps.iterator();
            while(mapsIterator.hasNext()) {
                try {
                    TreeMap map = (TreeMap)mapsIterator.next();
                    
                    int index = Integer.parseInt((String)map.get(indexKey));
                    if (index >= documents.length)
                        continue; // bad?
                    
                    documents[index] = new LimeXMLDocument(map,parsingResult.schemaURI);
                } catch(NumberFormatException bad) {
                    ErrorService.error(bad);
                }
            }
            results.add(documents);
        }
        return results;
    }
    /**
     * @param responses array is a set of responses. Some have meta-data
     * some do not. 
     * The aggregrate string should reflect the indexes of the 
     * responses
     *<p>
     * If none of the responses have any metadata, this method returns an
     * empty string.
     */
    public static String getAggregateString(Response[] responses){
        //this hashmap remembers the current state of the string for each uri
        HashMap uriToString /*LimeXMLSchemaURI -> String */ = new HashMap();
        for(int i=0; i< responses.length; i++) {
            LimeXMLDocument doc = responses[i].getDocument();
            if(doc==null)
                continue;
            aggregateResponse(uriToString, doc,i);
        }
        StringBuffer retStringB = new StringBuffer();

        //iterate over the map and close all the strings out.
        Iterator iter = uriToString.values().iterator();
        while(iter.hasNext()) {
            String str = ((StringBuffer)iter.next()).toString();
            int begin = str.indexOf("<",2);//index of opening outer(plural)
            int end = str.indexOf(" ",begin);
            retStringB.append(str);
            retStringB.append("</");
            retStringB.append(str.substring(begin+1,end));
            retStringB.append(">");
        }
        return retStringB.toString();
    }

    private static void aggregateResponse(HashMap uriToString, 
                                         LimeXMLDocument doc, int index) {
        if(doc == null)//response has no document
            return;
        String uri = doc.getSchemaURI();
        StringBuffer currStringB = (StringBuffer)uriToString.get(uri);

        if(currStringB == null) {//no entry so far
            //1. add the outer (plural form) - w/o the end
            String str = null;
            try {
                str = doc.getXMLString();
            } catch(SchemaNotFoundException e) {  
                return;//dont do anything to the map, just return
            }
            if(str==null || str.equals("")) 
                return;
            str = str.substring(0,str.lastIndexOf("<"));
            //2.append the index in the right place.
            int p = str.indexOf("<");//index of opening header
            Assert.that(p>=0, "LimeXMLDocument is broken");
            p = str.indexOf("<",p+1);//index of opening outer(plural) 
            Assert.that(p>=0, "LimeXMLDocument is broken");
            p = str.indexOf("<",p+1);//index of opening inner
            Assert.that(p>=0, "LimeXMLDocument is broken");
            int q = str.indexOf(">",p+1);//index of first closing tag
            int k = str.lastIndexOf("/",q-1);//is it tag closing inclusive?
            if(k!=-1 && p < k && k < q )//   "/" b/w open and close
                if(str.substring(k+1,q).trim().equals(""))
                    p=k;//white space only b/w / and >
                else
                    p=q;
            else
                p=q;
            String first = str.substring(0,p);
            String last = str.substring(p);
            StringBuffer strB = new StringBuffer(first.length() +
                                                 15 + last.length());
            // str = first+" index=\""+index+"\" "+last;
            strB.append(first);
            strB.append(" index=\"");
            strB.append(index);
            strB.append("\" ");
            strB.append(last);
            //3. add the entry in the map
            uriToString.put(uri, strB);
        }
        else {            
            //1.strip the plural form out of the xml string
            String str = null;
            try{
                str = doc.getXMLString();
            } catch(SchemaNotFoundException e) {
                return;//dont modify the string in the hashmap. just return
            }
            int begin = str.indexOf("<");//index of header
            Assert.that( begin != -1, str);
            begin = str.indexOf("<",begin+1);//index of outer(plural) tag-close
            Assert.that( begin != -1, str);
            begin = str.indexOf("<",begin+1);//index of begining of current tag
            Assert.that( begin != -1, str);            
            int end = str.lastIndexOf("<");
            Assert.that( end != -1, str);
            str = str.substring(begin,end);
            //2.insert the index 
            int p = str.indexOf(">");
            int q = str.lastIndexOf("/",p-1);
            if (q!=-1 && q < p) // 
                if(str.substring(q+1,p).trim().equals(""))
                   p = q;
            String first = str.substring(0,p);
            String last = str.substring(p);
            //  str = first+" index=\""+index+"\" "+last;
            //  currString = currString+str;
            currStringB.append(first);
            currStringB.append(" index=\"");
            currStringB.append(index);
            currStringB.append("\" ");
            currStringB.append(last);
            //3.concatinate the remaining xml to currString
            //4.put it back into the map
            uriToString.put(uri,currStringB);
        }
    }

    private static boolean debugOn = false;
    public static void debug(String out) {
        if (debugOn) 
            System.out.println(out);
    }

    /*
    // to run this test, you need to be one level above your lib directory.
    // Moreover, you need to have the following schemas: slashdotNews, audio,
    // video, and radiostations....
    public static void main(String argv[]) throws Exception {
        debugOn = false;
        LimeXMLDocumentHelper help = new LimeXMLDocumentHelper();
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();
        LimeXMLDocument doc = null;
        Response[] resps = new Response[5];
        
        resps[0] = new Response(0, 100, "File 1");
        doc = new LimeXMLDocument("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio identifier=\"/abc.txt/\" bitrate=\"192\" genre=\"/Blues/\"/></audios>");
        resps[1] = new Response(1, 200, "File 2",doc);
        resps[2] = new Response(0, 300, "File 3");
        resps[3] = new Response(3, 400, "File 4", new LimeXMLDocument("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"128\" genre=\"/Country/\"></audio></audios>"));
        resps[4] = new Response(0, 500, "File 5");
        
        for (int i = 0; i < resps.length; i++)
            debug("resps["+i+"].metadata = " +
                  resps[i].getMetadata());
        
        String xmlCollectionString = help.getAggregateString(resps);

        Assert.that(xmlCollectionString.equals("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio  bitrate=\"192\" genre=\"/Blues/\" index=\"1\" /><audio bitrate=\"128\" genre=\"/Country/\" index=\"3\" ></audio></audios>"));

        debug("Aggregate String (no disparates) = " + xmlCollectionString); 
                                               
        resps = new Response[10];
        resps[0] = new Response(0, 100, "File 1");
        resps[1] = new Response(1, 200, "File 2", new LimeXMLDocument("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"192\" genre=\"Blues\" identifier=\"def.txt\"/></audios>"));
        resps[2] = new Response(0, 300, "File 3");
        doc = new LimeXMLDocument("<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story identifier=\"ghi.txt\"><comments>Duh!</comments><author>Susheel</author></story></backslash>");
        
        resps[3] = new Response(3, 400, "File 4", doc);
        resps[4] = new Response(0, 500, "File 5");
        resps[5] = new Response(1, 200, "File 6", new LimeXMLDocument("<?xml version=\"1.0\"?><radioStations xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/radioStations.xsd\"><radioStation format=\"Blues\" city=\"New York\"/></radioStations>"));
        resps[6] = new Response(1, 200, "File 7", new LimeXMLDocument("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"160\" genre=\"Chamber Music\"/></audios>"));
        resps[7] = new Response(1, 200, "File 8", new LimeXMLDocument("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"170\" genre=\"Pop\"></audio></audios>"));
        resps[8] = new Response(1, 200, "File 9", new LimeXMLDocument("<?xml version=\"1.0\"?><radioStations xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/radioStations.xsd\"><radioStation format=\"Classic Rock\"></radioStation></radioStations>"));
        resps[9] = new Response(1, 200, "File 10", new LimeXMLDocument("<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story><image>J. Lo</image><title>Oops, I did it Again!</title></story></backslash>"));
        
        for (int i = 0; i < resps.length; i++)
            debug("resps["+i+"].metadata = " +
                  resps[i].getMetadata());
        
        debug("----->");
        xmlCollectionString = help.getAggregateString(resps);
        //System.out.println("Sumeet " + xmlCollectionString);
        Assert.that(xmlCollectionString.equals("<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story  index=\"3\" ><comments>Duh!</comments><author>Susheel</author></story><story index=\"9\" ><image>J. Lo</image><title>Oops, I did it Again!</title></story></backslash><?xml version=\"1.0\"?><radioStations xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/radioStations.xsd\"><radioStation format=\"Blues\" city=\"New York\" index=\"5\" /><radioStation format=\"Classic Rock\" index=\"8\" ></radioStation></radioStations><?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"192\" genre=\"Blues\"  index=\"1\" /><audio bitrate=\"160\" genre=\"Chamber Music\" index=\"6\" /><audio bitrate=\"170\" genre=\"Pop\" index=\"7\" ></audio></audios>"));
                                               
        debug("Aggregate String (disparates) = " + xmlCollectionString); 
        
        debug("--------------------------------");
        
        List madeXMLList = 
        help.getDocuments("<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story index=\"0\"><title>Susheel</title></story><story index=\"4\"><author>Daswani</author><section>News</section></story></backslash>", 6);
        LimeXMLDocument a  = new LimeXMLDocument("<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story><title>Susheel</title></story></backslash>");
        LimeXMLDocument b  = new LimeXMLDocument("<?xml version=\"1.0\"?><backslash xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/slashdotNews.xsd\"><story><author>Daswani</author><section>News</section></story></backslash>");
        LimeXMLDocument[] madeXML = (LimeXMLDocument[])madeXMLList.get(0);
        for (int i = 0; (i < 6) && (madeXML != null); i++)
            switch (i) {
            case 0:
            case 4:
                // these should have hexML
                Assert.that(madeXML[i] != null);
                if (i == 0)
                    Assert.that(madeXML[i].equals(a));
                else
                    Assert.that(madeXML[i].equals(b));
                break;
            case 1:
            case 2:
            case 3:
            case 5:
                Assert.that(madeXML[i] == null, ""+i);
            }
        
        debug("--------------------------------");
         
        // for this series of tests, see resps defined above....
        List retrievedXMLList = help.getDocuments(xmlCollectionString,
                                                   resps.length);
        Iterator arrays = retrievedXMLList.iterator();
        LimeXMLDocument[][] docsArray = 
        new LimeXMLDocument[retrievedXMLList.size()][resps.length];
        {   
            // there are 3, but just do it generallly....
            int index = 0;
            while (arrays.hasNext()) 
                docsArray[index++] = (LimeXMLDocument[]) arrays.next();
        }
        
        for (int i = 0; i < resps.length; i++) {
            switch (i) {
            case 0:
            case 2:
            case 4:
                // no docs here...
                break;
            default:
                // one should be non-null at the very least....
                Assert.that(docsArray[0][i] != null ||
                            docsArray[1][i] != null ||
                            docsArray[2][i] != null);
                for (int j = 0; i < 3; i++)
                    if (docsArray[j][i] != null)
                        Assert.that(docsArray[j][i].equals(resps[i].getDocument()));
            }                   
        }
    }
    */
}

package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import org.xml.sax.SAXException;
import java.io.IOException;
import com.sun.java.util.collections.List;


/**
 * Used to handle Rich Queries. The FileManager will check to see if the 
 * Rich Query part of the message is empty. If not then it will grab and 
 * instance of this class and use it to find a set of Responses for  that
 * Query.
 * <p>
 * Has a singleton pattern.
 * @author Sumeet Thadani
 */
public class RichQueryHandler{
    
    private static RichQueryHandler instance;// the instance
    
    /**
     * Call this method to get the singleton
     */
    public static synchronized RichQueryHandler instance(){
        if (instance != null)
            return instance;
        else{
            instance = new RichQueryHandler();
            return instance;
        }
    }
    
    /**
     * Returns an array of Responses that correspond to documents
     * that have a match with the XMLQuery String
     * Warning: Returns null if the XMLQuery is malformed.
     * <p>
     * This method is synchronized. Because if two threads from different
     * connections make two different queries. Then all bad things can happen.
     */
    public synchronized Response[] query(String XMLQuery,FileManager fManager){
        debug("Sumeet: "+XMLQuery);
        if (XMLQuery.equals(""))
            return null;
        LimeXMLDocument queryDoc = null;
        try{// if we catch an exception here the query is malformed.
            queryDoc=new LimeXMLDocument(XMLQuery);
        }catch (SAXException e){
            return null; //Return null
        }catch (IOException ee){
            return null;
        }catch(SchemaNotFoundException eee){
            return null;
        }
        String schema = queryDoc.getSchemaURI();
        SchemaReplyCollectionMapper mapper = 
                            SchemaReplyCollectionMapper.instance();
        LimeXMLReplyCollection replyCol = mapper.getReplyCollection(schema);
        if(replyCol == null)//no matching reply collection for schema
            return null;
        List matchingReplies = replyCol.getMatchingReplies(queryDoc);
        //matchingReplies = a List of LimeXMLDocuments that match the query
        int s = matchingReplies.size();
        Response[] retResponses = new Response[s];
        //FileManager fManager = FileManager.instance();
        //We need the MetaFileManager to get the FileDesc from full FileName
        //Note:FileManager has been changed to return a MetaFileManager now
        long index=-1;
        long size=-1;
        String name="";
        String metadata="";
        int z =0;
        boolean busy = 
            RouterService.getUploadManager().isBusy() &&
            RouterService.getUploadManager().isQueueFull();
        debug("RQH.query(): # of resps = " + s);
        for(int i=0; i<s;i++){
            LimeXMLDocument currDoc = (LimeXMLDocument)matchingReplies.get(i);
            String subjectFile = currDoc.getIdentifier();//returns null if none
            FileDesc fd = null;
            Response res = null;
            if(subjectFile==null){//pure data (data about NO file)
                index = LimeXMLProperties.DEFAULT_NONFILE_INDEX;
                size = 0;//there is no file, so no size
                name =" ";//leave blank
                res = new Response(index, size, name);
            }
            else { //meta-data about a specific file
                fd = fManager.file2index(subjectFile);                
                
                if( fd == null || 
                   (busy && fd.getNumberOfAlternateLocations() >= 10) ) {
                    // if fd is null, MetaFileManager is out of synch with
                    // FileManager -- this is bad.
                    continue;
                }
                
                //we found a file with the right name
				index = fd.getIndex();
				name =  fd.getName();//need not send whole path; just name + index
				size =  fd.getSize();
				res = new Response(fd);
            }

            res.setDocument(currDoc);
            retResponses[z] = res;
            z++;
        }

        // need to ensure that no nulls are returned in my response[]
        // z is a count of responses constructed, see just above...
        // s == retResponses.length
        if (z < s){
            Response[] temp = new Response[z];
            for (int i = 0, j = 0; i < s; i++) // at most s responses
                if (retResponses[i] != null)
                    temp[j++] = retResponses[i];
            retResponses = temp;
        }

        debug("RQH: num Response = " + retResponses.length);
        return retResponses;
    }


    private final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private void debug(Exception e) {
        if (debugOn)
            e.printStackTrace();
    }

}

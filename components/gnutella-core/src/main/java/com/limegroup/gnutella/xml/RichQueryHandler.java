package com.limegroup.gnutella.xml;

import java.io.File;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import java.util.List;
import java.util.Iterator;


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
    public synchronized Response[] query(LimeXMLDocument queryDoc) {
        String schema = queryDoc.getSchemaURI();
        SchemaReplyCollectionMapper mapper = 
                            SchemaReplyCollectionMapper.instance();
        LimeXMLReplyCollection replyCol = mapper.getReplyCollection(schema);
        if(replyCol == null)//no matching reply collection for schema
            return null;
        List matchingReplies = replyCol.getMatchingReplies(queryDoc);
        //matchingReplies = a List of LimeXMLDocuments that match the query
        int s = matchingReplies.size();
        if( s == 0 ) // no matching replies.
            return null; 
        
        Response[] retResponses = new Response[s];
        //FileManager fManager = FileManager.instance();
        //We need the MetaFileManager to get the FileDesc from full FileName
        //Note:FileManager has been changed to return a MetaFileManager now
        long index=-1;
        long size=-1;
        String name="";
        int z = 0;
        for(Iterator i = matchingReplies.iterator(); i.hasNext(); ) {
            LimeXMLDocument currDoc = (LimeXMLDocument)i.next();
            String subjectFile = currDoc.getIdentifier();//returns null if none
            FileDesc fd = null;
            Response res = null;
            if(subjectFile==null) { //pure data (data about NO file)
                index = LimeXMLProperties.DEFAULT_NONFILE_INDEX;
                size = 0; //there is no file, so no size
                name = " "; //leave blank
                res = new Response(index, size, name);
            } else { //meta-data about a specific file
                fd = RouterService.getFileManager().getFileDescForFile(
                    new File(subjectFile));
                if( fd == null) {
                    // if fd is null, MetaFileManager is out of synch with
                    // FileManager -- this is bad.
                    continue;
                } else { //we found a file with the right name
					index = fd.getIndex();
                    //need not send whole path; just name + index					
					name =  fd.getName();
					size =  fd.getSize();
					res = new Response(fd);
					fd.incrementHitCount();
                    RouterService.getCallback().handleSharedFileUpdate(
                        fd.getFile());
                }
            }
            
            // Note that if any response was invalid,
            // the array will be too small, and we'll
            // have to resize it.
            res.setDocument(currDoc);
            retResponses[z] = res;
            z++;
        }
        
        if( z == 0 )
            return null; // no responses

        // need to ensure that no nulls are returned in my response[]
        // z is a count of responses constructed, see just above...
        // s == retResponses.length        
        if (z < s) {
            Response[] temp = new Response[z];
            System.arraycopy(retResponses, 0, temp, 0, z);
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
}

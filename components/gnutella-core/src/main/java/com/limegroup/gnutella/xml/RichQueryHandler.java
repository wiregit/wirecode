package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.Response;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.List;


/**
 * Used to handle Rich Queries. The FileManager will check to see if the 
 * Rich Query part of the message is empty. If not then it will grab and 
 * instance of this class and use it to find a set of Responses for  that
 * Query.
 * <p>
 * Has a singleton pattern.
 * @author Sumeet Thadani
 */
class RichQueryHandler{
    
    static RichQueryHandler instance;// the instance
    
    
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
     */
    public Response[] query(String XMLQuery){
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
        LimeXMLReplyCollection replyDocs = mapper.getReplyCollection(schema);
        List matchingReplies = replyDocs.getMatchingReplies(queryDoc);
        //TODO1: Complete
        int size = matchingReplies.size();
        for(int i=0; i<size;i++){            
            
        }
        //find out if these replyDocuments correspond to dome file 
        //(by checking the identifier tags.)
        //If they correspond to files we make responses in the 
        // regular way...with an index and a a size and a name and the
        // meta info.

        //however if there is no file, we create the response with a
        //special index (like -1) if that is legal. And then this will allow
        //us to put the meta info in and not have the system check for
        //corresponding file.

        //Also, we have to a have a method called toXMLString in the 
        //LimeXMLDocument class.
        return null;
    }

}

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
    public static RichQueryHandler instance(){
        if (instance != null)
            return instance;
        else{
            instance = new RichQueryHandler();
            return instance;
        }
    }

    public Response[] query(String XMLQuery){
        LimeXMLDocument queryDoc = null;
        try{// if we catch an exception here the query is malformed.
            queryDoc=new LimeXMLDocument(XMLQuery);
        }catch (SAXException e){
            return null; //Return null
        }catch (IOException ee){
            return null;
        }            
        String schema = queryDoc.getSchemaURI();
        
        SchemaReplyCollectionMapper mapper = 
                            SchemaReplyCollectionMapper.instance();
        LimeXMLReplyCollection replyDocs = mapper.getReplyCollection(schema);
        List matchingReplies = replyDocs.getMatchingReplies(queryDoc);
        //TODO1: Complete
        return null;
    }

}

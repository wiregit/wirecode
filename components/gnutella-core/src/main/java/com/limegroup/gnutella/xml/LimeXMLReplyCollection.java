package com.limegroup.gnutella.xml;

import java.util.ArrayList;
import java.util.List;
import com.limegroup.gnutella.util.NameValue;

/**
 *  Stores a schema and a list of Replies corresponding to the 
 *  the corresponding to this schema.
 *  <p>
 *  So when a search comes in, we only have to look at the set of replies
 *  that correspond to the schema of the query.
 * 
 * @author Sumeet Thadani
 */

class LimeXMLReplyCollection{
    
    private String schemaURI;
    //a list of reply docs in the client machine that correspond to the Schema
    private ArrayList replyDocs;
    
    
    //Constructor
    public LimeXMLReplyCollection(String URI){
        schemaURI = URI;
        replyDocs = new ArrayList();
    }
    
    public String getSchemaURI(){
        return schemaURI;
    }

    public List getCollectionList(){
        return replyDocs;
    }

    
    /**
     * Returns and empty list if there are not matching documents with
     * that correspond to the same schema as the query.
     */    
    public List getMatchingReplies(LimeXMLDocument queryDoc){
        int size = replyDocs.size();
        List matchingReplyDocs = new ArrayList();
        for(int i=0;i<size;i++){            
            LimeXMLDocument currReplyDoc = (LimeXMLDocument)replyDocs.get(i);
            //Note: currReplyDoc may be null, in which case match will return 
            // false
            boolean match = match(currReplyDoc, queryDoc);
            if(match){
                matchingReplyDocs.add(currReplyDoc);
                match = false;
            }
        }
        return matchingReplyDocs;
    }

    public void addReply(LimeXMLDocument replyDoc){
        replyDocs.add(replyDoc);
    }


    public void appendCollectionList(List newReplyCollection){
        replyDocs.addAll(newReplyCollection);
    }

    private boolean match(LimeXMLDocument replyDoc, LimeXMLDocument queryDoc){
        if(replyDoc==null)
            return false;
        //First find the names of all the fields in the query
        List queryNameValues = queryDoc.getNameValueList();
        int size = queryNameValues.size();
        List fieldNames = new ArrayList(size);
        for(int i=0; i<size; i++){
            NameValue nameValue = (NameValue)queryNameValues.get(i);
            String fieldName = nameValue.getName();
            fieldNames.add(fieldName);
        }
        //compare these fields with the current reply document
        List currDocNameValues = replyDoc.getNameValueList();
        int matchCount=0;//number of matches
        int nullCount=0;//num of fields which are in query but null in ReplyDoc
        for(int j=0; j< size; j++){
            String currFieldName = (String)fieldNames.get(j);
            String queryValue = queryDoc.getValue(currFieldName);
            String replyDocValue = replyDoc.getValue(currFieldName);
            if(replyDocValue == null)
                nullCount++;
            if(replyDocValue.equals(queryValue))
                matchCount++;
        }
        //The metric of a correct match is that whatever fields are specified
        //in the query must have perfect match with the fields in the reply
        //unless the reply has a null for that feild, in which case we are OK 
        // with letting it slide. But if there is even one mismatch
        // we are going to return false.
        if( ( (nullCount+matchCount)/size ) < 1)
            return false;
        return true;
    }
}

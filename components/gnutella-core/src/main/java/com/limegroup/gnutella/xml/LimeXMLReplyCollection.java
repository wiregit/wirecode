package com.limegroup.gnutella.xml;

import java.util.ArrayList;
import java.util.List;
import com.limegroup.gnutella.util.NameValue;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;


/**
 *  Stores a schema and a list of Replies corresponding to the 
 *  the corresponding to this schema.
 *  <p>
 *  So when a search comes in, we only have to look at the set of replies
 *  that correspond to the schema of the query.
 * 
 * @author Sumeet Thadani
 */

public class LimeXMLReplyCollection{
    
    private String schemaURI;
    //a list of reply docs in the client machine that correspond to the Schema
    //Note: Each ReplyCollection is written out to 1 physical file on shutdown.
    private ArrayList replyDocs;
    private boolean done= false;
    
    //Constructor
    public LimeXMLReplyCollection(String URI) {
        schemaURI = URI;//store it away
        String schemaStr = LimeXMLSchema.getDisplayString(URI);
        String schemaName= schemaStr+".xml";
        replyDocs = new ArrayList();
        //Load up the docs from the file.
        LimeXMLProperties props = LimeXMLProperties.instance();
        String path = props.getXMLDocsDir();
        String content="";
        try{
            File file = new File(path,schemaName);
            RandomAccessFile f = new RandomAccessFile(file,"r");
            int len = (int)f.length();
            byte[] con = new byte[len];
            f.readFully(con,0,len);
            f.close();
            content = new String(con);
            con=null;//free the memory
        }catch (IOException e){//file had a problem. 
            //Do not put this collection in mapper
            done= false;
            return;
        }
        int startIndex = content.indexOf("<?xml");
        int endIndex = startIndex;
        String xmlDoc = "";
        boolean finished= false;
        while(!finished){
            startIndex = endIndex;//nextRound
            if (startIndex == -1){
                finished = true;
                continue;
            }
            endIndex=content.indexOf("<?xml",startIndex+1);
            if (endIndex > 0)
                xmlDoc = content.substring(startIndex, endIndex);
            else
                xmlDoc = content.substring(startIndex);
            String xmlString = "";
            StringTokenizer tok = new StringTokenizer(xmlDoc,"\n\t");
            while (tok.hasMoreTokens()){
                xmlString = xmlString+tok.nextToken();
            }
            LimeXMLDocument doc= null;
            try{
                doc = new LimeXMLDocument(xmlString);
            }catch(Exception e){//the xml is malformed
                //e.printStackTrace();
                continue;//just ignore this document. do not add or set done
            }
            addReply(doc);
            if(done == false)
                done = true;//set it to true coz now we have some data
        }
    }

    /** 
     * Secondary constructor: used when the user adds meta-data, fot
     * a particular schema - for which there are no previously existing
     * LimeXMlReplyCollection. In that case this constructor is called
     * <p>
     * This constructor must be created with at least one LimeXMLDocument in
     * hand. 
     */
    public LimeXMLReplyCollection(String uri, LimeXMLDocument doc){
        schemaURI = uri;
        replyDocs = new ArrayList();
        addReply(doc);
        done = true;
    }

    public boolean getDone(){
        return done;
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
    
    public void replaceDoc(LimeXMLDocument oldDoc, LimeXMLDocument newDoc){
        int size = replyDocs.size();
        for(int i=0;i<size;i++){
            Object o = replyDocs.get(i);
            if(o==oldDoc){
                replyDocs.remove(i);
                replyDocs.add(newDoc);
                break;
            }
        }
    }

    public boolean removeDoc(LimeXMLDocument doc){
        int size = replyDocs.size();
        boolean found = false;
        for(int i=0;i<size;i++){
            Object o = replyDocs.get(i);
            if(o==doc){
                found = true;
                replyDocs.remove(i);
                if(replyDocs.size() == 0){//if there are no more replies.
                    removeFromRepository();//remove this collection from map
                    //Note: this follows the convention of the MetaFileManager
                    //of not adding a ReplyCollection to the map if there are
                    //no docs in it.
                }
            }
        }
        boolean written = false;
        if(found)
            written = toDisk();
        if(!written && found)//put it back to maintin consistency
            replyDocs.add(doc);
        else if(found && written)
            return true;
        return false;
    }
        
    private void removeFromRepository(){
        SchemaReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        map.removeReplyCollection(this.schemaURI);
    }


    public boolean toDisk(){
        String schemaStr = LimeXMLSchema.getDisplayString(schemaURI);
        String schemaName= schemaStr+".xml";
        //Load up the docs from the file.
        LimeXMLProperties props = LimeXMLProperties.instance();
        String path = props.getXMLDocsDir();
        String content = "";
        int size = replyDocs.size();
        for(int i=0; i<size;i++){
            LimeXMLDocument currDoc = (LimeXMLDocument)replyDocs.get(i);
            String xml = currDoc.getXMLString();
            content = content+xml+"\n";
        }
        try{
            String fileName = path+File.separator+schemaName;            
            FileWriter writer = new FileWriter(fileName,false);//overwrite
            writer.write(content,0,content.length());
            writer.close();
        }catch(IOException e){
            return false;
        }
        return true;
        
        //TODO3: For later - the proposed technique is highly wasteful
        //to re-write all docs to file when only 1 has been modified or 
        //added. Replace this method. 
        //Note : A good way of doing this would be to store the 
        //start index and the end index of the file within the 
        //LimeXMLDocument...and use this info to change just that part of the
        //file...
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
            else if(replyDocValue.equals(queryValue))
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

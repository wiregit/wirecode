package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.mp3.*;
import java.io.*;
import java.util.*;

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
    public boolean audio = false;//package access
    private ID3Editor editor = null;


    //Constructor for audio Schema
    public LimeXMLReplyCollection(Map nameToFile, String URI){
        schemaURI = URI;
        audio = true;
        replyDocs = new ArrayList();
        ID3Reader id3Reader = new ID3Reader();
        String content = getContent();
        if(content!= null && !content.equals("")){
            int startIndex = content.indexOf(
                            XMLStringUtils.XML_DOC_START_IDENTIFIER);
            int endIndex = startIndex;
            String xmlDoc = "";
            boolean finished= false;
            while(!finished){
                startIndex = endIndex;//nextRound
                if (startIndex == -1){
                    finished = true;
                    continue;
                }
                endIndex=content.indexOf
                     (XMLStringUtils.XML_DOC_START_IDENTIFIER,startIndex+1);
                if (endIndex > 0)
                    xmlDoc = content.substring(startIndex, endIndex);
                else
                    xmlDoc = content.substring(startIndex);
                String xmlString = "";
                StringTokenizer tok = new StringTokenizer(xmlDoc,"\n\t");
                while (tok.hasMoreTokens()){
                    xmlString = xmlString+tok.nextToken();
                }
                String identifier = getIdentifierFromXMLStr(xmlString);
                if(identifier==null || identifier.equals(""))
                    continue;
                File f = (File)nameToFile.remove(identifier);
                String str;
                try{
                    str = id3Reader.readDocument(f,false);//its mixed
                }catch (Exception e){
                    str = "";
                }
                String xmlStr = joinAudioXMLStrings(str,xmlString);
                LimeXMLDocument doc = null;
                try{
                    doc = new LimeXMLDocument(xmlStr);
                }catch(Exception e){//the xml is malformed
                    //e.printStackTrace();
                    continue;//just ignore this document. dont add or set done
                }
                if(doc != null){
                    addReply(doc);
                    if(done == false)
                        done = true;//set it to true coz now we have some data
                }
            }
        }//end of if that checks if we have to deal with file data as well
        Iterator names = nameToFile.keySet().iterator();
        while(names.hasNext()){
            File file = (File)nameToFile.get(names.next());
            LimeXMLDocument d = null;
            try{
                String s = id3Reader.readDocument(file,true);//its not mixed
                d = new LimeXMLDocument(s);
            }catch(Exception e){
                continue;
            }
            if(d!=null){
                addReply(d);
                if(done == false)
                    done = true;
            }
        }
    }

    //Original Constructor
    public LimeXMLReplyCollection(String URI) {
        schemaURI = URI;//store it away
        replyDocs = new ArrayList();
        String content = getContent();
        if(content==null || content.equals("")){
            done = false;
            return;
        }
        int startIndex = content.indexOf(
                              XMLStringUtils.XML_DOC_START_IDENTIFIER);
        int endIndex = startIndex;
        String xmlDoc = "";
        boolean finished= false;
        while(!finished){
            startIndex = endIndex;//nextRound
            if (startIndex == -1){
                finished = true;
                continue;
            }
            endIndex=content.indexOf(XMLStringUtils.XML_DOC_START_IDENTIFIER,
                                     startIndex+1);
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
            if(doc!=null){
                addReply(doc);
                if(done == false)
                    done = true;//set it to true coz now we have some data
            }
        }
    }

    private String getIdentifierFromXMLStr(String xmlStr){
        int i = xmlStr.indexOf("identifier");
        i = xmlStr.indexOf("\"",i);
        int j = xmlStr.indexOf("\"",i+1);
        if(i<0 || j<0)
            return "";
        return xmlStr.substring(i+1,j);
    }
        
    private String joinAudioXMLStrings(String mp3Str, String fileStr){
        int p = fileStr.lastIndexOf("></audio>");//the one closing the root element
        String a = fileStr.substring(0,p);//all but the closing part
        String b = fileStr.substring(p);//closing part
        //phew, thank god this schema has depth 1.
        return(a+mp3Str+b);
    }

    private String getContent(){
        String schemaStr = LimeXMLSchema.getDisplayString(schemaURI);
        String schemaName= schemaStr+".xml";
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
            return "";
        }
        return content;
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
            boolean match = LimeXMLUtils.match(currReplyDoc, queryDoc);
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
                size--;//size of replyDocs is down
                i--;//we want to remain at the same index for next round.
                if(replyDocs.size() == 0){//if there are no more replies.
                    removeFromRepository();//remove this collection from map
                    //Note: this follows the convention of the MetaFileManager
                    //of not adding a ReplyCollection to the map if there are
                    //no docs in it.
                }
            }
        }
        boolean written = false;
        if(found){
            //ID3Editor editor = null;
            written = toDisk("");//no file modified...just del meta
        }
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


    public boolean toDisk(String modifiedFile){
        String schemaStr = LimeXMLSchema.getDisplayString(schemaURI);
        String schemaName= schemaStr+".xml";
        //Load up the docs from the file.
        LimeXMLProperties props = LimeXMLProperties.instance();
        String path = props.getXMLDocsDir();
        String content = "";
        int size = replyDocs.size();
        for(int i=0; i<size;i++){
            LimeXMLDocument currDoc = (LimeXMLDocument)replyDocs.get(i);
            String xml = "";
            try {
                xml = currDoc.getXMLStringWithIdentifier();
            }
            catch (SchemaNotFoundException snfe) {};
            if(audio){
                String fName = currDoc.getIdentifier();
                ID3Editor e = new ID3Editor();
                xml = e.removeID3Tags(xml);
                if(fName.equals(modifiedFile)){
                    this.editor = e;
                }
            }
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
    
    public boolean mp3ToDisk(String mp3FileName){
        int i = mp3FileName.lastIndexOf(".");
        if (i<0)
            return false;
        boolean wrote=false;
        boolean wrote2 = false;
        //write out to disk in the regular way
        wrote = toDisk(mp3FileName);//write the flat file stuff
        if (this.editor != null)
            wrote2 = this.editor.writeID3DataToDisk(mp3FileName);
        this.editor= null; //reset the value
        return (wrote && wrote2);
    }    


    public void appendCollectionList(List newReplyCollection){
        replyDocs.addAll(newReplyCollection);
    }


    public class MapSerializer {

        /** Where to serialize/deserialize from.
         */
        private String _backingStoreName;
        
        /** underlying map for hashmap access.
         */
        private HashMap _hashMap;

        /** @param whereToStore The name of the file to serialize from / 
         *  deserialize to.  
         *  @exception Exception Thrown if input file whereToStore is invalid.
         */
        public MapSerializer(String whereToStore) throws Exception {
            _backingStoreName = whereToStore;
            File file = new File(_backingStoreName);
            if (file.isDirectory())
                throw new Exception();
            else if (file.exists())
                deserializeFromFile();
            else
                _hashMap = new HashMap();
        }

        private void deserializeFromFile() throws Exception {            
            FileInputStream istream = new FileInputStream(_backingStoreName);
            ObjectInputStream objStream = new ObjectInputStream(istream);
            _hashMap = (HashMap) objStream.readObject();
            istream.close();
        }

        /** Call this method when you want to force the contents to the HashMap
         *  to disk.
         *  @exception Exception Thrown if force to disk failed.
         */
        public void commit() throws Exception {
            serializeToFile();
        }

        
        private void serializeToFile() throws Exception {
            FileOutputStream ostream = new FileOutputStream(_backingStoreName);
            ObjectOutputStream objStream = new ObjectOutputStream(ostream);
            objStream.writeObject(_hashMap);
            ostream.close();
        }

        /** @return The Map this class encases.
         */
        public Map getMap() {
            return _hashMap;
        }

    }


    /*
    public static void testMapSerializer(String argv[]) throws Exception {   
    LimeXMLReplyCollection.MapSerializer hms =
    new LimeXMLReplyCollection.MapSerializer("test.txt");
    
    Map hm = hms.getMap();
    
    System.out.println(""+hm);
    
    for (int i = 0; i < argv.length; i+=2) {
    try{
    hm.put(argv[i],argv[i+1]);
    }
    catch (Exception e) {};
    }
    hms.commit();
    }
    
    
    public static void main(String argv[]) throws Exception {
    testMapSerializer(argv);
    }
    */

}

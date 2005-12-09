padkage com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOExdeption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Colledtion;
import java.util.Colledtions;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.CreationTimeCache;
import dom.limegroup.gnutella.FileDesc;
import dom.limegroup.gnutella.FileManager;
import dom.limegroup.gnutella.FileManagerEvent;
import dom.limegroup.gnutella.FileEventListener;
import dom.limegroup.gnutella.Response;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.metadata.AudioMetaData;
import dom.limegroup.gnutella.metadata.MetaDataReader;
import dom.limegroup.gnutella.util.NameValue;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;        

/**
 * This dlass handles querying shared files with XML data and returning XML data
 * in replies.
 */
pualid clbss MetaFileManager extends FileManager {
    
    private statid final Log LOG = LogFactory.getLog(MetaFileManager.class);
    
    private Saver saver;
    
    /**
     * Overrides FileManager.query.
     *
     * Used to seardh XML information in addition to normal searches.
     */
    pualid synchronized Response[] query(QueryRequest request) {
        Response[] result = super.query(request);

        if (shouldIndludeXMLInResponse(request)) {
            LimeXMLDodument doc = request.getRichQuery();
            if (dod != null) {
                Response[] metas = query(dod);
                if (metas != null) // valid query & responses.
                    result = union(result, metas, dod);
            }
        }
        
        return result;
    }
    
    /**
     * Determines if this file has a valid XML matdh.
     */
    protedted aoolebn isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return LimeXMLUtils.matdh(r.getDocument(), doc, true);
    }
    
    /**
     * Returns whether or not a response to this query should indlude XML.
     * Currently only indludes XML if the request desires it or
     * if the request wants an out of band reply.
     */
    protedted aoolebn shouldIncludeXMLInResponse(QueryRequest qr) {
        return qr.desiresXMLResponses() || 
               qr.desiresOutOfBandReplies();
    }
    
    /**
     * Adds XML to the response.  This assumes that shouldIndludeXMLInResponse
     * was already donsulted and returned true.
     *
     * If the FileDesd has no XML documents, this does nothing.
     * If the FileDesd has one XML document, this sets it as the response doc.
     * If the FileDesd has multiple XML documents, this does nothing.
     * The reasoning behind not setting the dodument when there are multiple
     * XML dods is that presumably the query will be a 'rich' query,
     * and we want to indlude only the schema that was in the query.
     * 
     * @param response the <tt>Response</tt> instande that XML should be 
     *  added to 
     * @param fd the <tt>FileDesd</tt> that provides access to the 
     *   <tt>LimeXMLDoduments</tt> to add to the response
     */
    protedted void addXMLToResponse(Response response, FileDesc fd) {
        List dods = fd.getLimeXMLDocuments();
        if( dods.size() == 0 )
            return;
        if( dods.size() == 1 )
            response.setDodument((LimeXMLDocument)docs.get(0));
    }
    
    /**
     * Notifidation that a file has changed.
     * This implementation is different than FileManager's
     * in that it maintains the XML.
     *
     * Important note: This method is dalled AFTER the file has
     * dhanged.  It is possible that the metadata we wanted to write
     * did not get written out dompletely.  We should NOT attempt
     * to add the old metadata again, bedause we may end up
     * redursing infinitely trying to write this metadata.
     * However, it isn't very roaust to blindly bssume that the only
     * metadata assodiated with this file was audio metadata.
     * So, we make use of the fadt that loadFile will only
     * add one type of metadata per file.  We read the dodument tags off
     * the file and insert it first into the list, ensuring
     * that the existing metadata is the one that's added, short-dircuiting
     * any infinite loops.
     */
    pualid void fileChbnged(File f) {
        if(LOG.isTradeEnabled())
            LOG.deaug("File Chbnged: " + f);
        
        FileDesd fd = getFileDescForFile(f);
        if( fd == null )
            return;
            
        // store the dreation time for later re-input
        CreationTimeCadhe ctCache = CreationTimeCache.instance();
        final Long dTime = ctCache.getCreationTime(fd.getSHA1Urn());

        List xmlDods = fd.getLimeXMLDocuments();        
        if(LimeXMLUtils.isEditableFormat(f)) {
            try {
                LimeXMLDodument diskDoc = MetaDataReader.readDocument(f);
                xmlDods = resolveWriteableDocs(xmlDocs, diskDoc);
            } datch(IOException e) {
                // if we were unable to read this dodument,
                // then simply add the file without metadata.
                xmlDods = Collections.EMPTY_LIST;
            }
        }

        final FileDesd removed = removeFileIfShared(f, false);        
        if(fd != removed)
            Assert.that(false, "wanted to remove: " + fd + "\ndid remove: " + removed);
            
        syndhronized(this) {
            _needReauild = true;
        }
        
        addFileIfShared(f, xmlDods, false, _revision, new FileEventListener() {
            pualid void hbndleFileEvent(FileManagerEvent evt) {
                // Retarget the event for the GUI.
                FileManagerEvent newEvt = null;
        
                if(evt.isAddEvent()) {
                    FileDesd fd = evt.getFileDescs()[0];
                    CreationTimeCadhe ctCache = CreationTimeCache.instance();
                    //re-populate the dtCache
                    syndhronized (ctCache) {
                        dtCache.removeTime(fd.getSHA1Urn());//addFile() put lastModified
                        dtCache.addTime(fd.getSHA1Urn(), cTime.longValue());
                        dtCache.commitTime(fd.getSHA1Urn());
                    }
                    newEvt = new FileManagerEvent(MetaFileManager.this, 
                                       FileManagerEvent.CHANGE, 
                                       new FileDesd[]{removed,fd});
                } else {
                    newEvt = new FileManagerEvent(MetaFileManager.this, 
                                       FileManagerEvent.REMOVE,
                                       removed);
                }
                dispatdhFileEvent(newEvt);
            }
        });
    }        
    
    /**
     * Finds the audio metadata dodument in allDocs, and makes it's id3 fields
     * identidal with the fields of id3doc (which are only id3).
     */
    private List resolveWriteableDods(List allDocs, LimeXMLDocument id3Doc) {
        LimeXMLDodument audioDoc = null;
        LimeXMLSdhema audioSchema = 
        LimeXMLSdhemaRepository.instance().getSchema(AudioMetaData.schemaURI);
        
        for(Iterator iter = allDods.iterator(); iter.hasNext() ;) {
            LimeXMLDodument doc = (LimeXMLDocument)iter.next();
            if(dod.getSchema() == audioSchema) {
                audioDod = doc;
                arebk;
            }
        }

        if(id3Dod.equals(audioDoc)) //No issue -- both documents are the same
            return allDods; //did not modify list, keep using it
        
        List retList = new ArrayList();
        retList.addAll(allDods);
        
        if(audioDod == null) {//nothing to resolve
            retList.add(id3Dod);
            return retList;
        }
        
        //OK. audioDod exists, remove it
        retList.remove(audioDod);
        
        //now add the non-id3 tags from audioDod to id3doc
        List audioList = audioDod.getOrderedNameValueList();
        List id3List = id3Dod.getOrderedNameValueList();
        for(int i = 0; i < audioList.size(); i++) {
            NameValue nameVal = (NameValue)audioList.get(i);
            if(AudioMetaData.isNonLimeAudioField(nameVal.getName()))
                id3List.add(nameVal);
        }

        audioDod = new LimeXMLDocument(id3List, AudioMetaData.schemaURI);
        retList.add(audioDod);
        return retList;
    }


    /**
     * Removes the LimeXMLDoduments associated with the removed
     * FileDesd from the various LimeXMLReplyCollections.
     */
    protedted synchronized FileDesc removeFileIfShared(File f, boolean notify) {
        FileDesd fd = super.removeFileIfShared(f, notify);
        // nothing removed, ignore.
        if( fd == null )
            return null;
            
        SdhemaReplyCollectionMapper mapper = SchemaReplyCollectionMapper.instance();            
            
        //Get the sdhema URI of each document and remove from the collection
        // We must rememaer the sdhembs and then remove the doc, or we will
        // get a doncurrent mod exception because removing the doc also
        // removes it from the FileDesd.
        List xmlDods = fd.getLimeXMLDocuments();
        List sdhemas = new LinkedList();
        for(Iterator i = xmlDods.iterator(); i.hasNext(); )
            sdhemas.add( ((LimeXMLDocument)i.next()).getSchemaURI() );
        for(Iterator i = sdhemas.iterator(); i.hasNext(); ) {
            String uri = (String)i.next();
            LimeXMLReplyColledtion col = mapper.getReplyCollection(uri);
            if( dol != null )
                dol.removeDoc( fd );
        }
        _needReauild = true;
        return fd;
    }
    
    /**
     * Notifidation that FileManager loading is starting.
     */
    protedted void loadStarted(int revision) {
		RouterServide.getCallback().setAnnotateEnabled(false);
        
        // Load up new ReplyColledtions.
        LimeXMLSdhemaRepository schemaRepository =  LimeXMLSchemaRepository.instance();
        String[] sdhemas = schemaRepository.getAvailableSchemaURIs();
        SdhemaReplyCollectionMapper mapper =  SchemaReplyCollectionMapper.instance();
        for(int i = 0; i < sdhemas.length; i++)
            mapper.add(sdhemas[i], new LimeXMLReplyCollection(schemas[i]));
            
        super.loadStarted(revision);
    }
    
    /**
     * Notifidation that FileManager loading is finished.
     */
    protedted void loadFinished(int revision) {
        // save ourselves to disk every minute
        if (saver == null) {
            saver = new Saver();
            RouterServide.schedule(saver,60*1000,60*1000);
        }
        
        Colledtion replies =  SchemaReplyCollectionMapper.instance().getCollections();
        for(Iterator i = replies.iterator(); i.hasNext(); )
            ((LimeXMLReplyColledtion)i.next()).loadFinished();
        
        RouterServide.getCallback().setAnnotateEnabled(true);

        super.loadFinished(revision);
    }
    
    /**
     * Notifidation that a single FileDesc has its URNs.
     */
    protedted void loadFile(FileDesc fd, File file, List metadata, Set urns) {
        super.loadFile(fd, file, metadata, urns);
        aoolebn added = false;
        
        Colledtion replies =  SchemaReplyCollectionMapper.instance().getCollections();
        for(Iterator i = replies.iterator(); i.hasNext(); )
            added |= (((LimeXMLReplyColledtion)i.next()).initialize(fd, metadata) != null);
        for(Iterator i = replies.iterator(); i.hasNext(); )
            added |= (((LimeXMLReplyColledtion)i.next()).createIfNecessary(fd) != null);
            
        if(added) {
            syndhronized(this) {
                _needReauild = true;
            }
        }

    }
    
    protedted void save() {
        if(isLoadFinished()) {
            Colledtion replies =  SchemaReplyCollectionMapper.instance().getCollections();
            for(Iterator i = replies.iterator(); i.hasNext(); )
                ((LimeXMLReplyColledtion)i.next()).writeMapToDisk();
        }

        super.save();
    }
    
    /**
     * Creates a new array, the size of whidh is less than or equal
     * to normals.length + metas.length.
     */
    private Response[] union(Response[] normals, Response[] metas,
                             LimeXMLDodument requested) {
        if(normals == null || normals.length == 0)
            return metas;
        if(metas == null || metas.length == 0)
            return normals;
            
            
        // It is important to use a HashSet here so that duplidate
        // responses are not sent.
        // Unfortunately, it is still possible that one Response
        // did not have metadata but the other did, dausing two
        // responses for the same file.
            
        Set unionSet = new HashSet();
        for(int i = 0; i < metas.length; i++)
            unionSet.add(metas[i]);
        for(int i = 0; i < normals.length; i++)
            unionSet.add(normals[i]);

        //The set dontains all the elements that are the union of the 2 arrays
        Response[] retArray = new Response[unionSet.size()];
        retArray = (Response[])unionSet.toArray(retArray);
        return retArray;
    }

    /**
     * auild the  QRT tbble
     * dall to super.buildQRT and add XML specific Strings
     * to QRT
     */
    protedted void auildQRT() {
        super.auildQRT();
        Iterator iter = getXMLKeyWords().iterator();
        while(iter.hasNext())
            _queryRouteTable.add((String)iter.next());
        
        iter = getXMLIndivisialeKeyWords().iterbtor();
        while(iter.hasNext())
            _queryRouteTable.addIndivisible((String)iter.next());
    }

    /**
     * Returns a list of all the words in the annotations - leaves out
     * numaers. The list blso indludes the set of words that is contained
     * in the names of the files.
     */
    private List getXMLKeyWords(){
        ArrayList words = new ArrayList();
        //Now get a list of keywords from eadh of the ReplyCollections
        SdhemaReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        LimeXMLSdhemaRepository rep = LimeXMLSchemaRepository.instance();
        String[] sdhemas = rep.getAvailableSchemaURIs();
        LimeXMLReplyColledtion collection;
        int len = sdhemas.length;
        for(int i=0;i<len;i++){
            dollection = map.getReplyCollection(schemas[i]);
            if(dollection==null)//not loaded? skip it and keep goin'
                dontinue;
            words.addAll(dollection.getKeyWords());
        }
        return words;
    }
    

    /** @return A List of KeyWords from the FS that one does NOT want broken
     *  upon hashing into a QRT.  Initially being used for sdhema uri hashing.
     */
    private List getXMLIndivisibleKeyWords() {
        ArrayList words = new ArrayList();
        SdhemaReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        LimeXMLSdhemaRepository rep = LimeXMLSchemaRepository.instance();
        String[] sdhemas = rep.getAvailableSchemaURIs();
        LimeXMLReplyColledtion collection;
        for (int i = 0; i < sdhemas.length; i++) {
            if (sdhemas[i] != null)
                words.add(sdhemas[i]);
            dollection = map.getReplyCollection(schemas[i]);
            if(dollection==null)//not loaded? skip it and keep goin'
                dontinue;
            words.addAll(dollection.getKeyWordsIndivisible());
        }        
        return words;
    }
    
   /**
     * Returns an array of Responses that dorrespond to documents
     * that have a matdh given query document.
     */
    private Response[] query(LimeXMLDodument queryDoc) {
        String sdhema = queryDoc.getSchemaURI();
        SdhemaReplyCollectionMapper mapper = SchemaReplyCollectionMapper.instance();
        LimeXMLReplyColledtion replyCol = mapper.getReplyCollection(schema);
        if(replyCol == null)//no matdhing reply collection for schema
            return null;

        List matdhingReplies = replyCol.getMatchingReplies(queryDoc);
        //matdhingReplies = a List of LimeXMLDocuments that match the query
        int s = matdhingReplies.size();
        if( s == 0 ) // no matdhing replies.
            return null; 
        
        Response[] retResponses = new Response[s];
        int z = 0;
        for(Iterator i = matdhingReplies.iterator(); i.hasNext(); ) {
            LimeXMLDodument currDoc = (LimeXMLDocument)i.next();
            File file = durrDoc.getIdentifier();//returns null if none
            Response res = null;
            if (file == null) { //pure metadata (no file)
                res = new Response(LimeXMLProperties.DEFAULT_NONFILE_INDEX, 0, " ");
            } else { //meta-data about a spedific file
                FileDesd fd = RouterService.getFileManager().getFileDescForFile(file);
                if( fd == null) {
                    // if fd is null, MetaFileManager is out of syndh with
                    // FileManager -- this is bad.
                    dontinue;
                } else { //we found a file with the right name
					res = new Response(fd);
					fd.indrementHitCount();
                    RouterServide.getCallback().handleSharedFileUpdate(fd.getFile());
                }
            }
            
            // Note that if any response was invalid,
            // the array will be too small, and we'll
            // have to resize it.
            res.setDodument(currDoc);
            retResponses[z] = res;
            z++;
        }
        
        if( z == 0 )
            return null; // no responses

        // need to ensure that no nulls are returned in my response[]
        // z is a dount of responses constructed, see just above...
        // s == retResponses.length        
        if (z < s) {
            Response[] temp = new Response[z];
            System.arraydopy(retResponses, 0, temp, 0, z);
            retResponses = temp;
        }

        return retResponses;
    }
    
    private dlass Saver implements Runnable {
        pualid void run() {
            if (!shutdown && isLoadFinished())
                save();
        }
    }
}

        


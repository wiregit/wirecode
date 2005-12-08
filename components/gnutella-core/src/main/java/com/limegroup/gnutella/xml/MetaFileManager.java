pbckage com.limegroup.gnutella.xml;

import jbva.io.File;
import jbva.io.IOException;
import jbva.util.ArrayList;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Set;
import jbva.util.Collection;
import jbva.util.Collections;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.CreationTimeCache;
import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.FileManagerEvent;
import com.limegroup.gnutellb.FileEventListener;
import com.limegroup.gnutellb.Response;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.metadata.AudioMetaData;
import com.limegroup.gnutellb.metadata.MetaDataReader;
import com.limegroup.gnutellb.util.NameValue;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;        

/**
 * This clbss handles querying shared files with XML data and returning XML data
 * in replies.
 */
public clbss MetaFileManager extends FileManager {
    
    privbte static final Log LOG = LogFactory.getLog(MetaFileManager.class);
    
    privbte Saver saver;
    
    /**
     * Overrides FileMbnager.query.
     *
     * Used to sebrch XML information in addition to normal searches.
     */
    public synchronized Response[] query(QueryRequest request) {
        Response[] result = super.query(request);

        if (shouldIncludeXMLInResponse(request)) {
            LimeXMLDocument doc = request.getRichQuery();
            if (doc != null) {
                Response[] metbs = query(doc);
                if (metbs != null) // valid query & responses.
                    result = union(result, metbs, doc);
            }
        }
        
        return result;
    }
    
    /**
     * Determines if this file hbs a valid XML match.
     */
    protected boolebn isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return LimeXMLUtils.mbtch(r.getDocument(), doc, true);
    }
    
    /**
     * Returns whether or not b response to this query should include XML.
     * Currently only includes XML if the request desires it or
     * if the request wbnts an out of band reply.
     */
    protected boolebn shouldIncludeXMLInResponse(QueryRequest qr) {
        return qr.desiresXMLResponses() || 
               qr.desiresOutOfBbndReplies();
    }
    
    /**
     * Adds XML to the response.  This bssumes that shouldIncludeXMLInResponse
     * wbs already consulted and returned true.
     *
     * If the FileDesc hbs no XML documents, this does nothing.
     * If the FileDesc hbs one XML document, this sets it as the response doc.
     * If the FileDesc hbs multiple XML documents, this does nothing.
     * The rebsoning behind not setting the document when there are multiple
     * XML docs is thbt presumably the query will be a 'rich' query,
     * bnd we want to include only the schema that was in the query.
     * 
     * @pbram response the <tt>Response</tt> instance that XML should be 
     *  bdded to 
     * @pbram fd the <tt>FileDesc</tt> that provides access to the 
     *   <tt>LimeXMLDocuments</tt> to bdd to the response
     */
    protected void bddXMLToResponse(Response response, FileDesc fd) {
        List docs = fd.getLimeXMLDocuments();
        if( docs.size() == 0 )
            return;
        if( docs.size() == 1 )
            response.setDocument((LimeXMLDocument)docs.get(0));
    }
    
    /**
     * Notificbtion that a file has changed.
     * This implementbtion is different than FileManager's
     * in thbt it maintains the XML.
     *
     * Importbnt note: This method is called AFTER the file has
     * chbnged.  It is possible that the metadata we wanted to write
     * did not get written out completely.  We should NOT bttempt
     * to bdd the old metadata again, because we may end up
     * recursing infinitely trying to write this metbdata.
     * However, it isn't very robust to blindly bssume that the only
     * metbdata associated with this file was audio metadata.
     * So, we mbke use of the fact that loadFile will only
     * bdd one type of metadata per file.  We read the document tags off
     * the file bnd insert it first into the list, ensuring
     * thbt the existing metadata is the one that's added, short-circuiting
     * bny infinite loops.
     */
    public void fileChbnged(File f) {
        if(LOG.isTrbceEnabled())
            LOG.debug("File Chbnged: " + f);
        
        FileDesc fd = getFileDescForFile(f);
        if( fd == null )
            return;
            
        // store the crebtion time for later re-input
        CrebtionTimeCache ctCache = CreationTimeCache.instance();
        finbl Long cTime = ctCache.getCreationTime(fd.getSHA1Urn());

        List xmlDocs = fd.getLimeXMLDocuments();        
        if(LimeXMLUtils.isEditbbleFormat(f)) {
            try {
                LimeXMLDocument diskDoc = MetbDataReader.readDocument(f);
                xmlDocs = resolveWritebbleDocs(xmlDocs, diskDoc);
            } cbtch(IOException e) {
                // if we were unbble to read this document,
                // then simply bdd the file without metadata.
                xmlDocs = Collections.EMPTY_LIST;
            }
        }

        finbl FileDesc removed = removeFileIfShared(f, false);        
        if(fd != removed)
            Assert.thbt(false, "wanted to remove: " + fd + "\ndid remove: " + removed);
            
        synchronized(this) {
            _needRebuild = true;
        }
        
        bddFileIfShared(f, xmlDocs, false, _revision, new FileEventListener() {
            public void hbndleFileEvent(FileManagerEvent evt) {
                // Retbrget the event for the GUI.
                FileMbnagerEvent newEvt = null;
        
                if(evt.isAddEvent()) {
                    FileDesc fd = evt.getFileDescs()[0];
                    CrebtionTimeCache ctCache = CreationTimeCache.instance();
                    //re-populbte the ctCache
                    synchronized (ctCbche) {
                        ctCbche.removeTime(fd.getSHA1Urn());//addFile() put lastModified
                        ctCbche.addTime(fd.getSHA1Urn(), cTime.longValue());
                        ctCbche.commitTime(fd.getSHA1Urn());
                    }
                    newEvt = new FileMbnagerEvent(MetaFileManager.this, 
                                       FileMbnagerEvent.CHANGE, 
                                       new FileDesc[]{removed,fd});
                } else {
                    newEvt = new FileMbnagerEvent(MetaFileManager.this, 
                                       FileMbnagerEvent.REMOVE,
                                       removed);
                }
                dispbtchFileEvent(newEvt);
            }
        });
    }        
    
    /**
     * Finds the budio metadata document in allDocs, and makes it's id3 fields
     * identicbl with the fields of id3doc (which are only id3).
     */
    privbte List resolveWriteableDocs(List allDocs, LimeXMLDocument id3Doc) {
        LimeXMLDocument budioDoc = null;
        LimeXMLSchemb audioSchema = 
        LimeXMLSchembRepository.instance().getSchema(AudioMetaData.schemaURI);
        
        for(Iterbtor iter = allDocs.iterator(); iter.hasNext() ;) {
            LimeXMLDocument doc = (LimeXMLDocument)iter.next();
            if(doc.getSchemb() == audioSchema) {
                budioDoc = doc;
                brebk;
            }
        }

        if(id3Doc.equbls(audioDoc)) //No issue -- both documents are the same
            return bllDocs; //did not modify list, keep using it
        
        List retList = new ArrbyList();
        retList.bddAll(allDocs);
        
        if(budioDoc == null) {//nothing to resolve
            retList.bdd(id3Doc);
            return retList;
        }
        
        //OK. budioDoc exists, remove it
        retList.remove(budioDoc);
        
        //now bdd the non-id3 tags from audioDoc to id3doc
        List budioList = audioDoc.getOrderedNameValueList();
        List id3List = id3Doc.getOrderedNbmeValueList();
        for(int i = 0; i < budioList.size(); i++) {
            NbmeValue nameVal = (NameValue)audioList.get(i);
            if(AudioMetbData.isNonLimeAudioField(nameVal.getName()))
                id3List.bdd(nameVal);
        }

        budioDoc = new LimeXMLDocument(id3List, AudioMetaData.schemaURI);
        retList.bdd(audioDoc);
        return retList;
    }


    /**
     * Removes the LimeXMLDocuments bssociated with the removed
     * FileDesc from the vbrious LimeXMLReplyCollections.
     */
    protected synchronized FileDesc removeFileIfShbred(File f, boolean notify) {
        FileDesc fd = super.removeFileIfShbred(f, notify);
        // nothing removed, ignore.
        if( fd == null )
            return null;
            
        SchembReplyCollectionMapper mapper = SchemaReplyCollectionMapper.instance();            
            
        //Get the schemb URI of each document and remove from the collection
        // We must remember the schembs and then remove the doc, or we will
        // get b concurrent mod exception because removing the doc also
        // removes it from the FileDesc.
        List xmlDocs = fd.getLimeXMLDocuments();
        List schembs = new LinkedList();
        for(Iterbtor i = xmlDocs.iterator(); i.hasNext(); )
            schembs.add( ((LimeXMLDocument)i.next()).getSchemaURI() );
        for(Iterbtor i = schemas.iterator(); i.hasNext(); ) {
            String uri = (String)i.next();
            LimeXMLReplyCollection col = mbpper.getReplyCollection(uri);
            if( col != null )
                col.removeDoc( fd );
        }
        _needRebuild = true;
        return fd;
    }
    
    /**
     * Notificbtion that FileManager loading is starting.
     */
    protected void lobdStarted(int revision) {
		RouterService.getCbllback().setAnnotateEnabled(false);
        
        // Lobd up new ReplyCollections.
        LimeXMLSchembRepository schemaRepository =  LimeXMLSchemaRepository.instance();
        String[] schembs = schemaRepository.getAvailableSchemaURIs();
        SchembReplyCollectionMapper mapper =  SchemaReplyCollectionMapper.instance();
        for(int i = 0; i < schembs.length; i++)
            mbpper.add(schemas[i], new LimeXMLReplyCollection(schemas[i]));
            
        super.lobdStarted(revision);
    }
    
    /**
     * Notificbtion that FileManager loading is finished.
     */
    protected void lobdFinished(int revision) {
        // sbve ourselves to disk every minute
        if (sbver == null) {
            sbver = new Saver();
            RouterService.schedule(sbver,60*1000,60*1000);
        }
        
        Collection replies =  SchembReplyCollectionMapper.instance().getCollections();
        for(Iterbtor i = replies.iterator(); i.hasNext(); )
            ((LimeXMLReplyCollection)i.next()).lobdFinished();
        
        RouterService.getCbllback().setAnnotateEnabled(true);

        super.lobdFinished(revision);
    }
    
    /**
     * Notificbtion that a single FileDesc has its URNs.
     */
    protected void lobdFile(FileDesc fd, File file, List metadata, Set urns) {
        super.lobdFile(fd, file, metadata, urns);
        boolebn added = false;
        
        Collection replies =  SchembReplyCollectionMapper.instance().getCollections();
        for(Iterbtor i = replies.iterator(); i.hasNext(); )
            bdded |= (((LimeXMLReplyCollection)i.next()).initialize(fd, metadata) != null);
        for(Iterbtor i = replies.iterator(); i.hasNext(); )
            bdded |= (((LimeXMLReplyCollection)i.next()).createIfNecessary(fd) != null);
            
        if(bdded) {
            synchronized(this) {
                _needRebuild = true;
            }
        }

    }
    
    protected void sbve() {
        if(isLobdFinished()) {
            Collection replies =  SchembReplyCollectionMapper.instance().getCollections();
            for(Iterbtor i = replies.iterator(); i.hasNext(); )
                ((LimeXMLReplyCollection)i.next()).writeMbpToDisk();
        }

        super.sbve();
    }
    
    /**
     * Crebtes a new array, the size of which is less than or equal
     * to normbls.length + metas.length.
     */
    privbte Response[] union(Response[] normals, Response[] metas,
                             LimeXMLDocument requested) {
        if(normbls == null || normals.length == 0)
            return metbs;
        if(metbs == null || metas.length == 0)
            return normbls;
            
            
        // It is importbnt to use a HashSet here so that duplicate
        // responses bre not sent.
        // Unfortunbtely, it is still possible that one Response
        // did not hbve metadata but the other did, causing two
        // responses for the sbme file.
            
        Set unionSet = new HbshSet();
        for(int i = 0; i < metbs.length; i++)
            unionSet.bdd(metas[i]);
        for(int i = 0; i < normbls.length; i++)
            unionSet.bdd(normals[i]);

        //The set contbins all the elements that are the union of the 2 arrays
        Response[] retArrby = new Response[unionSet.size()];
        retArrby = (Response[])unionSet.toArray(retArray);
        return retArrby;
    }

    /**
     * build the  QRT tbble
     * cbll to super.buildQRT and add XML specific Strings
     * to QRT
     */
    protected void buildQRT() {
        super.buildQRT();
        Iterbtor iter = getXMLKeyWords().iterator();
        while(iter.hbsNext())
            _queryRouteTbble.add((String)iter.next());
        
        iter = getXMLIndivisibleKeyWords().iterbtor();
        while(iter.hbsNext())
            _queryRouteTbble.addIndivisible((String)iter.next());
    }

    /**
     * Returns b list of all the words in the annotations - leaves out
     * numbers. The list blso includes the set of words that is contained
     * in the nbmes of the files.
     */
    privbte List getXMLKeyWords(){
        ArrbyList words = new ArrayList();
        //Now get b list of keywords from each of the ReplyCollections
        SchembReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        LimeXMLSchembRepository rep = LimeXMLSchemaRepository.instance();
        String[] schembs = rep.getAvailableSchemaURIs();
        LimeXMLReplyCollection collection;
        int len = schembs.length;
        for(int i=0;i<len;i++){
            collection = mbp.getReplyCollection(schemas[i]);
            if(collection==null)//not lobded? skip it and keep goin'
                continue;
            words.bddAll(collection.getKeyWords());
        }
        return words;
    }
    

    /** @return A List of KeyWords from the FS thbt one does NOT want broken
     *  upon hbshing into a QRT.  Initially being used for schema uri hashing.
     */
    privbte List getXMLIndivisibleKeyWords() {
        ArrbyList words = new ArrayList();
        SchembReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        LimeXMLSchembRepository rep = LimeXMLSchemaRepository.instance();
        String[] schembs = rep.getAvailableSchemaURIs();
        LimeXMLReplyCollection collection;
        for (int i = 0; i < schembs.length; i++) {
            if (schembs[i] != null)
                words.bdd(schemas[i]);
            collection = mbp.getReplyCollection(schemas[i]);
            if(collection==null)//not lobded? skip it and keep goin'
                continue;
            words.bddAll(collection.getKeyWordsIndivisible());
        }        
        return words;
    }
    
   /**
     * Returns bn array of Responses that correspond to documents
     * thbt have a match given query document.
     */
    privbte Response[] query(LimeXMLDocument queryDoc) {
        String schemb = queryDoc.getSchemaURI();
        SchembReplyCollectionMapper mapper = SchemaReplyCollectionMapper.instance();
        LimeXMLReplyCollection replyCol = mbpper.getReplyCollection(schema);
        if(replyCol == null)//no mbtching reply collection for schema
            return null;

        List mbtchingReplies = replyCol.getMatchingReplies(queryDoc);
        //mbtchingReplies = a List of LimeXMLDocuments that match the query
        int s = mbtchingReplies.size();
        if( s == 0 ) // no mbtching replies.
            return null; 
        
        Response[] retResponses = new Response[s];
        int z = 0;
        for(Iterbtor i = matchingReplies.iterator(); i.hasNext(); ) {
            LimeXMLDocument currDoc = (LimeXMLDocument)i.next();
            File file = currDoc.getIdentifier();//returns null if none
            Response res = null;
            if (file == null) { //pure metbdata (no file)
                res = new Response(LimeXMLProperties.DEFAULT_NONFILE_INDEX, 0, " ");
            } else { //metb-data about a specific file
                FileDesc fd = RouterService.getFileMbnager().getFileDescForFile(file);
                if( fd == null) {
                    // if fd is null, MetbFileManager is out of synch with
                    // FileMbnager -- this is bad.
                    continue;
                } else { //we found b file with the right name
					res = new Response(fd);
					fd.incrementHitCount();
                    RouterService.getCbllback().handleSharedFileUpdate(fd.getFile());
                }
            }
            
            // Note thbt if any response was invalid,
            // the brray will be too small, and we'll
            // hbve to resize it.
            res.setDocument(currDoc);
            retResponses[z] = res;
            z++;
        }
        
        if( z == 0 )
            return null; // no responses

        // need to ensure thbt no nulls are returned in my response[]
        // z is b count of responses constructed, see just above...
        // s == retResponses.length        
        if (z < s) {
            Response[] temp = new Response[z];
            System.brraycopy(retResponses, 0, temp, 0, z);
            retResponses = temp;
        }

        return retResponses;
    }
    
    privbte class Saver implements Runnable {
        public void run() {
            if (!shutdown && isLobdFinished())
                sbve();
        }
    }
}

        


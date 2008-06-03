package com.limegroup.gnutella;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.limewire.collection.Function;
import org.limewire.collection.IntSet;
import org.limewire.collection.MultiIterator;
import org.limewire.collection.StringTrie;
import org.limewire.inspection.InspectableForSize;
import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

// TODO split this up further and remove query and response from here,
// or introduce a generic indexing class that can be used
@Singleton
public class SharedFilesKeywordIndexImpl implements SharedFilesKeywordIndex {

    /**
     * Constant for an empty <tt>Response</tt> array to return when there are
     * no matches.
     */
    private static final Response[] EMPTY_RESPONSES = new Response[0];
    
    /**
     * A trie mapping keywords in complete filenames to the indices in _files.
     * Keywords are the tokens when the filename is tokenized with the
     * characters from DELIMITERS as delimiters.
     * 
     * IncompleteFile keywords are NOT stored.
     * 
     * INVARIANT: For all keys k in _keywordTrie, for all i in the IntSet
     * _keywordTrie.get(k), _files[i]._path.substring(k)!=-1. Likewise for all
     * i, for all k in _files[i]._path where _files[i] is not an
     * IncompleteFileDesc, _keywordTrie.get(k) contains i.
     * 
     * Not threadsafe, hold lock on field.
     */
    @InspectableForSize("size of keyword trie")
    private final StringTrie<IntSet> keywordTrie = new StringTrie<IntSet>(true);
    
    /**
     * A trie mapping keywords in complete filenames to the indices in _files.
     * Contains ONLY incomplete keywords.
     * 
     * Not threadsafe, hold lock on field.
     */
    @InspectableForSize("size of incomplete keyword trie")
    private final StringTrie<IntSet> incompleteKeywordTrie = new StringTrie<IntSet>(true);
    
    private final Provider<CreationTimeCache> creationTimeCache;

    private final Provider<ResponseFactory> responseFactory;

    private final FileManager fileManager;

    private final Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper;

    private final ActivityCallback activityCallback;

    @Inject
    public SharedFilesKeywordIndexImpl(FileManager fileManager, Provider<CreationTimeCache> creationTimeCache,
            Provider<ResponseFactory> responseFactory, Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper,
            ActivityCallback activityCallback) {
        this.fileManager = fileManager;
        this.creationTimeCache = creationTimeCache;
        this.responseFactory = responseFactory;
        this.schemaReplyCollectionMapper = schemaReplyCollectionMapper;
        this.activityCallback = activityCallback;
    }
    
    public Response[] query(QueryRequest request) {
        Response[] result = queryInternal(request);
        if (request.shouldIncludeXMLInResponse()) {
            LimeXMLDocument doc = request.getRichQuery();
            if (doc != null) {
                Response[] metas = query(doc);
                if (metas != null) // valid query & responses.
                    result = union(result, metas);
            }
        }
        return result;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#query(com.limegroup.gnutella.messages.QueryRequest)
     */
    public Response[] queryInternal(QueryRequest request) {
        String str = request.getQuery();
        boolean includeXML = request.shouldIncludeXMLInResponse();

        //Special case: return up to 3 of your 'youngest' files.
        if (request.isWhatIsNewRequest()) 
            return respondToWhatIsNewRequest(request, includeXML);

        //Special case: return everything for Clip2 indexing query ("    ") and
        //browse queries ("*.*").  If these messages had initial TTLs too high,
        //StandardMessageRouter will clip the number of results sent on the
        //network.  Note that some initial TTLs are filterd by GreedyQuery
        //before they ever reach this point.
        if (str.equals(QueryRequest.INDEXING_QUERY) || str.equals(QueryRequest.BROWSE_QUERY))
            return EMPTY_RESPONSES;

        //Normal case: query the index to find all matches.  TODO: this
        //sometimes returns more results (>255) than we actually send out.
        //That's wasted work.
        //Trie requires that getPrefixedBy(String, int, int) passes
        //an already case-changed string.  Both search & urnSearch
        //do this kind of match, so we canonicalize the case for them.
        synchronized (keywordTrie) {
            str = keywordTrie.canonicalCase(str);
        }
        IntSet matches = search(str, null, request.desiresPartialResults());
        if(request.getQueryUrns().size() > 0)
            matches = urnSearch(request.getQueryUrns(),matches);
        
        if (matches==null)
            return EMPTY_RESPONSES;

        List<Response> responses = new LinkedList<Response>();
        final MediaType.Aggregator filter = MediaType.getAggregator(request);
        LimeXMLDocument doc = request.getRichQuery();

        // Iterate through our hit indices to create a list of results.
        for (IntSet.IntSetIterator iter=matches.iterator(); iter.hasNext();) { 
            int i = iter.next();
            FileDesc desc = fileManager.get(i);
            assert desc != null : "unexpected null in FileManager for query:\n"+ request;

            if ((filter != null) && !filter.allow(desc.getFileName()))
                continue;

            desc.incrementHitCount();
            activityCallback.handleSharedFileUpdate(desc.getFile());

            Response resp = responseFactory.get().createResponse(desc);
            if(includeXML) {
                addXMLToResponse(resp, desc);
                if(doc != null && resp.getDocument() != null &&
                   !isValidXMLMatch(resp, doc))
                    continue;
            }
            responses.add(resp);
        }
        if (responses.size() == 0)
            return EMPTY_RESPONSES;
        return responses.toArray(new Response[responses.size()]);
    }
    
    private static boolean isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return LimeXMLUtils.match(r.getDocument(), doc, true);
    }

    /**
     * Find all files with matching full URNs
     */
    private IntSet urnSearch(Iterable<URN> urnsIter, IntSet priors) {
        IntSet ret = priors;
        for(URN urn : urnsIter) {
            IntSet hits = fileManager.getIndicesForUrn(urn);
            if(hits!=null) {
                // double-check hits to be defensive (not strictly needed)
                IntSet.IntSetIterator iter = hits.iterator();
                while(iter.hasNext()) {
                    FileDesc fd = fileManager.get(iter.next());
                    // If the file is unshared or an incomplete file
                    // DO NOT SEND IT.
                    if(fd == null || fd instanceof IncompleteFileDesc)
                        continue;
                    if(fd.containsUrn(urn)) {
                        // still valid
                        if(ret==null) ret = new IntSet();
                        ret.add(fd.getIndex());
                    } 
                }
            }
        }
        return ret;
    }
     
    /**
     * Responds to a what is new request.
     */
    private Response[] respondToWhatIsNewRequest(QueryRequest request, 
                                                 boolean includeXML) {
        // see if there are any files to send....
        // NOTE: we only request up to 3 urns.  we don't need to worry
        // about partial files because we don't add them to the cache.
        // NOTE: this doesn't return Store files. getNewestUrns only 
        //       returns the top 3 shared files
        List<URN> urnList = creationTimeCache.get().getFiles(request, 3);
        if (urnList.size() == 0)
            return EMPTY_RESPONSES;
        
        // get the appropriate responses
        Response[] resps = new Response[urnList.size()];
        for (int i = 0; i < urnList.size(); i++) {
            URN currURN = urnList.get(i);
            FileDesc desc = fileManager.getFileDescForUrn(currURN);
            
            // should never happen since we don't add times for IFDs and
            // we clear removed files...
            if ((desc==null) || (desc instanceof IncompleteFileDesc))
                throw new RuntimeException("Bad Rep - No IFDs allowed!");
            
            // Formulate the response
            Response r = responseFactory.get().createResponse(desc);
            if(includeXML)
                addXMLToResponse(r, desc);
            
            // Cache it
            resps[i] = r;
        }
        return resps;
    }

    public void clear() {
        keywordTrie.clear();
        incompleteKeywordTrie.clear();
    }
    
    public void handleFileEvent(FileManagerEvent evt) {
        // building tries should be fast and non-blocking so can be done in
        // dispatch thread
        FileDesc[] fileDescs = evt.getFileDescs();
        switch (evt.getType()) {
        case ADD_FILE:
            addFileDescs(fileDescs);
            break;
        case REMOVE_FILE:
            removeFileDescs(fileDescs);
            break;
        case RENAME_FILE:
            removeFileDescs(fileDescs[0]);
            addFileDescs(fileDescs[1]);
            break;
        case CHANGE_FILE:
            addFileDescs(fileDescs);
            break;
        case FILEMANAGER_LOADED:
            trim();
            break;
        case FILEMANAGER_LOADING:
            clear();
            break;
        }
    }
    
    private void removeFileDescs(FileDesc...fileDescs) {
        for (FileDesc fileDesc : fileDescs) {
            if (fileDesc instanceof IncompleteFileDesc) {
                removeKeywords(incompleteKeywordTrie, fileDesc);
            } else {
                removeKeywords(keywordTrie, fileDesc);
            }
        }
    }
    
    private void addFileDescs(FileDesc...fileDescs) {
        boolean indexIncompleteFiles = SharingSettings.ALLOW_PARTIAL_SHARING.getValue()
        && SharingSettings.LOAD_PARTIAL_KEYWORDS.getValue();
        for (FileDesc fileDesc : fileDescs) {
            if (fileDesc instanceof IncompleteFileDesc) {
                IncompleteFileDesc ifd = (IncompleteFileDesc)fileDesc;
                if (indexIncompleteFiles && ifd.hasUrnsAndPartialData()) {
                    loadKeywords(incompleteKeywordTrie, fileDesc);
                }
            } else if(fileManager.isFileShared(fileDesc.getFile())){
                loadKeywords(keywordTrie, fileDesc);
            }
        }
    }
    
    /**
     * @param trie to update
     * @param fd to load keywords from
     */
    private void loadKeywords(StringTrie<IntSet> trie, FileDesc fd) {
        // Index the filename.  For each keyword...
        String[] keywords = extractKeywords(fd);
        
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            synchronized (trie) {
                //Ensure the _keywordTrie has a set of indices associated with keyword.
                IntSet indices = trie.get(keyword);
                if (indices == null) {
                    indices = new IntSet();
                    trie.add(keyword, indices);
                }
                //Add fileIndex to the set.
                indices.add(fd.getIndex());
            }
        }
    }
    

    private void removeKeywords(StringTrie<IntSet> trie, FileDesc fd) {
        //Remove references to this from index.
        String[] keywords = extractKeywords(fd);
        for (int j = 0; j < keywords.length; j++) {
            String keyword = keywords[j];
            synchronized (trie) {
                IntSet indices = trie.get(keyword);
                if (indices != null) {
                    indices.remove(fd.getIndex());
                    if (indices.size() == 0)
                        trie.remove(keyword);
                }
            }
        }        
    }
    
    /**
     * Returns a set of indices of files matching q, or null if there are no
     * matches.  Subclasses may override to provide different notions of
     * matching.  The caller of this method must not mutate the returned
     * value.
     */
    protected IntSet search(String query, IntSet priors, boolean partial) {
        //As an optimization, we lazily allocate all sets in case there are no
        //matches.  TODO2: we can avoid allocating sets when getPrefixedBy
        //returns an iterator of one element and there is only one keyword.
        IntSet ret=priors;

        //For each keyword in the query....  (Note that we avoid calling
        //StringUtils.split and take advantage of Trie's offset/limit feature.)
        for (int i=0; i<query.length(); ) { 
            if (QueryUtils.isDelimiter(query.charAt(i))) {
                i++;
                continue;
            }
            int j;
            for (j=i+1; j<query.length(); j++) {
                if (QueryUtils.isDelimiter(query.charAt(j)))
                    break;
            }

            //Search for keyword, i.e., keywords[i...j-1].
            Iterator<IntSet> iter;
            synchronized (keywordTrie) {
                iter = keywordTrie.getPrefixedBy(query, i, j);
            }
            if (SharingSettings.ALLOW_PARTIAL_SHARING.getValue() &&
                    SharingSettings.ALLOW_PARTIAL_RESPONSES.getValue() &&
                    partial) {
                Iterator<IntSet> incompleteIndices;
                synchronized (incompleteKeywordTrie) {
                    incompleteIndices = incompleteKeywordTrie.getPrefixedBy(query, i, j);
                }
                iter = new MultiIterator<IntSet>(iter, incompleteIndices);
            }

            synchronized (keywordTrie) {
                synchronized (incompleteKeywordTrie) {
                    if(iter.hasNext()) {
                        //Got match.  Union contents of the iterator and store in
                        //matches.  As an optimization, if this is the only keyword and
                        //there is only one set returned, return that set without 
                        //copying.
                        IntSet matches=null;
                        while (iter.hasNext()) {                
                            IntSet s= iter.next();
                            if (matches == null) {
                                if (i==0 && j==query.length() && !(iter.hasNext()))
                                    return s;
                                matches=new IntSet();
                            }
                            matches.addAll(s);
                        }
                        
                        //Intersect matches with ret.  If ret isn't allocated,
                        //initialize to matches.
                        if (ret == null)   
                            ret = matches;
                        else
                            ret.retainAll(matches);
                    } else {
                        //No match.  Optimization: no matches for keyword => failure
                        return null;
                    }
                    
                    //Optimization: no matches after intersect => failure
                    if (ret.size() == 0)
                        return null;        
                    i=j;
                }
            }
        } 
        if (ret==null || ret.size()==0)
            return null;
        return ret;
    }
    
    /**
     * Adds XML to the response. This assumes that shouldIncludeXMLInResponse
     * was already consulted and returned true.
     * 
     * If the FileDesc has no XML documents, this does nothing. If the FileDesc
     * has one XML document, this sets it as the response doc. If the FileDesc
     * has multiple XML documents, this does nothing. The reasoning behind not
     * setting the document when there are multiple XML docs is that presumably
     * the query will be a 'rich' query, and we want to include only the schema
     * that was in the query.
     * 
     * @param response the <tt>Response</tt> instance that XML should be added
     *        to
     * @param fd the <tt>FileDesc</tt> that provides access to the
     *        <tt>LimeXMLDocuments</tt> to add to the response
     */
    private void addXMLToResponse(Response response, FileDesc fd) {
        List<LimeXMLDocument> docs = fd.getLimeXMLDocuments();
        if (docs.size() == 1)
            response.setDocument(docs.get(0));
    }


    /**
     * Utility method to perform standardized keyword extraction for the given
     * <tt>FileDesc</tt>.  This handles extracting keywords according to 
     * locale-specific rules.
     * 
     * @param fd the <tt>FileDesc</tt> containing a file system path with 
     *  keywords to extact
     * @return an array of keyword strings for the given file
     */
    private static String[] extractKeywords(FileDesc fd) {
        return StringUtils.split(I18NConvert.instance().getNorm(fd.getPath()), 
            QueryUtils.DELIMITERS);
    }

    /** Ensures that this's index takes the minimum amount of space.  Only
     *  affects performance, not correctness; hence no modifies clause. */
    private void trim() {
        for (StringTrie<IntSet> trie : new StringTrie[] { keywordTrie, incompleteKeywordTrie }) {
            synchronized (trie) {
                trie.trim(new Function<IntSet, IntSet>() {
                    public IntSet apply(IntSet intSet) {
                        intSet.trim();
                        return intSet;
                    }
                });
            }
        }
    }

    /**
     * Creates a new array, the size of which is less than or equal to
     * normals.length + metas.length.
     */
    private static Response[] union(Response[] normals, Response[] metas) {
        if (normals == null || normals.length == 0)
            return metas;
        if (metas == null || metas.length == 0)
            return normals;

        // It is important to use a HashSet here so that duplicate
        // responses are not sent.
        // Unfortunately, it is still possible that one Response
        // did not have metadata but the other did, causing two
        // responses for the same file.

        Set<Response> unionSet = new HashSet<Response>();
        for (Response meta : metas)
            unionSet.add(meta);
        for (Response normal : normals)
            unionSet.add(normal);

        // The set contains all the elements that are the union of the 2 arrays
        Response[] retArray = new Response[unionSet.size()];
        retArray = unionSet.toArray(retArray);
        return retArray;
    }

    

    /**
     * Returns an array of Responses that correspond to documents that have a
     * match given query document.
     */
    private Response[] query(LimeXMLDocument queryDoc) {
        String schema = queryDoc.getSchemaURI();
        LimeXMLReplyCollection replyCol = schemaReplyCollectionMapper.get().getReplyCollection(schema);
        if (replyCol == null)// no matching reply collection for schema
            return null;

        List<LimeXMLDocument> matchingReplies = replyCol.getMatchingReplies(queryDoc);
        // matchingReplies = a List of LimeXMLDocuments that match the query
        int s = matchingReplies.size();
        if (s == 0) // no matching replies.
            return null;

        Response[] retResponses = new Response[s];
        int z = 0;
        for (LimeXMLDocument currDoc : matchingReplies) {
            File file = currDoc.getIdentifier();// returns null if none
            Response res = null;
            if (file == null) { // pure metadata (no file)
                res = responseFactory.get().createPureMetadataResponse();
            } else { // meta-data about a specific file
                FileDesc fd = fileManager.getSharedFileDescForFile(file);
                if (fd == null || fd instanceof IncompleteFileDesc) {
                    // fd == null is bad -- would mean MetaFileManager is out of sync.
                    // fd incomplete should never happen, but apparently is somehow...
                    // fd is store file, shouldn't be returning query hits for it then..
                    continue;
                } else { // we found a file with the right name
                    res = responseFactory.get().createResponse(fd);
                    fd.incrementHitCount();
                    activityCallback.handleSharedFileUpdate(fd.getFile());
                }
            }

            // Note that if any response was invalid,
            // the array will be too small, and we'll
            // have to resize it.
            res.setDocument(currDoc);
            retResponses[z] = res;
            z++;
        }

        if (z == 0)
            return null; // no responses

        // need to ensure that no nulls are returned in my response[]
        // z is a count of responses constructed, see just above...
        // s == retResponses.length
        if (z < s) {
            Response[] temp = new Response[z];
            System.arraycopy(retResponses, 0, temp, 0, z);
            retResponses = temp;
        }

        return retResponses;
    }

}

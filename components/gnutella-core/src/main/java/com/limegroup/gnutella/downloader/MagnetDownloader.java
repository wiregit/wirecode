package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;

/**
 * A ManagedDownloader for MAGNET URIs.  Unlike a ManagedDownloader, a
 * MagnetDownloader need not have an initial RemoteFileDesc.  Instead it can be
 * started with various combinations of the following:
 * <ul>
 * <li>initial URL (exact source)
 * <li>hash/URN (exact topic)
 * <li>file name (display name)
 * <li>search keywords (keyword topic)
 * </ul>
 * Names in parentheses are those given by the MAGNET specification at
 * http://magnet-uri.sourceforge.net/magnet-draft-overview.txt
 * <p>
 * Implementation note: this uses ManagedDownloader to try the initial download
 * location.  Unfortunately ManagedDownloader requires RemoteFileDesc's.  We can
 * fake up most of the RFD fields, but size presents problems.
 * ManagedDownloader depends on size for swarming purposes.  It is possible to
 * redesign the swarming algorithm to work around the lack of size, but this is
 * complex, especially with regard to HTTP/1.1 swarming.  For this reason, we
 * simply make a HEAD request to get the content length before starting the
 * download.  
 */
public class MagnetDownloader extends ManagedDownloader implements Serializable {
    /** Prevent versioning problems. */
    static final long serialVersionUID = 9092913030585214105L;
    /** The string to prefix download files with in the rare case that we don't
     *  have a download name and can't calculate one from the URN. */
    static final String DOWNLOAD_PREFIX="MAGNET download from ";

    /** The string to use for requery attempts, or null if not provided.
     *  INVARIANT: _textQuery!=null || _urn!=null */
    private String _textQuery;
    /** The URN of the file we're looking for, or null if not provided. */
    private URN _urn;
    /** The download filename, or null if not provided. */
    private String _filename;
    /** The default location, or null if not provided.  Not currently used, but
     *  may be useful later. */
    private String[] _defaultURLs;

    /**
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURLs</tt>, if specified. If that fails, or if defaultURLs does
     * not provide alternate locations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (Note that at least one must be
     * non-null.)  If <tt>filename</tt> is specified, it will be used as the
     * name of the complete file; otherwise it will be taken from any search
     * results or guessed from <tt>defaultURLs</tt>.
     *
     * @param manager controls download queuing; passed to superclass
     * @param filemanager shares saved files; passed to superclass
     * @param ifm maintains blocks stored on disk; passed to superclass
     * @param callback notifies GUI of updates; passed to superclass
     * @param urn the hash of the file (exact topic), or null if unknown
     * @param textQuery requery keywords (keyword topic), or null if unknown
     * @param filename the final file name, or null if unknown
     * @param defaultURLs the initial locations to try (exact source), or null 
     *  if unknown
     * @exception IllegalArgumentException both urn and textQuery are null 
     */
    public MagnetDownloader(DownloadManager manager,
                            FileManager filemanager,
                            IncompleteFileManager ifm,
                            ActivityCallback callback,
                            URN urn,
                            String textQuery,
                            String filename,
                            String [] defaultURLs)
            throws IllegalArgumentException {
        //Initialize superclass with no locations.  We'll add the default
        //location when the download control thread calls tryAllDownloads.
        super(manager, 
              new RemoteFileDesc[0],
              filemanager, 
              ifm, 
              callback);

        this._textQuery=textQuery;
        this._urn=urn;
        this._filename=filename;
        this._defaultURLs=defaultURLs;
    }
    
    /**
     * Overrides ManagedDownloader to ensure that the default location is tried.
     */
    protected void tryAllDownloads() {     

		for (int i = 0; _defaultURLs != null && i < _defaultURLs.length; i++) {
			//Send HEAD request to default location (if present)to get its size.
			//This can block, so it must be done here instead of in constructor.
			//See class overview and ManagedDownloader.tryAllDownloads.
			RemoteFileDesc defaultRFD=createRemoteFileDesc(_defaultURLs[i],
														   _filename,
														   _urn);
System.out.println("RFD: "+defaultRFD);
			if (defaultRFD!=null) {
				//Add the faked up location before starting download. Note that 
				//we must force ManagedDownloader to accept this RFD in case 
				//it has no hash and a name that doesn't match the search 
				//keywords.
				boolean added=super.addDownloadForced(defaultRFD);
				Assert.that(added, "Download rfd not accepted "+defaultRFD);
			}
		}

        //Start the downloads for real.
        super.tryAllDownloads();
    }


    /** 
     * Creates a faked-up RemoteFileDesc to pass to ManagedDownloader.  If a URL
     * is provided, issues a HEAD request to get the file size.  If this fails,
     * returns null.  Package-access and static for easy testing.
     */
    static RemoteFileDesc createRemoteFileDesc(String defaultURL,
                                               String filename,
                                               URN urn) {
        if (defaultURL==null)
            return null;

        try {
System.out.println("Creating Remote:"+defaultURL);
            URL url=new URL(defaultURL);
System.out.println("URL:"+url);
            int port=url.getPort();
            if (port<0)
                port=80;      //assume default for HTTP (not 6346)

            Set urns=new HashSet(1);
            if (urn!=null)
                urns.add(urn);
            
            return new URLRemoteFileDesc(
                url.getHost(),  //TODO: can this be null?
                port,
                0l,             //index--doesn't matter since we won't push
                filename(filename, url),
                contentLength(url),
                new byte[16],   //GUID--doesn't matter since we won't push
                SpeedConstants.T3_SPEED_INT,
                false,          //no chat support
                3,              //four [sic] star quality
                false,          //no browse host
                null,           //no metadata
                urns,
                url);           //url for GET request
        } catch (IOException e) {
System.out.println("Exception:"+e);
            return null;
        }

    } 

    /** Returns the filename to use for the download, guessed if necessary. 
     *  Package-access and static for easy testing. 
     *  @param filename the filename to use if non-null
     *  @param url the URL for the resource, which must not be null */
    static String filename(String filename, URL url) {
        //If the URI specified a download name, use that.
        if (filename!=null)
            return filename;

        //If the URL has a filename, return that.  Remember that URL.getFile()
        //may include directory information, e.g., "/path/file.txt" or "/path/".
        //It also returns "" if no file part.
        String path=url.getFile();   
        if (path.length()>0) {
            int i=path.lastIndexOf('/');
            if (i<0)
                return path;                  //e.g., "file.txt"
            if (i>=0 && i<(path.length()-1))
                return path.substring(i+1);   //e.g., "/path/to/file"
        }
         
        //In the rare case of no filename ("http://www.limewire.com" or
        //"http://www.limewire.com/path/"), just make something up.
        return DOWNLOAD_PREFIX+url.getHost();        
    }

    /** Returns the length of the content at the given URL. 
     *  @exception IOException couldn't find the length for some reason */
    private static int contentLength(URL url) throws IOException {
        URLConnection connection=null;
        try {
            //Send a HEAD request to the URL.
            connection=url.openConnection();
            if (! (connection instanceof HttpURLConnection))
                throw new IOException();
            HttpURLConnection httpConnection=(HttpURLConnection)connection;
            httpConnection.setRequestMethod("HEAD");
            //Send User-Agent to bypass browser blocking.
            httpConnection.setRequestProperty("User-Agent",
                                              CommonUtils.getHttpServer());
            //Extract Content-length, but only if the response was 200 OK.
            //Generally speaking any 2xx response is ok, but in this situation
            //we expect only 200.
            if (httpConnection.getResponseCode()!=HttpURLConnection.HTTP_OK)
                throw new IOException("No 200 OK");
            int length=httpConnection.getContentLength();
            if (length<0)
                throw new IOException("No content length");
            return length;
        } finally {
            if (connection!=null && (connection instanceof HttpURLConnection))
                ((HttpURLConnection)connection).disconnect();
        }
    }

    ////////////////////////////// Requery Logic ///////////////////////////

    /** 
     * Overrides ManagedDownloader to ensure that the first requery happens as
     * soon as necessary.  This will happen, for example, if there was no
     * initial location.  
     */
    protected long nextRequeryTime(int requeries) {
        if (requeries==0)
            return System.currentTimeMillis();   //now!
        else
            return super.nextRequeryTime(requeries);        
    }

    /** 
     * Overrides ManagedDownloader to use the query words and hash (if any)
     * specified by the MAGNET URI.  The initial requery is unmarked, bypassing
     * global rate limiting.
     */
    protected QueryRequest newRequery(int numRequeries) {        
        Set queryUrns=null;
        if (_urn!=null) {
            queryUrns=new HashSet(1);
            queryUrns.add(_urn);
        }

        boolean isRequery=numRequeries!=0;
        return new QueryRequest(QueryRequest.newQueryGUID(isRequery),
                                SettingsManager.instance().getTTL(),
                                0,                 //speed
                                _textQuery==null ? "" : _textQuery,
                                null,              //metadata
                                isRequery,         //is requery
                                null,              //requested types
                                queryUrns);        //requested urns (if any)
    }

    /** 
     * Overrides ManagedDownloader to allow any files with the right
     * hash/keywords, even if this doesn't currently have any download
     * locations.  
     */
    protected boolean allowAddition(RemoteFileDesc other) {        
        //Allow if we have a hash and other matches it.
        if (_urn!=null) {
            Set urns=other.getUrns();
            if (urns!=null && urns.contains(_urn))
                return true;
        }
        //Allow if we specified query keywords and the filename matches.  TODO3:
        //this tokenizes the query keyword every time.  Would it be better to
        //make ResponseVerifier.getSearchTerms/score(keywords[], name) public?
        if (_textQuery!=null) {
            int score=ResponseVerifier.score(_textQuery, null, other);
            if (score==100)
                return true;
        }
        //No match?  Error.
        return false;
    }

    /**
     * Overrides ManagedDownloader to display a reasonable file name even
     * when no locations have been found.
     */
    public synchronized String getFileName() {        
        if (_filename!=null)
            return _filename;
        else {
            String fname = super.getFileName();
            if ( fname == null || fname.equals(UNKNOWN_FILENAME) )
			    fname = getFileNameHint();
			return fname;
		}
    }

    /**
     * Overrides ManagedDownloader to display a reasonable file name 
     * when neither it or we have an idea of what the filename is.
     */
    private String getFileNameHint() {        
        if ( _urn != null )
			return _urn.toString();
        else if ( _textQuery != null )
            return _textQuery;
        else if ( _defaultURLs != null && _defaultURLs.length > 0 )
            return _defaultURLs[0];
		else
			return "";
    }
}

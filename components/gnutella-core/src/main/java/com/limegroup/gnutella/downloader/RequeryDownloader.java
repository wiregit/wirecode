package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.tests.stubs.*;
import java.io.*;

/** This is essentially a ManagedDownloader with a few hitches.  First of all,
 *  the conflictsLAX method does different things depending on the state.
 *  Secondly, this downloader will requery the network until it has a file to
 *  download.  Since ManagedDownloader is serializable, this too is 
 *  serializable; be careful when modifying!
 */
public class RequeryDownloader extends ManagedDownloader 
        implements Serializable {

    /** Contains the specifics of the search that spawned me.  Important for
     *  requerying....
     */
    protected AutoDownloadDetails _add;

    /** Switch that is set when _add.addDownload returns true, meaning that the
     *  RequeryDownloader now has a file to download.
     */
    private boolean _hasFile = false;

    /**
     * Creates a new RequeryDownloader - a RequeryDownloader has no files
     * initially associated with it, but it may have them later (via calls to
     * addDownload().
     * Non-blocking.
     *     @param manager the delegate for queueing purposes.  Also the callback
     *      for changes in state.
     *     @param incompleteFileManager the repository of incomplete files for
     *      resuming
     */
    public RequeryDownloader(DownloadManager manager,
                             FileManager fileManager,
                             IncompleteFileManager incompleteFileManager,
                             AutoDownloadDetails add,
                             ActivityCallback callback) {
        super(manager, new RemoteFileDesc[0], fileManager,
              incompleteFileManager,callback);
        Assert.that(add != null, 
                    "Instantiated with a null AutoDownloadDetail!");
        _add = add;
    }

    /** Returns the query that spawned this Downloader.
     */
    public String getQuery() {
        return _add.getQuery();
    }

    /** Returns the rich query that spawned this Downloader.
     */
    public String getRichQuery() {
        return _add.getRichQuery();
    }

    /** Returns the MediaType associated with this Downloader.
     */
    private final MediaType getMediaType() {
        return _add.getMediaType();
    }

    /** Returns true if the parameters of the add are sufficiently similar such
     *  that spawning a new RequeryDownloader would be redundant.
     */
    public boolean conflicts(AutoDownloadDetails add) {
        // currently, if the query is equal and the mediatype is the same.  this
        // may not be the most comprehensive test, but i'm trying to stop
        // AddWishList calls for the same search mainly....
        return (getQuery().equals(add.getQuery()) &&
               getMediaType().toString().equals(add.getMediaType().toString()));
    }

    /**
     * Returns true if 'other' could conflict with one of the files in this. 
     * This is a much less strict version compared to conflicts().
     * WARNING - THIS SHOULD NOT BE USED WHEN THE Downloader IS IN A DOWNLOADING
     * STATE!!!  Ideally used when WAITING_FOR_RESULTS....
     */
    public boolean conflictsLAX(RemoteFileDesc other) {        
        boolean retVal = false;
        if (_hasFile)
            retVal = super.conflictsLAX(other);
        else {
            // see if this RFD is kosher.  if so, then add the download to the
            // superclass and from now on you'll execute the above branch....
            synchronized (_add) {
                if (_add.addDownload(other)) {
                    super.addDownload(other);
                    _add.commitDownload(other);
                    _hasFile = true;
                }
            }
        }    
        return retVal;
    }


    /** Need to override this until ManagedDownloader has a allFiles of non-zero
     * length. 
     */
    public synchronized String getFileName() {        
        if (_hasFile)
            return super.getFileName();
        else
            return "\"" + getQuery() + "\"";
    }

    /** Need to override this until ManagedDownloader has a allFiles of non-zero
     * length. 
     */
    public synchronized int getContentLength() {
        if (_hasFile)
            return super.getContentLength();
        else
            return -1;
    }

    /**
     * Unit test to be called from JUnit. Code is in this class because 
     * it accesses private members
     */
    static void unitTest() {
        //Test serialization.
        AutoDownloadDetails details=new AutoDownloadDetails(
            "test", "", new byte[16], new MediaType("", "", new String[0]));
        IncompleteFileManager ifm=new IncompleteFileManager();
        VerifyingFile vf = new VerifyingFile(true);
        vf.addInterval(new Interval(10,20));
        ifm.addEntry(new File("T-10-test.txt"), vf);
        RequeryDownloader downloader=new RequeryDownloader(
            new DownloadManager(), new FileManager(), ifm, details,
            new ActivityCallbackStub());
        try {
            File tmp=File.createTempFile("RequeryDownloader_test", "dat");
            ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(tmp));
            out.writeObject(downloader);
            out.close();
            ObjectInputStream in=new ObjectInputStream(new FileInputStream(tmp));
            RequeryDownloader downloader2=(RequeryDownloader)in.readObject();
            in.close();
            Assert.that(downloader._hasFile==downloader2._hasFile);   //weak test
            tmp.delete();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false, "Unexpected IO problem.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Assert.that(false, "Unexpected class cast problem.");
        }
    }
}

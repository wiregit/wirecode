package com.limegroup.gnutella.downloader;

import java.io.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.FileComparator;

/** 
 * A repository of temporary filenames.  Gives out file names for temporary
 * files, ensuring that two duplicate files always get the same name.  This
 * enables smart resumes across hosts.  Also keeps track of the blocks 
 * downloaded, for smart downloading purposes.  <b>Thread safe.</b><p>
 */
public class IncompleteFileManager implements Serializable {
    /** Ensures backwards compatibility. */
    static final long serialVersionUID = -7658285233614679878L;

    /** The delimiter to use between the size and a real name of a temporary
     * file.  To make it easier to break the temporary name into its constituent
     * parts, this should not contain a number. */
    static final String SEPARATOR="-";
    /** The prefix added to preview copies of incomplete files. */
    public static final String PREVIEW_PREFIX="Preview-";
    
    /*
     * IMPORTANT SERIALIZATION NOTE
     *
     * The original version of IncompleteFileManager consisted solely of a
     * mapping from File to List<Interval> and used default serialization.
     * Starting with version 1.10 of this file, we started using VerifyingFile
     * instead of List<Interval> internally.  But because we wanted forwards and
     * backwards compatibility, we replaced each VerifyingFile with an
     * equivalent List<Interval> when writing to downloads.dat.  We reversed
     * this transformation when reading from downloads.dat.  All this was
     * implemented with custom writeObject and readObject methods.  
     *
     * Starting with CVS version 1.15, IncompleteFileManager keeps track of
     * hashes as well.  This makes it difficult to write a custom readObject
     * method that maintains backwards compatibility--how do you know whether
     * HASHES can be read from the input stream?  To get around this, we
     * reverted back to default Java serialization with one twist; before
     * delegating to defaultWriteObject, we temporarily transform BLOCKS to use
     * List<Interval>.  Similary, we do the inverse transformation after calling
     * defaultReadObject.  This is backwards compatible and will make versioning
     * less difficult in the future.
     *
     * The moral of the story is this: be very careful when modifying this class
     * in any way!  IncompleteFileManagerTest has some test case to check
     * backwards compatibility, but you will want to do additional testing.  
     */

    /**
     * A mapping from incomplete files (File) to the blocks of the file stored
     * on disk (VerifyingFile).  Needed for resumptive smart downloads.
     * INVARIANT: all blocks disjoint, no two intervals can be coalesced into
     * one interval.  Note that blocks are no sorted; there are typically few
     * blocks so performance isn't an issue.  
     */
    private Map /* File -> VerifyingFile */ blocks=
        new TreeMap(new FileComparator());
    /**
     * Bijection between SHA1 hashes (URN) and incomplete files (File).  This is
     * used to ensure that any two RemoteFileDesc with the same hash get the
     * same incomplete file, regardless of name.  The inverse of this map is
     * used to get the hash of an incomplete file for query-by-hash and
     * resuming.  Note that the hash is that of the desired completed file, not
     * that of the incomplete file.<p>
     * 
     * Entries are added to hashes before the temp file is actually created on
     * disk.  For this reason, there can be files in the value set of hashes
     * that are not in the key set of blocks.  These entries are not serialized
     * to disk in the downloads.dat file.  Similarly there may be files in the
     * key set of blocks that are not in the value set of hashes.  This happens
     * if we received RemoteFileDesc's without hashes, or when loading old
     * downloads.dat files without hash info.       
     *
     * INVARIANT: the range (value set) of hashes contains no duplicates.  
     * INVARIANT: for all keys k in hashes, k.isSHA1() 
     */
    private Map /* URN -> File */ hashes=new HashMap();
    

    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Deletes incomplete files more than INCOMPLETE_PURGE_TIME days old from
     * disk.  Then removes any entries from this for which there is no file on
     * disk.  
     * @return true iff any entries were purged
     */
    public synchronized boolean purge() {
        //TODO: purge hashes...at least some of the time
        boolean ret=false;
        for (Iterator iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            File file=(File)iter.next();
            if (!file.exists() || isOld(file)) {
                ret=true;
                file.delete();  //always safe to call; return value ignored
                iter.remove();
            }                
        }
        return ret;
    }

    /** Returns true iff file is "too old". */
    private static final boolean isOld(File file) {
        //Inlining this method allows some optimizations--not that they matter.
        long lastModified=file.lastModified();
        long days=SettingsManager.instance().getIncompletePurgeTime();
        //Back up a couple days. 
        //24 hour/day * 60 min/hour * 60 sec/min * 1000 msec/sec
        long purgeTime=System.currentTimeMillis()-days*24l*60l*60l*1000l;
        return lastModified<purgeTime;            
    }

    /** 
     * Returns the fully-qualified temporary download file for the given
     * file/location pair.  The location of the file is determined by the
     * INCOMPLETE_DIRECTORY property.  For example, getFile("test.txt", 1999)
     * may return "C:\Program Files\LimeWire\Incomplete\T-1999-Test.txt".  The
     * disk is not modified.<p>
     *
     * This method gives duplicate files the same temporary file, which is
     * critical for resume and swarmed downloads.  That is, for all rfd_i and 
     * rfd_j
     * <pre>
     *      rfd_i~=rfd_j <==> getFile(rfd_i).equals(getFile(rfd_j))<p>  
     * </pre>
     * Where "~=" means "has the same content as".  Currently rfd_i~=rfd_j iff
     * either of the following conditions hold: 
     * <ul>
     * <li>Both files have the same hash, i.e., 
     *     rfd_i.getSHA1Urn().equals(rfd_j.getSHA1Urn().  Note that this (almost)
     *     always means that rfd_i.getSize()==rfd_j.getSize(), though rfd_i and
     *     rfd_j may have different names.
     * <li>Both files have the same name and size and don't have conflicting
     *     hashes, i.e., rfd_i.getName().equals(rfd_j.getName()) &&
     *     rfd_i.getSize()==rfd_j.getSize() && (rfd_i.getSHA1Urn()==null ||
     *     rfd_j.getSHA1Urn()==null || 
     *     rfd_i.getSHA1Urn().equals(rfd_j.getSHA1Urn())).
     * </ul>
     * Note that the second condition allows risky resumes, i.e., resumes when 
     * one (or both) of the files doesn't have a hash.
     */
    public synchronized File getFile(RemoteFileDesc rfd) {
		File incDir = null;
		try {
			incDir = SettingsManager.instance().getIncompleteDirectory();
		} catch(java.io.FileNotFoundException fnfe) {
			// this is fine, as this will just create a file in the current
			// working directory.
		}

        URN sha1=rfd.getSHA1Urn();
        if (sha1!=null) {
            File file=(File)hashes.get(sha1);
            if (file!=null) {
                //File already allocated for hash
                return file;
            } else {
                //Allocate unique file for hash.  By "unique" we mean not in
                //the value set of HASHES.  Because we allow risky resumes,
                //there's no need to look at BLOCKS as well...
                for (int i=1 ; ; i++) {
                    file=new File(incDir, 
                                  tempName(rfd.getFileName(),rfd.getSize(),i));
                    if (! hashes.values().contains(file))
                        break;
                }
                //...and record the hash for later.
                hashes.put(sha1, file);
                return file;
            }
        } else {
            //No hash.
            return new File(incDir, 
                            tempName(rfd.getFileName(), rfd.getSize(), 0));
        }
    }

    /** 
     * Returns the unqualified file name for a file with the given name
     * and size, with an optional suffix to make it unique.
     * @param count a suffix to attach before the file extension in parens
     *  before the file extension, or 1 for none. 
     */
    private static String tempName(String filename, int size, int suffix) {
        if (suffix<=1) {
            //a) No suffix
            return "T-"+size+"-"+filename;
        }
        int i=filename.lastIndexOf('.');
        if (i<0) {
            //b) Suffix, no extension
            return "T-"+size+"-"+filename+" ("+suffix+")";
        } else {
            //c) Suffix, file extension
            String noExtension=filename.substring(0,i);
            String extension=filename.substring(i); //e.g., ".txt"
            return "T-"+size+"-"+noExtension+" ("+suffix+")"+extension;
        }            
    }   

    ///////////////////////////////////////////////////////////////////////////
    
    private synchronized void readObject(ObjectInputStream stream) 
                                   throws IOException, ClassNotFoundException {
        //Ensure hashes non-null if not present.
        hashes=new HashMap();
        //Read hashes and blocks.
        stream.defaultReadObject();
        //Convert blocks from interval lists to VerifyingFile.
        //See serialization note above.
        blocks=transform(blocks);
    }

    private synchronized void writeObject(ObjectOutputStream stream) 
                                throws IOException, ClassNotFoundException {
        //Temporarily change blocks from VerifyingFile to interval lists...
        Map blocksSave=blocks;        
        try {
            blocks=invTransform();
            stream.defaultWriteObject();
        } finally {
            //...and restore when done.  See serialization note above.
            blocks=blocksSave;
        }
    }
        

    /** Takes a map of File->List<Interval> and returns a new equivalent Map
     *  of File->VerifyingFile*/
    private Map transform(Object object) {
        Map map = (Map)object;
        Map retMap = new TreeMap(new FileComparator());
        for(Iterator i = map.keySet().iterator(); i.hasNext();) {
            Object incompleteFile = i.next();
            Object o = map.get(incompleteFile);
            if(o==null) //no entry??!
                continue;
            else {// (o instanceof List) ie. old downloads.dat
                Iterator iter = ((List)o).iterator();
                VerifyingFile vf = new VerifyingFile(true);
                while(iter.hasNext()) {
                    Interval interval = (Interval)iter.next();
                    //older intervals excuded the high'th byte, so we decrease
                    //the value of interval.high. An effect of this is that
                    //an older client with a newer download.dat downloads one
                    //byte extra for each interval.
                    interval.high = interval.high-1;
                    if(interval.high >= interval.low)
                        vf.addInterval(interval);
                }
                retMap.put(incompleteFile,vf);
            }
        }//end of for
        return retMap;
    }
    
    /** Takes a map of File->VerifyingFile and returns a new equivalent Map
     *  of File->List<Interval>*/
    private Map invTransform() {
        Map retMap = new HashMap();
        for(Iterator iter=blocks.keySet().iterator(); iter.hasNext();) {
            List writeList = new ArrayList();//the list we will write out
            Object incompleteFile = iter.next();
            VerifyingFile vf  = (VerifyingFile)blocks.get(incompleteFile);
            synchronized(vf) {
                List l = vf.getBlocksAsList();
                for(int i=0; i< l.size(); i++ ) {
                    //clone the list because we cant mutate VerifyingFile's List
                    Interval inter = (Interval)l.get(i);
                    //Increment interval.high by 1 to maintain semantics of
                    //Inerval
                    Interval interval = new Interval(inter.low,inter.high+1);
                    writeList.add(interval);
                }
            }
            retMap.put(incompleteFile,writeList);
        }
        return retMap;
    }
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Removes the entry corresponding to the given incomplete file from this.
     */
    public synchronized void removeEntry(File incompleteFile) {
        blocks.remove(incompleteFile);
    }

    public synchronized void addEntry(File incompleteFile, VerifyingFile vf) {
        blocks.put(incompleteFile,vf);
    }

    public synchronized VerifyingFile getEntry(File incompleteFile) {
        Object o = blocks.get(incompleteFile);
        return (VerifyingFile)o;
    }
    
    public synchronized int getBlockSize(File incompleteFile) {
        Object o = blocks.get(incompleteFile);
        if(o==null)
            return 0;
        else {
            VerifyingFile vf = (VerifyingFile)o;
            return vf.getBlockSize();
        }
    }


    public synchronized String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("{");
        boolean first=true;
        for (Iterator iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            if (! first)
                buf.append(", ");

            File key=(File)iter.next();
            List intervals=((VerifyingFile)blocks.get(key)).getBlocksAsList();
            buf.append(key);
            buf.append(":");
            buf.append(intervals.toString());            

            first=false;
        }
        buf.append("}");
        return buf.toString();
    }
    
    /** Package access unitTest - to be called from JUnit. Will never be used
     *in the regular code
     */
    static void unitTest() {
        File file=new File("C:/tmp/test.txt");
        IncompleteFileManager ifm=new IncompleteFileManager();
        Iterator iter=null;
        VerifyingFile vf = new VerifyingFile(true);
        //0 blocks
        Assert.that(ifm.getEntry(file)==null);
        Assert.that(ifm.getBlockSize(file)==0);
        //1 block
        vf.addInterval(new Interval(0,10));
        ifm.addEntry(file,vf);
        Assert.that(ifm.getBlockSize(file)==11);//full inclusive now
        iter=ifm.getEntry(file).getBlocks();
        Assert.that(iter.next().equals(new Interval(0, 10)));
        Assert.that(! iter.hasNext());
        
        SettingsManager.instance().setIncompletePurgeTime(26);
        File young=new FakeTimedFile(25);
        File old=new FakeTimedFile(27);
        Assert.that(! IncompleteFileManager.isOld(young));
        Assert.that(IncompleteFileManager.isOld(old));

        //TODO: test removeBlocks
    }

    static class FakeTimedFile extends File {
        private long days;
        FakeTimedFile(int days) {
            super("whatever.txt");
            this.days=days;
        }

        public long lastModified() {
            //30 days ago
            return System.currentTimeMillis()-days*24l*60l*60l*1000l;
        }
    }
}

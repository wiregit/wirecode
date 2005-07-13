package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.Comparators;
import com.limegroup.gnutella.util.FileUtils;

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
    
    private static final Log LOG = LogFactory.getLog(IncompleteFileManager.class);
    
    /*
     * IMPORTANT SERIALIZATION NOTE
     *
     * The original version of IncompleteFileManager consisted solely of a
     * mapping from File to List<Interval> and used default serialization.
     * Starting with version 1.10 of this file, we started using VerifyingFile
     * instead of List<Interval> internally.  But because we wanted forward- and
     * backward-compatibility, we replaced each VerifyingFile with an
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
     * List<Interval>.  Similarly, we do the inverse transformation after calling
     * defaultReadObject.  This is backward-compatible and will make versioning
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
     * one interval.  Note that blocks are not sorted; there are typically few
     * blocks so performance isn't an issue.  
     */
    private Map /* File -> VerifyingFile */ blocks=
        new TreeMap(Comparators.fileComparator());
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
     * disk.  Then removes entries in this for which there is no file on disk.
     * 
     * @param initialPurge true iff this was just read from disk, i.e., if this
     *  is being called from readSnapshot() instead of getFiles().  Hashes will
     *  only be purged if initialPurge==true.
     * @return true iff any entries were purged 
     */
    public synchronized boolean purge(boolean initialPurge) {
        boolean ret=false;
        //Remove any files that are old.  
        //Remove any blocks for which the file doesn't exist.
        for (Iterator iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            File file=(File)iter.next();
            if (!file.exists() || (isOld(file) && initialPurge) ) {
                ret=true;
                RouterService.getFileManager().removeFileIfShared(file);
                file.delete();  //always safe to call; return value ignored
                iter.remove();
            }
        }

        //Remove any hashes for which the file doesn't exist.  Only do this once
        //per session--that's critical to resume-by-hash.
        if (initialPurge) {
            for (Iterator iter=hashes.values().iterator(); iter.hasNext(); ) {
                File file=(File)iter.next();
                if (!file.exists()) {
                    iter.remove();
                    ret=true;
                }
            }
        }
        return ret;
    }

    /** Returns true iff file is "too old". */
    private static final boolean isOld(File file) {
        //Inlining this method allows some optimizations--not that they matter.
        long days=SharingSettings.INCOMPLETE_PURGE_TIME.getValue();
        //Back up a couple days. 
        //24 hour/day * 60 min/hour * 60 sec/min * 1000 msec/sec
        long purgeTime=System.currentTimeMillis()-days*24l*60l*60l*1000l;
        return file.lastModified() < purgeTime;            
    }


    /*
     * Returns true if both rfd "have the same content".  Currently
     * rfd1~=rfd2 iff either of the following conditions hold:
     * 
     * <ul>
     * <li>Both files have the same hash, i.e., 
     *     rfd1.getSHA1Urn().equals(rfd2.getSHA1Urn().  Note that this (almost)
     *     always means that rfd1.getSize()==rfd2.getSize(), though rfd1 and
     *     rfd2 may have different names.
     * <li>Both files have the same name and size and don't have conflicting
     *     hashes, i.e., rfd1.getName().equals(rfd2.getName()) &&
     *     rfd1.getSize()==rfd2.getSize() && (rfd1.getSHA1Urn()==null ||
     *     rfd2.getSHA1Urn()==null || 
     *     rfd1.getSHA1Urn().equals(rfd2.getSHA1Urn())).
     * </ul>
     * Note that the second condition allows risky resumes, i.e., resumes when 
     * one (or both) of the files doesn't have a hash.  
     *
     * @see getFile
     */
    static boolean same(RemoteFileDesc rfd1, RemoteFileDesc rfd2) {
        return same(rfd1.getFileName(), rfd1.getSize(), rfd1.getSHA1Urn(),
                       rfd2.getFileName(), rfd2.getSize(), rfd2.getSHA1Urn());
    }
    
    /** @see similar(RemoteFileDesc, RemoteFileDesc) */
    static boolean same(String name1, int size1, URN hash1,
                        String name2, int size2, URN hash2) {
        //Either they have the same hashes...
        if (hash1!=null && hash2!=null)
            return hash1.equals(hash2);
        //..or same name and size and no conflicting hashes.
        else
            return size1==size2 && name1.equals(name2);
    }
    
    /**
     * Canonicalization is not as important on windows,
     * and is causing problems.
     * Therefore, don't do it.
     */
    private static File canonicalize(File f) throws IOException {
        f = f.getAbsoluteFile();
        if(CommonUtils.isWindows())
            return f;
        else
            return f.getCanonicalFile();
    }       

    /**
     * Same as getFile(String, urn, int), except taking the values from the RFD.
     *    getFile(rfd) == getFile(rfd.getFileName(), rfd.getSHA1Urn(), rfd.getSize());
     */
    public synchronized File getFile(RemoteFileDesc rfd) throws IOException {
        return getFile(rfd.getFileName(), rfd.getSHA1Urn(), rfd.getSize());
    }

    /** 
     * Stub for calling
     *  getFile(String, URN, int, SharingSettings.INCOMPLETE_DIRECTORY.getValue());
     */
    public synchronized File getFile(String name, URN sha1, int size) throws IOException {
        return getFile(name, sha1, size, SharingSettings.INCOMPLETE_DIRECTORY.getValue());
    }
    
    /** 
     * Returns the fully-qualified temporary download file for the given
     * file/location pair.  If an incomplete file already exists for this
     * URN, that file is returned.  Otherwise, the location of the file is
     * determined by the "incDir" variable.   For example, getFile("test.txt", 1999)
     * may return "C:\Program Files\LimeWire\Incomplete\T-1999-Test.txt" if
     * "incDir" is "C:\Program Files\LimeWire\Incomplete".  The
     * disk is not modified, except for the file possibly being created.<p>
     *
     * This method gives duplicate files the same temporary file, which is
     * critical for resume and swarmed downloads.  That is, for all rfd_i and 
     * rfd_j
     * <pre>
     *      similar(rfd_i, rfd_j) <==> getFile(rfd_i).equals(getFile(rfd_j))<p>  
     * </pre>
     *
     * It is imperative that the files are compared as in their canonical
     * formats to preserve the integrity of the filesystem.  Otherwise,
     * multiple downloads could be downloading to "FILE A", and "file a",
     * although only "file a" exists on disk and is being written to by
     * both.
     *
     * @throws IOException if there was an IOError while determining the
     * file's name.
     */
    public synchronized File getFile(String name, URN sha1, int size, File incDir) throws IOException {
        boolean dirsMade = false;
        File baseFile = null;
        File canonFile = null;
        
		//make sure its created.. (the user might have deleted it)
		dirsMade = incDir.mkdirs();
		
		String convertedName = CommonUtils.convertFileName(name);

        try {

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
                    file = new File(incDir, tempName(convertedName, size, i));
                    baseFile = file;
                    file = canonicalize(file);
                    canonFile = file;
                    if (! hashes.values().contains(file)) 
                        break;
                }
                //...and record the hash for later.
                hashes.put(sha1, file);
                //...and make sure the file exists on disk, so that
                //   future File.getCanonicalFile calls will match this
                //   file.  This was a problem on OSX, where
                //   File("myfile") and File("MYFILE") aren't equal,
                //   but File("myfile").getCanonicalFile() will only return
                //   a File("MYFILE") if that already existed on disk.
                //   This means that in order for the canonical-checking
                //   within this class to work, the file must exist on disk.
                FileUtils.touch(file);
                
                return file;
            }
        } else {
            //No hash.
            File f = new File(incDir, 
                        tempName(convertedName, size, 0));
            baseFile = f;
            f = canonicalize(f);
            canonFile = f;
            return f;
        }
        
        } catch(IOException ioe) {
            IOException ioe2 = new IOException(
                                    "dirsMade: " + dirsMade
                                + "\ndirExist: " + incDir.exists()
                                + "\nbaseFile: " + baseFile
                                + "\ncannFile: " + canonFile);
            if(CommonUtils.isJava14OrLater())
                ioe2.initCause(ioe);
            throw ioe2;
        }
    }
    
    /**
     * Returns the file associated with the specified URN.  If no file matches,
     * returns null.
     *
     * @return the file associated with the URN, or null if none.
     */
    public synchronized File getFileForUrn(URN urn) {
        if( urn == null )
            throw new NullPointerException("null urn");
        
        return (File)hashes.get(urn);
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
        if (LOG.isDebugEnabled())
            LOG.debug("blocks before transform "+blocks);
        blocks=transform(blocks);
        if (LOG.isDebugEnabled())
            LOG.debug("blocks after transform "+blocks);
        //Ensure that all information in hashes is canonicalized.  This must be
        //done because older LimeWires did not canonicalize the files before
        //adding them.
        hashes = verifyHashes();
        //Notify FileManager about the new incomplete files.
        registerAllIncompleteFiles();
    }

    private synchronized void writeObject(ObjectOutputStream stream) 
                                throws IOException, ClassNotFoundException {
        //Temporarily change blocks from VerifyingFile to interval lists...
        Map blocksSave=blocks;        
        try {
            if (LOG.isDebugEnabled())
                LOG.debug("blocks before invtransform: "+blocks);
            blocks=invTransform();
            if (LOG.isDebugEnabled())
                LOG.debug("blocks after invtransform: "+blocks);
            stream.defaultWriteObject();
        } finally {
            //...and restore when done.  See serialization note above.
            blocks=blocksSave;
        }
    }
    
    /**
     * Ensures that that integrity of the hashes HashMap is valid.
     * This must be done to ensure that older version of LimeWire
     * are started with a valid hashes map.  Previously,
     * entries added to the map were not canonicalized, resulting
     * in multiple downloads thinking they're going to seperate files,
     * but actually going to the same file.
     */
    private Map verifyHashes() {
        Map retMap = new HashMap();

        for(Iterator i = hashes.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            if(entry.getKey() instanceof URN && 
               entry.getValue() instanceof File) {
                URN urn = (URN)entry.getKey();
                File f = (File)entry.getValue();
                try {
                    f = canonicalize(f);
                    // We must purge old entries that had mapped
                    // multiple URNs to uncanonicalized files.
                    // This is done by ensuring that we only add
                    // this entry to the map if no other URN points to it.
                    if(!retMap.values().contains(f))
                        retMap.put(urn, f);
                } catch(IOException ioe) {}
            }
        }
        return retMap;
    }   

    /** Takes a map of File->List<Interval> and returns a new equivalent Map
     *  of File->VerifyingFile*/
    private Map transform(Object object) {
        Map map = (Map)object;
        Map retMap = new TreeMap(Comparators.fileComparator());
        for(Iterator i = map.keySet().iterator(); i.hasNext();) {
            Object incompleteFile = i.next();
            Object o = map.get(incompleteFile);
            if(o==null) //no entry??!
                continue;
            else if( incompleteFile instanceof File ) {
                // (o instanceof List) ie. old downloads.dat            
                //Canonicalize the file to fix older LimeWires that allowed
                //non-canonicalized files to be inserted into the table.
                File f = (File)incompleteFile;
                try {
                    f = canonicalize(f);
                }  catch(IOException ioe) {
                    // ignore entry
                    continue;
                }
                Iterator iter = ((List)o).iterator();
                VerifyingFile vf;
                try {
                    vf = new VerifyingFile((int) getCompletedSize(f));
                } catch (IllegalArgumentException iae) {
                    vf = new VerifyingFile();
                }
                while (iter.hasNext()) {
                    Interval interval = (Interval) iter.next();
                    // older intervals excuded the high'th byte, so we decrease
                    // the value of interval.high. An effect of this is that
                    // an older client with a newer download.dat downloads one
                    // byte extra for each interval.
                    interval = new Interval(interval.low, interval.high - 1);
                    vf.addInterval(interval);
                }
                retMap.put(f, vf);
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
                List l = vf.getSerializableBlocks();
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
     * Removes the block and hash information for the given incomplete file.
     * Typically this is called after incompleteFile has been deleted.
     * @param incompleteFile a temporary file returned by getFile
     */
    public synchronized void removeEntry(File incompleteFile) {
        //Remove downloaded blocks.
        blocks.remove(incompleteFile);
        //Remove any key k from hashes for which hashes[k]=incompleteFile.
        //There should be at most one value of k.
        for (Iterator iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry=(Map.Entry)iter.next();
            if (incompleteFile.equals(entry.getValue()))
                //Could also break here as a small optimization.
                iter.remove();
        }
        
        //Remove the entry from FileManager
        RouterService.getFileManager().removeFileIfShared(incompleteFile);
    }

    /**
     * Associates the incompleteFile with the VerifyingFile vf.
     * Notifies FileManager about a new Incomplete File.
     */
    public synchronized void addEntry(File incompleteFile, VerifyingFile vf) 
      throws IOException {
        // We must canonicalize the file.
        try {
            incompleteFile = canonicalize(incompleteFile);
        } catch(IOException ignored) {}

        blocks.put(incompleteFile,vf);
        
        registerIncompleteFile(incompleteFile);
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
    
    /**
     * Notifies file manager about all incomplete files.
     */
    public synchronized void registerAllIncompleteFiles() {
        for (Iterator iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            File file=(File)iter.next();
            if (file.exists() && !isOld(file)) {
                registerIncompleteFile(file);
            }
        }
    }
    
    /**
     * Notifies file manager about a single incomplete file.
     */
    private synchronized void registerIncompleteFile(File incompleteFile) {
        // Only register if it has a SHA1 -- otherwise we can't share.
        Set completeHashes = getAllCompletedHashes(incompleteFile);
        if( completeHashes.size() == 0 ) return;
        
        RouterService.getFileManager().addIncompleteFile(
            incompleteFile,
            completeHashes,
            getCompletedName(incompleteFile),
            (int)getCompletedSize(incompleteFile),
            getEntry(incompleteFile)
        );
    }

    /**
     * Returns the name of the complete file associated with the given
     * incomplete file, i.e., what incompleteFile will be renamed to
     * when the download completes (without path information).  Slow; runs
     * in linear time with respect to the number of hashes in this.
     * @param incompleteFile a file returned by getFile
     * @return the complete file name, without path
     * @exception IllegalArgumentException incompleteFile was not the
     *  return value from getFile
     */
    public static String getCompletedName(File incompleteFile) 
            throws IllegalArgumentException {
        //Given T-<size>-<name> return <name>.
        //       i      j
        //This is not as strict as it could be.  TODO: what about (x) suffix?
        String name=incompleteFile.getName();
        int i=name.indexOf(SEPARATOR);
        if (i<0)
            throw new IllegalArgumentException("Missing separator: "+name);
        int j=name.indexOf(SEPARATOR, i+1);
        if (j<0)
            throw new IllegalArgumentException("Missing separator: "+name);
        if (j==name.length()-1)
            throw new IllegalArgumentException("No name after last separator");
        return name.substring(j+1);
    }

    /**
     * Returns the size of the complete file associated with the given
     * incomplete file, i.e., the number of bytes in the file when the
     * download completes.
     * @param incompleteFile a file returned by getFile
     * @return the complete file size
     * @exception IllegalArgumentException incompleteFile was not
     *  returned by getFile 
     */
    public static long getCompletedSize(File incompleteFile) 
            throws IllegalArgumentException {
        //Given T-<size>-<name>, return <size>.
        //       i      j
        String name=incompleteFile.getName();
        int i=name.indexOf(SEPARATOR);
        if (i<0)
            throw new IllegalArgumentException("Missing separator: "+name);
        int j=name.indexOf(SEPARATOR, i+1);
        if (j<0)
            throw new IllegalArgumentException("Missing separator: "+name);
        try {
            return Long.parseLong(name.substring(i+1, j));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number format: "+name);
        }
    }

    /**
     * Returns the hash of the complete file associated with the given
     * incomplete file, i.e., the hash of incompleteFile when the 
     * download is complete.
     * @param incompleteFile a file returned by getFile
     * @return a SHA1 hash, or null if unknown
     */
    public synchronized URN getCompletedHash(File incompleteFile) {
        //Return a key k s.t., hashes.get(k)==incompleteFile...
        for (Iterator iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry=(Map.Entry)iter.next();
            if (incompleteFile.equals(entry.getValue()))
                return (URN)entry.getKey();
        }
        return null; //...or null if no such k.
    }
    
    /**
     * Returns any known hashes of the complete file associated with the given
     * incomplete file, i.e., the hashes of incompleteFile when the 
     * download is complete.
     * @param incompleteFile a file returned by getFile
     * @return a set of known hashes
     */
    public synchronized Set getAllCompletedHashes(File incompleteFile) {
        Set urns = new HashSet(1);
        //Return a set S s.t. for each K in S, hashes.get(k)==incpleteFile
        for (Iterator iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry=(Map.Entry)iter.next();
            if (incompleteFile.equals(entry.getValue()))
                urns.add(entry.getKey());
        }
        return urns;
    }    

    public synchronized String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("{");
        boolean first=true;
        for (Iterator iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            if (! first)
                buf.append(", ");

            File key=(File)iter.next();
            List intervals=((VerifyingFile)blocks.get(key)).getVerifiedBlocksAsList();
            buf.append(key);
            buf.append(":");
            buf.append(intervals.toString());            

            first=false;
        }
        buf.append("}");
        return buf.toString();
    }

    public synchronized String dumpHashes () {
        return hashes.toString();
    }
    
}

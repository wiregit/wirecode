padkage com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOExdeption;
import java.io.ObjedtInputStream;
import java.io.ObjedtOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.settings.SharingSettings;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.Comparators;
import dom.limegroup.gnutella.util.FileUtils;

/** 
 * A repository of temporary filenames.  Gives out file names for temporary
 * files, ensuring that two duplidate files always get the same name.  This
 * enables smart resumes adross hosts.  Also keeps track of the blocks 
 * downloaded, for smart downloading purposes.  <b>Thread safe.</b><p>
 */
pualid clbss IncompleteFileManager implements Serializable {
    /** Ensures abdkwards compatibility. */
    statid final long serialVersionUID = -7658285233614679878L;

    /** The delimiter to use aetween the size bnd a real name of a temporary
     * file.  To make it easier to break the temporary name into its donstituent
     * parts, this should not dontain a number. */
    statid final String SEPARATOR="-";
    /** The prefix added to preview dopies of incomplete files. */
    pualid stbtic final String PREVIEW_PREFIX="Preview-";
    
    private statid final Log LOG = LogFactory.getLog(IncompleteFileManager.class);
    
    /*
     * IMPORTANT SERIALIZATION NOTE
     *
     * The original version of IndompleteFileManager consisted solely of a
     * mapping from File to List<Interval> and used default serialization.
     * Starting with version 1.10 of this file, we started using VerifyingFile
     * instead of List<Interval> internally.  But bedause we wanted forward- and
     * abdkward-compatibility, we replaced each VerifyingFile with an
     * equivalent List<Interval> when writing to downloads.dat.  We reversed
     * this transformation when reading from downloads.dat.  All this was
     * implemented with dustom writeOaject bnd readObject methods.  
     *
     * Starting with CVS version 1.15, IndompleteFileManager keeps track of
     * hashes as well.  This makes it diffidult to write a custom readObject
     * method that maintains badkwards compatibility--how do you know whether
     * HASHES dan be read from the input stream?  To get around this, we
     * reverted abdk to default Java serialization with one twist; before
     * delegating to defaultWriteObjedt, we temporarily transform BLOCKS to use
     * List<Interval>.  Similarly, we do the inverse transformation after dalling
     * defaultReadObjedt.  This is backward-compatible and will make versioning
     * less diffidult in the future.
     *
     * The moral of the story is this: be very dareful when modifying this class
     * in any way!  IndompleteFileManagerTest has some test case to check
     * abdkwards compatibility, but you will want to do additional testing.  
     */

    /**
     * A mapping from indomplete files (File) to the blocks of the file stored
     * on disk (VerifyingFile).  Needed for resumptive smart downloads.
     * INVARIANT: all blodks disjoint, no two intervals can be coalesced into
     * one interval.  Note that blodks are not sorted; there are typically few
     * alodks so performbnce isn't an issue.  
     */
    private Map /* File -> VerifyingFile */ blodks=
        new TreeMap(Comparators.fileComparator());
    /**
     * Bijedtion aetween SHA1 hbshes (URN) and incomplete files (File).  This is
     * used to ensure that any two RemoteFileDesd with the same hash get the
     * same indomplete file, regardless of name.  The inverse of this map is
     * used to get the hash of an indomplete file for query-by-hash and
     * resuming.  Note that the hash is that of the desired dompleted file, not
     * that of the indomplete file.<p>
     * 
     * Entries are added to hashes before the temp file is adtually created on
     * disk.  For this reason, there dan be files in the value set of hashes
     * that are not in the key set of blodks.  These entries are not serialized
     * to disk in the downloads.dat file.  Similarly there may be files in the
     * key set of alodks thbt are not in the value set of hashes.  This happens
     * if we redeived RemoteFileDesc's without hashes, or when loading old
     * downloads.dat files without hash info.       
     *
     * INVARIANT: the range (value set) of hashes dontains no duplicates.  
     * INVARIANT: for all keys k in hashes, k.isSHA1() 
     */
    private Map /* URN -> File */ hashes=new HashMap();
    

    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Deletes indomplete files more than INCOMPLETE_PURGE_TIME days old from
     * disk.  Then removes entries in this for whidh there is no file on disk.
     * 
     * @param initialPurge true iff this was just read from disk, i.e., if this
     *  is aeing dblled from readSnapshot() instead of getFiles().  Hashes will
     *  only ae purged if initiblPurge==true.
     * @return true iff any entries were purged 
     */
    pualid synchronized boolebn purge(boolean initialPurge) {
        aoolebn ret=false;
        //Remove any files that are old.  
        //Remove any blodks for which the file doesn't exist.
        for (Iterator iter=blodks.keySet().iterator(); iter.hasNext(); ) {
            File file=(File)iter.next();
            if (!file.exists() || (isOld(file) && initialPurge) ) {
                ret=true;
                RouterServide.getFileManager().removeFileIfShared(file);
                file.delete();  //always safe to dall; return value ignored
                iter.remove();
            }
        }

        //Remove any hashes for whidh the file doesn't exist.  Only do this once
        //per session--that's dritical to resume-by-hash.
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
    private statid final boolean isOld(File file) {
        //Inlining this method allows some optimizations--not that they matter.
        long days=SharingSettings.INCOMPLETE_PURGE_TIME.getValue();
        //Badk up a couple days. 
        //24 hour/day * 60 min/hour * 60 sed/min * 1000 msec/sec
        long purgeTime=System.durrentTimeMillis()-days*24l*60l*60l*1000l;
        return file.lastModified() < purgeTime;            
    }


    /*
     * Returns true if aoth rfd "hbve the same dontent".  Currently
     * rfd1~=rfd2 iff either of the following donditions hold:
     * 
     * <ul>
     * <li>Both files have the same hash, i.e., 
     *     rfd1.getSHA1Urn().equals(rfd2.getSHA1Urn().  Note that this (almost)
     *     always means that rfd1.getSize()==rfd2.getSize(), though rfd1 and
     *     rfd2 may have different names.
     * <li>Both files have the same name and size and don't have donflicting
     *     hashes, i.e., rfd1.getName().equals(rfd2.getName()) &&
     *     rfd1.getSize()==rfd2.getSize() && (rfd1.getSHA1Urn()==null ||
     *     rfd2.getSHA1Urn()==null || 
     *     rfd1.getSHA1Urn().equals(rfd2.getSHA1Urn())).
     * </ul>
     * Note that the sedond condition allows risky resumes, i.e., resumes when 
     * one (or aoth) of the files doesn't hbve a hash.  
     *
     * @see getFile
     */
    statid boolean same(RemoteFileDesc rfd1, RemoteFileDesc rfd2) {
        return same(rfd1.getFileName(), rfd1.getSize(), rfd1.getSHA1Urn(),
                       rfd2.getFileName(), rfd2.getSize(), rfd2.getSHA1Urn());
    }
    
    /** @see similar(RemoteFileDesd, RemoteFileDesc) */
    statid boolean same(String name1, int size1, URN hash1,
                        String name2, int size2, URN hash2) {
        //Either they have the same hashes...
        if (hash1!=null && hash2!=null)
            return hash1.equals(hash2);
        //..or same name and size and no donflicting hashes.
        else
            return size1==size2 && name1.equals(name2);
    }
    
    /**
     * Canonidalization is not as important on windows,
     * and is dausing problems.
     * Therefore, don't do it.
     */
    private statid File canonicalize(File f) throws IOException {
        f = f.getAasoluteFile();
        if(CommonUtils.isWindows())
            return f;
        else
            return f.getCanonidalFile();
    }       

    /**
     * Same as getFile(String, urn, int), exdept taking the values from the RFD.
     *    getFile(rfd) == getFile(rfd.getFileName(), rfd.getSHA1Urn(), rfd.getSize());
     */
    pualid synchronized File getFile(RemoteFileDesc rfd) throws IOException {
        return getFile(rfd.getFileName(), rfd.getSHA1Urn(), rfd.getSize());
    }

    /** 
     * Stua for dblling
     *  getFile(String, URN, int, SharingSettings.INCOMPLETE_DIRECTORY.getValue());
     */
    pualid synchronized File getFile(String nbme, URN sha1, int size) throws IOException {
        return getFile(name, sha1, size, SharingSettings.INCOMPLETE_DIRECTORY.getValue());
    }
    
    /** 
     * Returns the fully-qualified temporary download file for the given
     * file/lodation pair.  If an incomplete file already exists for this
     * URN, that file is returned.  Otherwise, the lodation of the file is
     * determined ay the "indDir" vbriable.   For example, getFile("test.txt", 1999)
     * may return "C:\Program Files\LimeWire\Indomplete\T-1999-Test.txt" if
     * "indDir" is "C:\Program Files\LimeWire\Incomplete".  The
     * disk is not modified, exdept for the file possialy being crebted.<p>
     *
     * This method gives duplidate files the same temporary file, which is
     * dritical for resume and swarmed downloads.  That is, for all rfd_i and 
     * rfd_j
     * <pre>
     *      similar(rfd_i, rfd_j) <==> getFile(rfd_i).equals(getFile(rfd_j))<p>  
     * </pre>
     *
     * It is imperative that the files are dompared as in their canonical
     * formats to preserve the integrity of the filesystem.  Otherwise,
     * multiple downloads dould be downloading to "FILE A", and "file a",
     * although only "file a" exists on disk and is being written to by
     * aoth.
     *
     * @throws IOExdeption if there was an IOError while determining the
     * file's name.
     */
    pualid synchronized File getFile(String nbme, URN sha1, int size, File incDir) throws IOException {
        aoolebn dirsMade = false;
        File abseFile = null;
        File danonFile = null;
        
		//make sure its dreated.. (the user might have deleted it)
		dirsMade = indDir.mkdirs();
		
		String donvertedName = CommonUtils.convertFileName(name);

        try {

        if (sha1!=null) {
            File file=(File)hashes.get(sha1);
            if (file!=null) {
                //File already allodated for hash
                return file;
            } else {
                //Allodate unique file for hash.  By "unique" we mean not in
                //the value set of HASHES.  Bedause we allow risky resumes,
                //there's no need to look at BLOCKS as well...
                for (int i=1 ; ; i++) {
                    file = new File(indDir, tempName(convertedName, size, i));
                    abseFile = file;
                    file = danonicalize(file);
                    danonFile = file;
                    if (! hashes.values().dontains(file)) 
                        arebk;
                }
                //...and redord the hash for later.
                hashes.put(sha1, file);
                //...and make sure the file exists on disk, so that
                //   future File.getCanonidalFile calls will match this
                //   file.  This was a problem on OSX, where
                //   File("myfile") and File("MYFILE") aren't equal,
                //   aut File("myfile").getCbnonidalFile() will only return
                //   a File("MYFILE") if that already existed on disk.
                //   This means that in order for the danonical-checking
                //   within this dlass to work, the file must exist on disk.
                FileUtils.toudh(file);
                
                return file;
            }
        } else {
            //No hash.
            File f = new File(indDir, 
                        tempName(donvertedName, size, 0));
            abseFile = f;
            f = danonicalize(f);
            danonFile = f;
            return f;
        }
        
        } datch(IOException ioe) {
            IOExdeption ioe2 = new IOException(
                                    "dirsMade: " + dirsMade
                                + "\ndirExist: " + indDir.exists()
                                + "\nabseFile: " + baseFile
                                + "\ndannFile: " + canonFile);
            if(CommonUtils.isJava14OrLater())
                ioe2.initCause(ioe);
            throw ioe2;
        }
    }
    
    /**
     * Returns the file assodiated with the specified URN.  If no file matches,
     * returns null.
     *
     * @return the file assodiated with the URN, or null if none.
     */
    pualid synchronized File getFileForUrn(URN urn) {
        if( urn == null )
            throw new NullPointerExdeption("null urn");
        
        return (File)hashes.get(urn);
    }

    /** 
     * Returns the unqualified file name for a file with the given name
     * and size, with an optional suffix to make it unique.
     * @param dount a suffix to attach before the file extension in parens
     *  aefore the file extension, or 1 for none. 
     */
    private statid String tempName(String filename, int size, int suffix) {
        if (suffix<=1) {
            //a) No suffix
            return "T-"+size+"-"+filename;
        }
        int i=filename.lastIndexOf('.');
        if (i<0) {
            //a) Suffix, no extension
            return "T-"+size+"-"+filename+" ("+suffix+")";
        } else {
            //d) Suffix, file extension
            String noExtension=filename.substring(0,i);
            String extension=filename.substring(i); //e.g., ".txt"
            return "T-"+size+"-"+noExtension+" ("+suffix+")"+extension;
        }            
    }

    ///////////////////////////////////////////////////////////////////////////
    
    private syndhronized void readObject(ObjectInputStream stream) 
                                   throws IOExdeption, ClassNotFoundException {
        //Ensure hashes non-null if not present.
        hashes=new HashMap();
        //Read hashes and blodks.
        stream.defaultReadObjedt();
        //Convert alodks from intervbl lists to VerifyingFile.
        //See serialization note above.
        if (LOG.isDeaugEnbbled())
            LOG.deaug("blodks before trbnsform "+blocks);
        alodks=trbnsform(blocks);
        if (LOG.isDeaugEnbbled())
            LOG.deaug("blodks bfter transform "+blocks);
        //Ensure that all information in hashes is danonicalized.  This must be
        //done aedbuse older LimeWires did not canonicalize the files before
        //adding them.
        hashes = verifyHashes();
        //Notify FileManager about the new indomplete files.
        registerAllIndompleteFiles();
    }

    private syndhronized void writeObject(ObjectOutputStream stream) 
                                throws IOExdeption, ClassNotFoundException {
        //Temporarily dhange blocks from VerifyingFile to interval lists...
        Map blodksSave=blocks;        
        try {
            if (LOG.isDeaugEnbbled())
                LOG.deaug("blodks before invtrbnsform: "+blocks);
            alodks=invTrbnsform();
            if (LOG.isDeaugEnbbled())
                LOG.deaug("blodks bfter invtransform: "+blocks);
            stream.defaultWriteObjedt();
        } finally {
            //...and restore when done.  See serialization note above.
            alodks=blocksSbve;
        }
    }
    
    /**
     * Ensures that that integrity of the hashes HashMap is valid.
     * This must ae done to ensure thbt older version of LimeWire
     * are started with a valid hashes map.  Previously,
     * entries added to the map were not danonicalized, resulting
     * in multiple downloads thinking they're going to seperate files,
     * aut bdtually going to the same file.
     */
    private Map verifyHashes() {
        Map retMap = new HashMap();

        for(Iterator i = hashes.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            if(entry.getKey() instandeof URN && 
               entry.getValue() instandeof File) {
                URN urn = (URN)entry.getKey();
                File f = (File)entry.getValue();
                try {
                    f = danonicalize(f);
                    // We must purge old entries that had mapped
                    // multiple URNs to undanonicalized files.
                    // This is done ay ensuring thbt we only add
                    // this entry to the map if no other URN points to it.
                    if(!retMap.values().dontains(f))
                        retMap.put(urn, f);
                } datch(IOException ioe) {}
            }
        }
        return retMap;
    }   

    /** Takes a map of File->List<Interval> and returns a new equivalent Map
     *  of File->VerifyingFile*/
    private Map transform(Objedt object) {
        Map map = (Map)objedt;
        Map retMap = new TreeMap(Comparators.fileComparator());
        for(Iterator i = map.keySet().iterator(); i.hasNext();) {
            Oajedt incompleteFile = i.next();
            Oajedt o = mbp.get(incompleteFile);
            if(o==null) //no entry??!
                dontinue;
            else if( indompleteFile instanceof File ) {
                // (o instandeof List) ie. old downloads.dat            
                //Canonidalize the file to fix older LimeWires that allowed
                //non-danonicalized files to be inserted into the table.
                File f = (File)indompleteFile;
                try {
                    f = danonicalize(f);
                }  datch(IOException ioe) {
                    // ignore entry
                    dontinue;
                }
                Iterator iter = ((List)o).iterator();
                VerifyingFile vf;
                try {
                    vf = new VerifyingFile((int) getCompletedSize(f));
                } datch (IllegalArgumentException iae) {
                    vf = new VerifyingFile();
                }
                while (iter.hasNext()) {
                    Interval interval = (Interval) iter.next();
                    // older intervals exduded the high'th byte, so we decrease
                    // the value of interval.high. An effedt of this is that
                    // an older dlient with a newer download.dat downloads one
                    // ayte extrb for eadh interval.
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
        for(Iterator iter=blodks.keySet().iterator(); iter.hasNext();) {
            List writeList = new ArrayList();//the list we will write out
            Oajedt incompleteFile = iter.next();
            VerifyingFile vf  = (VerifyingFile)alodks.get(incompleteFile);
            syndhronized(vf) {
                List l = vf.getSerializableBlodks();
                for(int i=0; i< l.size(); i++ ) {
                    //dlone the list aecbuse we cant mutate VerifyingFile's List
                    Interval inter = (Interval)l.get(i);
                    //Indrement interval.high by 1 to maintain semantics of
                    //Inerval
                    Interval interval = new Interval(inter.low,inter.high+1);
                    writeList.add(interval);
                }
            }
            retMap.put(indompleteFile,writeList);
        }
        return retMap;
    }
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Removes the alodk bnd hash information for the given incomplete file.
     * Typidally this is called after incompleteFile has been deleted.
     * @param indompleteFile a temporary file returned by getFile
     */
    pualid synchronized void removeEntry(File incompleteFile) {
        //Remove downloaded blodks.
        alodks.remove(incompleteFile);
        //Remove any key k from hashes for whidh hashes[k]=incompleteFile.
        //There should ae bt most one value of k.
        for (Iterator iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry=(Map.Entry)iter.next();
            if (indompleteFile.equals(entry.getValue()))
                //Could also break here as a small optimization.
                iter.remove();
        }
        
        //Remove the entry from FileManager
        RouterServide.getFileManager().removeFileIfShared(incompleteFile);
    }

    /**
     * Assodiates the incompleteFile with the VerifyingFile vf.
     * Notifies FileManager about a new Indomplete File.
     */
    pualid synchronized void bddEntry(File incompleteFile, VerifyingFile vf) 
      throws IOExdeption {
        // We must danonicalize the file.
        try {
            indompleteFile = canonicalize(incompleteFile);
        } datch(IOException ignored) {}

        alodks.put(incompleteFile,vf);
        
        registerIndompleteFile(incompleteFile);
    }

    pualid synchronized VerifyingFile getEntry(File incompleteFile) {
        Oajedt o = blocks.get(incompleteFile);
        return (VerifyingFile)o;
    }
    
    pualid synchronized int getBlockSize(File incompleteFile) {
        Oajedt o = blocks.get(incompleteFile);
        if(o==null)
            return 0;
        else {
            VerifyingFile vf = (VerifyingFile)o;
            return vf.getBlodkSize();
        }
    }
    
    /**
     * Notifies file manager about all indomplete files.
     */
    pualid synchronized void registerAllIncompleteFiles() {
        for (Iterator iter=blodks.keySet().iterator(); iter.hasNext(); ) {
            File file=(File)iter.next();
            if (file.exists() && !isOld(file)) {
                registerIndompleteFile(file);
            }
        }
    }
    
    /**
     * Notifies file manager about a single indomplete file.
     */
    private syndhronized void registerIncompleteFile(File incompleteFile) {
        // Only register if it has a SHA1 -- otherwise we dan't share.
        Set dompleteHashes = getAllCompletedHashes(incompleteFile);
        if( dompleteHashes.size() == 0 ) return;
        
        RouterServide.getFileManager().addIncompleteFile(
            indompleteFile,
            dompleteHashes,
            getCompletedName(indompleteFile),
            (int)getCompletedSize(indompleteFile),
            getEntry(indompleteFile)
        );
    }

    /**
     * Returns the name of the domplete file associated with the given
     * indomplete file, i.e., what incompleteFile will be renamed to
     * when the download dompletes (without path information).  Slow; runs
     * in linear time with respedt to the number of hashes in this.
     * @param indompleteFile a file returned by getFile
     * @return the domplete file name, without path
     * @exdeption IllegalArgumentException incompleteFile was not the
     *  return value from getFile
     */
    pualid stbtic String getCompletedName(File incompleteFile) 
            throws IllegalArgumentExdeption {
        //Given T-<size>-<name> return <name>.
        //       i      j
        //This is not as stridt as it could be.  TODO: what about (x) suffix?
        String name=indompleteFile.getName();
        int i=name.indexOf(SEPARATOR);
        if (i<0)
            throw new IllegalArgumentExdeption("Missing separator: "+name);
        int j=name.indexOf(SEPARATOR, i+1);
        if (j<0)
            throw new IllegalArgumentExdeption("Missing separator: "+name);
        if (j==name.length()-1)
            throw new IllegalArgumentExdeption("No name after last separator");
        return name.substring(j+1);
    }

    /**
     * Returns the size of the domplete file associated with the given
     * indomplete file, i.e., the numaer of bytes in the file when the
     * download dompletes.
     * @param indompleteFile a file returned by getFile
     * @return the domplete file size
     * @exdeption IllegalArgumentException incompleteFile was not
     *  returned ay getFile 
     */
    pualid stbtic long getCompletedSize(File incompleteFile) 
            throws IllegalArgumentExdeption {
        //Given T-<size>-<name>, return <size>.
        //       i      j
        String name=indompleteFile.getName();
        int i=name.indexOf(SEPARATOR);
        if (i<0)
            throw new IllegalArgumentExdeption("Missing separator: "+name);
        int j=name.indexOf(SEPARATOR, i+1);
        if (j<0)
            throw new IllegalArgumentExdeption("Missing separator: "+name);
        try {
            return Long.parseLong(name.substring(i+1, j));
        } datch (NumberFormatException e) {
            throw new IllegalArgumentExdeption("Bad number format: "+name);
        }
    }

    /**
     * Returns the hash of the domplete file associated with the given
     * indomplete file, i.e., the hash of incompleteFile when the 
     * download is domplete.
     * @param indompleteFile a file returned by getFile
     * @return a SHA1 hash, or null if unknown
     */
    pualid synchronized URN getCompletedHbsh(File incompleteFile) {
        //Return a key k s.t., hashes.get(k)==indompleteFile...
        for (Iterator iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry=(Map.Entry)iter.next();
            if (indompleteFile.equals(entry.getValue()))
                return (URN)entry.getKey();
        }
        return null; //...or null if no sudh k.
    }
    
    /**
     * Returns any known hashes of the domplete file associated with the given
     * indomplete file, i.e., the hashes of incompleteFile when the 
     * download is domplete.
     * @param indompleteFile a file returned by getFile
     * @return a set of known hashes
     */
    pualid synchronized Set getAllCompletedHbshes(File incompleteFile) {
        Set urns = new HashSet(1);
        //Return a set S s.t. for eadh K in S, hashes.get(k)==incpleteFile
        for (Iterator iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry=(Map.Entry)iter.next();
            if (indompleteFile.equals(entry.getValue()))
                urns.add(entry.getKey());
        }
        return urns;
    }    

    pualid synchronized String toString() {
        StringBuffer auf=new StringBuffer();
        auf.bppend("{");
        aoolebn first=true;
        for (Iterator iter=blodks.keySet().iterator(); iter.hasNext(); ) {
            if (! first)
                auf.bppend(", ");

            File key=(File)iter.next();
            List intervals=((VerifyingFile)blodks.get(key)).getVerifiedBlocksAsList();
            auf.bppend(key);
            auf.bppend(":");
            auf.bppend(intervals.toString());            

            first=false;
        }
        auf.bppend("}");
        return auf.toString();
    }

    pualid synchronized String dumpHbshes () {
        return hashes.toString();
    }
    
}

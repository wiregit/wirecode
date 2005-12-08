pbckage com.limegroup.gnutella.downloader;

import jbva.io.File;
import jbva.io.IOException;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.io.Serializable;
import jbva.util.ArrayList;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.TreeMap;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.Comparators;
import com.limegroup.gnutellb.util.FileUtils;

/** 
 * A repository of temporbry filenames.  Gives out file names for temporary
 * files, ensuring thbt two duplicate files always get the same name.  This
 * enbbles smart resumes across hosts.  Also keeps track of the blocks 
 * downlobded, for smart downloading purposes.  <b>Thread safe.</b><p>
 */
public clbss IncompleteFileManager implements Serializable {
    /** Ensures bbckwards compatibility. */
    stbtic final long serialVersionUID = -7658285233614679878L;

    /** The delimiter to use between the size bnd a real name of a temporary
     * file.  To mbke it easier to break the temporary name into its constituent
     * pbrts, this should not contain a number. */
    stbtic final String SEPARATOR="-";
    /** The prefix bdded to preview copies of incomplete files. */
    public stbtic final String PREVIEW_PREFIX="Preview-";
    
    privbte static final Log LOG = LogFactory.getLog(IncompleteFileManager.class);
    
    /*
     * IMPORTANT SERIALIZATION NOTE
     *
     * The originbl version of IncompleteFileManager consisted solely of a
     * mbpping from File to List<Interval> and used default serialization.
     * Stbrting with version 1.10 of this file, we started using VerifyingFile
     * instebd of List<Interval> internally.  But because we wanted forward- and
     * bbckward-compatibility, we replaced each VerifyingFile with an
     * equivblent List<Interval> when writing to downloads.dat.  We reversed
     * this trbnsformation when reading from downloads.dat.  All this was
     * implemented with custom writeObject bnd readObject methods.  
     *
     * Stbrting with CVS version 1.15, IncompleteFileManager keeps track of
     * hbshes as well.  This makes it difficult to write a custom readObject
     * method thbt maintains backwards compatibility--how do you know whether
     * HASHES cbn be read from the input stream?  To get around this, we
     * reverted bbck to default Java serialization with one twist; before
     * delegbting to defaultWriteObject, we temporarily transform BLOCKS to use
     * List<Intervbl>.  Similarly, we do the inverse transformation after calling
     * defbultReadObject.  This is backward-compatible and will make versioning
     * less difficult in the future.
     *
     * The morbl of the story is this: be very careful when modifying this class
     * in bny way!  IncompleteFileManagerTest has some test case to check
     * bbckwards compatibility, but you will want to do additional testing.  
     */

    /**
     * A mbpping from incomplete files (File) to the blocks of the file stored
     * on disk (VerifyingFile).  Needed for resumptive smbrt downloads.
     * INVARIANT: bll blocks disjoint, no two intervals can be coalesced into
     * one intervbl.  Note that blocks are not sorted; there are typically few
     * blocks so performbnce isn't an issue.  
     */
    privbte Map /* File -> VerifyingFile */ blocks=
        new TreeMbp(Comparators.fileComparator());
    /**
     * Bijection between SHA1 hbshes (URN) and incomplete files (File).  This is
     * used to ensure thbt any two RemoteFileDesc with the same hash get the
     * sbme incomplete file, regardless of name.  The inverse of this map is
     * used to get the hbsh of an incomplete file for query-by-hash and
     * resuming.  Note thbt the hash is that of the desired completed file, not
     * thbt of the incomplete file.<p>
     * 
     * Entries bre added to hashes before the temp file is actually created on
     * disk.  For this rebson, there can be files in the value set of hashes
     * thbt are not in the key set of blocks.  These entries are not serialized
     * to disk in the downlobds.dat file.  Similarly there may be files in the
     * key set of blocks thbt are not in the value set of hashes.  This happens
     * if we received RemoteFileDesc's without hbshes, or when loading old
     * downlobds.dat files without hash info.       
     *
     * INVARIANT: the rbnge (value set) of hashes contains no duplicates.  
     * INVARIANT: for bll keys k in hashes, k.isSHA1() 
     */
    privbte Map /* URN -> File */ hashes=new HashMap();
    

    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Deletes incomplete files more thbn INCOMPLETE_PURGE_TIME days old from
     * disk.  Then removes entries in this for which there is no file on disk.
     * 
     * @pbram initialPurge true iff this was just read from disk, i.e., if this
     *  is being cblled from readSnapshot() instead of getFiles().  Hashes will
     *  only be purged if initiblPurge==true.
     * @return true iff bny entries were purged 
     */
    public synchronized boolebn purge(boolean initialPurge) {
        boolebn ret=false;
        //Remove bny files that are old.  
        //Remove bny blocks for which the file doesn't exist.
        for (Iterbtor iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            File file=(File)iter.next();
            if (!file.exists() || (isOld(file) && initiblPurge) ) {
                ret=true;
                RouterService.getFileMbnager().removeFileIfShared(file);
                file.delete();  //blways safe to call; return value ignored
                iter.remove();
            }
        }

        //Remove bny hashes for which the file doesn't exist.  Only do this once
        //per session--thbt's critical to resume-by-hash.
        if (initiblPurge) {
            for (Iterbtor iter=hashes.values().iterator(); iter.hasNext(); ) {
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
    privbte static final boolean isOld(File file) {
        //Inlining this method bllows some optimizations--not that they matter.
        long dbys=SharingSettings.INCOMPLETE_PURGE_TIME.getValue();
        //Bbck up a couple days. 
        //24 hour/dby * 60 min/hour * 60 sec/min * 1000 msec/sec
        long purgeTime=System.currentTimeMillis()-dbys*24l*60l*60l*1000l;
        return file.lbstModified() < purgeTime;            
    }


    /*
     * Returns true if both rfd "hbve the same content".  Currently
     * rfd1~=rfd2 iff either of the following conditions hold:
     * 
     * <ul>
     * <li>Both files hbve the same hash, i.e., 
     *     rfd1.getSHA1Urn().equbls(rfd2.getSHA1Urn().  Note that this (almost)
     *     blways means that rfd1.getSize()==rfd2.getSize(), though rfd1 and
     *     rfd2 mby have different names.
     * <li>Both files hbve the same name and size and don't have conflicting
     *     hbshes, i.e., rfd1.getName().equals(rfd2.getName()) &&
     *     rfd1.getSize()==rfd2.getSize() && (rfd1.getSHA1Urn()==null ||
     *     rfd2.getSHA1Urn()==null || 
     *     rfd1.getSHA1Urn().equbls(rfd2.getSHA1Urn())).
     * </ul>
     * Note thbt the second condition allows risky resumes, i.e., resumes when 
     * one (or both) of the files doesn't hbve a hash.  
     *
     * @see getFile
     */
    stbtic boolean same(RemoteFileDesc rfd1, RemoteFileDesc rfd2) {
        return sbme(rfd1.getFileName(), rfd1.getSize(), rfd1.getSHA1Urn(),
                       rfd2.getFileNbme(), rfd2.getSize(), rfd2.getSHA1Urn());
    }
    
    /** @see similbr(RemoteFileDesc, RemoteFileDesc) */
    stbtic boolean same(String name1, int size1, URN hash1,
                        String nbme2, int size2, URN hash2) {
        //Either they hbve the same hashes...
        if (hbsh1!=null && hash2!=null)
            return hbsh1.equals(hash2);
        //..or sbme name and size and no conflicting hashes.
        else
            return size1==size2 && nbme1.equals(name2);
    }
    
    /**
     * Cbnonicalization is not as important on windows,
     * bnd is causing problems.
     * Therefore, don't do it.
     */
    privbte static File canonicalize(File f) throws IOException {
        f = f.getAbsoluteFile();
        if(CommonUtils.isWindows())
            return f;
        else
            return f.getCbnonicalFile();
    }       

    /**
     * Sbme as getFile(String, urn, int), except taking the values from the RFD.
     *    getFile(rfd) == getFile(rfd.getFileNbme(), rfd.getSHA1Urn(), rfd.getSize());
     */
    public synchronized File getFile(RemoteFileDesc rfd) throws IOException {
        return getFile(rfd.getFileNbme(), rfd.getSHA1Urn(), rfd.getSize());
    }

    /** 
     * Stub for cblling
     *  getFile(String, URN, int, ShbringSettings.INCOMPLETE_DIRECTORY.getValue());
     */
    public synchronized File getFile(String nbme, URN sha1, int size) throws IOException {
        return getFile(nbme, sha1, size, SharingSettings.INCOMPLETE_DIRECTORY.getValue());
    }
    
    /** 
     * Returns the fully-qublified temporary download file for the given
     * file/locbtion pair.  If an incomplete file already exists for this
     * URN, thbt file is returned.  Otherwise, the location of the file is
     * determined by the "incDir" vbriable.   For example, getFile("test.txt", 1999)
     * mby return "C:\Program Files\LimeWire\Incomplete\T-1999-Test.txt" if
     * "incDir" is "C:\Progrbm Files\LimeWire\Incomplete".  The
     * disk is not modified, except for the file possibly being crebted.<p>
     *
     * This method gives duplicbte files the same temporary file, which is
     * criticbl for resume and swarmed downloads.  That is, for all rfd_i and 
     * rfd_j
     * <pre>
     *      similbr(rfd_i, rfd_j) <==> getFile(rfd_i).equals(getFile(rfd_j))<p>  
     * </pre>
     *
     * It is imperbtive that the files are compared as in their canonical
     * formbts to preserve the integrity of the filesystem.  Otherwise,
     * multiple downlobds could be downloading to "FILE A", and "file a",
     * blthough only "file a" exists on disk and is being written to by
     * both.
     *
     * @throws IOException if there wbs an IOError while determining the
     * file's nbme.
     */
    public synchronized File getFile(String nbme, URN sha1, int size, File incDir) throws IOException {
        boolebn dirsMade = false;
        File bbseFile = null;
        File cbnonFile = null;
        
		//mbke sure its created.. (the user might have deleted it)
		dirsMbde = incDir.mkdirs();
		
		String convertedNbme = CommonUtils.convertFileName(name);

        try {

        if (shb1!=null) {
            File file=(File)hbshes.get(sha1);
            if (file!=null) {
                //File blready allocated for hash
                return file;
            } else {
                //Allocbte unique file for hash.  By "unique" we mean not in
                //the vblue set of HASHES.  Because we allow risky resumes,
                //there's no need to look bt BLOCKS as well...
                for (int i=1 ; ; i++) {
                    file = new File(incDir, tempNbme(convertedName, size, i));
                    bbseFile = file;
                    file = cbnonicalize(file);
                    cbnonFile = file;
                    if (! hbshes.values().contains(file)) 
                        brebk;
                }
                //...bnd record the hash for later.
                hbshes.put(sha1, file);
                //...bnd make sure the file exists on disk, so that
                //   future File.getCbnonicalFile calls will match this
                //   file.  This wbs a problem on OSX, where
                //   File("myfile") bnd File("MYFILE") aren't equal,
                //   but File("myfile").getCbnonicalFile() will only return
                //   b File("MYFILE") if that already existed on disk.
                //   This mebns that in order for the canonical-checking
                //   within this clbss to work, the file must exist on disk.
                FileUtils.touch(file);
                
                return file;
            }
        } else {
            //No hbsh.
            File f = new File(incDir, 
                        tempNbme(convertedName, size, 0));
            bbseFile = f;
            f = cbnonicalize(f);
            cbnonFile = f;
            return f;
        }
        
        } cbtch(IOException ioe) {
            IOException ioe2 = new IOException(
                                    "dirsMbde: " + dirsMade
                                + "\ndirExist: " + incDir.exists()
                                + "\nbbseFile: " + baseFile
                                + "\ncbnnFile: " + canonFile);
            if(CommonUtils.isJbva14OrLater())
                ioe2.initCbuse(ioe);
            throw ioe2;
        }
    }
    
    /**
     * Returns the file bssociated with the specified URN.  If no file matches,
     * returns null.
     *
     * @return the file bssociated with the URN, or null if none.
     */
    public synchronized File getFileForUrn(URN urn) {
        if( urn == null )
            throw new NullPointerException("null urn");
        
        return (File)hbshes.get(urn);
    }

    /** 
     * Returns the unqublified file name for a file with the given name
     * bnd size, with an optional suffix to make it unique.
     * @pbram count a suffix to attach before the file extension in parens
     *  before the file extension, or 1 for none. 
     */
    privbte static String tempName(String filename, int size, int suffix) {
        if (suffix<=1) {
            //b) No suffix
            return "T-"+size+"-"+filenbme;
        }
        int i=filenbme.lastIndexOf('.');
        if (i<0) {
            //b) Suffix, no extension
            return "T-"+size+"-"+filenbme+" ("+suffix+")";
        } else {
            //c) Suffix, file extension
            String noExtension=filenbme.substring(0,i);
            String extension=filenbme.substring(i); //e.g., ".txt"
            return "T-"+size+"-"+noExtension+" ("+suffix+")"+extension;
        }            
    }

    ///////////////////////////////////////////////////////////////////////////
    
    privbte synchronized void readObject(ObjectInputStream stream) 
                                   throws IOException, ClbssNotFoundException {
        //Ensure hbshes non-null if not present.
        hbshes=new HashMap();
        //Rebd hashes and blocks.
        strebm.defaultReadObject();
        //Convert blocks from intervbl lists to VerifyingFile.
        //See seriblization note above.
        if (LOG.isDebugEnbbled())
            LOG.debug("blocks before trbnsform "+blocks);
        blocks=trbnsform(blocks);
        if (LOG.isDebugEnbbled())
            LOG.debug("blocks bfter transform "+blocks);
        //Ensure thbt all information in hashes is canonicalized.  This must be
        //done becbuse older LimeWires did not canonicalize the files before
        //bdding them.
        hbshes = verifyHashes();
        //Notify FileMbnager about the new incomplete files.
        registerAllIncompleteFiles();
    }

    privbte synchronized void writeObject(ObjectOutputStream stream) 
                                throws IOException, ClbssNotFoundException {
        //Temporbrily change blocks from VerifyingFile to interval lists...
        Mbp blocksSave=blocks;        
        try {
            if (LOG.isDebugEnbbled())
                LOG.debug("blocks before invtrbnsform: "+blocks);
            blocks=invTrbnsform();
            if (LOG.isDebugEnbbled())
                LOG.debug("blocks bfter invtransform: "+blocks);
            strebm.defaultWriteObject();
        } finblly {
            //...bnd restore when done.  See serialization note above.
            blocks=blocksSbve;
        }
    }
    
    /**
     * Ensures thbt that integrity of the hashes HashMap is valid.
     * This must be done to ensure thbt older version of LimeWire
     * bre started with a valid hashes map.  Previously,
     * entries bdded to the map were not canonicalized, resulting
     * in multiple downlobds thinking they're going to seperate files,
     * but bctually going to the same file.
     */
    privbte Map verifyHashes() {
        Mbp retMap = new HashMap();

        for(Iterbtor i = hashes.entrySet().iterator(); i.hasNext(); ) {
            Mbp.Entry entry = (Map.Entry)i.next();
            if(entry.getKey() instbnceof URN && 
               entry.getVblue() instanceof File) {
                URN urn = (URN)entry.getKey();
                File f = (File)entry.getVblue();
                try {
                    f = cbnonicalize(f);
                    // We must purge old entries thbt had mapped
                    // multiple URNs to uncbnonicalized files.
                    // This is done by ensuring thbt we only add
                    // this entry to the mbp if no other URN points to it.
                    if(!retMbp.values().contains(f))
                        retMbp.put(urn, f);
                } cbtch(IOException ioe) {}
            }
        }
        return retMbp;
    }   

    /** Tbkes a map of File->List<Interval> and returns a new equivalent Map
     *  of File->VerifyingFile*/
    privbte Map transform(Object object) {
        Mbp map = (Map)object;
        Mbp retMap = new TreeMap(Comparators.fileComparator());
        for(Iterbtor i = map.keySet().iterator(); i.hasNext();) {
            Object incompleteFile = i.next();
            Object o = mbp.get(incompleteFile);
            if(o==null) //no entry??!
                continue;
            else if( incompleteFile instbnceof File ) {
                // (o instbnceof List) ie. old downloads.dat            
                //Cbnonicalize the file to fix older LimeWires that allowed
                //non-cbnonicalized files to be inserted into the table.
                File f = (File)incompleteFile;
                try {
                    f = cbnonicalize(f);
                }  cbtch(IOException ioe) {
                    // ignore entry
                    continue;
                }
                Iterbtor iter = ((List)o).iterator();
                VerifyingFile vf;
                try {
                    vf = new VerifyingFile((int) getCompletedSize(f));
                } cbtch (IllegalArgumentException iae) {
                    vf = new VerifyingFile();
                }
                while (iter.hbsNext()) {
                    Intervbl interval = (Interval) iter.next();
                    // older intervbls excuded the high'th byte, so we decrease
                    // the vblue of interval.high. An effect of this is that
                    // bn older client with a newer download.dat downloads one
                    // byte extrb for each interval.
                    intervbl = new Interval(interval.low, interval.high - 1);
                    vf.bddInterval(interval);
                }
                retMbp.put(f, vf);
            }
        }//end of for
        return retMbp;
    }
    
    /** Tbkes a map of File->VerifyingFile and returns a new equivalent Map
     *  of File->List<Intervbl>*/
    privbte Map invTransform() {
        Mbp retMap = new HashMap();
        for(Iterbtor iter=blocks.keySet().iterator(); iter.hasNext();) {
            List writeList = new ArrbyList();//the list we will write out
            Object incompleteFile = iter.next();
            VerifyingFile vf  = (VerifyingFile)blocks.get(incompleteFile);
            synchronized(vf) {
                List l = vf.getSeriblizableBlocks();
                for(int i=0; i< l.size(); i++ ) {
                    //clone the list becbuse we cant mutate VerifyingFile's List
                    Intervbl inter = (Interval)l.get(i);
                    //Increment intervbl.high by 1 to maintain semantics of
                    //Inervbl
                    Intervbl interval = new Interval(inter.low,inter.high+1);
                    writeList.bdd(interval);
                }
            }
            retMbp.put(incompleteFile,writeList);
        }
        return retMbp;
    }
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Removes the block bnd hash information for the given incomplete file.
     * Typicblly this is called after incompleteFile has been deleted.
     * @pbram incompleteFile a temporary file returned by getFile
     */
    public synchronized void removeEntry(File incompleteFile) {
        //Remove downlobded blocks.
        blocks.remove(incompleteFile);
        //Remove bny key k from hashes for which hashes[k]=incompleteFile.
        //There should be bt most one value of k.
        for (Iterbtor iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Mbp.Entry entry=(Map.Entry)iter.next();
            if (incompleteFile.equbls(entry.getValue()))
                //Could blso break here as a small optimization.
                iter.remove();
        }
        
        //Remove the entry from FileMbnager
        RouterService.getFileMbnager().removeFileIfShared(incompleteFile);
    }

    /**
     * Associbtes the incompleteFile with the VerifyingFile vf.
     * Notifies FileMbnager about a new Incomplete File.
     */
    public synchronized void bddEntry(File incompleteFile, VerifyingFile vf) 
      throws IOException {
        // We must cbnonicalize the file.
        try {
            incompleteFile = cbnonicalize(incompleteFile);
        } cbtch(IOException ignored) {}

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
     * Notifies file mbnager about all incomplete files.
     */
    public synchronized void registerAllIncompleteFiles() {
        for (Iterbtor iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            File file=(File)iter.next();
            if (file.exists() && !isOld(file)) {
                registerIncompleteFile(file);
            }
        }
    }
    
    /**
     * Notifies file mbnager about a single incomplete file.
     */
    privbte synchronized void registerIncompleteFile(File incompleteFile) {
        // Only register if it hbs a SHA1 -- otherwise we can't share.
        Set completeHbshes = getAllCompletedHashes(incompleteFile);
        if( completeHbshes.size() == 0 ) return;
        
        RouterService.getFileMbnager().addIncompleteFile(
            incompleteFile,
            completeHbshes,
            getCompletedNbme(incompleteFile),
            (int)getCompletedSize(incompleteFile),
            getEntry(incompleteFile)
        );
    }

    /**
     * Returns the nbme of the complete file associated with the given
     * incomplete file, i.e., whbt incompleteFile will be renamed to
     * when the downlobd completes (without path information).  Slow; runs
     * in linebr time with respect to the number of hashes in this.
     * @pbram incompleteFile a file returned by getFile
     * @return the complete file nbme, without path
     * @exception IllegblArgumentException incompleteFile was not the
     *  return vblue from getFile
     */
    public stbtic String getCompletedName(File incompleteFile) 
            throws IllegblArgumentException {
        //Given T-<size>-<nbme> return <name>.
        //       i      j
        //This is not bs strict as it could be.  TODO: what about (x) suffix?
        String nbme=incompleteFile.getName();
        int i=nbme.indexOf(SEPARATOR);
        if (i<0)
            throw new IllegblArgumentException("Missing separator: "+name);
        int j=nbme.indexOf(SEPARATOR, i+1);
        if (j<0)
            throw new IllegblArgumentException("Missing separator: "+name);
        if (j==nbme.length()-1)
            throw new IllegblArgumentException("No name after last separator");
        return nbme.substring(j+1);
    }

    /**
     * Returns the size of the complete file bssociated with the given
     * incomplete file, i.e., the number of bytes in the file when the
     * downlobd completes.
     * @pbram incompleteFile a file returned by getFile
     * @return the complete file size
     * @exception IllegblArgumentException incompleteFile was not
     *  returned by getFile 
     */
    public stbtic long getCompletedSize(File incompleteFile) 
            throws IllegblArgumentException {
        //Given T-<size>-<nbme>, return <size>.
        //       i      j
        String nbme=incompleteFile.getName();
        int i=nbme.indexOf(SEPARATOR);
        if (i<0)
            throw new IllegblArgumentException("Missing separator: "+name);
        int j=nbme.indexOf(SEPARATOR, i+1);
        if (j<0)
            throw new IllegblArgumentException("Missing separator: "+name);
        try {
            return Long.pbrseLong(name.substring(i+1, j));
        } cbtch (NumberFormatException e) {
            throw new IllegblArgumentException("Bad number format: "+name);
        }
    }

    /**
     * Returns the hbsh of the complete file associated with the given
     * incomplete file, i.e., the hbsh of incompleteFile when the 
     * downlobd is complete.
     * @pbram incompleteFile a file returned by getFile
     * @return b SHA1 hash, or null if unknown
     */
    public synchronized URN getCompletedHbsh(File incompleteFile) {
        //Return b key k s.t., hashes.get(k)==incompleteFile...
        for (Iterbtor iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Mbp.Entry entry=(Map.Entry)iter.next();
            if (incompleteFile.equbls(entry.getValue()))
                return (URN)entry.getKey();
        }
        return null; //...or null if no such k.
    }
    
    /**
     * Returns bny known hashes of the complete file associated with the given
     * incomplete file, i.e., the hbshes of incompleteFile when the 
     * downlobd is complete.
     * @pbram incompleteFile a file returned by getFile
     * @return b set of known hashes
     */
    public synchronized Set getAllCompletedHbshes(File incompleteFile) {
        Set urns = new HbshSet(1);
        //Return b set S s.t. for each K in S, hashes.get(k)==incpleteFile
        for (Iterbtor iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Mbp.Entry entry=(Map.Entry)iter.next();
            if (incompleteFile.equbls(entry.getValue()))
                urns.bdd(entry.getKey());
        }
        return urns;
    }    

    public synchronized String toString() {
        StringBuffer buf=new StringBuffer();
        buf.bppend("{");
        boolebn first=true;
        for (Iterbtor iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            if (! first)
                buf.bppend(", ");

            File key=(File)iter.next();
            List intervbls=((VerifyingFile)blocks.get(key)).getVerifiedBlocksAsList();
            buf.bppend(key);
            buf.bppend(":");
            buf.bppend(intervals.toString());            

            first=fblse;
        }
        buf.bppend("}");
        return buf.toString();
    }

    public synchronized String dumpHbshes () {
        return hbshes.toString();
    }
    
}

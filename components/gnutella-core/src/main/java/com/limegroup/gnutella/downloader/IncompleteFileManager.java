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
 *
 * The original version of this class ensured that two IncompleteFileManager
 * never gave out the same temporary files.  That restriction has now been
 * relaxed, so this class is really somewhat overkill.  However, in the future
 * it may become smarter by looking at hashes, etc. 
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
    
    /**
     * A mapping from temporary files (File) to the blocks of the file stored on
     * disk (List of Interval).  Needed for resumptive smart downloads.
     * INVARIANT: all blocks disjoint, no two intervals can be coalesced into
     * one interval.  Note that blocks are no sorted; there are typically few
     * blocks so performance isn't an issue.
     * <p>
     * Note: Older implementations mapped File -> List<Interval>. 
     * The current version of the code converts the Intervals to a VerifyingFile
     * and uses it for downloads. When the IncompleteFileManager needs to be 
     * serialized, we convert the VerifyingFile back to a List of Intervals
     * This is to reduce backwards compatibility and forwards compatibiliy 
     * issues.
     */
    private Map /* File -> VerifyingFile */ blocks=
        new TreeMap(new FileComparator());  
    

    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Deletes incomplete files more than INCOMPLETE_PURGE_TIME days old from
     * disk.  Then removes any entries from this for which there is no file on
     * disk.  
     * @return true iff any entries were purged
     */
    public synchronized boolean purge() {
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

    /** Returns the fully-qualified temporary download file for the given
     *  file/location pair.  The location of the file is determined by the
     *  INCOMPLETE_DIRECTORY property.  For example, getFile("test.txt", 1999) 
     *  may return "C:\Program Files\LimeWire\Incomplete\T-1999-Test.txt". 
     *  The disk is not modified.<p>
     *
     *  This method gives duplicate files the same temporary file.  That is, for
     *  all rfd_i and rfd_j
     *
     *       rfd_i~=rfd_j <==> getFile(rfd_i).equals(getFile(rfd_j))<p>  
     * 
     *  Currently rfd_i~=rfd_j if rfd_i.getName().equals(rfd_j) &&
     *  rfd_i.getSize()==rfd_j.getSize().  In the future, this definition may be
     *  strengthened to depend on hash values.
     */
    public File getFile(RemoteFileDesc rfd) {
        return getFile(rfd.getFileName(), rfd.getSize());
    } 

    /** Same thing as getFile(rfd), where rfd.getFile().equals(name) 
     *  and rfd.getSize()==size. */ 
    public File getFile(String name, int size) {
		File incDir = null;
		try {
			incDir = SettingsManager.instance().getIncompleteDirectory();
		} catch(java.io.FileNotFoundException fnfe) {
			// this is fine, as this will just create a file in the current
			// working directory.
		}
        return new File(incDir,"T"+SEPARATOR+size+SEPARATOR+name);
    }

    ///////////////////////////////////////////////////////////////////////////
    
    private synchronized void readObject(ObjectInputStream stream) 
                                   throws IOException, ClassNotFoundException {
        blocks = transform(stream.readObject());

    }

    private synchronized void writeObject(ObjectOutputStream stream) 
                                throws IOException, ClassNotFoundException {
        stream.writeObject(invTransform());
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
            List intervals=(List)blocks.get(key);
            buf.append(key);
            buf.append(":");
            buf.append(intervals.toString());            

            first=false;
        }
        buf.append("}");
        return buf.toString();
    }
    
    /*
    public static void main(String args[]) {
        File file=new File("C:/tmp/test.txt");
        IncompleteFileManager ifm=new IncompleteFileManager();
        Iterator iter=null;

        //0 blocks
        Assert.that(! ifm.getBlocks(file).hasNext());
        Assert.that(ifm.getBlockSize(file)==0);
        //1 block
        ifm.addBlock(file, 0, 10);
        Assert.that(ifm.getBlockSize(file)==10);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 10)));
        Assert.that(! iter.hasNext());
        //2 blocks.  Note that this is stricter than required by the
        //specification, which does not say anything about order.
        ifm.addBlock(file, 20, 30);
        Assert.that(ifm.getBlockSize(file)==20);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 10)));
        Assert.that(iter.next().equals(new Interval(20, 30)));
        Assert.that(! iter.hasNext());
        //Merge from above.  Still two blocks.
        ifm.addBlock(file, 10, 12);
        Assert.that(ifm.getBlockSize(file)==22);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(20, 30)));
        Assert.that(iter.next().equals(new Interval(0, 12)));
        Assert.that(! iter.hasNext());
        System.out.println(ifm.toString());
        //Merge from below.  Still two blocks.
        ifm.addBlock(file, 18, 20);
        Assert.that(ifm.getBlockSize(file)==24);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 12)));
        Assert.that(iter.next().equals(new Interval(18, 30)));
        Assert.that(! iter.hasNext());
        //Join outer blocks.  One block.
        ifm.addBlock(file, 12, 18);
        Assert.that(ifm.getBlockSize(file)==30);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 30)));
        Assert.that(! iter.hasNext());

        ifm=new IncompleteFileManager();
        ifm.addBlock(file, 0, 2);
        ifm.addBlock(file, 5, 7);
        ifm.addBlock(file, 10, 12);
        ifm.addBlock(file, 15, 17);        
        //Merge, consuming middle blocks
        ifm.addBlock(file, 1, 17);
        Assert.that(ifm.getBlockSize(file)==17);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 17)));
        Assert.that(! iter.hasNext());

        ifm=new IncompleteFileManager();
        ifm.addBlock(file, 0, 2);
        ifm.addBlock(file, 5, 7);
        ifm.addBlock(file, 10, 12);
        ifm.addBlock(file, 15, 17);        
        //Merge, consuming middle blocks
        ifm.addBlock(file, 0, 16);
        Assert.that(ifm.getBlockSize(file)==17);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 17)));
        Assert.that(! iter.hasNext());

        File file2=new File("C:/tmp/another file.txt");
        ifm=new IncompleteFileManager();
        ifm.addBlock(file, 0, 2);
        ifm.addBlock(file2, 10, 13);
        Assert.that(ifm.getBlockSize(file)==2);
        Assert.that(ifm.getBlockSize(file2)==3);
        System.out.println(ifm.toString());

        ///////////////////// Test inverse block operations /////////////////
        
        //No blocks
        ifm=new IncompleteFileManager();
        iter=ifm.getFreeBlocks(file, 10);
        Assert.that(iter.next().equals(new Interval(0, 10)));
        Assert.that(! iter.hasNext());
        
        //Not overlapping ends
        ifm.addBlock(file, 2, 5);
        ifm.addBlock(file, 8, 10);
        iter=ifm.getFreeBlocks(file, 12);
        Assert.that(iter.next().equals(new Interval(0, 2)));
        Assert.that(iter.next().equals(new Interval(5, 8)));
        Assert.that(iter.next().equals(new Interval(10, 12)));
        Assert.that(! iter.hasNext());        

        //Overlapping ends
        ifm=new IncompleteFileManager();
        ifm.addBlock(file, 0, 2);
        ifm.addBlock(file, 8, 10);
        iter=ifm.getFreeBlocks(file, 10);
        Assert.that(iter.next().equals(new Interval(2, 8)));
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
    */
}

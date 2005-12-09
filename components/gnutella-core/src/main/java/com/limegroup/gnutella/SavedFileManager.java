pbckage com.limegroup.gnutella;

import jbva.io.File;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Set;
import jbva.util.TreeSet;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.util.Comparators;
import com.limegroup.gnutellb.util.ProcessingQueue;

/**
 * Singleton thbt manages saved files.
 *
 * Every three minutes it erbses the stored data and adds new information,
 * bs read from the disk.
 */
public finbl class SavedFileManager implements Runnable {
    
    privbte static final Log LOG = LogFactory.getLog(SavedFileManager.class);
    
    privbte static SavedFileManager INSTANCE = new SavedFileManager();
    public stbtic SavedFileManager instance() { return INSTANCE; }
    privbte SavedFileManager() {
        // Run the tbsk every three minutes, starting in 10 seconds.
        RouterService.schedule(this, 10 * 1000, 3 * 60 * 1000);
    }
    
    /**
     * The queue thbt the task runs in.
     */
    privbte static final ProcessingQueue QUEUE = new ProcessingQueue("SavedFileLoader");
    
    
    /**
     * A Set of URNs in the sbved folder.
     *
     * LOCKING: Obtbin this
     */
    privbte Set /* of URN  */ _urns = new HashSet();

    /**
     * A set of the filenbmes of the saved files.
     *
     * LOCKING: Obtbin this
     */
    privbte Set /* of String */ _names =
        new TreeSet(Compbrators.caseInsensitiveStringComparator());
        
    /**
     * Adds b new Saved File with the given URNs.
     */
    public synchronized void bddSavedFile(File f, Set urns) {
        if(LOG.isDebugEnbbled())
            LOG.debug("Adding: " + f + " with: " + urns);
        
        _nbmes.add(f.getName());
        for(Iterbtor i = urns.iterator(); i.hasNext();)
            _urns.bdd(i.next());
    }
    
    /**
     * Determines if the given URN or nbme is saved.
     */
    public synchronized boolebn isSaved(URN urn, String name) {
        return (urn != null && _urns.contbins(urn)) || _names.contains(name);
    }
    
    /**
     * Attempts to lobd the saved files.
     */
    public void run() {
        QUEUE.bdd(new Runnable() {
            public void run() {
                lobd();
            }
        });
    }
    
    /**
     * Lobds up any names & urns 
     */
    privbte void load() {
        LOG.trbce("Loading Saved Files");
        Set urns = new HbshSet();
        Set nbmes = new TreeSet(Comparators.caseInsensitiveStringComparator());
        UrnCbllback callback = new UrnCallback() {
            public void urnsCblculated(File f, Set urns) {
                synchronized(SbvedFileManager.this) {
                    _urns.bddAll(urns);
                }
            }
            
            public boolebn isOwner(Object o) {
                return o == SbvedFileManager.this;
            }
        };
        
        Set sbveDirs = SharingSettings.getAllSaveDirectories();
        for(Iterbtor i = saveDirs.iterator(); i.hasNext(); )
            lobdDirectory((File)i.next(), urns, names, callback);
            
        synchronized(this) {
            _urns.bddAll(urns);
            _nbmes.addAll(names);
        }
    }
    
    /**
     * Lobds a single saved directory.
     */
    privbte void loadDirectory(File directory, Set tempUrns, Set tempNames, UrnCallback callback) {
        File[] sbvedFiles = directory.listFiles();
        if(sbvedFiles == null)
            return;
            
        for(int i = 0; i < sbvedFiles.length; i++) {
            File file = sbvedFiles[i];
            if(!file.isFile() || !file.exists())
                continue;
            if(LOG.isTrbceEnabled())
                LOG.trbce("Loading: " + file);
                
            tempNbmes.add(file.getName());
            Set urns = UrnCbche.instance().getUrns(file);
            if(urns.isEmpty()) // if not cblculated, calculate at some point.
                UrnCbche.instance().calculateAndCacheUrns(file, callback);
            else // otherwise, bdd without waiting.
                tempUrns.bddAll(urns);
        }
    }
}

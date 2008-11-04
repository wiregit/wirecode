package com.limegroup.gnutella.library;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.core.settings.SharingSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.AbstractSettingsGroup;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.GenericsUtils.ScanMode;

class LibraryFileData extends AbstractSettingsGroup {
    
    private static final Log LOG = LogFactory.getLog(LibraryFileData.class);
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /** Default file extensions. */
    private final String DEFAULT_MANAGED_EXTENSIONS_STRING =
        "asx;html;htm;xml;txt;pdf;ps;rtf;doc;tex;mp3;mp4;wav;wax;au;aif;aiff;"+
        "ra;ram;wma;wm;wmv;mp2v;mlv;mpa;mpv2;mid;midi;rmi;aifc;snd;flac;fla;"+
        "mpg;mpeg;asf;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;"+
        "exe;zip;gz;gzip;hqx;tar;tgz;z;rmj;lqt;rar;ace;sit;smi;img;ogg;rm;"+
        "bin;dmg;jve;nsv;med;mod;7z;iso;lwtp;pmf;m4a;bz2;sea;pf;arc;arj;"+
        "bz;tbz;mime;taz;ua;toast;lit;rpm;deb;pkg;sxw;l6t;srt;sub;idx;mkv;"+
        "ogm;shn;dvi;rmvp;kar;cdg;ccd;cue;c;h;m;java;jar;pl;py;pyc;"+
        "pyo;pyz;" +
        // Formerly sensitive extensions..
        "doc;pdf;xls;rtf;bak;csv;dat;docx;xlsx;xlam;xltx;xltm;xlsm;xlsb;dotm;" +
        "docm;dotx;dot;qdf;qtx;qph;qel;qdb;qsd;qif;mbf;mny";
    
    private final Collection<String> DEFAULT_MANAGED_EXTENSIONS =
        Collections.unmodifiableList(Arrays.asList(DEFAULT_MANAGED_EXTENSIONS_STRING.split(";")));
    
    private final Set<String> userExtensions = new HashSet<String>();
    private final Set<String> userRemoved = new HashSet<String>();
    private final Set<File> directoriesToManageRecursively = new HashSet<File>();
    private final Set<File> directoriesNotToManage = new HashSet<File>();
    private final Set<File> excludedFiles = new HashSet<File>();
    private final Map<File, FileProperties> libraryShareData = new HashMap<File, FileProperties>();
    
    private final File saveFile = new File(CommonUtils.getUserSettingsDir(), "library5.dat"); 
    
    private volatile boolean loaded = false;

    LibraryFileData() {
        SettingsGroupManager.instance().addSettingsGroup(this);
    }
    
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public void reload() {
        load();
    }
    
    @Override
    public boolean revertToDefault() {
        lock.writeLock().lock();
        try {
            userExtensions.clear();
            userRemoved.clear();
            directoriesToManageRecursively.clear();
            directoriesNotToManage.clear();
            excludedFiles.clear();
            libraryShareData.clear();
        } finally {
            lock.writeLock().unlock();
        }
        
        return true;
    }

    public boolean save() {
        Map<String, Object> save = new HashMap<String, Object>();
        lock.readLock().lock();
        try {
            save.put("USER_EXTENSIONS", userExtensions);
            save.put("USER_REMOVED", userRemoved);
            save.put("MANAGED_DIRECTORIES", directoriesToManageRecursively);
            save.put("DO_NOT_MANAGE", directoriesNotToManage);
            save.put("EXCLUDE_FILES", excludedFiles);
            save.put("SHARE_DATA", libraryShareData);
            
            try {
                ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(saveFile)));
                out.writeObject(save);
                out.flush();
                out.close();
            } catch(IOException iox) {
                LOG.debug("IOX saving library", iox);
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return true;
    }
    
    void load() {
        Map<String, Object> readMap = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(saveFile)));
            Object read = in.readObject();
            readMap = GenericsUtils.scanForMap(read, String.class, Object.class, ScanMode.REMOVE);
            if (readMap != null) {
                Set<String> userExtensions = GenericsUtils.scanForSet(readMap.get("USER_EXTENSIONS"),
                        String.class, ScanMode.REMOVE);
                Set<String> userRemoved = GenericsUtils.scanForSet(readMap.get("USER_REMOVED"),
                        String.class, ScanMode.REMOVE);
                Set<File> directoriesToManageRecursively = GenericsUtils.scanForSet(readMap
                        .get("MANAGED_DIRECTORIES"), File.class, ScanMode.REMOVE);
                Set<File> directoriesNotToManage = GenericsUtils.scanForSet(
                        readMap.get("DO_NOT_MANAGE"), File.class, ScanMode.REMOVE);
                Set<File> excludedFiles = GenericsUtils.scanForSet(readMap.get("EXCLUDE_FILES"),
                        File.class, ScanMode.REMOVE);
                Map<File, FileProperties> libraryShareData = GenericsUtils.scanForMap(readMap
                        .get("SHARE_DATA"), File.class, FileProperties.class, ScanMode.REMOVE);
                
                lock.writeLock().lock();
                try {
                    this.userExtensions.addAll(userExtensions);
                    this.userRemoved.addAll(userRemoved);
                    this.directoriesToManageRecursively.addAll(directoriesToManageRecursively);
                    this.directoriesNotToManage.addAll(directoriesNotToManage);
                    this.excludedFiles.addAll(excludedFiles);
                    this.libraryShareData.putAll(libraryShareData);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        } catch(Throwable throwable) {
            LOG.error("Error loading library", throwable);
        }
        
        this.loaded = true;
    }

    /** Returns true if the given file should be excluded from managing. */
    boolean isFileExcluded(File file) {
        lock.readLock().lock();
        try {
            return excludedFiles.contains(file);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds a managed file.  If explicit is true, this will explicitly add it to the list of managed files,
     * otherwise, it will just remove it from the list of excluded files.
     */
    void addManagedFile(File file, boolean explicit) {
        lock.writeLock().lock();
        try {
            excludedFiles.remove(file);
            if(explicit && !libraryShareData.containsKey(file)) {
                libraryShareData.put(file, new FileProperties());
            } 
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Removes a file from being managed.  If explicit is true, this will
     * explicitly add it to a list of excluded files that will not be
     * managed when a folder is scanned.
     * 
     * @param file
     * @param exclude
     */
    void removeManagedFile(File file, boolean explicit) {
        lock.writeLock().lock();
        try {
            libraryShareData.remove(file);
            if(explicit) {
                excludedFiles.add(file);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns a list of all files that should be managed. */
    Iterable<File> getManagedFiles() {
        List<File> indivFiles = new ArrayList<File>();
        lock.readLock().lock();
        try {
            indivFiles.addAll(libraryShareData.keySet());
        } finally {
            lock.readLock().unlock();
        }
        return indivFiles;
    }

    /** Retuns true if the given folder is the incomplete folder. */
    boolean isIncompleteDirectory(File folder) {
        return canonicalize(SharingSettings.INCOMPLETE_DIRECTORY.getValue()).equals(folder);
    }
    
    /** Sets the new directory to exclude from recursive management. */
    void setDirectoriesToExcludeFromManaging(Collection<File> folders) {
        lock.writeLock().lock();
        try {
            directoriesNotToManage.clear();
            directoriesNotToManage.addAll(folders);
            directoriesToManageRecursively.removeAll(folders);
        } finally {
            lock.writeLock().unlock();
        }
    }   

    /** Returns true if the given folder should be excluded. */
    boolean isFolderExcluded(File folder) {
        lock.readLock().lock();
        try {
            return directoriesNotToManage.contains(folder);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /** Adds a new directory to recursively manage. */
    void addDirectoryToManageRecursively(File folder) {
        lock.writeLock().lock();
        try {
            directoriesToManageRecursively.add(folder);
            directoriesNotToManage.remove(folder);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Sets the new directory to recursively manage. */
    void setDirectoriesToManageRecursively(Collection<File> folders) {
        lock.writeLock().lock();
        try {
            directoriesToManageRecursively.clear();
            directoriesToManageRecursively.addAll(folders);
            directoriesNotToManage.removeAll(folders);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns a list of all top-level directories that should be managed recursively. */
    List<File> getDirectoriesToManageRecursively() {
        lock.readLock().lock();
        try {
            return new ArrayList<File>(directoriesToManageRecursively);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setUserExtensions(Collection<String> addedExtensions) {
        lock.writeLock().lock();
        try {
            userExtensions.clear();
            userExtensions.addAll(addedExtensions);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setUserRemovedExtensions(Collection<String> removedExtensions) {
        lock.writeLock().lock();
        try {
            userRemoved.clear();
            userRemoved.addAll(removedExtensions);
        } finally {
            lock.writeLock().unlock();
        }
    }    
    
    /** Returns all extensions that are manageable. */
    Collection<String> getManagedExtensions() {
        lock.readLock().lock();
        Set<String> extensions = new HashSet<String>();        
        try {
            extensions.addAll(DEFAULT_MANAGED_EXTENSIONS);
            extensions.addAll(userExtensions);
            extensions.removeAll(userRemoved);
        } finally {
            lock.readLock().unlock();
        }
        return extensions;
    }

    void setManagedExtensions(Collection<String> newExtensions) {
        lock.writeLock().lock();
        try {
            userRemoved.clear();
            userRemoved.addAll(DEFAULT_MANAGED_EXTENSIONS);
            userRemoved.removeAll(newExtensions);
            
            userExtensions.clear();
            userExtensions.addAll(newExtensions);
            userExtensions.removeAll(DEFAULT_MANAGED_EXTENSIONS);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public Collection<String> getDefaultManagedExtensions() {
        return DEFAULT_MANAGED_EXTENSIONS;
    }
    
    /** Marks the given file as either shared or not shared with gnutella. */
    void setSharedWithGnutella(File file, boolean shared) {
        lock.writeLock().lock();
        try {
            FileProperties props = libraryShareData.get(file);
            if(props == null) {
                if(!shared) {
                    return; // Nothing to do.
                }
                props = new FileProperties();
                libraryShareData.put(file, props);
            }
            props.gnutella = shared;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Returns true if the file is shared with gnutella. */
    boolean isSharedWithGnutella(File file) {
        lock.readLock().lock();
        try {
            FileProperties props = libraryShareData.get(file);
            if(props != null) {
                return props.gnutella;
            } else {
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }        
    }

    /** Marks the given file as either shared or not shared with the given friend. */
    void setSharedWithFriend(File file, String friendId, boolean shared) {
        lock.writeLock().lock();
        try {
            FileProperties props = libraryShareData.get(file);
            if(props == null) {
                if(!shared) {
                    return; // Nothing to do.
                }
                props = new FileProperties();
                libraryShareData.put(file, props);
            }
            if(props.friends == null) {
                if(!shared) {
                    return; // Nothing to do.
                }
                props.friends = new HashSet<String>();
            }
            if(shared) {
                props.friends.add(friendId);
            } else {
                props.friends.remove(friendId);
                if(props.friends.isEmpty()) {
                    props.friends = null;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }    
    
    /** Returns true if the file is shared with the given friend. */
    boolean isSharedWithFriend(File file, String friendId) {
        lock.readLock().lock();
        try {
            FileProperties props = libraryShareData.get(file);
            if(props != null && props.friends != null) {
                return props.friends.contains(friendId);
            } else {
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    private File canonicalize(File file) {
        try {
            return FileUtils.getCanonicalFile(file);  
        } catch(IOException iox) {
            return file;
        }
    }
    
    private static class FileProperties implements Serializable {
        private boolean gnutella;
        private Set<String> friends;
    }
}

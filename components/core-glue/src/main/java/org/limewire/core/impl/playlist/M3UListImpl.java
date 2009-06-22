package org.limewire.core.impl.playlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.playlist.M3UList;
import org.limewire.io.IOUtils;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.util.FileUtils;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;

public class M3UListImpl implements M3UList {

    private static final String M3U_HEADER = "#EXTM3U";
    private static final String SONG_DELIM = "#EXTINF";
    private static final String SEC_DELIM  = ":";
    
    private final File file;
    private final LocalFileList localFileList;
    
    public M3UListImpl(File file, LocalFileList localFileList) {
        this.file = file;
        this.localFileList = localFileList;
    }
    
    @Override
    public void load() {
        BackgroundExecutorService.execute(new Runnable(){
            public void run() {
                try {
                    List<File> m3uFileList = loadInternalList();
                    for(File file : m3uFileList)
                        localFileList.addFile(file);
                } catch (Exception e) {
                    //TODO: give user feedback
                }
            }
        });
    }

    @Override
    public void save() {
        FilterList<LocalFileItem> filterList = new FilterList<LocalFileItem>(localFileList.getModel(), new AudioMatcher());
        final List<LocalFileItem> writeableList;
        
        filterList.getReadWriteLock().readLock().lock();
        try {
            writeableList = Collections.unmodifiableList(filterList.subList(0, filterList.size()));
        } finally {
            filterList.getReadWriteLock().readLock().unlock();
        }
        
        if(writeableList != null) {
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {
                    try {
                        saveInternalList(writeableList);
                    } catch (Exception e) {
                        //TODO: notify user
                    }
                }
            });
        }
    }
    
    /**
     * Attempts to write the list of files to an m3u list. 
     * Follows the standard m3u protocol. 
     */
    private void saveInternalList(List<LocalFileItem> writeableList) throws Exception {
        PrintWriter m3uFile = null;
        try {
            m3uFile = new PrintWriter(new FileWriter(file.getCanonicalPath(), false));

            m3uFile.write(M3U_HEADER);
            m3uFile.println();

            for(LocalFileItem currFile : writeableList) {
                // only save files that are local to the file system
                if(currFile.getFile().isFile() && !currFile.isIncomplete()){
                    File locFile = currFile.getFile();

                    // first line of song description...
                    m3uFile.write(SONG_DELIM);
                    m3uFile.write(SEC_DELIM);
                    // try to write out seconds info....
                    if(currFile.getPropertyString(FilePropertyKey.LENGTH) != null) {
                        m3uFile.write("" + currFile.getPropertyString(FilePropertyKey.LENGTH) + ",");
                    } else {
                        m3uFile.write("-1,");
                    }

                    m3uFile.write(currFile.getName());
                    m3uFile.println();
                    // canonical path follows...
                    m3uFile.write(locFile.getCanonicalPath());
                    m3uFile.println();
                }
            }
        } finally {
            IOUtils.close(m3uFile);
        }
    }
    
    /**
     * Attempts to open the current m3u list and locate and load
     * all the files described within that list. Returns the list
     * of Files that were succesfully found.
     */
    private List<File> loadInternalList() throws Exception {
        List<File> m3uFileList = new ArrayList<File>();
        BufferedReader m3uFile = null;
        try {
            m3uFile = new BufferedReader(new FileReader(file));
            String currLine = null;
            currLine = m3uFile.readLine();
            if (currLine == null || !(currLine.startsWith(M3U_HEADER) || currLine.startsWith(SONG_DELIM)))
                throw new IOException();
            if(currLine.startsWith(M3U_HEADER))
                currLine = m3uFile.readLine();
            
            for(; currLine != null; currLine = m3uFile.readLine()) {
                if (currLine.startsWith(SONG_DELIM)) {
                    currLine = m3uFile.readLine();
                    if(currLine == null)
                        break;
                    File currentFile = new File(currLine);
                    if (currentFile.exists() && !currentFile.isDirectory())
                        m3uFileList.add(currentFile);
                    else {
                        // try relative path to the playlist
                        currentFile = new File(file.getParentFile().getAbsolutePath(), currentFile.getPath());
                        if (currentFile.exists() && !currentFile.isDirectory() && 
                                FileUtils.isReallyInParentPath(file.getParentFile(), currentFile))
                            m3uFileList.add(currentFile);
                    }
                }
            }
        } finally {
            IOUtils.close(m3uFile);
        }
        return m3uFileList;
    }
    
    /**
     * Matches only files that we consider to be Audio files. Audio
     * files are defined by the Category.Audio.
     */
    private class AudioMatcher implements Matcher<LocalFileItem> {
        @Override
        public boolean matches(LocalFileItem item) {
            return item.getCategory() == Category.AUDIO;
        }
    }
}

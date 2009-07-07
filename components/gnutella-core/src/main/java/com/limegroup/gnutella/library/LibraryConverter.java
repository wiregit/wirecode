package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.StringArraySetting;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;


/**
 * Converts from old-style library settings (shared files)
 * to new-style library settings (managed files)
 */
@SuppressWarnings("deprecation")
class LibraryConverter {
    
    boolean isOutOfDate() {
        return LibrarySettings.VERSION.get() == LibrarySettings.LibraryVersion.FOUR_X.name();
    }
    
    void convert(LibraryFileData newData) {
        newData.revertToDefault();
        
        List<File> sharedFolders = new ArrayList<File>();
        List<File> excludedFolders = new ArrayList<File>();
        List<File> excludedFiles = new ArrayList<File>();
        List<String> extensions = new ArrayList<String>();
        
        OldLibraryData oldData = new OldLibraryData(); // load if necessary
        for(File folder : OldLibrarySettings.DIRECTORIES_TO_SHARE.get()) {
            if(!LibraryUtils.isSensitiveDirectory(folder) || oldData.SENSITIVE_DIRECTORIES_VALIDATED.contains(folder)) {
                folder = FileUtils.canonicalize(folder);
                sharedFolders.add(folder);
            }
        }
        
        excludedFolders.addAll(oldData.DIRECTORIES_NOT_TO_SHARE);
        
        for(File file : oldData.SPECIAL_FILES_TO_SHARE) {
            file = FileUtils.canonicalize(file);            
            if(addManagedFile(newData, file)) {
                newData.setFileInCollection(file, LibraryFileData.DEFAULT_SHARED_COLLECTION_ID, true);
            }
        }
        
        for(File file : oldData.FILES_NOT_TO_SHARE) {
            file = FileUtils.canonicalize(file);
            excludedFiles.add(file);
        }
        
        // Set the new managed extensions.
        extensions.addAll(Arrays.asList(OldLibrarySettings.getDefaultExtensions()));
        extensions.removeAll(Arrays.asList(StringArraySetting.decode(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.get())));
        extensions.addAll(Arrays.asList(StringArraySetting.decode(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.get())));        
        newData.setManagedExtensions(extensions);
        
        // Here's the bulk of the conversion -- loop through, recursively, previously 
        // shared directories & mark all potential files as shareable.
        convertSharedFiles(sharedFolders, excludedFolders, excludedFiles, extensions, newData);
        
        
        for(File file : oldData.SPECIAL_STORE_FILES) {
            file = FileUtils.canonicalize(file);
            addManagedFile(newData, file);
        }
        
        LibrarySettings.VERSION.set(LibrarySettings.LibraryVersion.FIVE_0_0.name());
        
        oldData.revertToDefault();
        OldLibrarySettings.DIRECTORIES_TO_SHARE.revertToDefault();
        OldLibrarySettings.DISABLE_SENSITIVE.revertToDefault();
        OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.revertToDefault();
        OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.revertToDefault();
        OldLibrarySettings.EXTENSIONS_TO_SHARE.revertToDefault();
    }

    /**
     * Checks to make sure that the file is not a document before adding file to managed file list.
     * Returns true if the files was not a document, false otherwise. 
     */
    private boolean addManagedFile(LibraryFileData newData, File file) {
        MediaType mediaType = MediaType.getMediaTypeForExtension(FileUtils.getFileExtension(file));
        if(!MediaType.getDocumentMediaType().equals(mediaType) && !MediaType.getProgramMediaType().equals(mediaType)) {
            newData.addManagedFile(file);
            return true;
        }
        return false;
    }
    
    private void convertSharedFiles(List<File> sharedFolders, List<File> excludedFolders,
            List<File> excludedFiles, List<String> extensions, LibraryFileData data) {
        Set<File> convertedDirectories = new HashSet<File>();
        for (File file : sharedFolders) {
            convertDirectory(file, extensions, sharedFolders, excludedFolders, excludedFiles,
                    convertedDirectories, data);
        }
    }
    
    private void convertDirectory(File directory, final Collection<String> extensions,
            final List<File> sharedFolders, final List<File> excludedFolders, final List<File> excludedFiles,
            Set<File> convertedDirectories, final LibraryFileData data) {
        // If we already converted this directory, exit.
        if (convertedDirectories.contains(directory)) {
            return;
        }
        
        convertedDirectories.add(directory);
        File[] fileList = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return LibraryUtils.isFileManagable(file)
                        && extensions.contains(FileUtils.getFileExtension(file).toLowerCase(Locale.US))
                        && !excludedFiles.contains(file);
            }
        });

        if(fileList != null) {
            for (File file : fileList) {
                file = FileUtils.canonicalize(file);
                if(addManagedFile(data, file)) {
                    data.setFileInCollection(file, LibraryFileData.DEFAULT_SHARED_COLLECTION_ID, true);
                }
            }
        }

        if(!LibraryUtils.isForcedShareDirectory(directory)) {
            File[] dirList = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File folder) {
                    return folder.isDirectory()
                            && folder.canRead()
                            && !data.isIncompleteDirectory(folder)
                            && !LibraryUtils.isApplicationSpecialShareDirectory(folder)
                            && !excludedFolders.contains(folder)
                            && !LibraryUtils.isFolderBanned(folder);
                }
            });
            
            if(dirList != null) {
                for (File subdir : dirList) {
                    convertDirectory(subdir, extensions, sharedFolders, excludedFolders, excludedFiles, convertedDirectories, data);
                }
            }
        }
    }
}

package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import org.limewire.core.settings.SharingSettings;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;


/**
 * A collection of FileDescs containing files shared with an individual friend.
 */
class FriendFileListImpl extends FileListImpl {
    
    private final String idName;
    
    private boolean addNewImagesAlways = false;
    private boolean addNewAudioAlways = false;
    private boolean addNewVideoAlways = false;
    

    public FriendFileListImpl(FileManager fileManager, Set<File> individualFiles, String id) {
        super(fileManager, individualFiles);
        if(id == null)
            throw new NullPointerException("ID cannot be null");
        idName = id;
        
        addNewAudioAlways = SharingSettings.containsFriendShareNewAudio(id);
        addNewImagesAlways = SharingSettings.containsFriendShareNewImages(id);
        addNewVideoAlways = SharingSettings.containsFriendShareNewVideo(id);
    }

    /**
     * Only called by events from FileManager. Will only add this
     * FileDesc if this list was explicitly waiting for this file
     */
    @Override
    protected void addPendingFileDesc(FileDesc fileDesc) {
        if(pendingFiles.contains(fileDesc.getFile()) || isSmartlySharedType(fileDesc.getFile()))
            add(fileDesc);
    }
    
    /**
     * Friend lists are based completely on individual files since there's no 
     * directory for files to defaultly reside in. Always add friend files as 
     * individuals
     */
    @Override
    protected void addAsIndividualFile(FileDesc fileDesc) {
        individualFiles.add(fileDesc.getFile());
    }
    
    /**
     * When a file is removed, if that file type was being smartly shared, then
     * the smart sharing function is remove for that file type.
     */
    @Override
    public boolean remove(FileDesc fileDesc) {
        stopSmartSharingType(fileDesc.getFile());
        return super.remove(fileDesc);
    }
    
    /**
     * As long as its not a store file it can be added
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        if( fileDesc.getLimeXMLDocuments().size() != 0 && 
                isStoreXML(fileDesc.getLimeXMLDocuments().get(0))) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Changes the smart sharing value for images. If true, all new images added to
     * the library will be shared with this list, if false, new images added to 
     * the library will not be automatically shared with this list but current images
     * will not be removed.
     */
    @Override
    public void setAddNewImageAlways(boolean value) {
        if(value != addNewImagesAlways) {
            if(value == false) {
                SharingSettings.removeFiendShareNewFiles(SharingSettings.SHARE_NEW_IMAGES_ALWAYS, idName);
            } else {
                SharingSettings.addFriendShareNewFiles(SharingSettings.SHARE_NEW_IMAGES_ALWAYS, idName);
            }
            addNewImagesAlways = value;
        }
    }
    
    /**
     * Returns true if image files are being smartly shraed with this friend, false otherwise.
     */
    @Override
    public boolean isAddNewImageAlways() {
        return addNewImagesAlways;
    }
    
    /**
     * Changes the smart sharing value for audio files. If true, all new audio files added to
     * the library will be shared with this list, if false, new audio files added to 
     * the library will not be automatically shared with this list but current audio files
     * will not be removed.
     */
    @Override
    public void setAddNewAudioAlways(boolean value) {
        if(value != addNewAudioAlways) {
            if(value == false) {
                SharingSettings.removeFiendShareNewFiles(SharingSettings.SHARE_NEW_AUDIO_ALWAYS, idName);
            } else {
                SharingSettings.addFriendShareNewFiles(SharingSettings.SHARE_NEW_AUDIO_ALWAYS, idName);
            }
            addNewAudioAlways = value;
        }
    }
    
    /**
     * Returns true if audio files are being smartly shared with this friend, false otherwise.
     */
    @Override
    public boolean isAddNewAudioAlways() {
        return addNewAudioAlways;
    }
    
    /**
     * Changes the smart sharing value for videos. If true, all new videos added to
     * the library will be shared with this list, if false, new videos added to 
     * the library will not be automatically shared with this list but current videos
     * will not be removed.
     */
    @Override
    public void setAddNewVideoAlways(boolean value) {
        if(value != addNewVideoAlways) {
            if(value == false) {
                SharingSettings.removeFiendShareNewFiles(SharingSettings.SHARE_NEW_VIDEO_ALWAYS, idName);
            } else {
                SharingSettings.addFriendShareNewFiles(SharingSettings.SHARE_NEW_VIDEO_ALWAYS, idName);
            }
            addNewVideoAlways = value;
        }
    }
    
    /**
     * Returns true if videos are being smartly shared with this friend, false otherwise.
     */
    @Override
    public boolean isAddNewVideoAlways() {
        return addNewVideoAlways;
    }
    
    /**
     * Returns true if this file type is being smartly shared. Smartly shared file
     * types are always added to this list.
     */
    private boolean isSmartlySharedType(File file) {
        if(addNewAudioAlways == false && addNewImagesAlways == false && addNewVideoAlways == false)
            return false;
        
        String ext = FileUtils.getFileExtension(file);
        MediaType type = MediaType.getMediaTypeForExtension(ext);
        if (type == MediaType.getAudioMediaType() && addNewAudioAlways)
            return true;
        else if (type == MediaType.getVideoMediaType() && addNewVideoAlways)
            return true;
        else if (type == MediaType.getImageMediaType() && addNewImagesAlways)
            return true;
        return false;
    }
    
    /**
     * Stops smartly sharing this file type if it was being smartly shared prior.
     */
    private void stopSmartSharingType(File file) {
        if(addNewAudioAlways == false && addNewImagesAlways == false && addNewVideoAlways == false)
            return;
        
        String ext = FileUtils.getFileExtension(file);
        MediaType type = MediaType.getMediaTypeForExtension(ext);
        if (type == MediaType.getAudioMediaType() && addNewAudioAlways)
            setAddNewAudioAlways(false);
        else if (type == MediaType.getVideoMediaType() && addNewVideoAlways)
            setAddNewVideoAlways(false);
        else if (type == MediaType.getImageMediaType() && addNewImagesAlways)
            setAddNewImageAlways(false);
    }
    
    @Override
    protected void fireAddEvent(FileDesc fileDesc) {
        fileDesc.incrementShareListCount();
        super.fireAddEvent(fileDesc);
    }

    @Override
    protected void fireRemoveEvent(FileDesc fileDesc) {
        fileDesc.decrementShareListCount();
        super.fireRemoveEvent(fileDesc);
    }

    @Override
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        oldFileDesc.decrementShareListCount();
        newFileDesc.incrementShareListCount();
        super.fireChangeEvent(oldFileDesc, newFileDesc);
    }
}

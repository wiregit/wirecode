package com.limegroup.gnutella.library;

import java.io.File;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;
import org.limewire.util.Objects;


/**
 * A collection of FileDescs containing files shared with an individual friend.
 */
class FriendFileListImpl extends AbstractFileList implements FriendFileList {
    
    private final String id;
    
    private boolean addNewImagesAlways = false;
    private boolean addNewAudioAlways = false;
    private boolean addNewVideoAlways = false;
    
    protected final LibraryFileData data;

    public FriendFileListImpl(LibraryFileData data, ManagedFileListImpl managedList,  String id) {
        super(managedList);
        this.id = Objects.nonNull(id, "id");
        this.data = data;
        
        addNewAudioAlways = LibrarySettings.containsFriendShareNewAudio(id);
        addNewImagesAlways = LibrarySettings.containsFriendShareNewImages(id);
        addNewVideoAlways = LibrarySettings.containsFriendShareNewVideo(id);
    }
    
    @Override
    public boolean add(FileDesc fileDesc) {
        return super.add(fileDesc);
    }
    
    /**
     * Returns false if it's an {@link IncompleteFileDesc} or it's a store
     * file.
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        if (fileDesc instanceof IncompleteFileDesc) {
            return false;
        }
        if( fileDesc.getLimeXMLDocuments().size() != 0 && 
                isStoreXML(fileDesc.getLimeXMLDocuments().get(0))) {
            return false;
        } 
        return true;
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
                LibrarySettings.removeFiendShareNewFiles(LibrarySettings.SHARE_NEW_IMAGES_ALWAYS, id);
            } else {
                LibrarySettings.addFriendShareNewFiles(LibrarySettings.SHARE_NEW_IMAGES_ALWAYS, id);
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
                LibrarySettings.removeFiendShareNewFiles(LibrarySettings.SHARE_NEW_AUDIO_ALWAYS, id);
            } else {
                LibrarySettings.addFriendShareNewFiles(LibrarySettings.SHARE_NEW_AUDIO_ALWAYS, id);
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
                LibrarySettings.removeFiendShareNewFiles(LibrarySettings.SHARE_NEW_VIDEO_ALWAYS, id);
            } else {
                LibrarySettings.addFriendShareNewFiles(LibrarySettings.SHARE_NEW_VIDEO_ALWAYS, id);
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
    
    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return isSmartlySharedType(file) || data.isSharedWithFriend(file, id);
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        // If we're smart sharing, stop doing it if we removed.
        //                       , if added do nothing special.
        // If not smart sharing, just synchronize the data.
        if(isSmartlySharedType(file)) {
            if(!added) {
                stopSmartSharingType(file);
            }
        } else {
            data.setSharedWithFriend(file, id, added);
        }        
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

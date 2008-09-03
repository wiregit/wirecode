/**
 * 
 */
package org.limewire.ui.swing.browser.download;

import java.io.File;

import org.limewire.util.Objects;
import org.mozilla.interfaces.nsIFile;
import org.mozilla.interfaces.nsILocalFile;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;

/**
 * Minimal implemenation of the nsILocalFile interface to be used by our own
 * nsIdownloadManager implementation. 
 */
final class LimeNoOpMozillaLocalFile implements nsILocalFile {

    private File file;

    private long fileSize;

    private String leafName = "";

    public LimeNoOpMozillaLocalFile(File file) {
        Objects.nonNull(file, "file");
        this.file = file.getAbsoluteFile();
    }

    @Override
    public nsIFile _clone() {
        return new LimeNoOpMozillaLocalFile(new File(file.getAbsolutePath()));
    }

    @Override
    public boolean _equals(nsIFile inFile) {
        return equals(inFile);
    }

    @Override
    public void append(String node) {
        // appends the file name to the directory
        this.file = new File(file.getAbsolutePath() + "/" + node);
    }

    @Override
    public void create(long type, long permissions) {
        // do nothing, let limewire handle this action
    }

    @Override
    public boolean exists() {
        // called multiple times by mozilla, first once to make sure that the
        // directory does exists,
        // then an additional time after the file name is appended to this file
        // to make sure the file does not exist,
        // if the file does exist, it sets the leaf value (2), (3) etc. until it
        // finds a file name not in use.
        return new File(file.getAbsolutePath() + leafName).exists();
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public long getLastModifiedTime() {
        return file.lastModified();
    }

    @Override
    public String getLeafName() {
        return leafName;
    }

    @Override
    public nsIFile getParent() {
        return new LimeNoOpMozillaLocalFile(file.getParentFile());
    }

    @Override
    public String getPath() {
        return file.getPath();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isExecutable() {
        return file.canExecute();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    @Override
    public boolean isReadable() {
        return file.canRead();
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean isSymlink() {
        return false;
    }

    @Override
    public boolean isWritable() {
        return file.canWrite();
    }

    @Override
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public void setLastModifiedTime(long lastModifiedTime) {
        file.setLastModified(lastModifiedTime);
    }

    @Override
    public void setLeafName(String leafName) {
        this.leafName = leafName;
    }

    @Override
    public nsISupports queryInterface(String aIID) {
        return Mozilla.queryInterface(this, aIID);
    }

    @Override
    public void appendRelativePath(String relativeFilePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDiskSpaceAvailable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getFollowLinks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPersistentDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRelativeDescriptor(nsILocalFile fromFile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initWithFile(nsILocalFile file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initWithPath(String filePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void launch() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reveal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFollowLinks(boolean followLinks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPersistentDescriptor(String persistentDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRelativeDescriptor(nsILocalFile fromFile, String relativeDesc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(nsIFile inFile, boolean recur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyTo(nsIFile newParentDir, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyToFollowingLinks(nsIFile newParentDir, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createUnique(long type, long permissions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public nsISimpleEnumerator getDirectoryEntries() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getFileSizeOfLink() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModifiedTimeOfLink() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getPermissions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getPermissionsOfLink() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTarget() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveTo(nsIFile newParentDir, String newName) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void normalize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(boolean recursive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLastModifiedTimeOfLink(long lastModifiedTimeOfLink) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPermissions(long permissions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPermissionsOfLink(long permissionsOfLink) {
        throw new UnsupportedOperationException();
    }
}
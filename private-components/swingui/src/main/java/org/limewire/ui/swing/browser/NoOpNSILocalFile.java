/**
 * 
 */
package org.limewire.ui.swing.browser;

import java.io.File;

import org.limewire.util.Objects;
import org.mozilla.interfaces.nsIFile;
import org.mozilla.interfaces.nsILocalFile;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;

final class NoOpNSILocalFile implements nsILocalFile {

    private File file;

    private long fileSize;

    private String leafName = "";

    public NoOpNSILocalFile(File file) {
        Objects.nonNull(file, "file");
        this.file = file.getAbsoluteFile();
    }

    @Override
    public void appendRelativePath(String relativeFilePath) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDiskSpaceAvailable() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return 0;
    }

    @Override
    public boolean getFollowLinks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return false;
    }

    @Override
    public String getPersistentDescriptor() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return null;
    }

    @Override
    public String getRelativeDescriptor(nsILocalFile fromFile) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return null;
    }

    @Override
    public void initWithFile(nsILocalFile file) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void initWithPath(String filePath) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void launch() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void reveal() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void setFollowLinks(boolean followLinks) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void setPersistentDescriptor(String persistentDescriptor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void setRelativeDescriptor(nsILocalFile fromFile, String relativeDesc) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public nsIFile _clone() {
        return new NoOpNSILocalFile(new File(file.getAbsolutePath()));
    }

    @Override
    public boolean _equals(nsIFile inFile) {
        return equals(inFile);
    }

    @Override
    public void append(String node) {
        this.file = new File(file.getAbsolutePath() + "/" + node);
    }

    @Override
    public boolean contains(nsIFile inFile, boolean recur) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return false;
    }

    @Override
    public void copyTo(nsIFile newParentDir, String newName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void copyToFollowingLinks(nsIFile newParentDir, String newName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void create(long type, long permissions) {
       //do nothing, let limewire handle this action
    }

    @Override
    public void createUnique(long type, long permissions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean exists() {
        return new File(file.getAbsolutePath() + leafName).exists();
    }

    @Override
    public nsISimpleEnumerator getDirectoryEntries() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return null;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public long getFileSizeOfLink() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return 0;
    }

    @Override
    public long getLastModifiedTime() {
        return file.lastModified();
    }

    @Override
    public long getLastModifiedTimeOfLink() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return 0;
    }

    @Override
    public String getLeafName() {
        return leafName;
    }

    @Override
    public nsIFile getParent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return null;
    }

    @Override
    public String getPath() {
        return file.getPath();
    }

    @Override
    public long getPermissions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return 0;
    }

    @Override
    public long getPermissionsOfLink() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return 0;
    }

    @Override
    public String getTarget() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
        // return null;
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
    public void moveTo(nsIFile newParentDir, String newName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void normalize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void remove(boolean recursive) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

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
    public void setLastModifiedTimeOfLink(long lastModifiedTimeOfLink) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void setLeafName(String leafName) {
        this.leafName = leafName;
    }

    @Override
    public void setPermissions(long permissions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public void setPermissionsOfLink(long permissionsOfLink) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();

    }

    @Override
    public nsISupports queryInterface(String aIID) {
        return Mozilla.queryInterface(this, aIID);
    }
}
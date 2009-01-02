package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.library.LibraryData;
import org.limewire.util.FileUtils;

public class LibraryManagerItemImpl implements LibraryManagerItem {

    private final boolean fullName;
    private final File file;
    private final LibraryData libraryData;
    private final LibraryManagerItem parent;
    private final List<File> excludedChildren;
    private final boolean showExcludedChildren;
    
    private List<LibraryManagerItem> children;
    
    public LibraryManagerItemImpl(LibraryManagerItem parent, 
            LibraryData libraryData,
            File file,
            boolean fullName,
            boolean showExcludedChildren) {
        this.showExcludedChildren = showExcludedChildren;
        this.parent = parent;
        this.libraryData = libraryData;
        this.file = canonicalize(file);
        this.fullName = fullName;
        this.excludedChildren = new ArrayList<File>();
    }
    
    @Override
    public String toString() {
        return file.toString() + " [excluded: " + excludedChildren + "]";
    }
    
    @Override
    public LibraryManagerItem getParent() {
        return parent;
    }
    
    private File canonicalize(File file) {
        try {
            return FileUtils.getCanonicalFile(file);
        } catch(IOException iox) {
            return file;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        } else if(obj instanceof LibraryManagerItem) {
            LibraryManagerItem o = (LibraryManagerItem)obj;
            return o.getFile().equals(file);
        } else {
            return false;
        }
    }
    
    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public String displayName() {
        if(fullName) {
            return file.getPath();
        } else if(file.getName() == null || file.getName().length() == 0) {
            return file.getPath();
        } else {
            return file.getName();
        }
    }

    @Override
    public List<LibraryManagerItem> getChildren() {
        if(children == null) {
            children = new ArrayList<LibraryManagerItem>();
            File[] folders = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return libraryData.isDirectoryAllowed(file);
                }
            });
            
            if(folders != null) {
                for(File folder : folders) {
                    if(!showExcludedChildren && libraryData.isDirectoryExcluded(folder)) {
                        excludedChildren.add(folder);
                    } else {
                        children.add(new LibraryManagerItemImpl(this, libraryData, folder, false, showExcludedChildren));
                    }
                }
            }
        }
        
        return children;
    }
    
    public int addChild(LibraryManagerItem item) {
        excludedChildren.remove(item.getFile());
        children.add(item);
        assert item.getParent() == this;
        return children.size() - 1;
    }

    public int removeChild(LibraryManagerItem item) {
        excludedChildren.add(item.getFile());
        int idx = children.indexOf(item);
        assert idx != -1;
        children.remove(idx);
        return idx;
    }    
    
    @Override
    public Collection<? extends File> getExcludedChildren() {
        return Collections.unmodifiableList(excludedChildren);
    }
    
    @Override
    public LibraryManagerItem getChildFor(File directory) {
        for(LibraryManagerItem child : children) {
            if(child.getFile().equals(directory)) {
                return child;
            }
        }
        return null;
    }    
}

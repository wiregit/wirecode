package org.limewire.ui.swing.util;

import java.io.File;

import javax.swing.Icon;

import org.limewire.util.FileUtils;


/**
 * A FileIconController that uses default icons.
 */
public class BasicFileIconController implements FileIconController {
    
    /** Returns the icon associated with the extension of the file. */
    public Icon getIconForFile(File f) {
        if(f == null)
            return null;
        
        String extension = FileUtils.getFileExtension(f);
        if(!extension.isEmpty())
            return getIconForExtension(extension);
        else
            return null;
    }
    
    /** Returns the icon assocated with the extension. */
    public Icon getIconForExtension(String ext) {
        return null; // TODO
    }

    /** Icons are always available immediately. */
    public boolean isIconForFileAvailable(File f) {
        return true;
    }
    
    /** This basic controller is always valid. */
    public boolean isValid() {
        return true;
    }
}

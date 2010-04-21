package org.limewire.ui.swing.util;

import java.io.File;

import javax.swing.Icon;

public interface FileIconController {
    /**
     * Retrieves the icon for a given file.
     * <p>
     * This call may take a short while to complete if disk access
     * is required.
     */
    public Icon getIconForFile(File f);
    
    /**
     * Retrieves the icon for a given extension.
     * <p>
     * This call may take a short while to complete if disk access
     * is required.
     */
    public Icon getIconForExtension(String ext);
    
    /**
     * Returns true if the controller thinks it can return the icon
     * for the given file without any waiting.
     */
    public boolean isIconForFileAvailable(File f);
    
    /**
     * Determines if this FileIconController is valid.
     */
    public boolean isValid();
}
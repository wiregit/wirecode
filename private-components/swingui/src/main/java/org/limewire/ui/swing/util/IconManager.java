package org.limewire.ui.swing.util;

import java.io.File;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Manages finding native icons for files and file types.
 */
@Singleton
public class IconManager {
    
    /** The current FileIconController. */
    private FileIconController fileController;
    
    @Inject
    IconManager() {
        // Always begin with the basic controller,
        // whose contruction can never block.
        fileController = new BasicFileIconController();
        
        // Then, in a new thread, try to change it to a controller
        // that can block.
        if (OSUtils.isMacOSX() || OSUtils.isWindows()) {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    final FileIconController newController =
                        new NativeFileIconController();
                    if (newController.isValid()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                fileController = newController;
                            }
                        });
                    }
                }
            }, "NativeFileIconLoader");
        }
    }
    
    /**
     * Returns the icon associated with this file.
     * If the file does not exist, or no icon can be found, returns
     * the icon associated with the extension.
     */
    public Icon getIconForFile(File f) {
        validate();
        return fileController.getIconForFile(f);
    }
    
    /**
     * Returns the icon assocated with the extension.
     * TODO: Implement better.
     */
    public Icon getIconForExtension(String ext) {
        validate();
        return fileController.getIconForExtension(ext);
    }
    
    /** Returns true if the icon can be returned immediately. */
    public boolean isIconForFileAvailable(File f) {
        validate();
        return fileController.isIconForFileAvailable(f);
    }
    
    /**
     * Reverts the IconController to a basic controller if at any point
     * in time the controller becomes invalid.
     * 
     * Returns true if the current controller is already valid.
     */
    private void validate() {
        if (!fileController.isValid())
            fileController = new BasicFileIconController();
    }
}
package org.limewire.ui.swing.util;

import java.awt.Component;
import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

/**
 * This is a utility class that displays a file chooser dialog to the user. 
 */
public final class FileChooser {
    
    private FileChooser() {}
    
    /**
     * Returns the last directory that was used in a FileChooser.
     * 
     * @return
     */
    public static File getLastInputDirectory() {
        File dir = ApplicationSettings.LAST_FILECHOOSER_DIRECTORY.getValue();
        if(dir == null || dir.getPath().equals("") || !dir.exists() || !dir.isDirectory())
            return CommonUtils.getCurrentDirectory();
        else 
            return dir;
    }
   
    public static File getInputDirectory(Component parent) {
        return getInputDirectory(parent, 
                I18nMarker.marktr("Select Folder"), 
                I18nMarker.marktr("Select"),
                null,
                null);
    }
    
    public static File getInputDirectory(Component parent, FileFilter filter) {
        return getInputDirectory(parent, 
                I18nMarker.marktr("Select Folder"), 
                I18nMarker.marktr("Select"),
                null,
                filter);
    }
    

    /**
     * Same as <tt>getInputFile</tt> that takes no arguments, except this
     * allows the caller to specify the parent component of the chooser as well
     * as other options.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param directory the directory to open the dialog to
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static File getInputDirectory(Component parent, 
                                         File directory) {
        return getInputDirectory(parent, 
                                 I18nMarker.marktr("Select Folder"), 
                                 I18nMarker.marktr("Select"),
                                 directory);
    }
    
    /**
     * Same as <tt>getInputFile</tt> that takes no arguments, except this
     * allows the caller to specify the parent component of the chooser as well
     * as other options.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param approveKey the key for the locale-specific string to use for the
     *        approve button text
     * @param directory the directory to open the dialog to
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static File getInputDirectory(Component parent, String titleKey,
                                         String approveKey, File directory) {
        return getInputDirectory(parent, 
                                 titleKey, 
                                 approveKey,
                                 directory,
                                 null);
    }
    
    /**
     * Same as <tt>getInputFile</tt> that takes no arguments, except this
     * allows the caller to specify the parent component of the chooser as well
     * as other options.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param approveKey the key for the locale-specific string to use for the
     *        approve button text
     * @param directory the directory to open the dialog to
     * @param filter the <tt>FileFilter</tt> instance for customizing the
     *        files that are displayed -- if this is null, no filter is used
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static File getInputDirectory(Component parent, String titleKey,
            String approveKey, File directory, FileFilter filter) {
        
        List<File> dirs = getInput(parent, titleKey, approveKey, directory,
                JFileChooser.DIRECTORIES_ONLY, JFileChooser.APPROVE_OPTION,
                false, filter);
        
        assert (dirs == null || dirs.size() <= 1) 
            : "selected more than one folder: " + dirs;
            
        if (dirs != null && dirs.size() == 1) {
            return dirs.get(0);
        } else {
            return null;
        }
    }
    
    /**
     * The implementation that the other methods delegate to. This provides the
     * caller with all available options for customizing the
     * <tt>JFileChooser</tt> instance. If a <tt>FileDialog</tt> is displayed
     * instead of a <tt>JFileChooser</tt> (on OS X, for example), most or all
     * of these options have no effect.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param approveKey the key for the locale-specific string to use for the
     *        approve button text
     * @param directory the directory to open the dialog to
     * @param mode the "mode" to open the <tt>JFileChooser</tt> in from the
     *        <tt>JFileChooser</tt> class, such as
     *        <tt>JFileChooser.DIRECTORIES_ONLY</tt>
     * @param option the option to look for in the return code, such as
     *        <tt>JFileChooser.APPROVE_OPTION</tt>
     * @param allowMultiSelect true if the chooser allows multiple files to be
     *        chosen
     * @param filter the <tt>FileFilter</tt> instance for customizing the
     *        files that are displayed -- if this is null, no filter is used
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static List<File> getInput(Component parent, String titleKey,
                                String approveKey,
                                File directory,
                                int mode,
                                int option,
                                boolean allowMultiSelect,
                                final FileFilter filter) {
            if(!OSUtils.isAnyMac()) {
                JFileChooser fileChooser = getDirectoryChooser(titleKey, approveKey, directory, mode, filter);
                fileChooser.setMultiSelectionEnabled(allowMultiSelect);
                boolean dispose = false;
                if(parent == null) {
	                dispose = true;
                    parent = FocusJOptionPane.createFocusComponent();
                }
                try {
                    if(fileChooser.showOpenDialog(parent) != option)
                        return null;
                } catch(NullPointerException npe) {
                    // ignore NPE.  can't do anything with it ...
                    return null;
                } finally {
                    if(dispose)
                        ((JFrame)parent).dispose();
                }
                
                if(allowMultiSelect) {
                    File[] chosen = fileChooser.getSelectedFiles();
                    if(chosen.length > 0)
                        setLastInputDirectory(chosen[0]);
                    return Arrays.asList(chosen);
                } else {
                    File chosen = fileChooser.getSelectedFile();
                    setLastInputDirectory(chosen);
                    return Collections.singletonList(chosen);
                }
                
            } else {
                FileDialog dialog;
                if(mode == JFileChooser.DIRECTORIES_ONLY) {
                    dialog = MacUtils.getFolderDialog(null);
                }
                else
                    dialog = new FileDialog(new LimeJFrame(), "");
                
                dialog.setTitle(I18n.tr(titleKey));
                if(filter != null) {
                    FilenameFilter f = new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return filter.accept(new File(dir, name));
                        }
                    };
                    dialog.setFilenameFilter(f);
                }
                
                dialog.setVisible(true);
                String dirStr = dialog.getDirectory();
                String fileStr = dialog.getFile();
                if((dirStr==null) || (fileStr==null))
                    return null;
                setLastInputDirectory(new File(dirStr));
                // if the filter didn't work, pretend that the person picked
                // nothing
                File f = new File(dirStr, fileStr);
                if(filter != null && !filter.accept(f))
                    return null;
                
                return Collections.singletonList(f);
            }       
    }
    
    /** Sets the last directory that was used for the FileChooser. */
    private static void setLastInputDirectory(File file) {
        if(file != null) {
            if(!file.exists() || !file.isDirectory())
                file = file.getParentFile();
            if(file != null) {
                if(file.exists() && file.isDirectory())
                    ApplicationSettings.LAST_FILECHOOSER_DIRECTORY.setValue(file);
            }
        }
    }
    
    /**
     * Returns a new <tt>JFileChooser</tt> instance for selecting directories
     * and with internationalized strings for the caption and the selection
     * button.
     * 
     * @param approveKey can be <code>null</code>
     * @param directory can be <code>null</code>
     * @param filter can be <code>null</code>
     * @return a new <tt>JFileChooser</tt> instance for selecting directories.
     */
    private static JFileChooser getDirectoryChooser(String titleKey,
            String approveKey, File directory, int mode, FileFilter filter) {
        JFileChooser chooser = null;
        if (directory == null)
            directory = getLastInputDirectory();
        

        if(directory == null) {
            chooser = new JFileChooser();
        } else {
            try {
                chooser = new JFileChooser(directory);
            } catch (NullPointerException e) {
                // Workaround for JRE bug 4711700. A NullPointer is thrown
                // sometimes on the first construction under XP look and feel,
                // but construction succeeds on successive attempts.
                try {
                    chooser = new JFileChooser(directory);
                } catch (NullPointerException npe) {
                    // ok, now we use the metal file chooser, takes a long time to load
                    // but the user can still use the program
                    UIManager.getDefaults().put("FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI");
                    chooser = new JFileChooser(directory);
                }
            } catch (ArrayIndexOutOfBoundsException ie) {
                // workaround for Windows XP, not sure if second try succeeds
                // then
                chooser = new JFileChooser(directory);
            }
        }
        if (filter != null) {
            chooser.setFileFilter(filter);
        } else {
            if (mode == JFileChooser.DIRECTORIES_ONLY) {
                chooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return true;
                    }
                    @Override
                    public String getDescription() {
                        return I18n.tr("All Folders");
                    }
                });
            }
        }
        chooser.setFileSelectionMode(mode);
        String title = I18n.tr(titleKey);
        chooser.setDialogTitle(title);

        if (approveKey != null) {
            String approveButtonText = I18n.tr(approveKey);
            chooser.setApproveButtonText(approveButtonText);
        }
        return chooser;
    }    

    /**
     * Opens a dialog asking the user to choose a file which is used for
     * saving to.
     * 
     * @param parent the parent component the dialog is centered on
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param suggestedFile the suggested file for saving
     * @return the file or <code>null</code> when the user cancelled the
     *         dialog
     */
    public static File getSaveAsFile(Component parent, String titleKey, File suggestedFile) {
        return getSaveAsFile(parent, titleKey, suggestedFile, null);
    }
    
    /**
     * Opens a dialog asking the user to choose a file which is used for
     * saving to.
     * 
     * @param parent the parent component the dialog is centered on
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param suggestedFile the suggested file for saving
     * @param the filter to use for what's shown.
     * @return the file or <code>null</code> when the user cancelled the
     *         dialog
     */
    public static File getSaveAsFile(Component parent, String titleKey,
                                     File suggestedFile, final FileFilter filter) {
        if (OSUtils.isAnyMac()) {
            FileDialog dialog = new FileDialog(GuiUtils.getMainFrame(),
                                               I18n.tr(titleKey),
                                               FileDialog.SAVE);
            dialog.setDirectory(suggestedFile.getParent());
            dialog.setFile(suggestedFile.getName()); 
            if (filter != null) {
                FilenameFilter f = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return filter.accept(new File(dir, name));
                    }
                };
                dialog.setFilenameFilter(f);
            }

            dialog.setVisible(true);
            String dir = dialog.getDirectory();
            setLastInputDirectory(new File(dir));
            String file = dialog.getFile();
            if ((dir != null) && (file != null)) {
                File f = new File(dir, file);
                if ((filter != null) && !filter.accept(f)) {
                    return null;
                } else {
                    return f;
                }
            } else {
                return null;
            }
            
        } else {
            JFileChooser chooser = getDirectoryChooser(titleKey, null, null, 
                    JFileChooser.FILES_ONLY, filter);
            chooser.setSelectedFile(suggestedFile);
            int ret = chooser.showSaveDialog(parent);
            File file = chooser.getSelectedFile();
            setLastInputDirectory(file);
            return (ret != JFileChooser.APPROVE_OPTION) ? null : file;
        }
    }
}

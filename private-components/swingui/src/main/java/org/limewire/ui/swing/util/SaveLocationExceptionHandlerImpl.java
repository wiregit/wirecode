package org.limewire.ui.swing.util;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;

/**
 * A universal handler for SaveLocationException messages generated while
 * performing downloads.
 */
public class SaveLocationExceptionHandlerImpl implements SaveLocationExceptionHandler {
    private static final Log LOG = LogFactory.getLog(SaveLocationExceptionHandlerImpl.class);

    private final SaveLocationManager saveLocationManager;

    /**
     * Constructs a SaveLocationExceptionHandler with the specified
     * SaveLocationManager.
     */
    @Inject
    public SaveLocationExceptionHandlerImpl(SaveLocationManager saveLocationManager) {
        this.saveLocationManager = saveLocationManager;
    }

    /**
     * Handles the supplied SaveLocationException. The method may take one of
     * several actions: eat the exception, try downloading again using the
     * supplied <code>downloadAction</code>, or popup a dialog to try and save
     * the download in a new location.
     * 
     * @param supportNewSaveFileName - Indicates that the downloader supports a
     *        new saveFileName. If true the user will have the option of picking
     *        a new file name, provided they do not have the setting to
     *        automatically rename the file turned on. Otherwise if it does not
     *        support a new file name, a directory chooser is opened that will
     *        allow the
     */
    public void handleSaveLocationException(final DownloadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveFileName) {

        // Create Runnable to execute task on UI thread. This is necessary
        // if the handler method has been invoked from a background thread.
        SwingUtils.invokeLater(new Runnable() {
            public void run() {
                handleException(downLoadAction, sle, supportNewSaveFileName);
            }
        });
    }

    /**
     * Handles the specified SaveLocationException. The method may prompt the
     * user for input, and should be executed from the UI thread.
     */
    private void handleException(final DownloadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveFileName) {

        if (sle.getErrorCode() == SaveLocationException.LocationCode.FILE_ALREADY_DOWNLOADING) {
            // ignore, just return because we are already downloading this file
            downLoadAction.downloadCanceled(sle);
            showErrorMessage(sle);
            return;
        }

        // check to make sure this is a SaveLocationException we can handle
        if ((sle.getErrorCode() != SaveLocationException.LocationCode.FILE_ALREADY_EXISTS)
                && (sle.getErrorCode() != SaveLocationException.LocationCode.FILE_IS_ALREADY_DOWNLOADED_TO)) {
            // Create user message.
            downLoadAction.downloadCanceled(sle);
            showErrorMessage(sle);
            return;
        }

        // select a save file name
        File saveFile = null;
        if (supportNewSaveFileName && SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue()) {
            saveFile = getAutoSaveFile(sle);
        } else {
            if (supportNewSaveFileName) {
                saveFile = FileChooser.getSaveAsFile(GuiUtils.getMainFrame(), I18n
                        .tr("Save File As..."), sle.getFile());
            } else {
                saveFile = sle.getFile();
                if (saveFile != null && saveFile.exists()) {
                    createOverwriteDialogue(saveFile, downLoadAction, sle, supportNewSaveFileName);
                    return;
                }
            }

            if (saveFile == null) {
                // null saveFile means user selected cancel
                downLoadAction.downloadCanceled(sle);
                return;
            }
        }

        // if the file already exists at this point, the user has already agreed
        // to overwrite it
        download(downLoadAction, supportNewSaveFileName, saveFile, saveFile.exists());
    }

    private void showErrorMessage(final SaveLocationException sle) {
        String message = null;

        switch (sle.getErrorCode()) {
        case FILE_ALREADY_DOWNLOADING:
            message = I18n.tr("Sorry, this file is already being downloaded.");
            break;
        case DIRECTORY_NOT_WRITEABLE:
        case DIRECTORY_DOES_NOT_EXIST:
        case NOT_A_DIRECTORY:
        case PATH_NAME_TOO_LONG:
            message = I18n.tr("Sorry, you can't download files to this location.");
            break;
        case NO_TORRENT_MANAGER:
            message = I18n.tr("Sorry, there was a problem loading the BitTorrent library.\n" +
                    "BitTorrent features cannot be used.");
            break;
        default:
            message = I18n.tr("Sorry, there was a problem downloading your file.");
            break;
        }

        // Log exception and display user message.
        LOG.error(message, sle);
        FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), message, I18n.tr("Download"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Iterates through possible file names until an available one is found and
     * is returned.
     */
    private File getAutoSaveFile(final SaveLocationException sle) {
        File saveFile;
        saveFile = sle.getFile();
        int index = 1;
        String fileName = FileUtils.getFilenameNoExtension(saveFile.getName());
        String extension = FileUtils.getFileExtension(saveFile);
        while (saveFile.exists() || saveLocationManager.isSaveLocationTaken(saveFile)) {
            String newFileName = fileName + "(" + index + ")";
            if (extension.length() > 0) {
                newFileName += "." + extension;
            }
            saveFile = new File(saveFile.getParentFile(), newFileName);
            index++;
        }
        return saveFile;
    }

    /**
     * Downloads the given file using the supplied download action. And handles
     * any possible SaveLocationExceptions.
     */
    private void download(final DownloadAction downLoadAction, final boolean supportNewSaveDir,
            File saveFile, boolean overwrite) {
        try {
            downLoadAction.download(saveFile, overwrite);
        } catch (SaveLocationException e1) {
            handleSaveLocationException(downLoadAction, e1, supportNewSaveDir);
        }
    }

    private void createOverwriteDialogue(final File saveFile, final DownloadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveFileName) {

        final JDialog dialog = new LimeJDialog(GuiUtils.getMainFrame());
        dialog.setModalityType(ModalityType.APPLICATION_MODAL);

        final MultiLineLabel message = new MultiLineLabel(I18n
                .tr("File already exists. What do you want to do?"), 400);

        final JTextField filePathField = new JTextField(25);
        filePathField.setEnabled(false);
        filePathField.setText(saveFile.getAbsolutePath());

        JToggleButton overwriteButton = null;
        overwriteButton = new JToggleButton(I18n.tr("Overwrite"));
        overwriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
                download(downLoadAction, supportNewSaveFileName, saveFile, true);
            }
        });

        JButton cancelButton = new JButton(I18n.tr("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                File saveFile = sle.getFile();
                File oldSaveFile = saveFile;
                File oldSaveFileParent = saveFile != null && saveFile.getParentFile() != null ? saveFile
                        .getParentFile()
                        : saveFile;
                saveFile = FileChooser
                        .getInputDirectory(GuiUtils.getMainFrame(), I18n.tr("Choose a new directory to save download."), I18n.tr("Select"), oldSaveFileParent);
                
                File newSaveParent = saveFile;
                if (newSaveParent != null && new File(newSaveParent, oldSaveFile.getName()).exists()) {
                    createOverwriteDialogue(newSaveParent, downLoadAction, sle, supportNewSaveFileName);
                    return;
                }
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("hidemode 3, gapy 10", "", ""));
        panel.add(message, "span 2, wrap");
        panel.add(filePathField, "grow x, push, wrap");
        panel.add(overwriteButton, "alignx right");
        panel.add(cancelButton);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setVisible(true);
    }
}

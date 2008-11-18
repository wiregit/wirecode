package org.limewire.ui.swing.util;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.ui.swing.components.MultiLineLabel;

public class SaveLocationExceptionHandlerImpl implements SaveLocationExceptionHandler {

    public void handleSaveLocationException(final DownLoadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveDir,
            final Component component) {

        if (sle.getErrorCode() == SaveLocationException.LocationCode.FILE_ALREADY_DOWNLOADING) {
            // ignore, just return
            return;
        }

        // TODO depending on append to file name setting, don't show the
        // dialogue, instead append a number to the end of the name.
        if (sle.getErrorCode() != SaveLocationException.LocationCode.FILE_ALREADY_EXISTS
                && sle.getErrorCode() != SaveLocationException.LocationCode.FILE_IS_ALREADY_DOWNLOADED_TO) {
            // TODO better user feedback
            throw new UnsupportedOperationException("Error starting download.", sle);
        }

        File saveFile = null;
        if (supportNewSaveDir) {
            saveFile = FileChooser.getSaveAsFile(component, I18n.tr("Save File As..."), sle
                    .getFile());
        } else {
            saveFile = sle.getFile();
        }

        if (saveFile == null) {
            return;
        }

        if (saveFile.exists()) {
            createOverwriteDialogue(saveFile, downLoadAction, sle, supportNewSaveDir, component);
        } else {
            download(downLoadAction, supportNewSaveDir, component, saveFile, false);
        }
    }

    private void download(final DownLoadAction downLoadAction, final boolean supportNewSaveDir,
            final Component component, File saveFile, boolean overwrite) {
        try {
            downLoadAction.download(saveFile, overwrite);
        } catch (SaveLocationException e1) {
            handleSaveLocationException(downLoadAction, e1, supportNewSaveDir, component);
        }
    }

    private void createOverwriteDialogue(final File saveFile, final DownLoadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveDir,
            final Component component) {

        final JDialog dialog = new JDialog();
        dialog.setLocationRelativeTo(component);
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
                download(downLoadAction, supportNewSaveDir, component, saveFile, true);
            }
        });

        JButton cancelButton = new JButton(I18n.tr("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                if(supportNewSaveDir) {
                    handleSaveLocationException(downLoadAction, sle, supportNewSaveDir, component);
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
        dialog.setVisible(true);
    }
}

package org.limewire.ui.swing.components;

import java.awt.Dimension;
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
import org.limewire.core.api.download.SaveLocationException.LocationCode;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;

public class SaveAsDialogue extends JDialog {

    private JButton saveButton = null;

    private JToggleButton overwriteButton = null;

    private File saveFile = null;

    public SaveAsDialogue(final File badFile, final SaveLocationException.LocationCode locationCode) {
        super();
        setModalityType(ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel();
        //TODO make looks pretty.
        panel.setLayout(new MigLayout("hidemode 2", "[][]", "[][][]"));
        panel.setPreferredSize(new Dimension(500, 500));

        final MultiLineLabel message = new MultiLineLabel(I18n.tr(getMessage(locationCode), badFile
                .getName()), 500);

        final JTextField filePathField = new JTextField(50);
        filePathField.setEnabled(false);
        filePathField.setText(badFile.getAbsolutePath());

        JButton browseButton = new JButton(I18n.tr("Browse"));
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile = FileChooser.getSaveAsFile(SaveAsDialogue.this, I18n
                        .tr("Save File As..."), badFile);
                if (saveFile != null && !saveFile.equals(badFile) && !saveFile.exists()) {
                    message.setText(I18n.tr("Save File?..."));
                    saveButton.setVisible(true);
                    overwriteButton.setVisible(false);
                } else {
                    message.setText(getMessage(locationCode));
                    overwriteButton.setVisible(true);
                    saveButton.setVisible(false);
                }
                
                if(saveFile != null) {
                    filePathField.setText(saveFile.getAbsolutePath());
                }
            }
        });

        saveButton = new JButton(I18n.tr("Save"));
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                SaveAsDialogue.this.dispose();
            }
        });
        saveButton.setVisible(false);

        overwriteButton = new JToggleButton(I18n.tr("Overwrite"));
        overwriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                SaveAsDialogue.this.dispose();
            }
        });

        JButton cancelButton = new JButton(I18n.tr("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SaveAsDialogue.this.dispose();
            }
        });
        panel.add(message, "span 2, wrap");
        panel.add(filePathField);
        panel.add(browseButton, "wrap");
        panel.add(saveButton);
        panel.add(overwriteButton);
        panel.add(cancelButton);
        setContentPane(panel);
        pack();

    }

    public void addActionListener(ActionListener actionListener) {
        saveButton.addActionListener(actionListener);
        overwriteButton.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        saveButton.removeActionListener(actionListener);
        overwriteButton.removeActionListener(actionListener);
    }

    private String getMessage(LocationCode locationCode) {
        return "A file with this name already exists at this location. You can overwrite or save the download as whatever...";
    }

    public File getSaveFile() {
        return saveFile;
    }

    public boolean isOverwrite() {
        return overwriteButton.isSelected();
    }
}

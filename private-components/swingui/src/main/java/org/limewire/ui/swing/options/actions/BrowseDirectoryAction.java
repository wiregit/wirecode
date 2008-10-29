package org.limewire.ui.swing.options.actions;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import javax.swing.Action;
import javax.swing.JTextField;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

public class BrowseDirectoryAction extends AbstractAction {

    private Container parent;

    private JTextField currentDirectoryTextField;

    public BrowseDirectoryAction(Container parent, JTextField currentDirectoryTextField) {
        this.parent = parent;
        this.currentDirectoryTextField = currentDirectoryTextField;

        putValue(Action.NAME, I18n.tr("Browse..."));
        putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a different Save Location"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String oldDirectory = currentDirectoryTextField.getText();
        File directory = FileChooser.getInputDirectory(parent, new File(oldDirectory));

        if (directory == null)
            return;

        if (isSaveDirectoryValid(directory)) {
            try {
                String newDirectory = directory.getCanonicalPath();
                currentDirectoryTextField.setText(newDirectory);
            } catch (IOException ioe) {
                //TODO implement better user feedback
                throw new UnsupportedOperationException("Error using directory: " + directory + " as save dir.", ioe);
            }
        } else {
            throw new UnsupportedOperationException("Error using directory: " + directory + " as save dir.");
            //TODO implement better user feedback
        }
    }

    /**
     * Utility method for checking whether or not the save directory is valid.
     * 
     * @param saveDir the save directory to check for validity
     * @return <tt>true</tt> if the save directory is valid, otherwise
     *         <tt>false</tt>
     */
    private static boolean isSaveDirectoryValid(File saveDir) {
        if (saveDir == null || saveDir.isFile())
            return false;

        if (!saveDir.exists())
            saveDir.mkdirs();

        if (!saveDir.isDirectory())
            return false;

        FileUtils.setWriteable(saveDir);

        Random generator = new Random();
        File testFile = null;
        for (int i = 0; i < 10 && testFile == null; i++) {
            StringBuilder name = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                name.append((char) ('a' + generator.nextInt('z' - 'a')));
            }
            name.append(".tmp");

            testFile = new File(saveDir, name.toString());
            if (testFile.exists()) {
                testFile = null; // try again!
            }
        }

        if (testFile == null) {
            return false;
        }

        RandomAccessFile testRAFile = null;
        try {
            testRAFile = new RandomAccessFile(testFile, "rw");

            // Try to write something just to make extra sure we're OK.
            testRAFile.write(7);
            testRAFile.close();
        } catch (FileNotFoundException e) {
            // If we could not open the file, then we can't write to that
            // directory.
            return false;
        } catch (IOException e) {
            // The directory is invalid if there was an error writing to it.
            return false;
        } finally {
            // Delete our test file.
            testFile.delete();
            try {
                if (testRAFile != null)
                    testRAFile.close();
            } catch (IOException ignored) {
            }
        }

        return saveDir.canWrite();
    }
}

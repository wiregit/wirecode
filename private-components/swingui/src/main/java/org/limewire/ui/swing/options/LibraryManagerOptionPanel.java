package org.limewire.ui.swing.options;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.library.manager.LibraryManagerItem;
import org.limewire.ui.swing.library.manager.LibraryManagerItemImpl;
import org.limewire.ui.swing.library.manager.LibraryManagerModel;
import org.limewire.ui.swing.library.manager.LibraryManagerTreeTable;
import org.limewire.ui.swing.library.manager.RootLibraryManagerItem;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;

public class LibraryManagerOptionPanel extends OptionPanel {

    private final LibraryData libraryData;
    
    private JButton addFolderButton;
    private JCheckBox musicCheckBox;
    private JCheckBox videoCheckBox;
    private JCheckBox imageCheckBox;
    private JCheckBox docCheckBox;
    
    private LibraryManagerTreeTable treeTable;
    
    private JButton okButton;
    private JButton cancelButton;
    
    public LibraryManagerOptionPanel(Action okAction, CancelDialogAction cancelAction, LibraryData libraryData) {
        this.libraryData = libraryData;
        
        setLayout(new MigLayout("", "[300!][grow]", ""));
        
        createComponents(okAction, cancelAction);
        cancelAction.setOptionPanel(this);
        
        add(new JLabel(I18n.tr("LimeWire will automatically scan the folders below and place files in your library.")), "span 2, wrap");
    
        add(new JScrollPane(treeTable), "span 2, growx, wrap");
        add(addFolderButton, "skip 1, alignx right, wrap");
        
        add(new JLabel(I18n.tr("Add the following types of files to your Library")), "span 2, wrap");
        add(getCheckBoxPanel(), "gapbottom 20, span 2, wrap");
        
        add(new MultiLineLabel(I18n.tr("Scanning these folders into your Library will not automatically share your files"), 300), "growx");
        add(okButton, "alignx right, split");
        add(cancelButton);
    }
    
    private void createComponents(Action okAction, Action cancelAction) {
        treeTable = new LibraryManagerTreeTable();
        addFolderButton = new JButton(new AddDirectoryAction(this));
        
        musicCheckBox = new JCheckBox();
        videoCheckBox = new JCheckBox();
        imageCheckBox = new JCheckBox();
        docCheckBox = new JCheckBox();
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
    }
    
    private JPanel getCheckBoxPanel() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout());
        
        p.add(musicCheckBox, "gapleft 25");
        p.add(new JLabel(I18n.tr("Music")), "gapright 18");
        
        p.add(videoCheckBox);
        p.add(new JLabel(I18n.tr("Videos")), "gapright 18");
        
        p.add(imageCheckBox);
        p.add(new JLabel(I18n.tr("Images")), "gapright 18");
        
        p.add(docCheckBox);
        p.add(new JLabel(I18n.tr("Documents")), "gapright 18");
        
        return p;
    }

    @Override
    void applyOptions() {
        // TODO Auto-generated method stub
        
    }

    @Override
    boolean hasChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void initOptions() {
        ArrayList<LibraryManagerItem> items = new ArrayList<LibraryManagerItem>();
        for(File file : libraryData.getDirectoriesToManageRecursively()) {
            items.add(new LibraryManagerItemImpl(libraryData, file, true));
        }

        treeTable.setTreeTableModel(new LibraryManagerModel(new RootLibraryManagerItem(items)));
    }
    
    private class AddDirectoryAction extends AbstractAction {

        private Container parent;
        
        public AddDirectoryAction(Container parent) {
            this.parent = parent;
            
            putValue(Action.NAME, I18n.tr("Add New Folder"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a folder to automatically scan for changes"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            File directory = FileChooser.getInputDirectory(parent, null);
            
            if(directory == null)
                return;
            
//            try {
//                String newDirectory = directory.getCanonicalPath();
//                currentDirectoryTextField.setText(newDirectory);
//            }catch(IOException ioe) {}
        }
    }    
}

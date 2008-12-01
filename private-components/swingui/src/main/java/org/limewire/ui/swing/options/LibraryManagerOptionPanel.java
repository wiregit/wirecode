package org.limewire.ui.swing.options;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
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
    private JCheckBox audioCheckBox;
    private JCheckBox videoCheckBox;
    private JCheckBox imageCheckBox;
    private JCheckBox docCheckBox;
    private JCheckBox programCheckBox;
    private JCheckBox otherCheckBox;
    
    private LibraryManagerTreeTable treeTable;
    
    private JButton okButton;
    private JButton cancelButton;
    
    public LibraryManagerOptionPanel(Action okAction, CancelDialogAction cancelAction, LibraryData libraryData) {
        this.libraryData = libraryData;
        
        setLayout(new MigLayout("", "[300!][grow]", ""));
        
        createComponents(okAction, cancelAction);
        cancelAction.setOptionPanel(this);
        
        add(new JLabel(I18n.tr("LimeWire will automatically scan the folders below and place files in your Library.")), "span 2, wrap");
    
        add(new JScrollPane(treeTable), "span 2, growx, wrap");
        add(addFolderButton, "skip 1, alignx right, wrap");
        
        add(new JLabel(I18n.tr("Add the following types of files to your Library:")), "span 2, wrap");
        add(getCheckBoxPanel(), "gapbottom 20, span 2, wrap");
        
        add(new MultiLineLabel(I18n.tr("Scanning these folders into your Library will not automatically share your files."), 300), "growx");
        add(okButton, "alignx right, split");
        add(cancelButton);
    }
    
    private void createComponents(Action okAction, Action cancelAction) {
        treeTable = new LibraryManagerTreeTable(libraryData);
        addFolderButton = new JButton(new AddDirectoryAction(this));
        
        audioCheckBox = new JCheckBox(I18n.tr(Category.AUDIO.toString()));
        audioCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        videoCheckBox = new JCheckBox(I18n.tr(Category.VIDEO.toString()));
        videoCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        imageCheckBox = new JCheckBox(I18n.tr(Category.IMAGE.toString()));
        imageCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        docCheckBox = new JCheckBox(I18n.tr(Category.DOCUMENT.toString()));
        docCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        programCheckBox = new JCheckBox(I18n.tr(Category.PROGRAM.toString()));
        programCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        otherCheckBox = new JCheckBox(I18n.tr(Category.OTHER.toString()));
        otherCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
    }
    
    private JPanel getCheckBoxPanel() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout());
        
        p.add(audioCheckBox, "gapleft 25, gapright 18");        
        p.add(videoCheckBox, "gapright 18");        
        p.add(imageCheckBox, "wrap");        
        p.add(docCheckBox, "gapleft 25, gapright 18");
        p.add(programCheckBox, "gapright 18");
        p.add(otherCheckBox);
        
        return p;
    }

    @Override
    public void applyOptions() {
        LibraryManagerModel model = treeTable.getLibraryModel();
        Collection<File> manage = model.getManagedDirectories();
        Collection<File> exclude = model.getExcludedDirectories();
        libraryData.setManagedOptions(manage, exclude, getManagedCategories());
    }

    private Collection<Category> getManagedCategories() {
        Collection<Category> categories = EnumSet.noneOf(Category.class);
        if(audioCheckBox.isSelected()) {
            categories.add(Category.AUDIO);
        }
        if(videoCheckBox.isSelected()) {
            categories.add(Category.VIDEO);
        }
        if(imageCheckBox.isSelected()) {
            categories.add(Category.IMAGE);
        }
        if(docCheckBox.isSelected()) {
            categories.add(Category.DOCUMENT);
        }
        if(programCheckBox.isSelected()) {
            categories.add(Category.PROGRAM);
        }
        if(otherCheckBox.isSelected()) {
            categories.add(Category.OTHER);
        }
        return categories;
    }

    @Override
    boolean hasChanged() {
        LibraryManagerModel model = treeTable.getLibraryModel();
        Collection<File> manage = model.getManagedDirectories();
        Collection<File> exclude = model.getExcludedDirectories();
        Collection<Category> categories = getManagedCategories();
        return !manage.equals(libraryData.getDirectoriesToManageRecursively())
            || !exclude.equals(libraryData.getDirectoriesToExcludeFromManaging())
            || !categories.equals(libraryData.getManagedCategories());
    }

    @Override
    public void initOptions() {
        RootLibraryManagerItem root = new RootLibraryManagerItem();
        for(File file : libraryData.getDirectoriesToManageRecursively()) {
            root.addChild(new LibraryManagerItemImpl(root, libraryData, file, true, true));
        }

        treeTable.setTreeTableModel(new LibraryManagerModel(root));
        
        Collection<Category> categories = libraryData.getManagedCategories();
        audioCheckBox.setSelected(categories.contains(Category.AUDIO));
        videoCheckBox.setSelected(categories.contains(Category.VIDEO));
        docCheckBox.setSelected(categories.contains(Category.DOCUMENT));
        imageCheckBox.setSelected(categories.contains(Category.IMAGE));
        programCheckBox.setSelected(categories.contains(Category.PROGRAM));
        otherCheckBox.setSelected(categories.contains(Category.OTHER));
        
        programCheckBox.setEnabled(libraryData.isProgramManagingAllowed());
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
            File folder = FileChooser.getInputDirectory(parent);            
            if(folder == null) {
                return;
            } else {
                if(libraryData.isDirectoryAllowed(folder)) {
                    treeTable.addDirectory(folder);
                } else {
                    // TODO: Display message?
                }
            }
        }
    }    
}

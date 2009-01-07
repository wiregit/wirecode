package org.limewire.ui.swing.wizard;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.settings.InstallSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.library.manager.LibraryManagerItemImpl;
import org.limewire.ui.swing.library.manager.LibraryManagerModel;
import org.limewire.ui.swing.library.manager.LibraryManagerTreeTable;
import org.limewire.ui.swing.library.manager.RootLibraryManagerItem;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

public class SetupPage2 extends WizardPage {

    /**
     * A switch to control whether this page overrides or updates any existing lw settings.
     * 
     * (When doing an update this will be true and the new directories will be appended to the list.)
     */
    private final boolean shouldKeepExistingDirectorySettings;
    
    private final String line1 = I18n.tr("LimeWire is ready to fill your Library");
    private final String line2 = I18n.tr("My Library is where you view, share and unshare your files.");
    private final String line2KeepExisting = I18n.tr("My Library is where you view, share and unshare your files.  Don't worry, we will still share files from the older version.");
    private final String autoText = I18n.tr("Automatically add files to My Library");
    private final String autoExplanation = I18n.tr("Have LimeWire automatically add files from My Documents and the Desktop to My Library.");
    private final String manualText = I18n.tr("Manually add files to My Library");
    private final String manualExplanation = I18n.tr("Select the folders and categories LimeWire automatically adds to My Library.");
    private final String manualExplanationOpen = I18n.tr("LimeWire will look in the following folders...");
    private final String bottomText = I18n.tr("You can change this option later from Tools > Options");
    
    private final LibraryData libraryData;
    
    private final ButtonGroup buttonGroup;
    private final JRadioButton autoButton;
    private final JRadioButton manualButton;
    
    private final JLabel manualLabel;
    
    private final LibraryManagerTreeTable treeTable;
    private final JScrollPane treeTableScrollPane;
    private final JXButton addFolderButton;
        
    public SetupPage2(SetupComponentDecorator decorator, IconManager iconManager, LibraryData libraryData) {
        this(decorator, iconManager, libraryData, false);
    }
    
    public SetupPage2(SetupComponentDecorator decorator, IconManager iconManager, LibraryData libraryData,
            boolean shouldKeepExistingDirectorySettings) {
        
        this.libraryData = libraryData;
        this.shouldKeepExistingDirectorySettings = shouldKeepExistingDirectorySettings;
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, nogrid"));
        
        ButtonSelectionListener buttonSelectionListener = new ButtonSelectionListener();
        
        autoButton = new JRadioButton();
        decorator.decorateLargeRadioButton(autoButton);
        autoButton.setSelected(true);
        autoButton.addActionListener(buttonSelectionListener);
        
        manualLabel = new MultiLineLabel(manualExplanation);
        decorator.decorateNormalText(manualLabel);
        
        manualButton = new JRadioButton();
        decorator.decorateLargeRadioButton(manualButton);
        manualButton.addActionListener(buttonSelectionListener);
        
        addFolderButton = new JXButton(new AddDirectoryAction(SetupPage2.this));
        decorator.decoratePlainButton(addFolderButton);
        
        treeTable = new LibraryManagerTreeTable(iconManager, libraryData);
        initManualPanel();
        treeTableScrollPane = new JScrollPane(treeTable);
        
        setTreeTableVisiable(false);
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(autoButton);
        buttonGroup.add(manualButton);
        
        JLabel label;

        add(autoButton, "gaptop 10, gapleft 40");
        label = new JLabel(autoText);
        label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(autoButton));
        decorator.decorateHeadingText(label);
        add(label, "gaptop 10, gapleft 10, wrap");
        
        label = new MultiLineLabel(autoExplanation, 500);
        decorator.decorateNormalText(label);
        add(label, "gapleft 76, wrap");
        
        manualButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (manualButton.isSelected()) {
                    manualLabel.setText(manualExplanationOpen);
                }
                else {
                    manualLabel.setText(manualExplanation);
                }
            }
        });
        add(manualButton, "gaptop 10, gapleft 40");
        
        label = new JLabel(manualText);
        decorator.decorateHeadingText(label);
        label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(manualButton));
        
        add(label, "gaptop 10, gapleft 10, wrap");

        add(manualLabel, "gapleft 76, wrap");
        
        add(treeTableScrollPane, "gaptop 10, gapleft 40, growx");
        add(addFolderButton, "gaptop 10, gapright 30, wrap");
    }

    private void initManualPanel() {
        RootLibraryManagerItem root = new RootLibraryManagerItem(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
        
        Set<File> totalList = new HashSet<File>();
        
        totalList.addAll(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
        
        // Always add existing manage settings, even if this is not technically an upgrade
        //  (aka there has been a settings problem)
        totalList.addAll(libraryData.getDirectoriesToManageRecursively());

        for ( File file : totalList ) {
            root.addChild(new LibraryManagerItemImpl(root, libraryData, file, false));
        }
        
        treeTable.setTreeTableModel(new LibraryManagerModel(root));
    }

    @Override
    public String getLine1() {
        return line1;
    }
    
    @Override
    public String getLine2() {
        if (shouldKeepExistingDirectorySettings) {
            return line2KeepExisting;
        }
        else {
            return line2;
        }
    }

    @Override
    public String getFooter() {
        return bottomText;
    }
    
    @Override
    public void applySettings() {
        InstallSettings.SCAN_FILES.setValue(true);
        
        Collection<File> manage;
        Collection<File> exclude = new HashSet<File>();
        
        if (manualButton.isSelected()) {
            LibraryManagerModel model = treeTable.getLibraryModel();
            manage = model.getRootChildrenAsFiles();
            exclude.addAll(model.getAllExcludedSubfolders());
        } else {
            manage = new HashSet<File>();

            manage.addAll(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
            manage.addAll(libraryData.getDirectoriesToManageRecursively());
        }
        
        // We should always add existing exclusions back to be safe
        exclude.addAll(libraryData.getDirectoriesToExcludeFromManaging());
        
        libraryData.setManagedOptions(manage, exclude, libraryData.getManagedCategories());
    }
    
    private void setTreeTableVisiable(boolean visible){
        treeTableScrollPane.setVisible(visible);
        addFolderButton.setVisible(visible);
    }
    
    private class ButtonSelectionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            setTreeTableVisiable(buttonGroup.getSelection() == manualButton.getModel());
        }        
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

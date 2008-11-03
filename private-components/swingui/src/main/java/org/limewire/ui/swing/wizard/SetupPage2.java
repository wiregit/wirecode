package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.settings.InstallSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.library.manager.LibraryManagerItem;
import org.limewire.ui.swing.library.manager.LibraryManagerItemImpl;
import org.limewire.ui.swing.library.manager.LibraryManagerModel;
import org.limewire.ui.swing.library.manager.LibraryManagerTreeTable;
import org.limewire.ui.swing.library.manager.RootLibraryManagerItem;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;



public class SetupPage2 extends WizardPage {

    private String line1 = I18n.tr("LimeWire is ready to fill your Library");
    private String line2 = I18n.tr("Your Library is a central location to view, share and unshare your files with the LimeWire Network or your friends.");
    
    private ButtonGroup buttonGroup;
    
    private JRadioButton autoButton;
    private String autoText = I18n.tr("Automatically manage my Library");
    private String autoExplanation = I18n.tr("Choose this option to have LimeWire automatically scan files into yourLibrary from My Documents and the Desktop.");
    private JRadioButton manualButton;
    private String manualText = I18n.tr("Manually manage my Library");
    private String manualExplanation = I18n.tr("Choose this option if you want to select which folders LimeWire scans into your Library.");
    

    private String bottomText1 = I18n.tr("Scanning these folders into your Library will not automatically share your files");
    private String bottomText2 = I18n.tr("You can change these options later from Tools > Options");
    
    private LibraryManagerTreeTable treeTable;
    private JScrollPane treeTableScrollPane;
    private JButton addFolderButton;
    private LibraryData libraryData;
    
    @Resource
    private Color backgroundColor;
    
    public SetupPage2(LibraryData libraryData){
        GuiUtils.assignResources(this);
        
        this.libraryData = libraryData;
        
        setBackground(backgroundColor);
        setLayout(new MigLayout("nogrid"));
        
        autoButton = new JRadioButton(autoText);
        autoButton.setOpaque(false);
        autoButton.setSelected(true);
        autoButton.addActionListener(new ButtonSelectionListener());
        manualButton = new JRadioButton(manualText);
        manualButton.setOpaque(false);
        manualButton.addActionListener(new ButtonSelectionListener());
        
        addFolderButton = new JButton(new AddDirectoryAction(SetupPage2.this));
        
        treeTable = new LibraryManagerTreeTable();
        initManualPanel();
        treeTableScrollPane = new JScrollPane(treeTable);
        
        setTreeTableVisiable(false);
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(autoButton);
        buttonGroup.add(manualButton);
        

        add(new JLabel(line1), "wrap");
        add(new JLabel(line2), "wrap");
        
        int radioIndent = 50;
        int explanationIndent = 70;
        add(autoButton, "gaptop 20, gap left " + radioIndent+ ", wrap");
        add(new MultiLineLabel(autoExplanation, 500),  "gap left " + explanationIndent+ ", wrap");
        add(manualButton, "gaptop 20, gap left " + radioIndent+ ", wrap");
        add(new MultiLineLabel(manualExplanation, 500),  "gap left " + explanationIndent+ ", wrap");
        add(treeTableScrollPane, "growx");
        add(addFolderButton, "wrap");

        add(new JLabel(bottomText1), "pushy, aligny bottom, wrap");
        add(new JLabel(bottomText2), "aligny bottom");        
    }

    private void initManualPanel() {
        ArrayList<LibraryManagerItem> items = new ArrayList<LibraryManagerItem>();
        for(File file : libraryData.getDirectoriesToManageRecursively()) {
            items.add(new LibraryManagerItemImpl(file, true));
        }

        treeTable.setTreeTableModel(new LibraryManagerModel(new RootLibraryManagerItem(items)));
    }


    @Override
    public void applySettings() {
        InstallSettings.SCAN_FILES.setValue(true);
        //TODO: actually set the values
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
            File directory = FileChooser.getInputDirectory(parent, null);
            
            if(directory == null)
                return;
            
//TODO: do something with the directory
        }
    } 
 
}

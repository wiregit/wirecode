package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.util.I18n;

public class LibraryManagerOptionPanel extends OptionPanel {

    private JButton addFolderButton;
    private JCheckBox musicCheckBox;
    private JCheckBox videoCheckBox;
    private JCheckBox imageCheckBox;
    private JCheckBox docCheckBox;
    
    private JTable libraryTable;
    
    private JButton okButton;
    private JButton cancelButton;
    
    public LibraryManagerOptionPanel(Action okAction, CancelDialogAction cancelAction) {
        setLayout(new MigLayout("", "[300!][grow]", ""));
        
        createComponents(okAction, cancelAction);
        cancelAction.setOptionPanel(this);
        
        add(new JLabel(I18n.tr("LimeWire will automatically scan the folders below and place files in your library.")), "span 2, wrap");
    
        add(new JScrollPane(libraryTable), "span 2, growx, wrap");
        add(addFolderButton, "skip 1, alignx right, wrap");
        
        add(new JLabel(I18n.tr("Add the following types of files to your Library")), "span 2, wrap");
        add(getCheckBoxPanel(), "gapbottom 20, span 2, wrap");
        
        add(new MultiLineLabel(I18n.tr("Scanning these folders into your Library will not automatically share your files"), 300), "growx");
        add(okButton, "alignx right, split");
        add(cancelButton);
    }
    
    private void createComponents(Action okAction, Action cancelAction) {
        
        libraryTable = new JTable();
        addFolderButton = new JButton(I18n.tr("Add New Folder"));
        
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
    
    public static void main(String args[]) {
        JFrame f = new JFrame();
        f.setSize(500,500);
        
        LibraryManagerOptionPanel l = new LibraryManagerOptionPanel(null, null);
        f.add(l);
        
        f.setDefaultCloseOperation(2);
        f.setVisible(true);
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
        // TODO Auto-generated method stub
        
    }
    
}

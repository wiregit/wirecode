package org.limewire.ui.swing.options;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

import net.miginfocom.swing.MigLayout;

public class LibraryManagerOptionPanel extends JPanel {

    private JButton addFolderButton;
    private JCheckBox musicCheckBox;
    private JCheckBox videoCheckBox;
    private JCheckBox imageCheckBox;
    private JCheckBox docCheckBox;
    
    private JTable libraryTable;
    
    private JButton okButton;
    private JButton cancelButton;
    
    public LibraryManagerOptionPanel() {
        setLayout(new MigLayout("debug", "[300!][grow]", ""));
        
        createComponents();
        
        add(new JLabel(I18n.tr("LimeWire will automatically scan the folders below and place files in your library.")), "span 2, wrap");
    
        add(new JScrollPane(libraryTable), "span 2, growx, wrap");
        add(addFolderButton, "skip 1, alignx right, wrap");
        
        add(new JLabel(I18n.tr("Add the following types of files to your Library")), "span 2, wrap");
        add(getCheckBoxPanel(), "gapbottom 20, span 2, wrap");
        
        add(new MultiLineLabel(I18n.tr("Scanning these folders into your Library will not automatically share your files"), 300), "growx");
        add(okButton, "alignx right, split");
        add(cancelButton);
    }
    
    private void createComponents() {
        
        libraryTable = new JTable();
        addFolderButton = new JButton(I18n.tr("Add New Folder"));
        
        musicCheckBox = new JCheckBox();
        videoCheckBox = new JCheckBox();
        imageCheckBox = new JCheckBox();
        docCheckBox = new JCheckBox();
        
        okButton = new JButton(I18n.tr("OK"));
        cancelButton = new JButton(I18n.tr("Cancel"));
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
        
        LibraryManagerOptionPanel l = new LibraryManagerOptionPanel();
        f.add(l);
        
        f.setDefaultCloseOperation(2);
        f.setVisible(true);
    }
    
}

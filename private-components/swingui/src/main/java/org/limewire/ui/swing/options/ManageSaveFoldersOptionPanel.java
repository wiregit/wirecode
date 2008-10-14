package org.limewire.ui.swing.options;

import java.awt.Color;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class ManageSaveFoldersOptionPanel extends OptionPanel {

    @Resource
    private Icon audioIcon;
    @Resource
    private Icon videoIcon;
    @Resource
    private Icon imageIcon;
    @Resource
    private Icon documentIcon;
    @Resource
    private Icon programIcon;
    @Resource
    private Icon otherIcon;
    
    private DisplayTextField audioTextField;
    private DisplayTextField videoTextField;
    private DisplayTextField imageTextField;
    private DisplayTextField documentTextField;
    private DisplayTextField programTextField;
    private DisplayTextField otherTextField;
    
    private JButton audioBrowseButton;
    private JButton videoBrowseButton;
    private JButton imageBrowseButton;
    private JButton documentBrowseButton;
    private JButton programBrowseButton;
    private JButton otherBrowseButton;
    
    private JButton defaultButton;
    private JButton okButton;
    private JButton cancelButton;
    
    public ManageSaveFoldersOptionPanel(Action okAction, CancelDialogAction cancelAction) {
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("gapy 10"));
        
        audioTextField = new DisplayTextField();
        videoTextField = new DisplayTextField();
        imageTextField = new DisplayTextField();
        documentTextField = new DisplayTextField();
        programTextField = new DisplayTextField();
        otherTextField = new DisplayTextField();
        
        audioBrowseButton = new JButton(new BrowseDirectoryAction(this, audioTextField));
        videoBrowseButton = new JButton(new BrowseDirectoryAction(this, videoTextField));
        imageBrowseButton = new JButton(new BrowseDirectoryAction(this, imageTextField));
        documentBrowseButton = new JButton(new BrowseDirectoryAction(this, documentTextField));
        programBrowseButton = new JButton(new BrowseDirectoryAction(this, programTextField));
        otherBrowseButton = new JButton(new BrowseDirectoryAction(this, otherTextField));
        
        cancelAction.setOptionPanel(this);
        
        defaultButton = new JButton();
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
        
        add(new JLabel(I18n.tr("Choose where specific file types get saved")), "span, wrap");
        
        add(new JLabel(I18n.tr("Audio"), audioIcon, SwingConstants.RIGHT), "wrap");
        add(audioTextField, "gapleft 25, gap unrelated");
        add(audioBrowseButton, "alignx right, wrap");
        
        add(new JLabel(I18n.tr("Video"), videoIcon, SwingConstants.RIGHT), "wrap");
        add(videoTextField, "gapleft 25, gap unrelated");
        add(videoBrowseButton, "alignx right, wrap");
        
        add(new JLabel(I18n.tr("Images"), imageIcon, SwingConstants.RIGHT), "wrap");
        add(imageTextField, "gapleft 25, gap unrelated");
        add(imageBrowseButton, "alignx right, wrap");
        
        add(new JLabel(I18n.tr("Documents"), documentIcon, SwingConstants.RIGHT), "wrap");
        add(documentTextField, "gapleft 25, gap unrelated");
        add(documentBrowseButton, "alignx right, wrap");
        
        add(new JLabel(I18n.tr("Programs"), programIcon, SwingConstants.RIGHT), "wrap");
        add(programTextField, "gapleft 25, gap unrelated");
        add(programBrowseButton, "alignx right, wrap");
        
        add(new JLabel(I18n.tr("Other"), otherIcon, SwingConstants.RIGHT), "wrap");
        add(otherTextField, "gapleft 25, gap unrelated");
        add(otherBrowseButton, "alignx right, wrap");
        
        add(defaultButton, "gaptop 10, push");
        add(okButton, "split 2, alignx right, gaptop 10");
        add(cancelButton, "alignx right, gaptop 10");
        
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
//        NamedMediaType t;
    }
    
//    private void initDirectory(File file, String location, JTextField textField) {
//        try {
////            File file = SharingSettings.getSaveDirectory();
//            if (file == null) {
//                throw (new FileNotFoundException());
//            }
//            location = file.getCanonicalPath();
//            textField.setText(file.getCanonicalPath());
//        } catch (FileNotFoundException fnfe) {
//            // simply use the empty string if we could not get the save
//            // directory.
//            //TODO: change this to a real setting?? 
//            location = "";
//            textField.setText("");
//        } catch (IOException ioe) {
//            location = "";
//            textField.setText("");
//        }
//    }
    
    private class DisplayTextField extends JTextField {
        public DisplayTextField() {
            setEditable(false);
            setBackground(Color.WHITE);
            setColumns(40);
        }
    }

}

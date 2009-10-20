package org.limewire.ui.swing.properties;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.BitziLookupAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.CopyMagnetLinkToClipboardAction;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Provider;

/**
 * Displays general information about a PropertiableFile.
 */
class FileInfoOverviewPanel implements FileInfoPanel {

    @Resource private Font smallFont;
    @Resource private Font smallBoldFont;
    @Resource private Font headerFont;
    
    private final FileInfoType type;
    private PropertiableFile propertiableFile;
    private final Provider<IconManager> iconManager;
    private final MagnetLinkFactory magnetLinkFactory;
    private final CategoryIconManager categoryIconManager;
    private final ThumbnailManager thumbnailManager;
    private final LibraryManager libraryManager;
    private final RenameAction renameAction;
    
    private final JPanel component;
    private JTextField nameLabel;
    
    public FileInfoOverviewPanel(FileInfoType type, PropertiableFile propertiableFile, 
            Provider<IconManager> iconManager, MagnetLinkFactory magnetLinkFactory, 
            CategoryIconManager categoryIconManager, ThumbnailManager thumbnailManager,
            LibraryManager libraryManager) {
        this.type = type;
        this.propertiableFile = propertiableFile;
        this.iconManager = iconManager;
        this.magnetLinkFactory = magnetLinkFactory;
        this.categoryIconManager = categoryIconManager;
        this.thumbnailManager = thumbnailManager;
        this.libraryManager = libraryManager;
        this.renameAction = new RenameAction();
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx, insets 10 3 10 10"));
        init();
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
        //component never changes state
    }
    
    @Override
    public void unregisterListeners() {
        //no listeners registered
    }
    

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
        this.propertiableFile = file;
        nameLabel.setText(propertiableFile.getFileName());
    }
    
    public void enableRename() {
        renameAction.actionPerformed(null);
    }
    
    private void init() {
        component.setOpaque(false);

        addOverviewCategory();
        
        HyperlinkButton copyToClipboard = null;
        if(type == FileInfoType.LOCAL_FILE){
            if(propertiableFile instanceof LocalFileItem && ((LocalFileItem)propertiableFile).isShareable()) {
                copyToClipboard = new HyperlinkButton();
                copyToClipboard.setFont(smallFont);
                copyToClipboard.setAction(new AbstractAction(I18n.tr("Copy Link")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        StringSelection sel = new StringSelection(magnetLinkFactory.createMagnetLink((LocalFileItem)propertiableFile));
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                    }
                });
            }
        } else if(type == FileInfoType.REMOTE_FILE) {
            if(propertiableFile instanceof VisualSearchResult) {
                copyToClipboard = new HyperlinkButton();
                copyToClipboard.setFont(smallFont);
                copyToClipboard.setAction(new CopyMagnetLinkToClipboardAction((VisualSearchResult)propertiableFile));
            }
        } 
        HyperlinkButton moreFileInfo = new HyperlinkButton(new BitziLookupAction(propertiableFile));
        moreFileInfo.setFont(smallFont);
      
        if(type == FileInfoType.LOCAL_FILE)
            component.add(new HyperlinkButton(renameAction), "cell 1 1, alignx right");
        component.add(moreFileInfo, "cell 1 2, alignx right");
        if(copyToClipboard != null)
            component.add(copyToClipboard, "cell 1 3, alignx right");
    }
    

    /**
     * Loads values for the Overview Panel. If a given field is null or 
     * doesn't exist, the field is ignored.
     */
    private void addOverviewCategory() {
        Icon icon = getIcon(propertiableFile);
        JPanel iconDock = new JPanel();
        iconDock.setOpaque(false);
        iconDock.add(new JLabel(icon));
        component.add(iconDock, "aligny top, growy, gap 7, gaptop 5, dock west");
        nameLabel = createLabelField(propertiableFile.getFileName());
        nameLabel.setFont(headerFont);
        nameLabel.setPreferredSize(new Dimension(440, 26));
        component.add(nameLabel, "growx, span, wrap");
        component.add(createLabel(I18n.tr("Size:")), "split 2");
        component.add(createLabelField(FileInfoUtils.getFileSize(propertiableFile)), "growx, wrap");
        
        switch(propertiableFile.getCategory()) {
        case AUDIO:
            String time = FileInfoUtils.getLength(propertiableFile);
            if(time != null) {
                component.add(createLabel(I18n.tr("Length:")), "split 2");
                component.add(createLabelField(time), "growx, wrap");
            }
            String bitrate = propertiableFile.getPropertyString(FilePropertyKey.BITRATE);
            if(bitrate != null) {
                component.add(createLabel(I18n.tr("Bitrate:")), "split 2");
                String quality = FileInfoUtils.getQuality(propertiableFile);
                if(quality != null)
                    component.add(createLabelField(bitrate + " kbps (" + quality + ")"), "growx, wrap");
                else
                    component.add(createLabelField(bitrate + " kbps"), "growx, wrap");
            }
            break;
        case VIDEO:
            String width = propertiableFile.getPropertyString(FilePropertyKey.WIDTH);
            String height = propertiableFile.getPropertyString(FilePropertyKey.HEIGHT);
            if(width != null && height != null) {
                component.add(createLabel(I18n.tr("Dimensions:")), "split 2");
                component.add(createLabelField(width + "px X " + height + "px"), "growx, wrap");
            }
            break;
        case IMAGE:
            String date = FileInfoUtils.convertDate(propertiableFile);
            if(date.length() > 0) {
                component.add(createLabel(I18n.tr("Date Created:")), "split 2");
                component.add(createLabelField(date), "growx, wrap");
            }
            break;
        case DOCUMENT:
            component.add(createLabel(I18n.tr("Type:")), "split 2");
            component.add(createLabelField(iconManager.get().getMIMEDescription(propertiableFile)), "growx, wrap");
            date = FileInfoUtils.convertDate(propertiableFile);
            if(date.length() > 0) {
                component.add(createLabel(I18n.tr("Date Created:")), "split 2");
                component.add(createLabelField(date), "growx, wrap");
            }
            break;
        case PROGRAM:
            break;
        case OTHER:
            component.add(createLabel(I18n.tr("Type:")), "split 2");
            component.add(createLabelField(iconManager.get().getMIMEDescription(propertiableFile)), "growx, wrap");
            break;
        }
        component.add(createLabel(I18n.tr("Hash:")), "split 2");
        String urn = propertiableFile.getUrn() == null ? "" : propertiableFile.getUrn().toString();
        component.add(createLabelField(urn), "growx, wrap");
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallBoldFont);
        return label;
    }
    
    private JTextField createLabelField(String text) {
        JTextField field = new JTextField(text);
        field.setCaretPosition(0);
        field.setEditable(false);
        field.setOpaque(false);
        field.setFont(smallFont);
        field.setBorder(BorderFactory.createEmptyBorder(0,1,0,1));
        return field;
    }
    
    /**
     * Returns the appropriate Icon for the file type.
     */
    private Icon getIcon(PropertiableFile propertiableFile) {
        switch(type){
        case LOCAL_FILE:
            switch (propertiableFile.getCategory()) {
            case IMAGE:
                return thumbnailManager.getThumbnailForFile(((LocalFileItem)propertiableFile).getFile());
            default:
                return categoryIconManager.getIcon(propertiableFile);
            }
        case DOWNLOADING_FILE:
        case REMOTE_FILE:
        default:
            return categoryIconManager.getIcon(propertiableFile);
        }
    }
    
    /**
     * Handles renaming of the file name. This is handled using
     * a popup menu to get the support of loosing focus and
     * properlly disappearing.
     */
    private class RenameAction extends AbstractAction {

        private final JTextField textField;
        private final JPopupMenu menu;
        
        public RenameAction() {
            super(I18n.tr("Rename"));
            
            textField = new JTextField();
            textField.setFont(headerFont);
            textField.addKeyListener(new KeyListener(){
                @Override
                public void keyPressed(KeyEvent e) {}
                @Override
                public void keyReleased(KeyEvent e) {}

                @Override
                public void keyTyped(KeyEvent e) {
                    if(e.getKeyChar() == KeyEvent.VK_ENTER) {
                        menu.setVisible(false);
                    }
                }
            });

            menu = new JPopupMenu();
            menu.add(textField);
            menu.addPopupMenuListener(new PopupMenuListener(){
                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    saveFileName();
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    saveFileName();
                }

                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            });
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!menu.isVisible()) {
                setText();

                menu.show(component, nameLabel.getLocation().x-3, nameLabel.getLocation().y-3);
                textField.requestFocusInWindow();
                textField.selectAll();
            }
        }
        
        private void setText() {
            if(propertiableFile instanceof LocalFileItem) {
                int maxFileLength = getMaxFileSize(((LocalFileItem)propertiableFile).getFile());
                textField.setDocument(new FileNameDocument(maxFileLength));
            }
            String fileName = FileUtils.getFilenameNoExtension(nameLabel.getText());
            textField.setText(fileName);
            textField.setFont(headerFont);
            ResizeUtils.forceWidth(textField, nameLabel.getWidth());
        }
        
        private void saveFileName() {
            String newFileName = textField.getText().trim();
            LocalFileItem oldFileItem = (LocalFileItem) propertiableFile;
            // check the new file name is valid
            if(!isValidFileName(newFileName)) {
                textField.setText(oldFileItem.getName());
                return;
            }
            
            // check if the name hasn't changed, just return
            if(newFileName.equals(oldFileItem.getName())) {
                return;
            }

            File oldFile = oldFileItem.getFile();
            
            newFileName = newFileName + "." + FileUtils.getFileExtension(oldFile);
            File newFile = new File(oldFile.getParentFile(), newFileName);
            
            // try performing the file rename, if something goes wrong, revert textfield.
            if(FileUtils.forceRename(oldFile, newFile)) {
                updateFileNameInLibrary(oldFile, newFile);
            } else {
            	newFile.delete();
                textField.setText(oldFileItem.getName());
            }
        }
        
        /**
         * Notifies the library that the fileName has been changed.
         */
        private void updateFileNameInLibrary(File oldFile, File newFile) {
            libraryManager.getLibraryManagedList().fileRenamed(oldFile, newFile);
        }
        
        /**
         * Returns true if the text is a valid file name.
         */
        private boolean isValidFileName(String fileName) {
            return fileName != null && fileName.length() > 0 && CommonUtils.santizeString(fileName).equals(fileName);
        }
        
    }
    
    /**
     * Returns the maximum length the filename can be.
     */
    private int getMaxFileSize(File file) {
        String fileName = FileUtils.getFilenameNoExtension(file.getName());
        return OSUtils.getMaxPathLength() - file.getAbsolutePath().length() + fileName.length();
    }
    
    /**
     * Prevents a TextField from having a String beyond a certain length. Also
     * prevents non-valid filename characters from being entered.
     */
    private class FileNameDocument extends PlainDocument {
        
        private int maxLength = Integer.MAX_VALUE;
        
        public FileNameDocument(int length) {
            this.maxLength = length;
        }
        
        @Override
        public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
            if(str == null)
                return;
            String santize = CommonUtils.santizeString(str);
            // only add the string if it contains valid characters and is below the max length
            if(santize.equals(str) && getLength() + str.length() <= maxLength) {
                super.insertString(offset, str, attr);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }
}

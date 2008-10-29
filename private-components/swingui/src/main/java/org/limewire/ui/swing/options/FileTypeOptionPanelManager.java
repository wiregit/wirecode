package org.limewire.ui.swing.options;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.components.CheckBoxList;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListCheckChangeEvent;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListCheckChangeListener;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListSelectionEvent;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListSelectionListener;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.CategoryUtils;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.MediaType;

import com.google.inject.Inject;
import com.google.inject.Singleton;



/**
 * Constructs the file type sharing panel to be used in the options
 *  menu and setup manager.  Includes external interface for saving
 *  and reloading settings.  
 */

@Singleton
public final class FileTypeOptionPanelManager {

//    private static final int MAX_EXT_LENGTH = 15;
    
    private final JPanel mainContainer;
    private final CardLayout mediaLayout;
    private final JPanel currentPanel;
    
    private CheckBoxList<Category> sidePanel;
        
    private Map<Category,CheckBoxList<String>> panels;
    
    private final ExtensionProvider extensionProvider = new ExtensionProvider();
    private final ExtensionsExtrasProvider extensionExtrasProvider = new ExtensionsExtrasProvider();
    private final Set<String> mediaNames;
    
    private Set<Category> mediaKeys;
    private Set<Category> mediaUnchecked;   
    private Category currentKey;
    
    // Used if extensions will be add-able in the future
//    private CheckBoxList<String> otherPanel;
//    private Category otherKey;
    
    private final Collection<String> originalExtensions;

    private final CategoryIconManager categoryIconManager;
    private final IconManager iconManager;
    private final LibraryData libraryData;
    
    @Inject
    public FileTypeOptionPanelManager(CategoryIconManager categoryIconManager,
            IconManager iconManager, LibraryManager libraryManager) {
        this.categoryIconManager = categoryIconManager;
        this.iconManager = iconManager;
        this.libraryData = libraryManager.getLibraryData();
        this.originalExtensions = libraryData.getManagedExtensions();
        
        this.mainContainer   = new JPanel(new BorderLayout());        
        this.mediaLayout  = new CardLayout();
        this.currentPanel = new JPanel(mediaLayout);
        
        mediaNames = new HashSet<String>();
        for ( MediaType mt : MediaType.getDefaultMediaTypes() ) {
            mediaNames.add(mt.toString()); // TODO: should be the name, need to confirm
        }
    }
    
    /**
     * Switches the active media panel to the one of the given key
     */
    private void switchPanel(Category mediaKey) {

        this.currentKey = mediaKey;
        this.mediaLayout.show(currentPanel, mediaKey.toString());
        this.sidePanel.setItemSelected(mediaKey);
    }
    
//    /** 
//     * Reverts the set extensions to the limewire defaults
//     */
//    private void revert() {
//        Category oldKey = this.currentKey;
//        
//        libraryData.setManagedExtensions(libraryData.getDefaultExtensions());
//        initCore();
//        buildUI();
//        
//        this.switchPanel(oldKey);
//        this.sidePanel.setItemSelected(oldKey);
//        
//    }
    
    /** 
     * Repaints the side panel (to update comment text)
     */
    private void refreshSidePanel() {
        this.sidePanel.update();
    }
    
    /**
     * Sets the enabled setting of a panel of a given key
     */
    private void setPanelEnabled(Category mediaKey, boolean enabled) {
       this.panels.get(mediaKey).setEnabled(enabled);
    }

    /**
     * Returns the panel that this class is used to produce
     */
    public Container getContainer() {
        return this.mainContainer;
    }
   
    void initCore() {        
        Collection<String> extensions = libraryData.getManagedExtensions();
        Collection<String> defaultExtensions = libraryData.getDefaultManagedExtensions();
                        
        Set<String> allExtensions = new HashSet<String>(extensions);
        allExtensions.addAll(defaultExtensions);
        Map<Category, List<String>> extensionsByType = createExtensionsMap(allExtensions);

        mediaKeys = new TreeSet<Category>();
        Set<Category> s; 
        if ((s = extensionsByType.keySet()) != null) {        
            mediaKeys.addAll(s);
        }

        this.panels = new LinkedHashMap<Category,CheckBoxList<String>>();
        this.mediaUnchecked = new HashSet<Category>();
        this.currentKey = null;

        PanelsCheckChangeListener refreshListener = new PanelsCheckChangeListener(this);
        
        for (Category key : mediaKeys) {
            if (this.currentKey == null) {
                this.currentKey = key;
            }
            
            Collection<String> extensionsInCategory = extensionsByType.get(key);
            Collection<String> notSelectedInCategory = new HashSet<String>(extensionsInCategory);
            notSelectedInCategory.removeAll(extensions);
            
            CheckBoxList<String> newPanel = new CheckBoxList<String>(extensionsInCategory, notSelectedInCategory, 
                    extensionProvider, extensionExtrasProvider,
                    CheckBoxList.SELECT_FIRST_OFF);
            
            newPanel.setCheckChangeListener(refreshListener);
            
            if (extensionsInCategory.equals(notSelectedInCategory)) {
                newPanel.setEnabled(false);
                this.mediaUnchecked.add(key);
            }
            
            this.panels.put(key, newPanel);
        }
    }

    
    public void buildUI() {
        
        if (this.panels.isEmpty()) return;
        
        this.mainContainer.removeAll();
        this.currentPanel.removeAll();
        
        
        for (Category key : mediaKeys) {
            this.currentPanel.add(this.panels.get(key), key.toString());
        }
        
        this.sidePanel = new CheckBoxList<Category>(this.mediaKeys, this.mediaUnchecked,
                new MediaProvider(), new MediaExtrasProvider(),
                CheckBoxList.SELECT_FIRST_ON);
        
        this.sidePanel.setPreferredSize(new Dimension(150, 0));
        this.sidePanel.setSelectionListener(new SideSelectListener(this));
        this.sidePanel.setCheckChangeListener(new CheckChangeListener(this));
        this.sidePanel.setItemSelected(this.currentKey);
        this.sidePanel.setCheckBoxesVisible(false);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                this.sidePanel, this.currentPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(180);
        
        this.mainContainer.add(splitPane, BorderLayout.CENTER);
        
        this.mainContainer.validate();

    }
    
    private static Map<Category, List<String>> createExtensionsMap(Collection<String> extensions) {
        Map<Category, List<String>> extensionsByType = new LinkedHashMap<Category, List<String>>();
        for (String extension : extensions) { 
            Category nm = CategoryUtils.getCategory(MediaType.getMediaTypeForExtension(extension));
            if (nm == null) {
                nm = CategoryUtils.getCategory(MediaType.getOtherMediaType());
            }
            if (!extensionsByType.containsKey(nm)) {
                extensionsByType.put(nm, new ArrayList<String>(8));
            }
            List<String> typeExtension = extensionsByType.get(nm);
            if (!typeExtension.contains(extension)) {
                typeExtension.add(extension);
            }
        }
        
        return extensionsByType;
    }
    
    private Collection<String> getExtensions() {
        Set<String> elements = new HashSet<String>();
        
        for ( CheckBoxList<String> panel : this.panels.values() ) {
             elements.addAll(panel.getCheckedElements());
        }
        return elements;
    }
    
    
//    private Collection<String> getUncheckedExtensions() {
//        Set<String> elements = new HashSet<String>();
//        
//        for ( CheckBoxList<String> panel : this.panels.values() ) {
//             elements.addAll(panel.getUncheckedElements());
//        }
//        return elements;
//    }
//    
//    private boolean checkExt(String text) {
//        if (text == null) {
//            return false;
//        }
//        
//        String malformedMsg = I18n.tr("The extension name was not valid, could not add.");
//        
//        if (   text.length() == 0 
//            || text.length() > MAX_EXT_LENGTH
//           ) {                            
//            JOptionPane.showMessageDialog(this.mainContainer, malformedMsg);
//            return false;
//        }
//        
//        
//           
//        
//        if (libraryData.getDefaultExtensions().contains(text)) {
//            MediaType type = MediaType.getMediaTypeForExtension(text);
//            
//            if (type == null) {
//                type = MediaType.getOtherMediaType();
//            }
//            
//            CheckBoxList<String> panel = this.panels.get(type); 
//            
//            if (panel == null) {
//                JOptionPane.showMessageDialog(this.mainContainer, malformedMsg);
//                return false;    
//            }
//         
//            this.switchPanel(CategoryUtils.getCategory(type));
//            panel.setItemChecked(text);
//            this.sidePanel.setItemSelected(CategoryUtils.getCategory(type));
//            
//            return false;
//        }
//
//        if (this.otherPanel.getElements().contains(text)) {
//            CheckBoxList<String> panel = this.panels.get(this.otherKey); 
//            
//            if (panel == null) {
//                JOptionPane.showMessageDialog(this.mainContainer, malformedMsg);
//                return false;    
//            }
//            
//            this.switchPanel(this.otherKey);
//            panel.setItemChecked(text);
//            this.sidePanel.setItemSelected(this.otherKey);
//            
//            return false;
//        }
//        
//        
//        return true;
//    }
//
//    
//    
//    
//    private class RestoreAction extends AbstractAction {
//        
//        public RestoreAction() {
//            putValue(Action.NAME, I18n.tr("Restore Defaults"));
//            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share the Default File Extensions"));
//        }
//        
//        public void actionPerformed(ActionEvent e) {
//            int answer = JOptionPane.showConfirmDialog(mainContainer, 
//                    new Object[] {new MultiLineLabel(I18n
//                            .tr("This options clears any extension sharing changes you made and sets LimeWire's extension sharing preferences to the original preferences. Do you wish to continue?"), 300)},
//                            I18n.tr("Extension Sharing Settings"), JOptionPane.YES_NO_OPTION);
//            if (answer == JOptionPane.YES_OPTION) {
//                revert();
//            }               
//        }
//    }
    

    // Methods to integrate with PaneItem
    
    public void initOptions() {
        this.initCore();
        this.buildUI();
    }

    public boolean applyOptions() {
        libraryData.setManagedExtensions(getExtensions());        
        return false;
    }

    public boolean hasChanged() {
        return !this.originalExtensions.equals(this.getExtensions());
    }
    
    
    // Listener Classes
    
    private static class SideSelectListener implements
            CheckBoxListSelectionListener {

        private FileTypeOptionPanelManager parent;

        public SideSelectListener(FileTypeOptionPanelManager parent) {
            this.parent = parent;
        }

        public void valueChanged(CheckBoxListSelectionEvent e) {
            parent.switchPanel((Category)e.getSelected());
        }
    }

    private static class CheckChangeListener implements
            CheckBoxListCheckChangeListener<Category> {

        private FileTypeOptionPanelManager parent;

        public CheckChangeListener(FileTypeOptionPanelManager parent) {
            this.parent = parent;
        }

        public void valueChanged(CheckBoxListCheckChangeEvent<Category> e) {
            this.parent.setPanelEnabled(e.getSelected(), e.getChecked());
            this.parent.refreshSidePanel();
        }
    }  
    
    private static class PanelsCheckChangeListener implements
    CheckBoxListCheckChangeListener<MediaType> {

        private FileTypeOptionPanelManager parent;

        public PanelsCheckChangeListener(FileTypeOptionPanelManager parent) {
            this.parent = parent;
        }

        public void valueChanged(CheckBoxListCheckChangeEvent<MediaType> e) {
            this.parent.refreshSidePanel();
        }
    }  

    
    
    
    private String getDescriptionText(String obj) {
        if (obj == null) {
            throw new IllegalArgumentException();
        }
        
        Icon icon = iconManager.getIconForExtension(obj);
        
        if (icon == null) {
            return null;
        }
        
        if (icon.toString().indexOf("@") > -1) {
            return null;
        }
        
        if (mediaNames.contains(icon.toString())) {
            return null;
        }
        
        return icon.toString();
    }
    
    // Providers   
    
    private class ExtensionProvider implements CheckBoxList.TextProvider<String> {
        
        public String getText(String obj) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }
            
            return obj;
        }
        
        public String getToolTipText(String obj) {
            return getDescriptionText(obj);
        }

        public Icon getIcon(String obj) {
            if (obj == null) {
                throw new NullPointerException("Null object passed to icon lookup.");
            }
            
            Icon icon = iconManager.getIconForExtension(obj);
            
            return icon != null ? icon : iconManager.getBlankIcon();
        }
    }
    
    
    private class MediaProvider 
        implements CheckBoxList.TextProvider<Category> {
        
        public String getText(Category obj) {
                    
            return obj.toString();
        }
        
        public String getToolTipText(Category obj) {
            return null;
        }

        public Icon getIcon(Category obj) {
            return categoryIconManager.getIcon(obj);
        }
    }
        
    private class MediaExtrasProvider
        implements CheckBoxList.ExtrasProvider<Category> {
        
        public MediaExtrasProvider() {

        }        
        
        @Override
        public boolean isSeparated(Category obj) {
            return false;
        }

        @Override
        public String getComment(Category obj) {
            CheckBoxList<String> panel = panels.get(obj);
            
            if (panel == null) {
                return "(0)";
            }
            
            return "(" + panel.getCheckedElements().size() + ")";
        }

        @Override
        public int getCommentFeildSize() {
            return 0;
        }
    }    
    
    private class ExtensionsExtrasProvider
        implements CheckBoxList.ExtrasProvider<String> {
    
        public ExtensionsExtrasProvider() {
        }        
    
        @Override
        public boolean isSeparated(String obj) {
            return false;
        }

        @Override
        public String getComment(String obj) {
            return getDescriptionText(obj);
        }

        @Override
        public int getCommentFeildSize() {
            return 70;
        }
    }    

}

package org.limewire.ui.swing.options;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.setting.StringArraySetting;
import org.limewire.ui.swing.components.CheckBoxList;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListCheckChangeEvent;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListCheckChangeListener;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListSelectionEvent;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListSelectionListener;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.MediaType; // TODO: convert NamedMediaType!

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;


/**
 * Constructs the file type sharing panel to be used in the options
 *  menu and setup manager.  Includes external interface for saving
 *  and reloading settings.  
 */
public final class FileTypeOptionPanelManager {

    private static final int MAX_EXT_LENGTH = 15;
    
    public final static String TITLE = I18nMarker.marktr("Sharing Extensions");

    public final static String LABEL = I18nMarker
            .marktr("Select the file types that you wish to share with LimeWire.  (This does not automatically share all files with these types.  Only files in your Shared Folders will be shared.)");
    
    public final static String URL 
        = "http://www.limewire.org/wiki/index.php?title=Sharing_Extensions";
    
    
    
    private Component                    parent = null;
    private JPanel                       mainContainer;
    private CheckBoxList<MediaType> sidePanel;
    
    private CardLayout                   mediaLayout;
    private JPanel                       currentPanel;
        
    private CheckBoxList<String>         customPanel;
    private Set<String>                  customUnchecked;
    
    private Map<MediaType,CheckBoxList<String>> panels;
    
    private final ExtensionProvider extensionProvider = new ExtensionProvider();

    public static final String CUSTOM = "__Custom$";
    public static final String OTHER = "_Other$";
    public static final String CUSTOM_NAME = I18nMarker.marktr("My Extensions");
    public static final String OTHER_NAME = I18nMarker.marktr("Other");

    
    private static final MediaType otherMedia = new MediaType(OTHER);
        // TODO: = new MediaType(new MediaType(OTHER_NAME, OTHER, null),null);

    private static final MediaType customMedia = new MediaType(CUSTOM);
        // TODO: = new MediaType(new MediaType("CUSTOM_NAME", CUSTOM, null),null);

    
    private JCheckBox disableSensitive;
    private boolean migrate;
    private Set<MediaType> mediaKeys;
    private Set<MediaType> mediaUnchecked;        


    /**
     * Stores the key for the custom panel
     */
    private MediaType               customKey = customMedia;
    
    /**
     * Stores the key for the currently viewed media type
     */
    private MediaType               currentKey;
    
    private String                       originalExtensions;
    
    public FileTypeOptionPanelManager(Component parent) {
        this();
        
        this.parent = parent;
    }
    
    public FileTypeOptionPanelManager() {
        this.customUnchecked = new HashSet<String>();
        
        this.mainContainer   = new JPanel(new BorderLayout());
        
        this.mediaLayout  = new CardLayout();
        this.currentPanel = new JPanel(mediaLayout);
    }

    
    
    
    /**
     * Switches the active media panel to the one of the given key
     */
    private void switchPanel(MediaType mediaKey) {

        this.currentKey = mediaKey;
        this.mediaLayout.show(currentPanel, mediaKey.toString());
        this.sidePanel.setItemSelected(mediaKey);
    }
    
    /**
     * Adds a new extension of type name 
     */
    public void addCustomExt(String name) {
        Set<String> customExts = this.customPanel.getElements();
        customExts.add(name);
        
        this.customUnchecked = this.customPanel.getRawUncheckedElementsAsSet();
        this.customPanel.setElements(customExts, this.customUnchecked);
        
        this.switchPanel(this.customKey);
        this.sidePanel.setItemSelected(this.customKey);
    }
    
    /** 
     * Reverts the set extensions to the limewire defaults
     */
    private void revert() {
        MediaType oldKey = this.currentKey;
        
        SharingSettings.EXTENSIONS_TO_SHARE.revertToDefault();
        SharingSettings.EXTENSIONS_LIST_CUSTOM.revertToDefault();
        SharingSettings.EXTENSIONS_LIST_UNSHARED.revertToDefault();
        SharingSettings.DISABLE_SENSITIVE.revertToDefault();
        
        initCore();
        buildUI();
        
        this.switchPanel(oldKey);
        this.sidePanel.setItemSelected(oldKey);
        
    }
    
    /** 
     * Repaints the side panel (to update comment text)
     */
    private void refreshSidePanel() {
        this.sidePanel.update();
    }
    
    /**
     * Sets the enabled setting of a panel of a given key
     */
    private void setPanelEnabled(MediaType mediaKey, boolean enabled) {
       this.panels.get(mediaKey).setEnabled(enabled);
    }

    /**
     * Returns the panel that this class is used to produce
     */
    public Container getContainer() {
        return this.mainContainer;
    }
   
    void initCore() {
        this.customUnchecked.clear();
        
        Set<String> customExts = new HashSet<String>();
        
        String[] totalExtensions = SharingSettings.getDefaultExtensions();
        String[] selectedExtensions;
        
        
        migrate = SharingSettings.EXTENSIONS_MIGRATE.getValue();
        
        if (migrate) { 
            selectedExtensions = StringArraySetting.decode(SharingSettings.EXTENSIONS_TO_SHARE.getValue().toLowerCase());
            
            for ( int i = 0 ; i<selectedExtensions.length ; i++ ) {
                if (!contains(totalExtensions, selectedExtensions[i])) {
                    customExts.add(selectedExtensions[i]);
                }
            }
        }
        else {
            String[] custom           = StringArraySetting.decode(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().toLowerCase());
            String[] unselected       = StringArraySetting.decode(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().toLowerCase());
                        
            Set<String> extSet = new HashSet<String>();
            Set<String> newTotalSet = new HashSet<String>();
            
            for ( int i=0 ; i<totalExtensions.length ; i++ ) {
                extSet.add(totalExtensions[i]);
                newTotalSet.add(totalExtensions[i]);
            }

            for ( int i=0 ; i<custom.length ; i++ ) {
                if (custom[i].length() > 0) {
                    if (!contains(totalExtensions, custom[i]))
                    customExts.add(custom[i]);
                }
            }
            
            for ( int i=0 ; i<unselected.length ; i++ ) {
                extSet.remove(unselected[i]);
                
                if (customExts.contains(unselected[i])) {
                    this.customUnchecked.add(unselected[i]);
                }
            }

            
            totalExtensions = newTotalSet.toArray(new String[newTotalSet.size()]);
                        
            selectedExtensions = new String[extSet.size()];
            selectedExtensions = extSet.toArray(selectedExtensions);
        }
                
        Map<MediaType, List<String>> extensionsByType = createExtensionsMap(selectedExtensions, customExts);
        Map<MediaType, List<String>> defaultsByType   = createExtensionsMap(totalExtensions, customExts);

        mediaKeys = new TreeSet<MediaType>();
        Set<MediaType> s; 
        if ((s = extensionsByType.keySet()) != null) {        
            mediaKeys.addAll(s);
        }
        if ((s = defaultsByType.keySet()) != null) {        
            mediaKeys.addAll(s);
        }

        this.panels = new LinkedHashMap<MediaType,CheckBoxList<String>>();
        this.mediaUnchecked = new HashSet<MediaType>();
        this.currentKey = null;

        PanelsCheckChangeListener refreshListener = new PanelsCheckChangeListener(this);
        
        for (MediaType key : mediaKeys) {

            if (this.currentKey == null) {
                this.currentKey = key;
            }
            
            List<String> list = extensionsByType.get(key);
            List<String> defList = defaultsByType.get(key);

            
            Set<String> total = new TreeSet<String>();
            Set<String> notSelected = new TreeSet<String>();

            if (defList != null) {
                total.addAll(defList);
                notSelected.addAll(defList);
            }

            if (list != null) {
                total.addAll(list);
                notSelected.removeAll(list);
            }

            
            CheckBoxList<String> newPanel = new CheckBoxList<String>(total, notSelected, extensionProvider,
                    CheckBoxList.SELECT_FIRST_OFF);
            
            newPanel.setDisabledTooltip(I18n.tr("To allow selection enable sharing of sensitive types."));
            newPanel.setCheckChangeListener(refreshListener);
            
            if (total.equals(notSelected)) {
                newPanel.setEnabled(false);
                this.mediaUnchecked.add(key);
            }
            
            this.panels.put(key, newPanel);
        }
        
        this.insertNewCustomPanel(customExts);
        
        this.originalExtensions = this.getExtensions();
    }

    
    public void buildUI() {
        
        this.mainContainer.removeAll();
        this.currentPanel.removeAll();
        
        
        for (MediaType key : mediaKeys) {
            this.currentPanel.add(this.panels.get(key), key.toString());
        }
        
        this.sidePanel = new CheckBoxList<MediaType>(this.mediaKeys, this.mediaUnchecked,
                new MediaProvider(), new MediaExtrasProvider(this.panels),
                CheckBoxList.SELECT_FIRST_ON);
        
        this.sidePanel.setPreferredSize(new Dimension(150, 0));
        this.sidePanel.setSelectionListener(new SideSelectListener(this));
        this.sidePanel.setCheckChangeListener(new CheckChangeListener(this));
        this.sidePanel.setItemSelected(this.currentKey);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                this.sidePanel, this.currentPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(180);
        
        this.mainContainer.add(splitPane, BorderLayout.CENTER);
        
        this.addBottomPanel();
        
        this.shareProtect(!SharingSettings.DISABLE_SENSITIVE.getValue());
        
        this.mainContainer.validate();

    }
    
    /** 
     * Turn on or off sensitive file protection on the given subset of files
     *  as specified in SharingSettings
     */
    private void shareProtect(boolean state) {

        String[] disabled = SharingSettings.getDefaultDisabledExtensions();
        
        Set<String> totalDisabled = new HashSet<String>();
                
        for( String item : disabled ) {
            totalDisabled.add(item);
        }
        
        for ( CheckBoxList<String> panel : this.panels.values() ) {
            panel.setItemsEnabled(totalDisabled, state);
        }

        this.refreshSidePanel();
    }
    

    
    
    
    private static Map<MediaType, List<String>> createExtensionsMap(String[] extensions, Set<String> ignoreList) {

        if (extensions == null || extensions.length == 0) {
            return Collections.emptyMap();
        }
        Map<MediaType, List<String>> extensionsByType = new LinkedHashMap<MediaType, List<String>>();

        for (String extension : extensions) { 
            MediaType nm = MediaType.getMediaTypeForExtension(extension);
            if (nm == null) {
                nm = otherMedia;
            }
            if (!extensionsByType.containsKey(nm)) {
                extensionsByType.put(nm, new ArrayList<String>(8));
            }
            List<String> typeExtension = extensionsByType.get(nm);
            if (   !typeExtension.contains(extension)
                && (ignoreList == null || !ignoreList.contains(extension))) {
                typeExtension.add(extension);
            }
        }
        
        return extensionsByType;
    }
    
    private String getExtensions() {
        Set<String> elements = new HashSet<String>();
        
        for ( CheckBoxList<String> panel : this.panels.values() ) {
             elements.addAll(panel.getCheckedElements());
        }
         
        String[] array = elements.toArray(new String[elements.size()]);
        return StringArraySetting.encode(array);
    }
    
    
    private String getUncheckedExtensions() {
        Set<String> elements = new HashSet<String>();
        
        for ( CheckBoxList<String> panel : this.panels.values() ) {
             elements.addAll(panel.getUncheckedElements());
        }
         
        String[] array = elements.toArray(new String[elements.size()]);
        return StringArraySetting.encode(array);
    }
    
    private static boolean contains(Object[] list, Object value) {
        for ( int i=0 ; i<list.length ; i++ ) {
            if (list[i].equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean checkExt(String text) {
        if (text == null) {
            return false;
        }
        
        String malformedMsg = I18n.tr("The extension name was not valid, could not add.");
        
        if (   text.length() == 0 
            || text.length() > MAX_EXT_LENGTH
           ) {                            
            JOptionPane.showMessageDialog(this.parent, malformedMsg);
            return false;
        }
        
        
           
        
        if (contains(SharingSettings.getDefaultExtensions(), text)) {
            MediaType type = MediaType.getMediaTypeForExtension(text);
            
            if (type == null) {
                type = otherMedia;
            }
            
            CheckBoxList<String> panel = this.panels.get(type); 
            
            if (panel == null) {
                JOptionPane.showMessageDialog(this.parent, malformedMsg);
                return false;    
            }
         
            this.switchPanel(type);
            panel.setItemChecked(text);
            this.sidePanel.setItemSelected(type);
            
            return false;
        }

        if (this.customPanel.getElements().contains(text)) {
            CheckBoxList<String> panel = this.panels.get(this.customKey); 
            
            if (panel == null) {
                JOptionPane.showMessageDialog(this.parent, malformedMsg);
                return false;    
            }
            
            this.switchPanel(this.customKey);
            panel.setItemChecked(text);
            this.sidePanel.setItemSelected(this.customKey);
            
            return false;
        }
        
        
        return true;
    }

    
    private class AddExtAction extends AbstractAction {
        
        public AddExtAction() {
            putValue(Action.NAME, I18n.tr("Add New Extension"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Add a New Extension to My Extensions"));
        }
        
        public void actionPerformed(ActionEvent e) {
            String text = JOptionPane.showInputDialog(parent, I18n.tr("Enter the extension pattern:"), 
                    I18n.tr("Extension Sharing Settings"), JOptionPane.DEFAULT_OPTION);
            
            if (text != null) {
                text = text.toLowerCase();
            }
            else {
                return;
            }        
            
            if (checkExt(text)) {                           
                addCustomExt(text);
            }
        }
    }
    
    private class RestoreAction extends AbstractAction {
        
        public RestoreAction() {
            putValue(Action.NAME, I18n.tr("Restore Defaults"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share the Default File Extensions"));
        }
        
        public void actionPerformed(ActionEvent e) {
            int answer = JOptionPane.showConfirmDialog(parent, 
                    new Object[] {new MultiLineLabel(I18n
                            .tr("This options clears any extension sharing changes you made and sets LimeWire's extension sharing preferences to the original preferences. Do you wish to continue?"), 300)},
                            I18n.tr("Extension Sharing Settings"), JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                revert();
            }               
        }
    }
    
    private void addBottomPanel() {

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        
        this.disableSensitive = new JCheckBox(I18n.tr("Do Not Share Sensitive File Types"));        
        this.disableSensitive.setToolTipText(I18n.tr("This stops LimeWire from sharing certain files that may contain sensitive information."));
        this.disableSensitive.setSelected(SharingSettings.DISABLE_SENSITIVE.getValue());
        
        JPanel buffer = new JPanel(new BorderLayout());
        buffer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        this.disableSensitive.setBorder(null);
        buffer.add(this.disableSensitive);
  
        
        bottomPanel.add(new JButton(new RestoreAction()), BorderLayout.WEST);
        bottomPanel.add(buffer, BorderLayout.SOUTH);
        bottomPanel.add(new JButton(new AddExtAction()), BorderLayout.EAST);
        
        this.disableSensitive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shareProtect(!((JCheckBox)e.getSource()).isSelected());            
            }
        });
                
        this.mainContainer.add(bottomPanel, BorderLayout.SOUTH);
       
    }

    private void insertNewCustomPanel(Set<String> customExts) {
        
        this.customPanel = new CheckBoxList<String>(customExts, this.customUnchecked,
                                   extensionProvider, CheckBoxList.SELECT_FIRST_OFF);
        
        this.customPanel.setRemovable(true);
        this.customPanel.setCheckChangeListener(new PanelsCheckChangeListener(this));
        
        this.currentPanel.add(this.customPanel, this.customKey.toString());
        
        this.mediaKeys.add(this.customKey);
        
        this.panels.put(this.customKey, this.customPanel);
    }
    
     

    // Methods to integrate with PaneItem
    
    public void initOptions() {
        this.initCore();
        this.buildUI();
    }

    public boolean applyOptions() {
      
        String newList = this.getExtensions();
              
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(newList);
        GuiCoreMediator.getFileManager().loadSettings();
               
        SharingSettings.DISABLE_SENSITIVE.setValue(
                   this.disableSensitive == null 
                || this.disableSensitive.isSelected());
        
        // Save new database
        
        String[] customArray = new String[this.customPanel.getElements().size()];
        customArray = this.customPanel.getElements().toArray(customArray);
        
        SharingSettings.EXTENSIONS_MIGRATE.setValue(false);
        SharingSettings.EXTENSIONS_LIST_UNSHARED.setValue(getUncheckedExtensions());
        SharingSettings.EXTENSIONS_LIST_CUSTOM.setValue(StringArraySetting.encode(customArray)); 
        
        return false;
    }

    public boolean isDirty() {
        return    !this.originalExtensions.equals(this.getExtensions())
               || !this.disableSensitive.isSelected() == SharingSettings.DISABLE_SENSITIVE.getValue();
    }

  
    
    
    
    
    
    
    
    
    // Listener Classes
    
    private static class SideSelectListener implements
            CheckBoxListSelectionListener {

        private FileTypeOptionPanelManager parent;

        public SideSelectListener(FileTypeOptionPanelManager parent) {
            this.parent = parent;
        }

        public void valueChanged(CheckBoxListSelectionEvent e) {
            parent.switchPanel((MediaType)e.getSelected());
        }
    }

    private static class CheckChangeListener implements
            CheckBoxListCheckChangeListener<MediaType> {

        private FileTypeOptionPanelManager parent;

        public CheckChangeListener(FileTypeOptionPanelManager parent) {
            this.parent = parent;
        }

        public void valueChanged(CheckBoxListCheckChangeEvent<MediaType> e) {
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

    
    
    
    // Providers   
    
    private static class ExtensionProvider implements CheckBoxList.TextProvider<String> {
        
        private Set<String> mediaNames;
        
        public ExtensionProvider() {
            mediaNames = new HashSet<String>();
            for ( MediaType mt : MediaType.getDefaultMediaTypes() ) {
                mediaNames.add(mt.toString()); // TODO: should be the name, need to confirm
            }
        }
        
        public String getText(String obj) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }
            
            return obj;
        }
        
        public String getToolTipText(String obj) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }
            
            Icon icon = null;// TODO: IconManager.instance().getIconForExtension(obj);
            
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

        public Icon getIcon(String obj) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }
            
            Icon icon = null; // TODO: IconManager.instance().getIconForExtension(obj);
            return icon != null ? icon : new GUIUtils.EmptyIcon(obj, 16, 16);
        }
        
    }
    
    
    private static class MediaProvider 
        implements CheckBoxList.TextProvider<MediaType> {
        
        public String getText(MediaType obj) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }
            
            if (CUSTOM.equals(obj.toString())) { // TODO: = old getName()??
                return CUSTOM_NAME;
            }
            
            if (OTHER.equals(obj.toString())) { // TODO: = old getName()??
                return OTHER_NAME;
            }
            
            return obj.toString(); // TODO: = old getName()??
        }
        
        public String getToolTipText(MediaType obj) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }
            
            if (obj.toString() == null) { // TODO: = old getName()??
                return null; 
            }
            
            if (CUSTOM.equals(obj.toString())) { // TODO: = old getName()??
                return I18n.tr("File types you added to share.");
            } 
            
            if (OTHER.equals(obj.toString())) { // TODO: = old getName()??
                return I18n.tr("Other types of files.");
            }
            
            return obj.getDescriptionKey(); // TODO: obj.getMediaType().getDescriptionKey(); 
        }

        public Icon getIcon(MediaType obj) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }

            if      (CUSTOM.equals(obj.toString())) {  // TODO: = old getName()??
                Icon icon = GUIMediator.getThemeImage("custom");
                if (icon != null) {
                    return icon;
                }
            }
            else if (OTHER.equals(obj.toString())) {  // TODO: = old getName()??
                Icon icon = GUIMediator.getThemeImage("lime");
                if (icon != null) {
                    return icon;
                }
            }
            
            return null; // TODO obj.getIcon();
        }
    }
        
    private static class MediaExtrasProvider
        implements CheckBoxList.ExtrasProvider<MediaType> {
        
        Map<MediaType,CheckBoxList<String>> panels;
        
        public MediaExtrasProvider(Map<MediaType,CheckBoxList<String>> panels) {
            this.panels = panels;
        }        
        
        public boolean isSeparated(MediaType obj) {
            return customMedia.equals(obj);
        }

        public String getComment(MediaType obj) {
            CheckBoxList<String> panel = panels.get(obj);
            
            if (panel == null) {
                return "(0)";
            }
            
            return "(" + panel.getCheckedElements().size() + ")";
        }
    }    

}

package org.limewire.ui.swing.options;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.ui.swing.options.actions.ApplyOptionAction;
import org.limewire.ui.swing.options.actions.CancelOptionAction;
import org.limewire.ui.swing.options.actions.TabAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Main Dialog for the Options
 */
@Singleton
public class OptionsDialog extends JDialog implements OptionsTabNavigator {
    
    @Resource
    private Color headerColor;
    @Resource
    private Color dividerColor;
    @Resource
    private Color backgroundColor;
    
    private static final String LIBRARY = I18n.tr("Library");
    private static final String SEARCH = I18n.tr("Search");
    private static final String DOWNLOADS = I18n.tr("Downloads");
    private static final String SECURITY = I18n.tr("Security");
    private static final String MISC = I18n.tr("Misc");
    private static final String ADVANCED = I18n.tr("Advanced");
    
    private LibraryOptionPanel libraryOptionPanel;
    private SearchOptionPanel searchOptionPanel;
    private DownloadOptionPanel downloadOptionPanel;
    private SecurityOptionPanel securityOptionPanel;
    private MiscOptionPanel miscOptionPanel;
    private AdvancedOptionPanel advancedOptionPanel;
    
    private Map<String, OptionTabItem> cards = new HashMap<String,OptionTabItem>();
    private Map<String, OptionPanel> panels = new HashMap<String, OptionPanel>();
    private OptionTabItem selectedItem;
    
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    private JPanel headerPanel;
    private JPanel footerPanel;
    
    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    
    @Inject
    public OptionsDialog(AppFrame appFrame, LibraryOptionPanel libraryOptionPanel, SearchOptionPanel searchOptionPanel, 
            DownloadOptionPanel downloadOptionPanel, SecurityOptionPanel securityOptionPanel, MiscOptionPanel miscOptionPanel,
            AdvancedOptionPanel advancedOptionPanel) { 
        super(GuiUtils.getMainFrame(), I18n.tr("Options"));

        GuiUtils.assignResources(this); 
        
        this.libraryOptionPanel = libraryOptionPanel;
        this.searchOptionPanel = searchOptionPanel;
        this.downloadOptionPanel = downloadOptionPanel;
        this.securityOptionPanel = securityOptionPanel;
        this.miscOptionPanel = miscOptionPanel;
        this.advancedOptionPanel = advancedOptionPanel;

        setSize(700,600);
        setPreferredSize(new Dimension(700,600));
        setResizable(false);
        setModalityType(ModalityType.APPLICATION_MODAL);
        
        setDefaultCloseOperation(2);
        
        createComponents();
        
        initOptions();
        
        pack();
    }
    
    private void initOptions() {
        for(OptionPanel panel : panels.values()) {
            panel.initOptions();
        }
    }
    
    public void applyOptions() {
        for(OptionPanel panel : panels.values()) {
            panel.applyOptions();
        }
        
        //TODO: more checks here. Look at OptionsPaneManager.applyOptions
    }
    
    private void createComponents() {
        setLayout(new MigLayout("gap 0, insets 0 0 0 0, fill", "fill", "[50!, fill][fill][40!, fill]"));
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);
        
        headerPanel = new JPanel();
        headerPanel.setBackground(headerColor);
        
        footerPanel = new JPanel();
        
        add(headerPanel, "wrap");
        add(cardPanel, "wrap");
        add(footerPanel);
        
        createPanels();
        createFooter();
        createHeader();
        
        select(LIBRARY);
    }
    
    private void createPanels() {
        panels.put(LIBRARY, libraryOptionPanel);
        panels.put(SEARCH, searchOptionPanel);
        panels.put(DOWNLOADS, downloadOptionPanel);
        panels.put(SECURITY, securityOptionPanel);
        panels.put(MISC, miscOptionPanel);
        panels.put(ADVANCED, advancedOptionPanel);
        
        cardPanel.add(panels.get(LIBRARY), LIBRARY);
        cardPanel.add(panels.get(SEARCH), SEARCH);
        cardPanel.add(panels.get(DOWNLOADS), DOWNLOADS);
        cardPanel.add(panels.get(SECURITY), SECURITY);
        cardPanel.add(panels.get(MISC), MISC);
        cardPanel.add(panels.get(ADVANCED), ADVANCED);
        
        for(OptionPanel panel : panels.values()) 
            panel.setBackground(backgroundColor);
    }
    
    private void createHeader() {
        headerPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0"));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, dividerColor));
        
        headerPanel.add(new FancyOptionTabButton(new TabAction(null, addOptionTab(LIBRARY, this))));
        headerPanel.add(new FancyOptionTabButton(new TabAction(null, addOptionTab(SEARCH, this))));
        headerPanel.add(new FancyOptionTabButton(new TabAction(null, addOptionTab(DOWNLOADS, this))));
        headerPanel.add(new FancyOptionTabButton(new TabAction(null, addOptionTab(SECURITY, this))));
        headerPanel.add(new FancyOptionTabButton(new TabAction(null, addOptionTab(MISC, this))));
        headerPanel.add(new FancyOptionTabButton(new TabAction(null, addOptionTab(ADVANCED, this))));
    }
    
    private void createFooter() {
        footerPanel.setLayout(new MigLayout());
        footerPanel.setBackground(backgroundColor);
        
        helpButton = new JButton(I18n.tr("Help"));
        helpButton.setPreferredSize(new Dimension(50,30));
        
        okButton = new JButton(I18n.tr("OK"));
        okButton.setPreferredSize(new Dimension(60,30));
        okButton.addActionListener(new ApplyOptionAction(this));
        
        cancelButton = new JButton(I18n.tr("Cancel"));
        cancelButton.setPreferredSize(new Dimension(60,30));
        cancelButton.addActionListener(new CancelOptionAction(this));
        
        footerPanel.add(helpButton, "push");
        footerPanel.add(okButton);
        footerPanel.add(cancelButton);
    }

    @Override
    public OptionTabItem addOptionTab(final String title, final OptionsTabNavigator navigator) {
        return new OptionsTabItemImpl(title, navigator);
    }
    

    @Override
    public void select(String title) {
        if(selectedItem != null)
            ((OptionsTabItemImpl)selectedItem).fireSelected(false);
        selectedItem = cards.get(title);
        ((OptionsTabItemImpl)selectedItem).fireSelected(true);
        cardLayout.show(cardPanel, title);
    }
    
    private class OptionsTabItemImpl implements OptionTabItem {

        private final List<TabItemListener> listeners = new CopyOnWriteArrayList<TabItemListener>();
        private final String name;
        private final OptionsTabNavigator navigator;
        
        public OptionsTabItemImpl(String title, OptionsTabNavigator navigator) {
            this.name = title;
            this.navigator = navigator;

            cards.put(title, this);
        }
        
        @Override
        public String getId() {
            return name;
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public void select() {
            navigator.select(name);
        }

        @Override
        public void addTabItemListener(TabItemListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeTabItemListener(TabItemListener listener) {
            listeners.remove(listener);
        }
        
        public void fireSelected(boolean selected) {
            for(TabItemListener listener : listeners) {
                listener.itemSelected(selected);
            }
        }
    }
}

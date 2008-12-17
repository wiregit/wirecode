package org.limewire.ui.swing.options;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.ui.swing.options.actions.ApplyOptionAction;
import org.limewire.ui.swing.options.actions.CancelOptionAction;
import org.limewire.ui.swing.options.actions.HelpAction;
import org.limewire.ui.swing.options.actions.TabAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Main Dialog for the Options
 */
public class OptionsDialog extends LimeJDialog implements OptionsTabNavigator {
    
    @Resource
    private Color headerColor;
    @Resource
    private Color dividerColor;
    @Resource
    private Color backgroundColor;
    @Resource
    private Icon securityIcon;
    @Resource
    private Icon advancedIcon;
    @Resource
    private Icon downloadsIcon;
    @Resource
    private Icon libraryIcon;
    @Resource
    private Icon miscIcon;
    @Resource
    private Icon searchIcon;
    
    
    private static final String LIBRARY = I18n.tr("Library");
    private static final String SEARCH = I18n.tr("Search");
    private static final String DOWNLOADS = I18n.tr("Downloads");
    private static final String SECURITY = I18n.tr("Security");
    private static final String MISC = I18n.tr("Misc");
    private static final String ADVANCED = I18n.tr("Advanced");
    
    private Provider<LibraryOptionPanel> libraryOptionPanel;
    private Provider<SearchOptionPanel> searchOptionPanel;
    private Provider<DownloadOptionPanel> downloadOptionPanel;
    private Provider<SecurityOptionPanel> securityOptionPanel;
    private Provider<MiscOptionPanel> miscOptionPanel;
    private Provider<AdvancedOptionPanel> advancedOptionPanel;
    
    private Map<String, OptionTabItem> cards = new HashMap<String,OptionTabItem>();
    private Map<String, OptionPanel> panels = new HashMap<String, OptionPanel>();
    private List<String> list = new ArrayList<String>();
    private OptionTabItem selectedItem;
    
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    private JPanel headerPanel;
    private JPanel footerPanel;
    
    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    
    @Inject
    public OptionsDialog(Provider<LibraryOptionPanel> libraryOptionPanel, Provider<SearchOptionPanel> searchOptionPanel,
            Provider<DownloadOptionPanel> downloadOptionPanel, Provider<SecurityOptionPanel> securityOptionPanel,
            Provider<MiscOptionPanel> miscOptionPanel, Provider<AdvancedOptionPanel> advancedOptionPanel,
            AppFrame appFrame) {
        super(appFrame.getMainFrame(), I18n.tr("Options"), true);

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
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        createComponents();
        
        pack();
    }
    
    public void applyOptions() {
        boolean restartRequired = false;
        for(OptionPanel panel : panels.values()) {
            restartRequired |= panel.applyOptions();
        }
        
        //TODO: more checks here. Look at OptionsPaneManager.applyOptions

        // if at least one option requires a restart before taking effect, notify user
        if (restartRequired) {
            FocusJOptionPane.showMessageDialog(this,
                            I18n.tr("One or more options will take effect the next time LimeWire is restarted."),
                            I18n.tr("Message"),
                            JOptionPane.INFORMATION_MESSAGE);
        }

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
        
        createFooter();
        createHeader();
        
        select(LIBRARY);
    }
    
    private void createHeader() {
        headerPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0"));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, dividerColor));
        
        
        MoveDown down = new MoveDown();
        MoveUp up = new MoveUp();
        
        createButton(LIBRARY, libraryIcon, libraryOptionPanel, down, up);
        createButton(SEARCH, searchIcon, searchOptionPanel, down, up);
        createButton(DOWNLOADS, downloadsIcon, downloadOptionPanel, down, up);
        createButton(SECURITY, securityIcon, securityOptionPanel, down, up);
        createButton(MISC, miscIcon, miscOptionPanel, down, up);
        createButton(ADVANCED, advancedIcon, advancedOptionPanel, down, up);
    }
    
    private void createButton(String title, Icon icon,  Provider<? extends OptionPanel> provider, MoveDown down, MoveUp up) {
        FancyOptionTabButton button = new FancyOptionTabButton(new TabAction(icon, addOptionTab(title, this, provider)));
        
        button.getActionMap().put(MoveDown.KEY, down);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), MoveDown.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), MoveDown.KEY);
        
        button.getActionMap().put(MoveUp.KEY, up);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), MoveUp.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), MoveUp.KEY);
        
        headerPanel.add(button);
    }
    
    private class MoveDown extends AbstractAction {
        final static String KEY = "MOVE_DOWN";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            next();
        }
    }
    
    private class MoveUp extends AbstractAction {
        final static String KEY = "MOVE_UP";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            previous();
        }
    }
    
    private void createFooter() {
        footerPanel.setLayout(new MigLayout());
        footerPanel.setBackground(backgroundColor);
        
        helpButton = new JButton(new HelpAction());
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
    public OptionTabItem addOptionTab(final String title, final OptionsTabNavigator navigator, final Provider<? extends OptionPanel> optionProvider) {
        return new OptionsTabItemImpl(title, navigator, optionProvider);
    }
    
    public void next() {
        for(int i = 0; i < list.size(); i++) {
            if(list.get(i).equals(selectedItem.getId())) {
                if(i == list.size() -1) {
                    select(list.get(0));
                } else
                    select(list.get(i+1));
                break;
            }
        }
    }
    
    public void previous() {
        for(int i = 0; i < list.size(); i++) {
            if(list.get(i).equals(selectedItem.getId())) {
                if(i == 0) {
                    select(list.get(list.size()-1));
                } else
                    select(list.get(i-1));
                break;
            }
        }
    }

    @Override
    public void select(String title) {
        if(selectedItem != null)
            ((OptionsTabItemImpl)selectedItem).fireSelected(false);
        selectedItem = cards.get(title);
        if(!panels.containsKey(title)) {
            createPanel(selectedItem.getId(), selectedItem.getOptionPanel());
        }
        ((OptionsTabItemImpl)selectedItem).fireSelected(true);
        cardLayout.show(cardPanel, title);
    }
    
    /**
     * Lazily loads and inits a subPanel in the OptionDialog
     */
    private void createPanel(String id, OptionPanel panel) {
        panel.setBackground(backgroundColor);
        panels.put(selectedItem.getId(), panel);
        cardPanel.add(panels.get(selectedItem.getId()), selectedItem.getId());
        panel.initOptions();
    }
    
    private class OptionsTabItemImpl implements OptionTabItem {

        private final List<TabItemListener> listeners = new CopyOnWriteArrayList<TabItemListener>();
        private final String name;
        private final OptionsTabNavigator navigator;
        private final Provider<? extends OptionPanel> provider;
        
        public OptionsTabItemImpl(String title, OptionsTabNavigator navigator, Provider<? extends OptionPanel> optionProvider) {
            this.name = title;
            this.navigator = navigator;
            this.provider = optionProvider;
            
            cards.put(title, this);
            list.add(title);
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
        
        public OptionPanel getOptionPanel() {
            return provider.get();
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

    /**
     * Recalls init options on all created panels.
     */
    public void initOptions() {
        for(OptionPanel optionPanel : panels.values()) {
            optionPanel.initOptions();
        }
    }
}

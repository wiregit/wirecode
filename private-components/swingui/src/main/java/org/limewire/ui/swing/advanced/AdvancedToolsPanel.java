package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.options.TabItemListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Main content panel for the Advanced Tools window.  The window is displayed
 * by calling the <code>display(String)</code> method. 
 */
@Singleton
public class AdvancedToolsPanel extends JPanel {
    /** Identifier for Advanced Tools window. */
    private static final String WINDOW_TITLE = I18nMarker.marktr("Advanced Tools");
    /** Identifier for Connections tab. */
    public static final String CONNECTIONS = I18nMarker.marktr("Connections");
    /** Identifier for Logging tab. */
    public static final String LOGGING = I18nMarker.marktr("Logging");
    /** Identifier for Console tab. */
    public static final String CONSOLE = I18nMarker.marktr("Console");

    @Resource
    private Color headerColor;
    @Resource
    private Color dividerColor;
    @Resource
    private Color backgroundColor;

    /** Action to select next tab. */
    private Action nextTabAction = new NextTabAction();
    /** Action to select previous tab. */
    private Action prevTabAction = new PrevTabAction();
    /** Window listener for main frame. */
    private WindowListener mainFrameListener = new MainFrameListener();
    /** Currently selected tab item. */
    private AdvancedTabItem selectedItem;
    /** Map containing tab items. */
    private Map<String, AdvancedTabItem> tabItemMap = new HashMap<String, AdvancedTabItem>();
    /** Map containing tab panels created. */
    private Map<String, JPanel> tabPanelMap = new HashMap<String, JPanel>();
    /** List of tab identifiers. */
    private List<String> tabIdList = new ArrayList<String>();
    /** Window used to display the panel. */
    private Window window;
    
    private JPanel headerPanel;
    
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    /**
     * Constructs a window panel for the Advanced Tools window that uses the 
     * injected Provider instances to create the tab content panels.
     */
    @Inject
    public AdvancedToolsPanel(Provider<ConnectionsPanel> connectionsPanel, 
        Provider<LoggingPanel> loggingPanel, 
        Provider<ConsolePanel> consolePanel) {

        // Inject annotated resource values.
        GuiUtils.assignResources(this);

        // Initialize components.
        initComponents();
        
        // Add tabs to dialog.
        addTab(CONNECTIONS, connectionsPanel);
        addTab(LOGGING, loggingPanel);
        addTab(CONSOLE, consolePanel);
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setLayout(new BorderLayout());

        // Create header panel to hold tab buttons.
        headerPanel = new JPanel();
        headerPanel.setBackground(headerColor);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, dividerColor));
        headerPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0"));
        
        // Create panel to hold tab content panels.
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);
        
        add(headerPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
    }

    /**
     * Adds a tab to the dialog using the specified identifier and panel
     * provider.  The identifier should be marked for translation by a prior
     * call to I18nMarker.marktr(). 
     */
    private void addTab(String id, Provider<? extends JPanel> provider) {
        // Create tab item and add to map.
        AdvancedTabItem tabItem = new AdvancedTabItem(id, provider);
        this.tabItemMap.put(id, tabItem);
        
        // Add tab identifier to list.
        this.tabIdList.add(id);
        
        // Add button to header.
        headerPanel.add(createButton(tabItem));
    }

    /**
     * Creates a tab button for the specified tab item.
     */
    private JButton createButton(TabItem tabItem) {
        // Create the tab button.  The TabAction instance translates the tab 
        // item identifier into an I18N display name.
        TabButton button = new TabButton(new TabAction(tabItem, null));
        
        // Add inputs and action to select previous tab.
        button.getActionMap().put(PrevTabAction.KEY, this.prevTabAction);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), PrevTabAction.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), PrevTabAction.KEY);
        
        // Add inputs and action to select next tab.
        button.getActionMap().put(NextTabAction.KEY, this.nextTabAction);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NextTabAction.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), NextTabAction.KEY);

        return button;
    }

    /**
     * Displays this panel in a modeless window.
     * @param tabId identifier for initial tab, either CONNECTIONS, LOGGING, or
     *  CONSOLE
     */
    public void display(String tabId) {
        if (this.window == null) {
            // Create modeless window.
            JFrame frame = new LimeJFrame(I18n.tr(WINDOW_TITLE));

            // Set window properties. 
            frame.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            frame.setPreferredSize(new Dimension(800, 600));
            frame.setResizable(true);
            
            // Add listener to handle system menu close action.
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    disposeWindow();
                }
            });
            
            // Add listener to handle parent window events.
            GuiUtils.getMainFrame().addWindowListener(this.mainFrameListener);

            // Set content pane and validate.
            frame.setContentPane(this);
            frame.pack();

            // Set window position.
            frame.setLocationRelativeTo(GuiUtils.getMainFrame());
            
            // Save window reference.
            this.window = frame;

            // Select default tab.
            select(tabId);
        }

        // Display window.
        this.window.setVisible(true);
        this.window.toFront();
    }
    
    /**
     * Closes the window that is displaying this panel.
     */
    public void disposeWindow() {
        if (this.window != null) {
            // Remove window listener from main GUI frame.
            GuiUtils.getMainFrame().removeWindowListener(this.mainFrameListener);
            
            // Dispose of this window and clear reference.
            this.window.dispose();
            this.window = null;
        }
    }
    
    /**
     * Minimizes the window that is displaying this panel.
     * @param visible true if minimized window remains visible in taskbar
     */
    public void minimizeWindow(boolean visible) {
        if (this.window instanceof Frame) {
            ((Frame) this.window).setExtendedState(Frame.ICONIFIED);
            this.window.setVisible(visible);
        }
    }
    
    /**
     * Restores the window that is displaying this panel.
     */
    public void restoreWindow() {
        if (this.window instanceof Frame) {
            ((Frame) this.window).setExtendedState(Frame.NORMAL);
            this.window.setVisible(true);
        }
    }
    
    /**
     * Selects the tab item with the specified identifier.
     * @param tabId the identifier of the tab
     */
    private void select(String tabId) {
        // De-select current item.
        if (selectedItem != null) {
            selectedItem.fireSelected(false);
        }
        
        // Get selected item.
        selectedItem = tabItemMap.get(tabId);

        // Lazily create tab panel and add to dialog.  
        if(!tabPanelMap.containsKey(tabId)) {
            JPanel panel = selectedItem.getTabPanel();
            panel.setBackground(backgroundColor);
            tabPanelMap.put(tabId, panel);
            cardPanel.add(panel, tabId);
        }
        
        // Select new item.
        selectedItem.fireSelected(true);

        // Display selected tab panel.
        cardLayout.show(cardPanel, tabId);
    }

    /**
     * Selects the next tab item.
     */
    private void selectNext() {
        // Find the selected tab, and select the next one.
        for (int i = 0; i < tabIdList.size(); i++) {
            if (tabIdList.get(i).equals(selectedItem.getId())) {
                int nextTab = (i + 1) % tabIdList.size();
                select(tabIdList.get(nextTab));
                break;
            }
        }
    }
    
    /**
     * Selects the previous tab item.
     */
    private void selectPrev() {
        // Find the selected tab, and select the previous one.
        for (int i = 0; i < tabIdList.size(); i++) {
            if (tabIdList.get(i).equals(selectedItem.getId())) {
                int prevTab = ((i == 0) ?  tabIdList.size() : i) - 1;
                select(tabIdList.get(prevTab));
                break;
            }
        }
    }
    
    /**
     * A TabItem for the Advanced Tools dialog. 
     */
    private class AdvancedTabItem extends AbstractTabItem {
        
        private final Provider<? extends JPanel> provider;

        /**
         * Constructs a tab item using the specified identifier and panel 
         * provider.
         */
        public AdvancedTabItem(String id, Provider<? extends JPanel> provider) {
            super(id);
            this.provider = provider;
        }

        @Override
        public void select() {
            AdvancedToolsPanel.this.select(getId());
        }
        
        public JPanel getTabPanel() {
            return this.provider.get();
        }
    }

    /**
     * An Action associated with a tab button.  This updates the "selected" 
     * value when its associated tab item is selected or de-selected.
     */
    private class TabAction extends AbstractAction {

        /**
         * Constructs a TabAction with the specified TabItem and icon.  The
         * value returned by tabItem.getId() must be marked for translation.
         */
        public TabAction(TabItem tabItem, Icon icon) {
            super(I18n.tr(tabItem.getId()), icon);
            
            // Store identifier as action command.
            putValue(Action.ACTION_COMMAND_KEY, tabItem.getId());

            // Install listener to change state when tab item is selected. 
            tabItem.addTabItemListener(new TabItemListener() {
                @Override
                public void itemSelected(boolean selected) {
                    putValue(SELECTED_KEY, selected);
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            select((String) getValue(Action.ACTION_COMMAND_KEY));
        }
    }

    /**
     * An Action that handles events to select the next tab.
     */
    private class NextTabAction extends AbstractAction {
        final static String KEY = "NEXT";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            selectNext();
        }
    }
    
    /**
     * An Action that handles events to select the previous tab.
     */
    private class PrevTabAction extends AbstractAction {
        final static String KEY = "PREV";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            selectPrev();
        }
    }
    
    /**
     * A listener that handles window events on the main GUI frame.  This is
     * used to minimize or restore the Advanced Tools window when the main GUI
     * frame is minimized or restored. 
     */
    private class MainFrameListener extends WindowAdapter {
        
        public void windowClosed(WindowEvent e) {
            disposeWindow();
        }
        
        public void windowDeiconified(WindowEvent e) {
            restoreWindow();
        }
        
        public void windowIconified(WindowEvent e) {
            minimizeWindow(e.getWindow().isVisible());
        }
    }
}

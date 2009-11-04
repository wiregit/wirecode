package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.network.BandwidthCollector;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.ClearFinishedDownloadAction;
import org.limewire.ui.swing.downloads.DownloadHeaderPopupMenu;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.downloads.FixStalledDownloadAction;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.mainframe.BottomPanel.TabId;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Control panel that is displayed above the downloads/uploads tables.
 */
public class BottomHeaderPanel {

    @Resource private Icon moreButtonArrow;
    @Resource private Icon scrollPaneNubIcon;
    @Resource private Font hyperlinkFont;
    @Resource private Color highlightBackground;
    @Resource private Color highlightBorderColor;
    @Resource private Color selectionTopGradientColor;
    @Resource private Color selectionBottomGradientColor;
    @Resource private Color selectionTopBorderColor;
    @Resource private Color selectionBottomBorderColor;
    @Resource private Font textFont;
    @Resource private Color textForeground;
    @Resource private Color textSelectedForeground;

    private final DownloadMediator downloadMediator;    
    private final DownloadHeaderPopupMenu downloadHeaderPopupMenu;
    private final ClearFinishedDownloadAction clearFinishedDownloadAction;
    private final FixStalledDownloadAction fixStalledDownloadAction;
    private final UploadMediator uploadMediator;
    private final ComboBoxDecorator comboBoxDecorator;
    private final BandwidthCollector bandwidthCollector;
    private final BottomPanel bottomPanel;
    
    private final JXPanel component;
    private JComponent dragComponent;

    private Map<TabId, Action> actionMap = new EnumMap<TabId, Action>(TabId.class);
    private List<TabActionMap> tabActionList;
    
    private FancyTabList tabList;
    private JLabel titleTextLabel;
    private HyperlinkButton fixStalledButton;
    private HyperlinkButton clearFinishedNowButton;
    private JPanel downloadButtonPanel;
    private JPanel uploadButtonPanel;
    private LimeComboBox downloadOptionsButton;
    private LimeComboBox uploadOptionsButton;
    
    private EventList<DownloadItem> activeDownloadList;
    private TabId selectedTab;
    
    @Inject
    public BottomHeaderPanel(DownloadMediator downloadMediator,
            DownloadHeaderPopupMenu downloadHeaderPopupMenu, 
            ClearFinishedDownloadAction clearFinishedNowAction,
            FixStalledDownloadAction fixStalledDownloadAction,
            UploadMediator uploadMediator,
            ComboBoxDecorator comboBoxDecorator, 
            BarPainterFactory barPainterFactory, 
            DockIconFactory iconFactory,
            BandwidthCollector bandwidthCollector,
            @Assisted BottomPanel bottomPanel) {
        
        this.downloadMediator = downloadMediator;
        this.downloadHeaderPopupMenu = downloadHeaderPopupMenu;
        this.clearFinishedDownloadAction = clearFinishedNowAction;
        this.fixStalledDownloadAction = fixStalledDownloadAction;
        this.uploadMediator = uploadMediator;
        this.comboBoxDecorator = comboBoxDecorator;
        this.bandwidthCollector = bandwidthCollector;
        this.bottomPanel = bottomPanel;
        
        GuiUtils.assignResources(this);
        hyperlinkFont = FontUtils.deriveUnderline(hyperlinkFont, true);
        
        component = new JXPanel(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding, fill"));
        component.setBackgroundPainter(barPainterFactory.createDownloadSummaryBarPainter());
        ResizeUtils.forceHeight(component, 20);
        
        // initialize the dock icon since it registers as a Service
        iconFactory.createDockIcon();
        
        initialize();
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    public JComponent getDragComponent() {
        return dragComponent;
    }
    
    private void initialize(){
        initializeComponents();
        initializeTabList();
        layoutComponents();        
    }

    private void initializeComponents(){        
        titleTextLabel = new JLabel(I18n.tr("Downloads"));
        titleTextLabel.setFont(textFont);
        titleTextLabel.setForeground(textForeground);
        
        clearFinishedNowButton = new HyperlinkButton(clearFinishedDownloadAction);
        clearFinishedNowButton.setFont(hyperlinkFont);
        clearFinishedNowButton.setEnabled(false);

        fixStalledButton = new HyperlinkButton(fixStalledDownloadAction);
        fixStalledButton.setFont(hyperlinkFont);
        fixStalledButton.setVisible(false);
        
        dragComponent = new JLabel(scrollPaneNubIcon);
        dragComponent.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));

        downloadButtonPanel = new JPanel(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding"));
        downloadButtonPanel.setOpaque(false);

        uploadButtonPanel = new JPanel(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding"));
        uploadButtonPanel.setOpaque(false);
        
        initializeOptionsButton();

        // Install listener to show appropriate popup menu.
        component.addMouseListener(new MousePopupListener() {
            @Override
            public void handlePopupMouseEvent(MouseEvent e) {
                // Determine popup menu.
                JPopupMenu popupMenu = null;
                if (downloadButtonPanel.isVisible()) {
                    popupMenu = downloadHeaderPopupMenu;
                } else if (uploadButtonPanel.isVisible()) {
                    popupMenu = uploadMediator.getHeaderPopupMenu();
                }
                
                // Display popup menu.
                if (popupMenu != null) {
                    popupMenu.show(component, e.getX(), e.getY());
                }
            }
        });
    }
    
    private void layoutComponents(){
        downloadButtonPanel.add(fixStalledButton, "gapafter 5, hidemode 3");
        downloadButtonPanel.add(clearFinishedNowButton, "gapafter 5, hidemode 3");
        downloadButtonPanel.add(downloadOptionsButton, "gapafter 5");
        
        List<JButton> uploadButtons = uploadMediator.getHeaderButtons();
        for (JButton button : uploadButtons) {
            button.setFont(hyperlinkFont);
            uploadButtonPanel.add(button, "gapafter 5");
        }
        uploadButtonPanel.add(uploadOptionsButton, "gapafter 5");
        
        component.add(tabList, "growy, push, hidemode 3");
        component.add(titleTextLabel, "gapbefore 5, push, hidemode 3");
        component.add(downloadButtonPanel, "hidemode 3");
        component.add(uploadButtonPanel, "hidemode 3");
        
        component.add(dragComponent, "pos 0.5al 0");
    }
        
    @Inject
    public void register(){
        activeDownloadList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateExcluder(DownloadState.ERROR, DownloadState.DONE, DownloadState.CANCELLED));
        activeDownloadList.addListEventListener(new LabelUpdateListListener());

        uploadMediator.getActiveList().addListEventListener(new ListEventListener<UploadItem>() {
            @Override
            public void listChanged(ListEvent<UploadItem> listChanges) {
                updateUploadTitle();
            }
        });
        
        // Add setting listener to clear finished downloads.  When set, we
        // clear finished downloads and hide the "clear finished" button.
        SharingSettings.CLEAR_DOWNLOAD.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                boolean clearDownloads = SharingSettings.CLEAR_DOWNLOAD.getValue();
                if (clearDownloads) {
                    clearFinishedNowButton.doClick();
                }
                clearFinishedNowButton.setVisible(!clearDownloads);
            }
        });
        
        initializeListListeners();
    }

    private void initializeListListeners(){
        EventList<DownloadItem> doneList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.DONE));
        EventList<DownloadItem> stalledList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.STALLED));

        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearFinishedNowButton.setEnabled(listChanges.getSourceList().size() > 0);
            }
        });
        
        stalledList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                fixStalledButton.setVisible(listChanges.getSourceList().size() != 0);                
            }
        });        
    }

    private void initializeOptionsButton(){
        // Create options button for downloads.
        downloadOptionsButton = new LimeComboBox();
        downloadOptionsButton.setText(I18n.tr("Options"));
        
        comboBoxDecorator.decorateMiniComboBox(downloadOptionsButton);
        
        downloadOptionsButton.setFont(hyperlinkFont);
        downloadOptionsButton.setIcon(moreButtonArrow);
        downloadOptionsButton.setForeground(fixStalledButton.getForeground());
        ResizeUtils.forceHeight(downloadOptionsButton, 16);
        
        downloadHeaderPopupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                downloadHeaderPopupMenu.removeAll();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                downloadHeaderPopupMenu.removeAll();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                downloadHeaderPopupMenu.populate();
            }
        });
        
        downloadOptionsButton.overrideMenu(downloadHeaderPopupMenu);
        
        // Create options button for uploads.
        uploadOptionsButton = new LimeComboBox();
        uploadOptionsButton.setText(I18n.tr("Options"));
        comboBoxDecorator.decorateMiniComboBox(uploadOptionsButton);
        
        uploadOptionsButton.setFont(hyperlinkFont);
        uploadOptionsButton.setIcon(moreButtonArrow);
        uploadOptionsButton.setForeground(fixStalledButton.getForeground());
        ResizeUtils.forceHeight(uploadOptionsButton, 16);
        
        uploadOptionsButton.overrideMenu(uploadMediator.getHeaderPopupMenu());
    }
    
    /**
     * Initializes the tab list to select content.
     */
    private void initializeTabList() {
        // Create actions for tab list.
        Action downloadAction = new ShowDownloadsAction();
        Action uploadAction = new ShowUploadsAction();
        actionMap.put(TabId.DOWNLOADS, downloadAction);
        actionMap.put(TabId.UPLOADS, uploadAction);
        tabActionList = TabActionMap.createMapForMainActions(downloadAction, uploadAction);
        
        // Create tab list.
        tabList = new FancyTabList(tabActionList);
        
        // Set tab list attributes.
        tabList.setTabTextColor(textForeground);
        tabList.setTextFont(textFont);
        tabList.setTabTextSelectedColor(textSelectedForeground);
        tabList.setUnderlineEnabled(false);
        tabList.setSelectionPainter(new TabPainter(selectionTopGradientColor, selectionBottomGradientColor, 
                selectionTopBorderColor, selectionBottomBorderColor));
        tabList.setHighlightPainter(new TabPainter(highlightBackground, highlightBackground, 
                highlightBorderColor, highlightBorderColor));
    }
    
    /**
     * Selects the tab for the specified tab id.
     */
    public void selectTab(TabId tabId) {
        Action mainAction = actionMap.get(tabId);
        List<FancyTab> tabs = tabList.getTabs();
        for (FancyTab tab : tabs) {
            if (mainAction == tab.getTabActionMap().getMainAction()) {
                tab.select();
                break;
            }
        }
    }
    
    /**
     * Selects the content for the specified tab id.
     */
    private void select(TabId tabId) {
        selectedTab = tabId;
        
        bottomPanel.show(tabId);
        updateHeader(tabId);
        updateLayout();
    }
    
    /**
     * Updates the header title and controls.
     */
    private void updateHeader(TabId tabId) {
        switch (tabId) {
        case DOWNLOADS:
            updateDownloadTitle();
            downloadButtonPanel.setVisible(true);
            uploadButtonPanel.setVisible(false);
            break;
            
        case UPLOADS:
            updateUploadTitle();
            downloadButtonPanel.setVisible(false);
            uploadButtonPanel.setVisible(true);
            break;
        }
    }
    
    /**
     * Updates the component layout based on the visible tables.
     */
    private void updateLayout() {
        boolean downloadVisible = DownloadSettings.SHOW_DOWNLOADS_TRAY.getValue();
        boolean uploadVisible  = UploadSettings.SHOW_UPLOADS_TRAY.getValue();
        
        if (downloadVisible && uploadVisible) {
            tabList.setVisible(true);
            titleTextLabel.setVisible(false);
            ResizeUtils.forceHeight(component, 26);
        } else {
            tabList.setVisible(false);
            titleTextLabel.setVisible(true);
            ResizeUtils.forceHeight(component, 20);
        }
    }
    
    /**
     * Updates title for Downloads tray.
     */
    private void updateDownloadTitle() {
        String title;
        int size = activeDownloadList.size();
        
        // Create title with size and bandwidth.
        if (SwingUiSettings.SHOW_TOTAL_BANDWIDTH.getValue()) {
            int bandwidth = bandwidthCollector.getCurrentDownloaderBandwidth();
            title = (size > 0) ? I18n.tr("Downloads ({0} | {1} KB/sec)", size, bandwidth) : I18n.tr("Downloads");
        } else {
            title = (size > 0) ? I18n.tr("Downloads ({0})", size) : I18n.tr("Downloads");
        }

        // Apply title to tab action and label.
        actionMap.get(TabId.DOWNLOADS).putValue(Action.NAME, title);
        if (selectedTab == TabId.DOWNLOADS) titleTextLabel.setText(title);
    }
    
    /**
     * Updates title for Uploads tray.
     */
    private void updateUploadTitle() {
        String title;
        int size = uploadMediator.getActiveList().size();
        
        // Create title with size and bandwidth.
        if (SwingUiSettings.SHOW_TOTAL_BANDWIDTH.getValue()) {
            int bandwidth = bandwidthCollector.getCurrentUploaderBandwidth();
            title = (size > 0) ? I18n.tr("Uploads ({0} | {1} KB/sec)", size, bandwidth) : I18n.tr("Uploads");
        } else {
            title = (size > 0) ? I18n.tr("Uploads ({0})", size) : I18n.tr("Uploads");
        }
        
        // Apply title to tab action and label.
        actionMap.get(TabId.UPLOADS).putValue(Action.NAME, title);
        if (selectedTab == TabId.UPLOADS) titleTextLabel.setText(title);
    }
    
    /**
     * Listener to update tab and header title when download list changes. 
     */
    private class LabelUpdateListListener implements ListEventListener<DownloadItem> {       
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            updateDownloadTitle();
        }
    }
    
    /**
     * Action to display downloads table.
     */
    private class ShowDownloadsAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            select(TabId.DOWNLOADS);
        }
    }
    
    /**
     * Action to display uploads table.
     */
    private class ShowUploadsAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            select(TabId.UPLOADS);
        }
    }
    
    /**
     * A Painter used to render the selected or highlighted tab.
     */  
    private static class TabPainter extends RectanglePainter<FancyTab> {
        
        public TabPainter(Color topGradient, Color bottomGradient, 
                Color topBorder, Color bottomBorder) {
            setFillPaint(new GradientPaint(0, 0, topGradient, 0, 1, bottomGradient));
            setBorderPaint(new GradientPaint(0, 0, topBorder, 0, 1, bottomBorder));
            
            setRoundHeight(10);
            setRoundWidth(10);
            setRounded(true);
            setPaintStretched(true);
            setInsets(new Insets(2,0,1,0));
                    
            setAntialiasing(true);
            setCacheable(true);
        }
    }
}

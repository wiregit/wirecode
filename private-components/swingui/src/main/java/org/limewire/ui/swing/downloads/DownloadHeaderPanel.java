package org.limewire.ui.swing.downloads;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.mainframe.BottomPanel.TabAction;
import org.limewire.ui.swing.mainframe.BottomPanel.TabId;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
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
 * Panel that is displayed above the download table.
 */
public class DownloadHeaderPanel {

    @Resource
    private Icon moreButtonArrow;
    @Resource
    private Font hyperlinkFont;
    // TODO define resources
    private Color highlightBackground = Color.decode("#d8d8d8");
    private Color highlightBorderColor = Color.decode("#d8d8d8");
    private Color selectionTopGradientColor = Color.decode("#565656");
    private Color selectionBottomGradientColor = Color.decode("#8b8b8b");
    private Color selectionTopBorderColor = Color.decode("#383838");
    private Color selectionBottomBorderColor = Color.decode("#383838");
    private Font textFont = Font.decode("DIALOG-PLAIN-11");
    private Color textForeground = Color.decode("#313131");
    private Color textSelectedForeground = Color.decode("#ffffff");

    private final DownloadMediator downloadMediator;    
    private final DownloadHeaderPopupMenu downloadHeaderPopupMenu;
    private final ClearFinishedDownloadAction clearFinishedDownloadAction;
    private final FixStalledDownloadAction fixStalledDownloadAction;
    private final ComboBoxDecorator comboBoxDecorator;
    
    private final JXPanel component;

    private List<TabActionMap> tabActionList;
    private Map<TabId, Action> actionMap = new EnumMap<TabId, Action>(TabId.class);
    
    private FancyTabList tabList;
    private JLabel titleTextLabel;
    private HyperlinkButton fixStalledButton;
    private HyperlinkButton clearFinishedNowButton;
    private LimeComboBox moreButton;      
    
    private EventList<DownloadItem> activeList;
    private boolean downloadVisible;
    
    @Inject
    public DownloadHeaderPanel(DownloadMediator downloadMediator, DownloadHeaderPopupMenu downloadHeaderPopupMenu, 
            ClearFinishedDownloadAction clearFinishedNowAction, FixStalledDownloadAction fixStalledDownloadAction,
            ComboBoxDecorator comboBoxDecorator, BarPainterFactory barPainterFactory, DockIconFactory iconFactory,
            @Assisted List<TabActionMap> tabActionList) {
        
        this.downloadMediator = downloadMediator;
        this.downloadHeaderPopupMenu = downloadHeaderPopupMenu;
        this.clearFinishedDownloadAction = clearFinishedNowAction;
        this.fixStalledDownloadAction = fixStalledDownloadAction;
        this.comboBoxDecorator = comboBoxDecorator;
        this.tabActionList = tabActionList;
        
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

        initializeMoreButton();
    }
    
    private void layoutComponents(){
        component.add(tabList, "growy, push, hidemode 3");
        component.add(titleTextLabel, "gapbefore 5, push, hidemode 3");
        component.add(fixStalledButton, "gapafter 5, hidemode 3");  
        component.add(clearFinishedNowButton, "gapafter 5, hidemode 3");
        component.add(moreButton, "gapafter 5");  
    }
        
    @Inject
    public void register(){
        activeList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateExcluder(DownloadState.ERROR, DownloadState.DONE, DownloadState.CANCELLED));
        downloadMediator.getDownloadList().addListEventListener(new LabelUpdateListListener());

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

    private void initializeMoreButton(){
        moreButton = new LimeComboBox();
        moreButton.setText(I18n.tr("Options"));
        
        comboBoxDecorator.decorateMiniComboBox(moreButton);
        
        moreButton.setFont(hyperlinkFont);
        moreButton.setIcon(moreButtonArrow);
        moreButton.setForeground(fixStalledButton.getForeground());
        ResizeUtils.forceHeight(moreButton, 16);
        
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
        
        moreButton.overrideMenu(downloadHeaderPopupMenu);
    }
    
    /**
     * Initializes the tab list to select content.
     */
    private void initializeTabList() {
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
        
        // Initialize action map.
        for (TabActionMap tabActionMap : tabActionList) {
            TabAction action = (TabAction) tabActionMap.getMainAction();
            actionMap.put(action.getTabId(), action);
        }
    }
    
    /**
     * Selects the tab associated with the specified tab id.
     */
    private void selectAction(TabId tabId) {
        // Update indicator.
        downloadVisible = (tabId == TabId.DOWNLOADS);
        
        // Select tab.
        List<FancyTab> tabs = tabList.getTabs();
        for (FancyTab tab : tabs) {
            TabAction action = (TabAction) tab.getTabActionMap().getMainAction();
            if (tabId == action.getTabId()) {
                tab.select();
            }
        }
    }
    
    /**
     * Selects the Downloads tab.
     */
    public void selectDownloads(boolean uploadVisible) {
        selectAction(TabId.DOWNLOADS);
        updateDownloadTitle();
        updateLayout(true, uploadVisible);
    }
    
    /**
     * Selects the Uploads tab.
     */
    public void selectUploads(boolean downloadVisible) {
        selectAction(TabId.UPLOADS);
        updateUploadTitle();
        updateLayout(downloadVisible, true);
    }
    
    /**
     * Updates component layout based on specified visibility indicators.
     */
    private void updateLayout(boolean downloadVisible, boolean uploadVisible) {
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
        String title = (activeList.size() > 0) ?
                I18n.tr("Downloads ({0})", activeList.size()) : I18n.tr("Downloads");

        actionMap.get(TabId.DOWNLOADS).putValue(Action.NAME, title);
        if (downloadVisible) titleTextLabel.setText(title);
    }
    
    /**
     * Updates title for Uploads tray.
     */
    private void updateUploadTitle() {
        // TODO get uploads count
        String title = I18n.tr("Uploads");

        actionMap.get(TabId.UPLOADS).putValue(Action.NAME, title);
        if (!downloadVisible) titleTextLabel.setText(title);
    }
    
    private class LabelUpdateListListener implements ListEventListener<DownloadItem> {       
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
//            if (activeList.size() > 0) {
//                titleTextLabel.setText(I18n.tr("Downloads({0})", activeList.size()));
//            } else {
//                titleTextLabel.setText(I18n.tr("Downloads"));
//            }
            updateDownloadTitle();
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

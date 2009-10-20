package org.limewire.ui.swing.downloads;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
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
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.mainframe.BottomPanel;
import org.limewire.ui.swing.mainframe.BottomPanel.TabId;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
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
    private final UploadMediator uploadMediator;
    private final ComboBoxDecorator comboBoxDecorator;
    private final BottomPanel bottomPanel;
    
    private final JXPanel component;

    private Map<TabId, Action> actionMap = new EnumMap<TabId, Action>(TabId.class);
    private List<TabActionMap> tabActionList;
    
    private FancyTabList tabList;
    private JLabel titleTextLabel;
    private HyperlinkButton fixStalledButton;
    private HyperlinkButton clearFinishedNowButton;
    private LimeComboBox downloadMoreButton;      
    private LimeComboBox uploadMoreButton;      
    
    private EventList<DownloadItem> activeList;
    private boolean downloadSelected;
    
    @Inject
    public BottomHeaderPanel(DownloadMediator downloadMediator, DownloadHeaderPopupMenu downloadHeaderPopupMenu, 
            ClearFinishedDownloadAction clearFinishedNowAction, FixStalledDownloadAction fixStalledDownloadAction,
            UploadMediator uploadMediator,
            ComboBoxDecorator comboBoxDecorator, BarPainterFactory barPainterFactory, DockIconFactory iconFactory,
            @Assisted BottomPanel bottomPanel) {
        
        this.downloadMediator = downloadMediator;
        this.downloadHeaderPopupMenu = downloadHeaderPopupMenu;
        this.clearFinishedDownloadAction = clearFinishedNowAction;
        this.fixStalledDownloadAction = fixStalledDownloadAction;
        this.uploadMediator = uploadMediator;
        this.comboBoxDecorator = comboBoxDecorator;
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
        component.add(downloadMoreButton, "gapafter 5, hidemode 3");
        component.add(uploadMoreButton, "gapafter 5, hidemode 3");
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
        downloadMoreButton = new LimeComboBox();
        downloadMoreButton.setText(I18n.tr("Options"));
        
        comboBoxDecorator.decorateMiniComboBox(downloadMoreButton);
        
        downloadMoreButton.setFont(hyperlinkFont);
        downloadMoreButton.setIcon(moreButtonArrow);
        downloadMoreButton.setForeground(fixStalledButton.getForeground());
        ResizeUtils.forceHeight(downloadMoreButton, 16);
        
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
        
        downloadMoreButton.overrideMenu(downloadHeaderPopupMenu);
        
        // Create options button for uploads.
        uploadMoreButton = new LimeComboBox();
        uploadMoreButton.setText(I18n.tr("Options"));
        comboBoxDecorator.decorateMiniComboBox(uploadMoreButton);
        
        uploadMoreButton.setFont(hyperlinkFont);
        uploadMoreButton.setIcon(moreButtonArrow);
        uploadMoreButton.setForeground(fixStalledButton.getForeground());
        ResizeUtils.forceHeight(uploadMoreButton, 16);
        
        uploadMoreButton.overrideMenu(uploadMediator.getHeaderPopupMenu());
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
        bottomPanel.show(tabId);
        
        downloadSelected = (tabId == TabId.DOWNLOADS);
        
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
            downloadMoreButton.setVisible(true);
            uploadMoreButton.setVisible(false);
            break;
            
        case UPLOADS:
            updateUploadTitle();
            downloadMoreButton.setVisible(false);
            uploadMoreButton.setVisible(true);
            break;
        }
    }
    
    /**
     * Updates the component layout based on the visible tables.
     */
    private void updateLayout() {
        boolean downloadVisible = DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue();
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
        String title = (activeList.size() > 0) ?
                I18n.tr("Downloads ({0})", activeList.size()) : I18n.tr("Downloads");

        actionMap.get(TabId.DOWNLOADS).putValue(Action.NAME, title);
        if (downloadSelected) titleTextLabel.setText(title);
    }
    
    /**
     * Updates title for Uploads tray.
     */
    private void updateUploadTitle() {
        // TODO get uploads count
        String title = I18n.tr("Uploads");

        actionMap.get(TabId.UPLOADS).putValue(Action.NAME, title);
        if (!downloadSelected) titleTextLabel.setText(title);
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

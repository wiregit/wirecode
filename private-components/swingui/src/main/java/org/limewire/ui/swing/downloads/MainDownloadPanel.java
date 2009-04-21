package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.BackAction;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.dnd.DownloadableTransferHandler;
import org.limewire.ui.swing.dock.DockIcon;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.NotImplementedException;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MainDownloadPanel extends JPanel {    
    @Resource
    private Color topBorderColor;
    @Resource
    private Color outerBorderColor;
    @Resource
    private Color topGradientColor;
    @Resource
    private Color bottomGradientColor;
    @Resource
    private Icon moreIcon;
    @Resource
    private Icon moreIconRollover;
    @Resource
    private Icon moreIconPressed;
	
    
    public static final String NAME = "MainDownloadPanel";
	
	private final DownloadMediator downloadMediator;
	
	private final JXPanel headerPanel;
    
	private LimeComboBox moreButton;
    private JXButton clearFinishedNowButton;
    private HyperlinkButton fixStalledButton;
    private final DockIcon dock;
    
    private JCheckBoxMenuItem clearFinishedCheckBox;
    private JXLabel titleTextLabel;
    
    private TrayNotifier notifier;
    
    private final SortedList<DownloadItem> sortList;
	
    private final AbstractDownloadsAction pauseAction = new AbstractDownloadsAction(I18n.tr("Pause All")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.pauseAll();
        }
    };

    private final AbstractDownloadsAction resumeAction = new AbstractDownloadsAction(I18n.tr("Resume All")) {
        public void actionPerformed(ActionEvent e) {
            downloadMediator.resumeAll();
        }
    };

    private final AbstractDownloadsAction clearFinishedNowAction = new AbstractDownloadsAction(I18n.tr("Clear Finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.clearFinished();
            dock.draw(0);
        }
    };
    
    private final AbstractDownloadsAction fixStalledAction = new AbstractDownloadsAction(I18n.tr("Fix Stalled")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.fixStalled();
        }
    };
   
    private final AbstractDownloadsAction clearFinishedAction = new AbstractDownloadsAction(I18n.tr("Clear When Finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            SharingSettings.CLEAR_DOWNLOAD.setValue(clearFinishedCheckBox.isSelected());
        }
    };

    
    private final AbstractDownloadsAction cancelStallededAction = new AbstractDownloadsAction(I18n.tr("All Stalled")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.cancelStalled();
        }
    };
    
    private final AbstractDownloadsAction cancelErrorAction = new AbstractDownloadsAction(I18n.tr("All Error")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.cancelError();
        }
    };
    
    private final AbstractDownloadsAction cancelAllAction = new AbstractDownloadsAction(I18n.tr("All Downloads")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.cancelAll();
        }
    };
    
    private final Action statusSortAction = new AbstractAction(I18n.tr("Status")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            sortList.setComparator(new StateComparator());
        }
    };  
    private final Action orderSortAction = new AbstractAction(I18n.tr("Order Added")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            sortList.setComparator(new OrderAddedComparator());
        }
    };  
    private final Action nameSortAction = new AbstractAction(I18n.tr("Name")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            sortList.setComparator(new NameComparator());
        }
    };  
    private final Action progressSortAction = new AbstractAction(I18n.tr("Progress")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            sortList.setComparator(new ProgressComparator());
        }
    };  
    private final Action timeRemainingSortAction = new AbstractAction(I18n.tr("Time Left")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            sortList.setComparator(new TimeRemainingComparator());
        }
    };  
    private final Action speedSortAction = new AbstractAction(I18n.tr("Speed")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            sortList.setComparator(new SpeedComparator());
        }
    };  
    private final Action fileTypeSortAction = new AbstractAction(I18n.tr("File Type")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            sortList.setComparator(new FileTypeComparator());
        }
    };  
    private final Action extensionSortAction = new AbstractAction(I18n.tr("File Extension")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            sortList.setComparator(new FileExtensionComparator());
        }
    };
    
    private final Action downloadSettingsAction = new AbstractAction(I18n.tr("Download Settings...")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            throw new NotImplementedException("Implement me, please!!!!!!");
        }
    };
    
    
    private abstract class AbstractDownloadsAction extends AbstractAction {

        private AbstractDownloadsAction(String name) {
            super(name);
        }
        
        /**
         * Enables this action if the supplied downloadSize is greater than zero,
         * and updates the dock icon with the number of downloads.
         * @param downloadSize
         */
        public void setEnablementFromDownloadSize(int downloadSize) {
            setEnabled(downloadSize > 0);
            dock.draw(downloadSize);
        }
    }
    
	/**
	 * Create the panel
	 */
	@Inject
	public MainDownloadPanel(DownloadTableFactory downloadTableFactory, 
	        DownloadMediator downloadMediator,
	        ComboBoxDecorator comboBoxFactory,
	        DockIconFactory dockIconFactory,
	        BackAction backAction, 
	        TrayNotifier notifier, 
	        DownloadListManager downloadListManager) {
	    GuiUtils.assignResources(this);
	    
		this.downloadMediator = downloadMediator;
		this.notifier = notifier;
		
		dock = dockIconFactory.createDockIcon();
		
		setLayout(new BorderLayout());
		
        resumeAction.setEnabled(false);
        pauseAction.setEnabled(false);
        clearFinishedNowAction.setEnabled(false);
        
        sortList = GlazedListsFactory.sortedList(downloadMediator.getDownloadList(), new OrderAddedComparator());
        JXTable table = downloadTableFactory.create(sortList);
        table.setTableHeader(null);
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        add(pane, BorderLayout.CENTER);
		
		JPanel headerTitlePanel = new JPanel(new MigLayout("insets 4 0 4 0, gap 0 0 0 0, novisualpadding, fill, aligny center"));
        headerTitlePanel.setOpaque(false);        
        titleTextLabel = new JXLabel(I18n.tr("Downloads"));
        headerTitlePanel.add(titleTextLabel, "gapbottom 2");        
        
        headerPanel = new HeaderBar(headerTitlePanel);
        
        initHeader();
		add(headerPanel, BorderLayout.NORTH);		
		
		EventList<DownloadItem> pausableList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
		        new PausableMatcher());
		EventList<DownloadItem> resumableList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new ResumableMatcher());
		EventList<DownloadItem> doneList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                        new DownloadStateMatcher(DownloadState.DONE));
		
		pausableList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                pauseAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });

        resumableList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                resumeAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });
        
        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearFinishedNowAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });
        
        downloadMediator.getDownloadList().addListEventListener(new VisibilityListListener(downloadMediator.getDownloadList()));
    }
	
	
	
	private void initHeader() {
	    headerPanel.setBackgroundPainter(new HeaderBackgroundPainter());
        clearFinishedNowButton = new HyperlinkButton(clearFinishedNowAction);

        fixStalledButton = new HyperlinkButton(fixStalledAction);	    
	    
	    initializeMoreButton();

	    clearFinishedCheckBox = new JCheckBoxMenuItem(clearFinishedAction);

	    clearFinishedCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
	    SharingSettings.CLEAR_DOWNLOAD.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        clearFinishedCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());                        
                    }
                });
            }
	    });
	    
	    JMenu cancelSubMenu = new JMenu(I18n.tr("Cancel"));
        cancelSubMenu.add(cancelStallededAction);
        cancelSubMenu.add(cancelErrorAction);
        cancelSubMenu.add(cancelAllAction);
        
        JMenu sortSubMenu = initializeSortSubMenu();        
	    
	    JPopupMenu menu = new JPopupMenu();
	    menu.add(pauseAction);
	    menu.add(resumeAction);
	    menu.add(cancelSubMenu);
        menu.addSeparator();
        menu.add(sortSubMenu);
        menu.addSeparator();
	    menu.add(clearFinishedCheckBox);
	    menu.addSeparator();
	    menu.add(downloadSettingsAction);
	    
	    moreButton.overrideMenu(menu);
	    
	    headerPanel.setLayout(new MigLayout("insets 0, fillx, filly","push[][]"));
        headerPanel.add(fixStalledButton, "gapafter 5");
        headerPanel.add(clearFinishedNowButton, "gapafter 5");
	    headerPanel.add(moreButton, "gapafter 5");
	}
	
	private JMenu initializeSortSubMenu(){
	    JMenu sortSubMenu = new JMenu("Sort by");
	    
        JMenuItem order = new JCheckBoxMenuItem(orderSortAction);
        JMenuItem name = new JCheckBoxMenuItem(nameSortAction);
        JMenuItem progress = new JCheckBoxMenuItem(progressSortAction);
        JMenuItem timeRemaining = new JCheckBoxMenuItem(timeRemainingSortAction);
        JMenuItem speed = new JCheckBoxMenuItem(speedSortAction);
        JMenuItem status = new JCheckBoxMenuItem(statusSortAction);
        JMenuItem fileType = new JCheckBoxMenuItem(fileTypeSortAction);
        JMenuItem extension = new JCheckBoxMenuItem(extensionSortAction);
        

        ButtonGroup sortButtonGroup = new ButtonGroup();
        sortButtonGroup.add(order);
        sortButtonGroup.add(name);
        sortButtonGroup.add(progress);
        sortButtonGroup.add(timeRemaining);
        sortButtonGroup.add(speed);
        sortButtonGroup.add(status);
        sortButtonGroup.add(fileType);
        sortButtonGroup.add(extension);
        
        sortSubMenu.add(order);
        sortSubMenu.add(name);
        sortSubMenu.add(progress);
        sortSubMenu.add(timeRemaining);
        sortSubMenu.add(speed);
        sortSubMenu.add(status);
        sortSubMenu.add(fileType);
        sortSubMenu.add(extension);
        
        return sortSubMenu;
	}
	
	public JComponent getHeader(){
	    return headerPanel;
	}
	
	private static class PausableMatcher implements Matcher<DownloadItem> {
        @Override
        public boolean matches(DownloadItem item) {
            return item.getState().isPausable();
        }
    }

    private static class ResumableMatcher implements Matcher<DownloadItem> {
        @Override
        public boolean matches(DownloadItem item) {
            return item.getState().isResumable();
        }
    }
    
    private class VisibilityListListener implements ListEventListener<DownloadItem> {
        private EventList<DownloadItem> list;

        public VisibilityListListener(EventList<DownloadItem> list) {
            this.list = list;
        }

        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            if (list.size() > 0) {
                setVisible(true);
                titleTextLabel.setText(I18n.tr("Downloads({0})", list.size()));
            } else {
                titleTextLabel.setText(I18n.tr("Downloads"));
            }
        }
    }
    
    /**
     * Painter for the background of the header
     */
    private class HeaderBackgroundPainter extends AbstractPainter<JXPanel> {

        private RectanglePainter<JXPanel> painter;
        
        public HeaderBackgroundPainter() {
            painter = new RectanglePainter<JXPanel>();
            painter.setFillPaint(new GradientPaint(0,0, topGradientColor, 0, 1, bottomGradientColor, false));
            painter.setFillVertical(true);
            painter.setFillHorizontal(true);
            painter.setPaintStretched(true);
            painter.setBorderPaint(null);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
            painter.paint(g, object, width, height);
            
            // paint the top border
            g.setColor(outerBorderColor);
            g.drawLine(0, 0, width, 0);
            g.setColor(topBorderColor);
            g.drawLine(0, 1, width, 1);

            //paint the bottom border
            g.setColor(outerBorderColor);
            g.drawLine(0, height-1, width, height-1);
        }
    }
    
    @Inject
    public void register(DownloadListManager downloadListManager, SaveLocationExceptionHandler saveLocationExceptionHandler) {
        setTransferHandler(new DownloadableTransferHandler(downloadListManager, saveLocationExceptionHandler));
        
        // handle individual completed downloads
        initializeDownloadListeners(downloadListManager);
    }
    
    private void initializeDownloadListeners(final DownloadListManager downloadListManager) {
        // handle individual completed downloads
        downloadListManager.addPropertyChangeListener(new DownloadPropertyListener());

        downloadListManager.getDownloads().addListEventListener(
                new ListEventListener<DownloadItem>() {
                    @Override
                    public void listChanged(ListEvent<DownloadItem> listChanges) {
                        // only show the notification messages if the tray
                        // downloads are forced to be invisible
                        if (shouldShowNotification()) {
                            while (listChanges.nextBlock()) {
                                if (listChanges.getType() == ListEvent.INSERT) {
                                    int index = listChanges.getIndex();
                                    final DownloadItem downloadItem = listChanges.getSourceList().get(index);
                                    SwingUtils.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifier.showMessage(new Notification(I18n.tr("Download Started"), downloadItem.getFileName(), 
                                                    new AbstractAction(I18n.tr("Show")) {
                                                @Override
                                                public void actionPerformed(ActionEvent e) {
                                                    setSize(getPreferredSize().width, 80);
                                                }
                                            }));
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
    }
    
    private boolean shouldShowNotification(){
        return !isShowing() || getVisibleRect().height < 5;
    }
    
    private void initializeMoreButton(){
        moreButton = new LimeComboBox();
        moreButton.setIcon(moreIcon);
        moreButton.setRolloverIcon(moreIconRollover);
        moreButton.setPressedIcon(moreIconPressed);
        moreButton.setMargin(new Insets(0, 0, 0, 0));
        moreButton.setBorderPainted(false);
        moreButton.setContentAreaFilled(false);
        moreButton.setFocusPainted(false);
        moreButton.setRolloverEnabled(false);
        moreButton.setHideActionText(true);
        moreButton.setBorder(BorderFactory.createEmptyBorder());
        moreButton.setOpaque(false);
        moreButton.addMouseListener(new ActionHandListener());
    }

    private class DownloadPropertyListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(DownloadListManager.DOWNLOAD_COMPLETED)) {
                final DownloadItem downloadItem = (DownloadItem) event.getNewValue();
                notifier.showMessage(new Notification(I18n.tr("Download Complete"), downloadItem
                        .getFileName(), new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ActionMap map = Application.getInstance().getContext().getActionManager()
                                .getActionMap();
                        map.get("restoreView").actionPerformed(e);

                        if (downloadItem.isLaunchable()) {
                            DownloadItemUtils.launch(downloadItem);
                        }
                    }
                }));
            }
        }
    }

    private static class StateComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return getSortPriority(o1.getState()) - getSortPriority(o2.getState());
        }   
        
        private int getSortPriority(DownloadState state){
            
            switch(state){
            case DONE: return 1;
            case FINISHING: return 2;
            case DOWNLOADING: return 3;
            case RESUMING: return 4;
            case CONNECTING: return 5;
            case PAUSED: return 6;
            case REMOTE_QUEUED: return 7;
            case LOCAL_QUEUED: return 8;
            case TRYING_AGAIN: return 9;
            case STALLED: return 10;
            case ERROR: return 11;            
            }
            
           throw new IllegalArgumentException("Unknown DownloadState: " + state);
        }
    }
    
    private static class OrderAddedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return 0;
        }   
     
    }
    
    private static class NameComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getTitle().compareTo(o2.getTitle());
        }   
     
    } 
    private static class ProgressComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o2.getPercentComplete() - o1.getPercentComplete();
        }   
     
    } 
    private static class TimeRemainingComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)(o1.getRemainingDownloadTime() - o2.getRemainingDownloadTime());
        }   
     
    }
    
    
    private static class SpeedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)(o2.getDownloadSpeed() - o1.getDownloadSpeed());
        }   
     
    }
    
    
    private static class FileTypeComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getCategory().compareTo(o2.getCategory());
        }   
     
    }
    
    
    private static class FileExtensionComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return FileUtils.getFileExtension(o1.getDownloadingFile()).compareTo(FileUtils.getFileExtension(o2.getDownloadingFile()));
        }   
     
    }
    
}

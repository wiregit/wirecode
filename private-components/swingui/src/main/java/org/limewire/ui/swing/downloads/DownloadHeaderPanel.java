package org.limewire.ui.swing.downloads;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.dock.DockIcon;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.DownloadMediator.SortOrder;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;

public class DownloadHeaderPanel extends JXPanel {

    @Resource
    private Color topBorderColor;

    @Resource
    private Color outerBorderColor;

    @Resource
    private Color topGradientColor;

    @Resource
    private Color bottomGradientColor;


    @Resource
    private Icon upArrow;
    @Resource
    private Icon downArrow;

    private final DownloadMediator downloadMediator;    

    private final DockIcon dock;

    private LimeComboBox moreButton;

    private JXButton clearFinishedNowButton;
    private JLabel clearFinishedLabel;

    private HyperlinkButton fixStalledButton;
    private JLabel fixStalledLabel;

    private JCheckBoxMenuItem clearFinishedCheckBox;

    private JXLabel titleTextLabel;
    private AbstractButton isDescending;
    
    
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
            if (confirmCancellation(I18n.tr("Cancel all stalled downloads?"))) {
                downloadMediator.cancelStalled();
            }
        }
    };
    
    private final AbstractDownloadsAction cancelErrorAction = new AbstractDownloadsAction(I18n.tr("All Error")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (confirmCancellation(I18n.tr("Cancel all error downloads?"))) {
                downloadMediator.cancelError();
            }
        }
    };
    
    private final AbstractDownloadsAction cancelAllAction = new AbstractDownloadsAction(I18n.tr("All Downloads")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (confirmCancellation(I18n.tr("Cancel all downloads?"))) {
                downloadMediator.cancelAll();
            }
        }
    };
    
    private final Action statusSortAction = new SortAction(I18n.tr("Status"), SortOrder.STATUS);  
    private final Action orderSortAction = new SortAction(I18n.tr("Order Added"), SortOrder.ORDER_ADDED);  
    private final Action nameSortAction = new SortAction(I18n.tr("Name"), SortOrder.NAME);
    private final Action progressSortAction = new SortAction(I18n.tr("Progress"), SortOrder.PROGRESS);  
    private final Action timeRemainingSortAction = new SortAction(I18n.tr("Time Left"), SortOrder.TIME_REMAINING);
    private final Action speedSortAction = new SortAction(I18n.tr("Speed"), SortOrder.SPEED);
    private final Action fileTypeSortAction = new SortAction(I18n.tr("File Type"), SortOrder.FILE_TYPE);
    private final Action extensionSortAction = new SortAction(I18n.tr("File Extension"), SortOrder.EXTENSION);

    private final ComboBoxDecorator comboBoxDecorator;
    
    private final EventList<DownloadItem> activeList;
    
    private final Action downloadSettingsAction = new AbstractAction(I18n.tr("Download Options...")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            new OptionsDisplayEvent(OptionsDialog.DOWNLOADS).publish();
        }
    };

    
    @Inject
    public DownloadHeaderPanel(DownloadMediator downloadMediator, DockIconFactory dockIconFactory,
            ComboBoxDecorator comboBoxDecorator) {
        GuiUtils.assignResources(this);
        
        this.downloadMediator = downloadMediator;
        dock = dockIconFactory.createDockIcon();   
        this.comboBoxDecorator = comboBoxDecorator;
        

        activeList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateExcluder(DownloadState.ERROR, DownloadState.DONE, DownloadState.CANCELLED));

        initialize();
    }
    
    private void initialize(){
        initializeComponents();        
        layoutComponents();        
        setupPainter();
    }
  

    private void initializeComponents(){        
        clearFinishedNowButton = new HyperlinkButton(clearFinishedNowAction);
        clearFinishedNowButton.setVisible(false);
        clearFinishedLabel = new JLabel(clearFinishedNowButton.getText());
        clearFinishedLabel.setFont(clearFinishedNowButton.getFont());
        clearFinishedLabel.setPreferredSize(clearFinishedNowButton.getPreferredSize());
        clearFinishedLabel.setEnabled(false);

        fixStalledButton = new HyperlinkButton(fixStalledAction);
        fixStalledButton.setVisible(false);
        fixStalledLabel = new JLabel(fixStalledButton.getText());
        fixStalledLabel.setFont(fixStalledButton.getFont());
        fixStalledLabel.setEnabled(false);

        initializeMoreButton();
        
        titleTextLabel = new JXLabel(I18n.tr("Downloads"));
    }
    
    private void layoutComponents(){
        setLayout(new MigLayout("insets 4 0 4 0, gap 0, novisualpadding, fill"));
        add(titleTextLabel, "gapbefore 5, push");   
        add(fixStalledButton, "gapafter 5, hidemode 3");  
        add(fixStalledLabel, "gapafter 5, hidemode 3");
        add(clearFinishedNowButton, "gapafter 5, hidemode 3");
        add(clearFinishedLabel, "gapafter 5, hidemode 3");
        add(moreButton, "gapafter 5");  
    }
    
    private void setupPainter() {
        setOpaque(false); 
        setBackgroundPainter(new DownloadHeaderBackgroundPainter());
    }
    
    @Inject
    public void register(){
        downloadMediator.getDownloadList().addListEventListener(new LabelUpdateListListener());
        initializeListListeners();
    }

    private void initializeListListeners(){
        EventList<DownloadItem> pausableList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new PausableMatcher());
        EventList<DownloadItem> resumableList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new ResumableMatcher());
        EventList<DownloadItem> doneList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.DONE));
        EventList<DownloadItem> stalledList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.STALLED));
        EventList<DownloadItem> errorList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.ERROR));
        
        pausableList.addListEventListener(new ActionEnablementListListener(pauseAction));
        resumableList.addListEventListener(new ActionEnablementListListener(resumeAction));        
        errorList.addListEventListener(new ActionEnablementListListener(cancelErrorAction));        
        downloadMediator.getDownloadList().addListEventListener(new ActionEnablementListListener(cancelAllAction));    
        
        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearFinishedNowButton.setVisible(listChanges.getSourceList().size()>0);
                clearFinishedLabel.setVisible(!clearFinishedNowButton.isVisible());
            }
        });
        
        stalledList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                fixStalledButton.setVisible(listChanges.getSourceList().size() != 0);
                fixStalledLabel.setVisible(!fixStalledButton.isVisible());
                
                cancelStallededAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });        
    }
    
    
    private void initializeMoreButton(){
        pauseAction.setEnabled(false);
        resumeAction.setEnabled(false);
        cancelErrorAction.setEnabled(false);
        cancelAllAction.setEnabled(false);
        cancelStallededAction.setEnabled(false);
        
        clearFinishedCheckBox = new JCheckBoxMenuItem(clearFinishedAction);

        clearFinishedCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
        SharingSettings.CLEAR_DOWNLOAD.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        clearFinishedCheckBox
                                .setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
                    }
                });
            }
        });

        JPopupMenu menu = new JPopupMenu();
        menu.add(pauseAction);
        menu.add(resumeAction);
        menu.add(createCancelSubMenu());
        menu.addSeparator();
        menu.add(createSortSubMenu());
        menu.addSeparator();
        menu.add(clearFinishedCheckBox);
        menu.addSeparator();
        menu.add(downloadSettingsAction);

        moreButton = new LimeComboBox();
        comboBoxDecorator.decorateLinkComboBox(moreButton);
        moreButton.setText(I18n.tr("Options"));
//        moreButton.setRolloverIcon(moreIconRollover);
//        moreButton.setPressedIcon(moreIconPressed);
//        moreButton.setMargin(new Insets(0, 0, 0, 0));
//        moreButton.setBorderPainted(false);
//        moreButton.setContentAreaFilled(false);
//        moreButton.setFocusPainted(false);
//        moreButton.setRolloverEnabled(false);
//        moreButton.setHideActionText(true);
//        moreButton.setBorder(BorderFactory.createEmptyBorder());
//        moreButton.setOpaque(false);
//        moreButton.addMouseListener(new ActionHandListener()); 

        moreButton.overrideMenu(menu);        
    }
    
    private JMenu createCancelSubMenu(){
        JMenu cancelSubMenu = new JMenu(I18n.tr("Cancel"));
        
        cancelSubMenu.add(cancelStallededAction);
        cancelSubMenu.add(cancelErrorAction);
        cancelSubMenu.add(cancelAllAction);

        return cancelSubMenu;
    }

    private JMenu createSortSubMenu(){
        JMenu sortSubMenu = new JMenu(I18n.tr("Sort by"));
        
        JCheckBoxMenuItem orderAdded = new JCheckBoxMenuItem(orderSortAction);
        JCheckBoxMenuItem name = new JCheckBoxMenuItem(nameSortAction);
        JCheckBoxMenuItem progress = new JCheckBoxMenuItem(progressSortAction);
        JCheckBoxMenuItem timeRemaining = new JCheckBoxMenuItem(timeRemainingSortAction);
        JCheckBoxMenuItem speed = new JCheckBoxMenuItem(speedSortAction);
        JCheckBoxMenuItem status = new JCheckBoxMenuItem(statusSortAction);
        JCheckBoxMenuItem fileType = new JCheckBoxMenuItem(fileTypeSortAction);
        JCheckBoxMenuItem extension = new JCheckBoxMenuItem(extensionSortAction);        

        final UsefulButtonGroup sortButtonGroup = new UsefulButtonGroup();
        sortButtonGroup.add(orderAdded);
        sortButtonGroup.add(name);
        sortButtonGroup.add(progress);
        sortButtonGroup.add(timeRemaining);
        sortButtonGroup.add(speed);
        sortButtonGroup.add(status);
        sortButtonGroup.add(fileType);
        sortButtonGroup.add(extension);
        
        isDescending = new JMenuItem(new AbstractAction(I18n.tr("Reverse Order")){
            {
                putValue(Action.SELECTED_KEY, true);
                putValue(Action.SMALL_ICON, downArrow);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                putValue(Action.NAME, isDescending.isSelected()? I18n.tr("Reverse Order") : I18n.tr("Reverse Order"));
                putValue(Action.SMALL_ICON, isDescending.isSelected()? downArrow : upArrow);
                sortButtonGroup.getSelectedButton().getAction().actionPerformed(null);
            }            
        });
        isDescending.setModel(new JToggleButton.ToggleButtonModel());
        isDescending.setBorderPainted(false);
        isDescending.setContentAreaFilled(false);
        isDescending.setOpaque(false);
       // isDescending.setSelected(true);
        
        orderAdded.setSelected(true);
        
        sortSubMenu.add(orderAdded);
        sortSubMenu.add(name);
        sortSubMenu.add(progress);
        sortSubMenu.add(timeRemaining);
        sortSubMenu.add(speed);
        sortSubMenu.add(status);
        sortSubMenu.add(fileType);
        sortSubMenu.add(extension);
        sortSubMenu.addSeparator();
        sortSubMenu.add(isDescending);
        
        return sortSubMenu;
    }
    
    private boolean confirmCancellation(String message){
        return FocusJOptionPane.showConfirmDialog(GuiUtils.getMainFrame(), message, I18n.tr("Cancel"),
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.YES_OPTION;
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
    
    /**
     * Painter for the background of the header
     */
    private class DownloadHeaderBackgroundPainter extends AbstractPainter<JXPanel> {

        private RectanglePainter<JXPanel> painter;

        public DownloadHeaderBackgroundPainter() {
            painter = new RectanglePainter<JXPanel>();
            painter.setFillPaint(new GradientPaint(0, 0, topGradientColor, 0, 1,
                    bottomGradientColor, false));
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

            // paint the bottom border
            g.setColor(outerBorderColor);
            g.drawLine(0, height - 1, width, height - 1);
        }
    }
    
    private class LabelUpdateListListener implements ListEventListener<DownloadItem> {       

        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            if (activeList.size() > 0) {
                titleTextLabel.setText(I18n.tr("Downloads({0})", activeList.size()));
            } else {
                titleTextLabel.setText(I18n.tr("Downloads"));
            }
        }
    }
    
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
    
    private class SortAction extends AbstractAction{
        
        private final SortOrder order;

        public SortAction(String title, SortOrder order){
            super(title);
            this.order = order;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.setSortOrder(order, !isDescending.isSelected());
        }
    }; 
    
    private static class ActionEnablementListListener implements ListEventListener<DownloadItem> {
        private AbstractDownloadsAction action;
        public ActionEnablementListListener(AbstractDownloadsAction action){
            this.action = action;
        }
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            action.setEnablementFromDownloadSize(listChanges.getSourceList().size());
        }                
    }
    
    private static class UsefulButtonGroup extends ButtonGroup {
        
        public AbstractButton getSelectedButton(){
            ButtonModel selectedModel = getSelection();
            for (AbstractButton button : buttons){
                if(button.getModel() == selectedModel){
                    return button;
                }
            }
            //nothing found
            return null;
        }
    }
}

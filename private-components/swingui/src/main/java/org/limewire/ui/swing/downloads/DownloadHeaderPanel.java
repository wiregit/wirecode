package org.limewire.ui.swing.downloads;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

/**
 * Panel that is displayed above the download table.
 */
public class DownloadHeaderPanel {
    @Resource
    private Color topBorderColor;
    @Resource
    private Color outerBorderColor;
    @Resource
    private Color topGradientColor;
    @Resource
    private Color bottomGradientColor;
    @Resource
    private Icon moreButtonArrow;
    @Resource
    private Icon moreButtonArrowDownState;
    @Resource
    private Font hyperlinkFont;
    @Resource
    private Color selectedFontColor;
    private Color hyperlinkColor;

    private final DownloadMediator downloadMediator;    
    private final DownloadHeaderPopupMenu downloadHeaderPopupMenu;
    private final ClearFinishedDownloadAction clearFinishedDownloadAction;
    private final FixStalledDownloadAction fixStalledDownloadAction;
    private final JXPanel component;

    private JXLabel titleTextLabel;
    private HyperlinkButton fixStalledButton;
    private JXButton clearFinishedNowButton;
    private JLabel clearFinishedLabel;
    private HyperlinkButton moreButton;      
    
    private long menuClosedDelayTime;

    private EventList<DownloadItem> activeList;
    
    @Inject
    public DownloadHeaderPanel(DownloadMediator downloadMediator, DownloadHeaderPopupMenu downloadHeaderPopupMenu, 
            ClearFinishedDownloadAction clearFinishedNowAction, FixStalledDownloadAction fixStalledDownloadAction) {       
        this.downloadMediator = downloadMediator;
        this.downloadHeaderPopupMenu = downloadHeaderPopupMenu;
        this.clearFinishedDownloadAction = clearFinishedNowAction;
        this.fixStalledDownloadAction = fixStalledDownloadAction;
        this.component = new JXPanel(new MigLayout("insets 2 0 2 0, gap 0, novisualpadding, fill"));
        
        GuiUtils.assignResources(this);
        
        initialize();
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    private void initialize(){
        initializeComponents();        
        layoutComponents();        
        setupPainter();
    }

    private void initializeComponents(){        
        titleTextLabel = new JXLabel(I18n.tr("Downloads"));
        
        clearFinishedNowButton = new HyperlinkButton(clearFinishedDownloadAction);
        clearFinishedNowButton.setFont(hyperlinkFont);
        clearFinishedNowButton.setVisible(false);
        clearFinishedLabel = new JLabel(clearFinishedNowButton.getText());
        clearFinishedLabel.setFont(hyperlinkFont);
        clearFinishedLabel.setFont(clearFinishedNowButton.getFont());
        clearFinishedLabel.setPreferredSize(clearFinishedNowButton.getPreferredSize());
        clearFinishedLabel.setEnabled(false);

        fixStalledButton = new HyperlinkButton(fixStalledDownloadAction);
        fixStalledButton.setFont(hyperlinkFont);
        fixStalledButton.setVisible(false);

        initializeMoreButton();
    }
    
    private void layoutComponents(){
        component.add(titleTextLabel, "gapbefore 5, push");   
        component.add(fixStalledButton, "gapafter 5, hidemode 3");  
        component.add(clearFinishedNowButton, "gapafter 5, hidemode 3");
        component.add(clearFinishedLabel, "gapafter 5, hidemode 3");
        component.add(moreButton, "gapafter 5");  
    }
    
    private void setupPainter() {
        component.setOpaque(false); 
        component.setBackgroundPainter(new DownloadHeaderBackgroundPainter());
    }
    
    @Inject
    public void register(){
        activeList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateExcluder(DownloadState.ERROR, DownloadState.DONE, DownloadState.CANCELLED));
        downloadMediator.getDownloadList().addListEventListener(new LabelUpdateListListener());

        moreButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!downloadHeaderPopupMenu.isShowing() && System.currentTimeMillis() - menuClosedDelayTime > 200) {
                    downloadHeaderPopupMenu.init();
                    downloadHeaderPopupMenu.show(moreButton, 0, moreButton.getHeight());
                }
            }      
        });      
        downloadHeaderPopupMenu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                cancel();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                cancel();
                downloadHeaderPopupMenu.removeAll();
            }

            private void cancel() {
                menuClosedDelayTime = System.currentTimeMillis();
                moreButton.setNormalForeground(hyperlinkColor);
                moreButton.setRolloverForeground(hyperlinkColor);
                moreButton.setIcon(moreButtonArrow);
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                moreButton.setNormalForeground(selectedFontColor);
                moreButton.setRolloverForeground(selectedFontColor);
                moreButton.setIcon(moreButtonArrowDownState);

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
                clearFinishedNowButton.setVisible(listChanges.getSourceList().size()>0);
                clearFinishedLabel.setVisible(!clearFinishedNowButton.isVisible());
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
        moreButton = new HyperlinkButton(I18n.tr("Options"));
        moreButton.setFont(hyperlinkFont);
        moreButton.setIcon(moreButtonArrow);
        moreButton.setHorizontalTextPosition(SwingConstants.LEFT);
        moreButton.setFocusPainted(false);
        moreButton.setBorder(BorderFactory.createEmptyBorder(1,6,1,6));
        hyperlinkColor = moreButton.getForeground();

        moreButton.setBackgroundPainter(new Painter<JXButton>() {
            @Override
            public void paint(Graphics2D g, JXButton object, int width, int height) {
                if (downloadHeaderPopupMenu.isShowing()) {
                    g = (Graphics2D)g.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(Color.BLACK);
                    g.fillRoundRect(0, 0, width, height, 10, 10);
                    g.dispose();
                }
            }
        });
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
}

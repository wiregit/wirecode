package org.limewire.ui.swing.downloads.table;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.downloads.table.renderer.DownloadButtonPanel;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.assistedinject.AssistedInject;

public class DownloadTableCellImpl extends JXPanel implements DownloadTableCell {

    private final CategoryIconManager categoryIconManager;
    private final ProgressBarDecorator progressBarDecorator; 

    
    private JLabel iconLabel;
    private JLabel titleLabel;
    private JLabel statusLabel;
    private LimeProgressBar progressBar;
    private JLabel timeLabel;

   private DownloadButtonPanel buttonPanel;
   
    @Resource private Icon warningIcon;
    @Resource private int progressBarWidth;
    @Resource private int progressBarHeight;
    @Resource private Color titleLabelColour;
    @Resource private Color statusLabelColour;
    @Resource private Color warningLabelColour;
    @Resource private Color errorLabelColour;
    @Resource private Color finishedLabelColour;
    @Resource private Color linkColour;
    @Resource private Font statusFontPlainMin;
    @Resource private Font statusFontPlainFull;
    @Resource private Font titleFont;
    @Resource private Color borderPaint;
    @Resource private Icon downloadingIcon;
        
    
    @AssistedInject
    public DownloadTableCellImpl(CategoryIconManager categoryIconManager,
            ProgressBarDecorator progressBarDecorator) {
        
        GuiUtils.assignResources(this);

        this.categoryIconManager = categoryIconManager;
        this.progressBarDecorator = progressBarDecorator;
        
        initComponents();
    }
    
    public void setEditorListener(ActionListener editorListener) {
        buttonPanel.addActionListener(editorListener);
    }
    
    public void update(DownloadItem item) {
        if (item == null) { // can be null because of accessibility calls.
            return;
        }

        update(item.getState(), item);
    }

    private void initComponents() {
        
        this.setBackgroundPainter(this.createCellPainter());               
                                
        iconLabel = new JLabel();

        titleLabel = new JLabel();

        statusLabel = new JLabel();
        statusLabel.setFont(statusFontPlainFull);
        statusLabel.setForeground(statusLabelColour);
        
        progressBar = new LimeProgressBar(0, 100);
        progressBarDecorator.decoratePlain(progressBar);        
        Dimension size = new Dimension(progressBarWidth, progressBarHeight);
        progressBar.setMaximumSize(size);
        progressBar.setMinimumSize(size);
        progressBar.setPreferredSize(size);
        
        timeLabel = new JLabel();
        timeLabel.setFont(statusFontPlainFull);

        buttonPanel = new DownloadButtonPanel(null);
        createView();
    }
    

    
    private void createView() {
        setLayout(new MigLayout("insets 0 0 0 0, gap 0 0 0 0, novisualpadding, nogrid, aligny center"));
        add(iconLabel, "gapleft 4, gapright 6");
        add(titleLabel);
        add(progressBar, "growx 0, hidemode 0");
        add(timeLabel);
        add(statusLabel);
        add(buttonPanel, "growx, push, gapright 6");
    }

    
    
    private void update(DownloadState state, DownloadItem item) {

        updateIcon(state, item);
        titleLabel.setText(item.getTitle());
        timeLabel.setForeground(statusLabelColour);
        timeLabel.setFont(statusFontPlainFull);

        progressBar.setValue(item.getPercentComplete());
        progressBar.setVisible(state == DownloadState.DOWNLOADING);

        progressBar.setEnabled(state != DownloadState.PAUSED);

        statusLabel.setText(getMessage(state, item));

        if (item.getRemainingDownloadTime() > Long.MAX_VALUE - 1000) {
            timeLabel.setVisible(false);
        } else {
            timeLabel.setText(I18n.tr("{0} left", CommonUtils.seconds2time(item
                    .getRemainingDownloadTime())));
            timeLabel.setVisible(item.getState() == DownloadState.DOWNLOADING);
        }

        updateButtons(item);
    }
    
    private void updateIcon(DownloadState state, DownloadItem item) {
        switch (state) {
        case ERROR:
            iconLabel.setIcon(warningIcon);
            break;

        case FINISHING:
        case DONE:
            iconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
            break;
            
        default:
            iconLabel.setIcon(downloadingIcon);
        }

        updateButtons(item);
    }
    

    private void updateButtons(DownloadItem item) {
        buttonPanel.updateButtons(item.getState());
    }
    
    private Painter<JXPanel> createCellPainter() {
        AbstractPainter<JXPanel> painter = new AbstractPainter<JXPanel>() {

            @Override
            protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
                g.setPaint(borderPaint);
                g.drawLine(0, height-1, width-0, height-1);
            }
        } ;
        
        painter.setCacheable(true);
        painter.setAntialiasing(false);
        //painter.setFilters(PainterUtils.createSoftenFilter(0.025f));
                        
        return painter;
    }
    
    private String getMessage(DownloadState state, DownloadItem item) {
        switch (state) {
        case RESUMING:
            return I18n.tr("Resuming at {0}%",
                    item.getPercentComplete());
        case CANCELLED:
            return I18n.tr("Cancelled");
        case FINISHING:
            return I18n.tr("Finishing download...");
        case DONE:
            return I18n.tr("Done - ");
        case CONNECTING:
            return I18n.tr("Connecting...");
        case DOWNLOADING:
            // {0}: current size
            // {1}: total size
            // {2}: download speed
            // {3}: number of people
            return I18n.trn("Downloading {0} of {1} ({2}) from {3} person",
                    "Downloading {0} of {1} ({2}) from {3} people",
                    item.getDownloadSourceCount(),
                    GuiUtils.toUnitbytes(item.getCurrentSize()), 
                    GuiUtils.toUnitbytes(item.getTotalSize()),
                    GuiUtils.rate2speed(item.getDownloadSpeed()), 
                    item.getDownloadSourceCount());
        case TRYING_AGAIN:
            return getTryAgainMessage(item.getRemainingTimeInState());
        case STALLED:
            return I18n.tr("Stalled - {0} of {1} ({2}%). - ", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()),
                    GuiUtils.toUnitbytes(item.getTotalSize()),
                    item.getPercentComplete()
                    );
        case ERROR:         
            return I18n.tr("Unable to download: ");
        case PAUSED:
            // {0}: current size, {1} total size, {2} percent complete
            return I18n.tr("Paused - {0} of {1} ({2}%)", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()), GuiUtils.toUnitbytes(item.getTotalSize()),
                    item.getPercentComplete());
        case LOCAL_QUEUED:
            return getQueueTimeMessage(item.getRemainingTimeInState());
        case REMOTE_QUEUED:
            if(item.getRemoteQueuePosition() == -1 || item.getRemoteQueuePosition() == Integer.MAX_VALUE){
                return getQueueTimeMessage(item.getRemainingTimeInState());
            }
            return I18n.trn("Waiting - Next in line",
                    "Waiting - {0} in line",
                    item.getRemoteQueuePosition(), item.getRemoteQueuePosition());
        default:
            return null;
        }
        
    }
    
    private String getTryAgainMessage(long tryingAgainTime) {
        if(tryingAgainTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Searching for people with this file...");                
        } else {
            return I18n.tr("Searching for people with this file... ({0} left)", CommonUtils.seconds2time(tryingAgainTime));
        }
    }
    
    private String getQueueTimeMessage(long queueTime){
        if(queueTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Waiting - remaining time unknown");                
        } else {
            return I18n.tr("Waiting - Starting in {0}", CommonUtils.seconds2time(queueTime));
        }
    }

    @Override
    public Component getComponent() {
        return this;
    }

//    /**
//     * Class to make trimming the title length inside the current GridBagLayout possible 
//     */
//    private class LabelContainer extends JPanel {
//        private final JLabel label = new JLabel();
//        
//        public LabelContainer() {
//            this.setLayout(new BorderLayout());
//            this.setOpaque(false);
//            this.setBorder(BorderFactory.createEmptyBorder());
//            
//            this.label.setFont(titleFont);
//            this.label.setForeground(titleLabelColour);
//            this.label.setMaximumSize(new Dimension(progressBarWidth-30, 20));
//            this.label.setPreferredSize(new Dimension(progressBarWidth-30, 20));
//            
//            this.add(this.label, BorderLayout.WEST);
//        }
//        
//        public void setText(String text) {
//            this.label.setText(text);
//        }
//    }
}

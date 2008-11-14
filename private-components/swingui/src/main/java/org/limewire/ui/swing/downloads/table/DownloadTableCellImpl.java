package org.limewire.ui.swing.downloads.table;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.LimeProgressBarFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.assistedinject.AssistedInject;

public class DownloadTableCellImpl extends JPanel implements DownloadTableCell {

    private final CategoryIconManager categoryIconManager;
    private final LimeProgressBarFactory progressBarFactory;
    
    private CardLayout statusViewLayout;
    private final static String FULL_LAYOUT = "Full download display";
    private JPanel fullPanel;
    private static String MIN_LAYOUT = "Condensed download display";
    private JPanel minPanel;

    private DownloadButtonPanel minButtonPanel;
    private JLabel minIconLabel;
    private JLabel minTitleLabel;
    private JLabel minStatusLabel;
    private JXHyperlink minLinkButton;
    
    private DownloadButtonPanel fullButtonPanel;
    private JLabel fullIconLabel;
    private JLabel fullTitleLabel;
    private JLabel fullStatusLabel;
    private LimeProgressBar fullProgressBar;
    private JLabel fullTimeLabel;
   
    @Resource private Icon warningIcon;
    @Resource private Icon downloadIcon;
    @Resource private int progressBarWidth;
    @Resource private Color titleLabelColour;
    @Resource private Color statusLabelColour;
    @Resource private Color stalledLabelColour;
    @Resource private Color finishedLabelColour;
    @Resource private Color linkColour;
    @Resource private Color progressBarBorderColour;
    @Resource private Font statusFontPlain;
    @Resource private Font statusFontBold;
    @Resource private Font titleFont;
    
    
    private ActionListener editorListener = null;
    
    private List<JComponent> textComponents = new ArrayList<JComponent>();
    
    @AssistedInject
    public DownloadTableCellImpl(CategoryIconManager categoryIconManager, LimeProgressBarFactory progressBarFactory) {
        GuiUtils.assignResources(this);

        this.categoryIconManager = categoryIconManager;
        this.progressBarFactory = progressBarFactory;
        
        initComponents();
    }
    
    public void setEditorListener(ActionListener editorListener) {
        this.editorListener = editorListener;
        this.minButtonPanel.setActionListener(editorListener);
        this.fullButtonPanel.setActionListener(editorListener);
        this.minLinkButton.addActionListener(editorListener);
    }
    
    public void update(DownloadItem item) {
        updateComponent(this, item);
    }
    
    private void initComponents() {
        
        statusViewLayout = new CardLayout();
        this.setLayout(statusViewLayout);
        
        fullPanel = new JPanel(new GridBagLayout());
        minPanel  = new JPanel(new GridBagLayout());
        
        fullPanel.setOpaque(false);
        minPanel.setOpaque(false);

        Border blankBorder = BorderFactory.createEmptyBorder();
        fullPanel.setBorder(blankBorder);
        minPanel.setBorder(blankBorder);
        this.setBorder(blankBorder);
        
        this.add(fullPanel, FULL_LAYOUT);
        this.add( minPanel, MIN_LAYOUT);
        this.statusViewLayout.show(this, FULL_LAYOUT);
        
        
        minIconLabel = new JLabel();
        
        minTitleLabel = new JLabel();
        minTitleLabel.setFont(titleFont);
        minTitleLabel.setForeground(titleLabelColour);
        textComponents.add(minTitleLabel);

        minStatusLabel = new JLabel();
        minStatusLabel.setFont(statusFontPlain);
        minStatusLabel.setForeground(statusLabelColour);
        textComponents.add(minStatusLabel);

        minButtonPanel = new DownloadButtonPanel(editorListener);
        minButtonPanel.setOpaque(false);

        minLinkButton = new JXHyperlink();
        minLinkButton.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        minLinkButton.addActionListener(editorListener);
        minLinkButton.setForeground(linkColour);
        minLinkButton.setFont(statusFontPlain);
                                
        fullIconLabel = new JLabel();

        fullTitleLabel = new JLabel();
        fullTitleLabel.setFont(titleFont);
        fullTitleLabel.setForeground(titleLabelColour);
        textComponents.add(fullTitleLabel);

        fullStatusLabel = new JLabel();
        fullStatusLabel.setFont(statusFontPlain);
        fullStatusLabel.setForeground(statusLabelColour);
        fullStatusLabel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        fullStatusLabel.setIconTextGap(0);
        fullStatusLabel.setIcon(downloadIcon);
        
        textComponents.add(fullStatusLabel);

        fullProgressBar = progressBarFactory.create();
        Dimension size = new Dimension(progressBarWidth, 16);
        fullProgressBar.setMaximumSize(size);
        fullProgressBar.setMinimumSize(size);
        fullProgressBar.setPreferredSize(size);
        fullProgressBar.setBorder(BorderFactory.
                createLineBorder(progressBarBorderColour));
        
        fullTimeLabel = new JLabel();
        fullTimeLabel.setFont(statusFontPlain);
        
        fullButtonPanel = new DownloadButtonPanel(editorListener);
        fullButtonPanel.setOpaque(false);
        
        createFullView();
        createMinView();
    }
    
    private void createMinView() {
        GridBagConstraints gbc = new GridBagConstraints();

        Insets insets = new Insets(0,10,0,0);
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridheight = 5;
        minPanel.add(minIconLabel, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        minPanel.add(minTitleLabel, gbc);
        
        gbc.insets = new Insets(0,30,0,0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 5;
        minPanel.add(minButtonPanel, gbc);  
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 0;
        minPanel.add(minStatusLabel, gbc);
       
        gbc.insets = new Insets(0,0,0,0);
        gbc.gridx++;
        minPanel.add(minLinkButton, gbc);
        
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 0;
        minPanel.add(Box.createHorizontalStrut(progressBarWidth-16), gbc);
            
    }
    
    private void createFullView() {
        GridBagConstraints gbc = new GridBagConstraints();

        Insets insets = new Insets(0,10,0,0);
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        fullPanel.add(fullIconLabel, gbc);
        
        gbc.insets = new Insets(0,5,0,0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        fullPanel.add(fullTitleLabel, gbc);
        
        gbc.insets = new Insets(3,10,0,0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 3;
        fullPanel.add(fullProgressBar, gbc);
        
        gbc.insets = new Insets(0,30,0,0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx += gbc.gridwidth;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        fullPanel.add(fullButtonPanel, gbc);  
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        fullPanel.add(fullStatusLabel, gbc);
        
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        fullPanel.add(fullTimeLabel, gbc);    
    }
    
    private void updateMin(DownloadTableCellImpl editor, DownloadItem item) {
        
        editor.minTitleLabel.setText(item.getTitle());
        
        switch (item.getState()) {
        
        
        case ERROR :
        case STALLED :
            
            editor.minIconLabel.setIcon(warningIcon);
            editor.minStatusLabel.setForeground(stalledLabelColour);
            editor.minStatusLabel.setFont(statusFontBold);
            
            break;
            
        case DONE :
            
            editor.minIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
            editor.minStatusLabel.setForeground(finishedLabelColour);
            editor.minStatusLabel.setFont(statusFontBold);
            
            break;
            
        default :
            editor.minIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));     
            editor.minStatusLabel.setForeground(statusLabelColour);
            editor.minStatusLabel.setFont(statusFontPlain);
            
        }
        
        editor.minStatusLabel.setText(getMessage(item));
        
        updateButtonsMin(editor, item);      
    }
    
    
    
    private void updateFull(DownloadTableCellImpl editor, DownloadItem item) {
        
        editor.fullIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
        editor.fullTitleLabel.setText(item.getTitle());
        
        long totalSize = item.getTotalSize();
        long curSize = item.getCurrentSize();
        if (curSize < totalSize) {
            editor.fullProgressBar.setHidden(false);
            editor.fullProgressBar.setMaximum((int) item.getTotalSize());
            editor.fullProgressBar.setValue((int) item.getCurrentSize());
        }
        
        editor.fullStatusLabel.setText(getMessage(item));
        
        editor.fullTimeLabel.setText(CommonUtils.seconds2time(item.getRemainingDownloadTime()));
        editor.fullTimeLabel.setVisible(true);
         
        updateButtonsFull(editor, item);      
    }
    

    private void updateButtonsMin(DownloadTableCellImpl editor, DownloadItem item) {
        DownloadState state = item.getState();
        minButtonPanel.updateButtons(state);
        
        switch (state) {
        
            case ERROR :
                minLinkButton.setVisible(true);
                minLinkButton.setText("<html><u>" + I18n.tr(item.getErrorState().getMessage()) + "</u></html>");
                
                break;
                
            case STALLED :

                minLinkButton.setVisible(true);
                minLinkButton.setText("<html><u>Refresh Now</u></html>");

                break;
                
            default:
                minLinkButton.setVisible(false);
        }
    }
    
    private void updateButtonsFull(DownloadTableCellImpl editor, DownloadItem item) {
        DownloadState state = item.getState();
        fullButtonPanel.updateButtons(state);

        if (state == DownloadState.DOWNLOADING) {
            fullProgressBar.setEnabled(true);
        } 
        else { 
            fullProgressBar.setEnabled(false);
        }
    }

    
    private void updateComponent(DownloadTableCellImpl editor, DownloadItem item){
        if (item.getState() == DownloadState.DOWNLOADING) {
            editor.statusViewLayout.show(this, FULL_LAYOUT);
            updateFull(editor, item);
        } 
        else {
            editor.statusViewLayout.show(this, MIN_LAYOUT);
            updateMin(editor, item);
        }
    }
    
    

    private String getMessage(DownloadItem item) {
        switch (item.getState()) {
        case CANCELLED:
            return I18n.tr("Cancelled");
        case FINISHING:
            return I18n.tr("Finishing download...");
            //TODO: correct time for finishing
            //return I18n.tr("Finishing download, {0} remaining", item.getRemainingStateTime());
        case DONE:
            return I18n.tr("Done");
        case CONNECTING:
            return I18n.tr("Connecting...");
        case DOWNLOADING:
            //TODO : uploaders in DownloadItem & plural, not sure if this TODO is addressed
            // with adding proper plural handling?
            // {0}: current file size, {2} final file size, {3}, number of people
            return I18n.trn("Downloading {0} of {1} ({2}) from {3} person",
                    "Downloading {0} of {1} ({2}) from {3} people",
                    item.getDownloadSourceCount(),
                    GuiUtils.toUnitbytes(item.getCurrentSize()), 
                    GuiUtils.toUnitbytes(item.getTotalSize()),
                    GuiUtils.rate2speed(item.getDownloadSpeed()), 
                    item.getDownloadSourceCount());
        case STALLED:
            return I18n
                    .tr("Stalled - ");
        case ERROR:         
            return I18n.tr("Unable to download: ");
        case PAUSED:
            return I18n.tr("Paused - {0} of {1} ({2}%)", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()), GuiUtils.toUnitbytes(item.getTotalSize()),
                    item.getPercentComplete());
        case LOCAL_QUEUED:
            long queueTime = item.getRemainingQueueTime();
            if(queueTime == DownloadItem.UNKNOWN_TIME){
                return I18n.tr("Queued - remaining time unknown");                
            } else {
                return I18n.tr("Queued - About {0} before download can begin", CommonUtils.seconds2time(queueTime));
            }
        case REMOTE_QUEUED:
            if(item.getQueuePosition() == -1){
                return I18n.tr("Queued - About {0} before download can begin", CommonUtils.seconds2time(item.getRemainingQueueTime()));
            }
            return I18n.trn("Queued - {0} person ahead of you for this file",
                    "Queued - {0} people ahead of you for this file",
                    item.getQueuePosition(), item.getQueuePosition());
        default:
            throw new IllegalArgumentException("Unknown DownloadState: " + item.getState());
        }
        
    }

    @Override
    public Component getComponent() {
        return this;
    }

}

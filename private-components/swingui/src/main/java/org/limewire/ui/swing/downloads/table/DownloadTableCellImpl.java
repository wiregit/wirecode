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
import org.limewire.ui.swing.downloads.LimeProgressBar;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.assistedinject.AssistedInject;

public class DownloadTableCellImpl extends JPanel implements DownloadTableCell {

    private CategoryIconManager categoryIconManager;
    
    private CardLayout statusViewLayout;
    private final static String FULL_LAYOUT = "Full download display";
    private JPanel fullPanel;
    private final static String MIN_LAYOUT = "Condensed download display";
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
   
    @Resource
    private Icon warningIcon;
    
    @Resource
    private Icon downloadIcon;
    
    
    // TODO: convert to resources
    private static final int PROGRESS_BAR_WIDTH =  538;
    private Color itemLabelColour     = new Color(0x21,0x52,0xa6);
    private Color statusLabelColour   = new Color(0x31,0x31,0x31);
    private Color stalledLabelColour  = new Color(0xb3,0x1c,0x20);
    private Color finishedLabelColour = new Color(0x16,0x7e,0x11);
    private Color linkColour          = new Color(0x2b,0x5b,0xaa);
    private Color progressBarBorderColour = new Color(0x8a,0x8a,0x8a);
    
    private ActionListener editorListener = null;
    
    private static final Font STATUS_FONT_PLAIN = new Font("Arial", Font.PLAIN, 10);
    private static final Font STATUS_FONT_BOLD = new Font("Arial", Font.BOLD, 10);
    
    private List<JComponent> textComponents = new ArrayList<JComponent>();
    
    @AssistedInject
    public DownloadTableCellImpl(CategoryIconManager categoryIconManager) {
        GuiUtils.assignResources(this);

        initComponents();
        
        this.categoryIconManager = categoryIconManager;
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
        minTitleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        minTitleLabel.setForeground(itemLabelColour);
        textComponents.add(minTitleLabel);

        minStatusLabel = new JLabel();
        minStatusLabel.setFont(STATUS_FONT_PLAIN);
        minStatusLabel.setForeground(statusLabelColour);
        textComponents.add(minStatusLabel);

        minButtonPanel = new DownloadButtonPanel(editorListener);
        minButtonPanel.setOpaque(false);

        minLinkButton = new JXHyperlink();
        minLinkButton.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        minLinkButton.addActionListener(editorListener);
        minLinkButton.setForeground(linkColour);
        minLinkButton.setFont(STATUS_FONT_PLAIN);
                                
        fullIconLabel = new JLabel();

        fullTitleLabel = new JLabel();
        fullTitleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        fullTitleLabel.setForeground(itemLabelColour);
        textComponents.add(fullTitleLabel);

        fullStatusLabel = new JLabel();
        fullStatusLabel.setFont(STATUS_FONT_PLAIN);
        fullStatusLabel.setForeground(statusLabelColour);
        fullStatusLabel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        fullStatusLabel.setIconTextGap(0);
        fullStatusLabel.setIcon(downloadIcon);
        
        textComponents.add(fullStatusLabel);

        fullProgressBar = new LimeProgressBar();
        Dimension size = new Dimension(PROGRESS_BAR_WIDTH, 16);
        fullProgressBar.setMaximumSize(size);
        fullProgressBar.setMinimumSize(size);
        fullProgressBar.setPreferredSize(size);
        fullProgressBar.setBorder(BorderFactory.
                createLineBorder(progressBarBorderColour));
        
        fullTimeLabel = new JLabel();
        fullTimeLabel.setFont(STATUS_FONT_PLAIN);
        
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
        gbc.gridheight = 3;
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
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
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
        minPanel.add(Box.createHorizontalStrut(PROGRESS_BAR_WIDTH-16), gbc);
            
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
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.gridx += gbc.gridwidth;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
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
            editor.minStatusLabel.setFont(STATUS_FONT_BOLD);
            
            break;
            
        case DONE :
            
            editor.minIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
            editor.minStatusLabel.setForeground(finishedLabelColour);
            editor.minStatusLabel.setFont(STATUS_FONT_BOLD);
            
            break;
            
        default :
            editor.minIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));     
            editor.minStatusLabel.setForeground(statusLabelColour);
            editor.minStatusLabel.setFont(STATUS_FONT_PLAIN);
            
        }
        
        
        editor.minStatusLabel.setText(getMessage(item));
        
        updateButtonsMin(item);      
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
         
        
        updateButtonsFull(item);      
    }
    

    private void updateButtonsMin(DownloadItem item) {
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
    
    private void updateButtonsFull(DownloadItem item) {
        DownloadState state = item.getState();
        fullButtonPanel.updateButtons(state);

        if (state == DownloadState.DOWNLOADING) {
            fullProgressBar.setEnabled(true);
            fullProgressBar.setHidden(false);
        } else if (state == DownloadState.PAUSED) {
            fullProgressBar.setEnabled(false);
            fullProgressBar.setHidden(false);
        } else {
            fullProgressBar.setHidden(true);
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

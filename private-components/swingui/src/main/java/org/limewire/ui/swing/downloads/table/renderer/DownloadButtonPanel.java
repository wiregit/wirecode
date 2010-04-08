package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.transfer.TransferRendererResources;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Used by DownloadButtonRendererEditor.
 */
public class DownloadButtonPanel extends JPanel {

    private final JButton pauseButton;
    private final JButton resumeButton;
    private final JButton tryAgainButton;
    private final JButton searchAgainButton;
    private final JButton markAsGoodButton;

    public DownloadButtonPanel() {
        this(null);
    }
    /**
     * Create the panel.
     */
    public DownloadButtonPanel(ActionListener actionListener) {
        super(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding, fill, aligny center"));
        
        GuiUtils.assignResources(this);		

        setOpaque(false);

        Font font = new TransferRendererResources().getFont();

        pauseButton = new HyperlinkButton(I18n.tr("Pause"));
        pauseButton.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
        pauseButton.addActionListener(actionListener);
        pauseButton.setToolTipText(I18n.tr("Pause download"));		
        pauseButton.setFont(font);

        tryAgainButton = new HyperlinkButton(I18n.tr("Try again"));
        tryAgainButton.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        tryAgainButton.addActionListener(actionListener);
        tryAgainButton.setToolTipText(I18n.tr("Try again"));    
        tryAgainButton.setFont(font);

        resumeButton =  new HyperlinkButton(I18n.tr("Resume"));;
        resumeButton.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
        resumeButton.addActionListener(actionListener);
        resumeButton.setVisible(false);
        resumeButton.setToolTipText(I18n.tr("Resume download"));    
        resumeButton.setFont(font);
        
        searchAgainButton =  new HyperlinkButton(I18n.tr("Search Again"));;
        searchAgainButton.setActionCommand(DownloadActionHandler.SEARCH_AGAIN_COMMAND);
        searchAgainButton.addActionListener(actionListener);
        searchAgainButton.setVisible(false);
        searchAgainButton.setToolTipText(I18n.tr("Search Again"));    
        searchAgainButton.setFont(font);

        markAsGoodButton =  new HyperlinkButton(I18n.tr("Mark as Good"));;
        markAsGoodButton.setActionCommand(DownloadActionHandler.MARK_GOOD_COMMAND);
        markAsGoodButton.addActionListener(actionListener);
        markAsGoodButton.setVisible(false);
        markAsGoodButton.setToolTipText(I18n.tr("{0}Marking the files you like will improve your search results.{1}Your ratings are private and won't be shared with anyone else.{2}",
                "<html>", "<br/>", "</html>"));
        markAsGoodButton.setFont(font);

        add(resumeButton, "hidemode 3");
        add(pauseButton, "hidemode 3");
        add(tryAgainButton, "hidemode 3");
        add(searchAgainButton, "hidemode 3");
        add(markAsGoodButton, "hidemode 3");
    }
    
    public void addActionListener(ActionListener actionListener){
        pauseButton.addActionListener(actionListener);
        resumeButton.addActionListener(actionListener);
        tryAgainButton.addActionListener(actionListener);
        searchAgainButton.addActionListener(actionListener);
        markAsGoodButton.addActionListener(actionListener);
    }


    public void updateButtons(DownloadItem item) {
        if (item.getDownloadItemType() != DownloadItemType.ANTIVIRUS) {
            DownloadState state = item.getState();
            boolean canTryAgain = item.isTryAgainEnabled();

            pauseButton.setVisible(state == DownloadState.DOWNLOADING);  //used to be connecting also. keeping consistent with tray
            resumeButton.setVisible(state.isResumable());
            tryAgainButton.setVisible(state == DownloadState.STALLED && canTryAgain);
            searchAgainButton.setVisible(state == DownloadState.STALLED && !canTryAgain && !item.isStoreDownload());
            markAsGoodButton.setVisible(state.isFinished() && !item.hasBeenMarkedAsGood());
        } else {
            // Hide all buttons for anti-virus updates.
            pauseButton.setVisible(false);
            resumeButton.setVisible(false);
            tryAgainButton.setVisible(false);
            searchAgainButton.setVisible(false);
            markAsGoodButton.setVisible(false);
        }
    }
}

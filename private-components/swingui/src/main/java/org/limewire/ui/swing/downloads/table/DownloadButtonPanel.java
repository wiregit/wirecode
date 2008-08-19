package org.limewire.ui.swing.downloads.table;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Renderer and editor for DownloadTables. Editors must be initialized with 
 * <code>editor.initializeEditor(downloadItems)</code>
 */
public class DownloadButtonPanel extends JPanel {
		
	private JButton pauseButton;
    private JButton cancelButton;
    private JButton resumeButton;
    private JButton tryAgainButton;


    @Resource
    private Icon pauseIcon;    
    @Resource
    private Icon pauseIconPressed;    
    @Resource
    private Icon pauseIconRollover;
    @Resource
    private Icon cancelIcon;
    @Resource
    private Icon cancelIconPressed;
    @Resource
    private Icon cancelIconRollover;
    @Resource
    private Icon resumeIcon;
    @Resource
    private Icon resumeIconPressed;
    @Resource
    private Icon resumeIconRollover;
    @Resource
    private Icon tryAgainIcon;
    @Resource
    private Icon tryAgainIconPressed;
    @Resource
    private Icon tryAgainIconRollover;
    

	/**
	 * Create the panel
	 */
	public DownloadButtonPanel(ActionListener actionListener) {
	    super(new FlowLayout(FlowLayout.LEADING, 0,0));
	    GuiUtils.assignResources(this);		

		pauseButton = createIconButton(pauseIcon, pauseIconRollover, pauseIconPressed);
		pauseButton.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
		pauseButton.addActionListener(actionListener);
		pauseButton.setToolTipText(I18n.tr("Pause"));		
		

		cancelButton = createIconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
		cancelButton.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
		cancelButton.addActionListener(actionListener);
        cancelButton.setToolTipText(I18n.tr("Cancel Download"));

		resumeButton = createIconButton(resumeIcon, resumeIconRollover, resumeIconPressed);
		resumeButton.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
		resumeButton.addActionListener(actionListener);
		resumeButton.setVisible(false);
        resumeButton.setToolTipText(I18n.tr("Resume"));
		
		tryAgainButton = createIconButton(tryAgainIcon, tryAgainIconRollover, tryAgainIconPressed);
		tryAgainButton.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
		tryAgainButton.addActionListener(actionListener);
		tryAgainButton.setToolTipText(I18n.tr("Try Again"));
        
		
		add(resumeButton);
		add(pauseButton);
		add(tryAgainButton);
		add(cancelButton);
	}	
	
	public void setActionListener(ActionListener actionListener){
	    pauseButton.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);
        resumeButton.addActionListener(actionListener);
        tryAgainButton.addActionListener(actionListener);
	}

	

	public void updateButtons(DownloadState state) {
		pauseButton.setVisible(state.isPausable());
		resumeButton.setVisible(state.isResumable());
		cancelButton.setVisible(state.isCancellable());
		tryAgainButton.setVisible(state.isSearchAgainable());	
	}


	
	//TODO: move this out of renderer
	private JButton createIconButton(Icon icon, Icon rolloverIcon, Icon pressedIcon) {
        JButton button = new JButton();
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setIcon(icon);
        button.setRolloverIcon(rolloverIcon);
        button.setPressedIcon(pressedIcon);
        button.setHideActionText(true);
        return button;
    }   

}

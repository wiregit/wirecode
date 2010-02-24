package org.limewire.ui.swing.activation;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.table.TableCellEditor;

import org.jdesktop.application.Resource;
import org.limewire.activation.api.ActivationItem;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

public class ActivationInfoAction extends AbstractAction {

    private static final String DOWNLOAD_UPDATE_URL = "http://www.limewire.com/client_redirect/?page=update";
    
    private final TableCellEditor editor;
    private final JComponent parent;
    private final Application application;
    
    @Resource
    private Font font;

    public ActivationInfoAction(TableCellEditor editor, JComponent parent, Application application) {
        this.editor = editor;
        this.parent = parent;
        this.application = application;
        
        GuiUtils.assignResources(this);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() instanceof IconButton) {
            ((IconButton) e.getSource()).resetDefaultCursor();
        }
        
        Object item = editor.getCellEditorValue();
        if (item instanceof ActivationItem) {
            ActivationItem activationItem = (ActivationItem) item;
            LabelWithLinkSupport label = new LabelWithLinkSupport();
            label.setText(getMessage(activationItem));
            label.setMaximumSize(new Dimension(330, 80));
            label.setPreferredSize(new Dimension(330, 40));
            
            FocusJOptionPane.showMessageDialog(parent.getRootPane().getParent(), label, activationItem.getLicenseName(), JOptionPane.PLAIN_MESSAGE);
            editor.cancelCellEditing();
        }
    }
    
    private String getMessage(ActivationItem item) {
        switch(item.getStatus()) {
        case UNAVAILABLE:
            return I18n.tr("{0} is no longer supported by LimeWire.", item.getLicenseName());
        case UNUSEABLE_LW:
            return 
            "<html>" + "<font size=\"3\" face=\"" + font.getFontName() + "\">" 
            + I18n.tr("{0} is not supported by LimeWire {1}. ", item.getLicenseName(), application.getVersion()) 
            + I18n.tr("Please {0}upgrade{1} to the latest version.", "<a href='" + application.addClientInfoToUrl(DOWNLOAD_UPDATE_URL) + "'>", "</a>")
            + "</html>";
        case UNUSEABLE_OS:
            return I18n.tr("{0} is not supported by {1} {2}. We apologize for the inconvenience.", item.getLicenseName(), OSUtils.getOS(), OSUtils.getOSVersion());
        case EXPIRED:
            return I18n.tr("{0} is expired.", item.getLicenseName());
        default:
            return "";
        }
    }

}

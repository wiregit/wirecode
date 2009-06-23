package org.limewire.ui.swing.warnings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.mainframe.LimeWireLayeredPane;
import org.limewire.ui.swing.util.CategoryUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DocumentWarningPanel extends Panel implements Resizable, ComponentListener {
    // heavy weight so it can be on top of other heavy weight components

    private final JLayeredPane layeredPane;

    private final AtomicBoolean showing = new AtomicBoolean(false);

    @Resource
    private Color backgroundColor;

    @Resource
    private Font font;

    @Resource
    private Color fontColor;

    @Resource
    private Color linkFontColor;

    @Inject
    public DocumentWarningPanel(final SharedFileListManager shareListManager,
            @LimeWireLayeredPane JLayeredPane layeredPane, ButtonDecorator buttonDecorator) {
        this.layeredPane = layeredPane;
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("insets 10"));
        setSize(320, 190);
        setPreferredSize(new Dimension(320, 190));
        setMaximumSize(new Dimension(320, 190));
        setMinimumSize(new Dimension(320, 190));
        setBackground(backgroundColor);

        final String learnMoreUrl = "http://www.limewire.com/client_redirect/?page=documentsSharing";
        HTMLLabel htmlLabel = new HTMLLabel(
                I18n
                        .tr(
                                "<html><body>Warning: you are sharing Documents with the world. These Documents may contain personal information. <a href=\"{0}\">learn more</a></body></html>",
                                learnMoreUrl));
        htmlLabel.setEditable(false);
        htmlLabel.setOpaque(false);
        htmlLabel.setFocusable(false);
        htmlLabel.setHtmlFont(font);
        htmlLabel.setHtmlForeground(fontColor);
        htmlLabel.setHtmlLinkForeground(linkFontColor);

        add(htmlLabel, "span 2, wrap");
        MultiLineLabel sharingLabel = new MultiLineLabel(I18n
                .tr("Do you want to keep sharing Documents with the world?"));
        sharingLabel.setForeground(fontColor);
        sharingLabel.setFont(font);

        add(sharingLabel, "span 2, gaptop 20, wrap");
        JXButton continueSharingButton = new JXButton(new AbstractAction(I18n
                .tr("Continue Sharing")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                cleanup();
                SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD.setValue(false);
            }
        });
        buttonDecorator.decorateDarkFullButton(continueSharingButton);
        
        JXButton unshareAllButton = new JXButton(new AbstractAction(I18n.tr("Unshare All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                cleanup();
                SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD.setValue(true);
                shareListManager.removeDocumentsFromPublicLists();
            }
        });
        buttonDecorator.decorateDarkFullButton(unshareAllButton);
        
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(continueSharingButton);
        buttons.add(unshareAllButton);
        buttons.setBackground(backgroundColor);
        
        add(buttons, "alignx center, gaptop 15");
    }

    private void cleanup() {
        layeredPane.removeComponentListener(this);
        layeredPane.remove(this);
        showing.set(false);
    }

    @Inject
    public void register(SharedFileListManager sharedFileListManager) {
        sharedFileListManager.getModel().getReadWriteLock().readLock().lock();
        try {
            for (SharedFileList shareList : sharedFileListManager.getModel()) {
                if (shareList.isPublic()) {
                    shareList.getSwingModel().addListEventListener(
                            new ListEventListener<LocalFileItem>() {
                                @Override
                                public void listChanged(ListEvent<LocalFileItem> listChanges) {
                                    while (listChanges.next()) {
                                        if (listChanges.getType() == ListEvent.INSERT
                                                || listChanges.getType() == ListEvent.UPDATE) {
                                            LocalFileItem localFileItem = listChanges
                                                    .getSourceList().get(listChanges.getIndex());
                                            if (CategoryUtils.getCategory(localFileItem.getFile()) == Category.DOCUMENT
                                                    && SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD
                                                            .getValue()) {
                                                showDocumentSharingWarning();
                                            }
                                        }
                                    }
                                }
                            });
                }
            }
        } finally {
            sharedFileListManager.getModel().getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public void resize() {
        Rectangle parentBounds = layeredPane.getBounds();
        int w = getPreferredSize().width;
        int h = getPreferredSize().height;
        setLocation(parentBounds.width - w, parentBounds.height - h);
    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentResized(ComponentEvent e) {
        resize();
    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    public void showDocumentSharingWarning() {
        if (!showing.getAndSet(true)) {
            layeredPane.add(this, JLayeredPane.MODAL_LAYER);
            layeredPane.addComponentListener(this);
            resize();
        }
    }
}

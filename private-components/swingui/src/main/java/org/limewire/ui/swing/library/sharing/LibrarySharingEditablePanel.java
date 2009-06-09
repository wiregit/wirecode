package org.limewire.ui.swing.library.sharing;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.library.sharing.actions.ApplySharingAction;
import org.limewire.ui.swing.library.sharing.actions.CancelSharingAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibrarySharingEditablePanel {
    
    private final JPanel component;
    private PromptTextField filterTextField;
    private HyperlinkButton allButton;
    private HyperlinkButton noneButton;
    private JXButton applyButton;
    private HyperlinkButton cancelButton;
    
    private Provider<LibrarySharingTable> sharingTableProvider;
    private Provider<LibrarySharingEditableRendererEditor> renderer;
    private Provider<LibrarySharingEditableRendererEditor> editor;
    
    private LibrarySharingTable sharingTable;
    
    @Inject
    public LibrarySharingEditablePanel(Provider<LibrarySharingTable> sharingTableProvider,
            Provider<LibrarySharingEditableRendererEditor> renderer,
            Provider<LibrarySharingEditableRendererEditor> editor,
            ApplySharingAction applyAction, CancelSharingAction cancelAction) {
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx", "[125!]", ""));
        
        this.sharingTableProvider = sharingTableProvider;
        this.renderer = renderer;
        this.editor = editor;
        
        component.setOpaque(false);
        
        component.add(new JLabel(I18n.tr("Share list with...")), "gapleft 5, gaptop 5, wrap");
        
        filterTextField = new PromptTextField(I18n.tr("Find..."));
        
        component.add(filterTextField, "gapleft 5, gaptop 5, wmax 115, wrap");

        component.add(new JLabel(I18n.tr("Select")), "gapleft 5, gaptop 5, wrap");
        allButton = new HyperlinkButton(I18n.tr("all"));
        noneButton = new HyperlinkButton(I18n.tr("none"));
        component.add(allButton, "gapleft 15, gaptop 5, wrap");
        component.add(noneButton, "gapleft 15, gaptop 5, wrap");
        
        //TODO: this needs to be created lazily
        initTable();
        
        applyButton = new JXButton(applyAction);
        cancelButton = new HyperlinkButton(cancelAction);
        
        component.add(applyButton, "split 2, gaptop 5, gapright unrelated, alignx center");
        component.add(cancelButton, "gaptop 5, wrap");
    }
    
    private void initTable() {
        sharingTable = sharingTableProvider.get();
        sharingTable.enableEditing(true);
                
        JScrollPane scrollPane = new JScrollPane(sharingTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        
        sharingTable.getColumnModel().getColumn(0).setCellRenderer(renderer.get());
        sharingTable.getColumnModel().getColumn(0).setCellEditor(editor.get());
        
        component.add(scrollPane, "growx, gaptop 5, wrap");
    }
    
    public JComponent getComponent() {
        return component;
    }
}

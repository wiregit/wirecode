package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;

public class SharedFileCountPopupPanel extends Panel implements Resizable {
   
    @Resource private Color dividerForeground = PainterUtils.TRASPARENT;;
    @Resource private Color rolloverBackground = PainterUtils.TRASPARENT;
    @Resource private Color activeBackground = PainterUtils.TRASPARENT;
    @Resource private Color activeBorder = PainterUtils.TRASPARENT;
    @Resource private Color border = PainterUtils.TRASPARENT;
    @Resource private Font headerFont;
    
    private final SharedFileCountPanel sharedFileCountPanel;
    private final HeaderBarDecorator barDecorator;
    private final SharedFileListManager shareListManager;
    
    private JXPanel frame = null;
    private JTable table = null;
    
    private final AbstractAction closeAction = new AbstractAction(I18n.tr("Hide")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            sharedFileCountPanel.repaint();
        }
    };
        
    @Inject
    public SharedFileCountPopupPanel(SharedFileCountPanel sharedFileCountPanel,
            HeaderBarDecorator barDecorator, SharedFileListManager shareListManager) {
        super(new BorderLayout());
        
        this.sharedFileCountPanel = sharedFileCountPanel;
        this.barDecorator = barDecorator;
        this.shareListManager = shareListManager;
        
        GuiUtils.assignResources(this);
        
        setUpButton();
        
        setVisible(false);
    }

    @Inject
    public void register() {
        sharedFileCountPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setVisible(!isVisible());
                sharedFileCountPanel.repaint();
            }
        });
        
        sharedFileCountPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (isVisible()) {
                    resize();
                }
            }
        });
    }
    
    private void initContent() {
        
        frame = new JXPanel(new BorderLayout());
        frame.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, border));
        
        HeaderBar bar = new HeaderBar(I18n.tr("Sharing"));
        barDecorator.decorateBasic(bar);
        bar.setFont(headerFont);
        bar.setLayout(new BorderLayout());
        ResizeUtils.forceHeight(bar, 18);
        
        HyperlinkButton closeButton = new HyperlinkButton(closeAction);
        closeButton.setFont(headerFont);
        closeButton.setNormalForeground(Color.WHITE);
        closeButton.setRolloverForeground(Color.WHITE);
        bar.add(closeButton, BorderLayout.EAST);
        
        frame.add(bar, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        
        JLabel titleLabel = new JLabel(I18n.tr("You're sharing the following lists"));
        titleLabel.setOpaque(false);
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        
        table = new JTable(new EventTableModel<SharedFileList>(shareListManager.getModel(),
                new TableFormat<SharedFileList>() {
                    @Override
                    public int getColumnCount() {
                        return 2;
                    }
                    @Override
                    public String getColumnName(int column) {
                        if (column == 0) {
                            return "Name";
                        } 
                        else {
                            return "Files";
                        }
                    }
                    @Override
                    public Object getColumnValue(SharedFileList baseObject, int column) {
                        if (column == 0) {
                            return baseObject.getCollectionName();
                        } 
                        else {
                            return I18n.trn("{0} file", "{0} files", baseObject.size());
                            		
                        }
                    }
        }));
        
        table.setOpaque(false);
        table.setShowGrid(false);
        table.setFocusable(false);
        table.setCellSelectionEnabled(false);

        final ListEventListener<LocalFileItem> repaintListener = new RepaintListener();
            
 
        
        for ( SharedFileList item : shareListManager.getModel() ) {
            item.getSwingModel().addListEventListener(repaintListener);
        }
        
        shareListManager.getModel().addListEventListener(new ListEventListener<SharedFileList>() {
            @Override
            public void listChanged(ListEvent<SharedFileList> listChanges) {
                while(listChanges.next()) {
                    if (listChanges.getType() == ListEvent.INSERT) {
                        listChanges.getSourceList().get(listChanges.getIndex()).
                            getModel().addListEventListener(repaintListener);
                    }
                }
            }
        });
        
        contentPanel.add(table, BorderLayout.CENTER);

        frame.add(scrollPane, BorderLayout.CENTER);

        add(frame, BorderLayout.CENTER);
        
        frame.setPreferredSize(new Dimension(300, 200));
        
        frame.validate();
        frame.invalidate();
        validate();
        invalidate();
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (table == null && visible) {
            initContent();
        }
        
        super.setVisible(visible);
        
        if (visible) {
            resize();
        }
    }
    
    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = frame.getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds((int)sharedFileCountPanel.getBounds().getX(),
                (int)parentBounds.getHeight() - h, w, h);
    }
    
    private void setUpButton() {
        sharedFileCountPanel.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
        sharedFileCountPanel.setFocusPainted(false);
        sharedFileCountPanel.setBorderPainted(false);
        sharedFileCountPanel.setFocusable(false);
        sharedFileCountPanel.setOpaque(false);
        sharedFileCountPanel.setBackgroundPainter(new StatusBarPopupButtonPainter());
    }

    private class StatusBarPopupButtonPainter extends AbstractPainter<JXButton>{

        public StatusBarPopupButtonPainter() {
            setAntialiasing(false);
            setCacheable(false);
        }

        @Override
        protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
            if(SharedFileCountPopupPanel.this.isVisible()) {
                g.setPaint(activeBackground);
                g.fillRect(0, 0, width, height);
                g.setPaint(border);
                g.drawLine(0, 0, 0, height-1);
                g.drawLine(0, height-1, width-1, height-1);
                g.drawLine(width-1, 0, width-1, height-1);
            } else if (object.getModel().isRollover()) {
                g.setPaint(rolloverBackground);
                g.fillRect(0, 2, width-1, height-2);
                g.setPaint(activeBorder);
                g.drawLine(0, 1, 0, height-1);
                g.drawLine(width-1, 1, width-1, height-1);
            }
            else {
                g.setPaint(dividerForeground);
                g.drawLine(0, 3, 0, height-4);
                g.drawLine(width-1, 3, width-1, height-4);
            }
        }    
    }
    
    private class RepaintListener implements ListEventListener<LocalFileItem> {
            
        private final Timer repaintTimer; 
            
        public RepaintListener() {
        
            repaintTimer = new Timer(15, new ActionListener() {
                   @Override
                   public void actionPerformed(ActionEvent e) {
                       table.repaint();
                   }
            });
            repaintTimer.setRepeats(false);
        } 
            
        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            repaintTimer.start();
        }
    }
}

package org.limewire.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class Wizard extends JPanel {
    
    @Resource Color background;
    @Resource Color border;
    @Resource Color titleBarBackground;
    @Resource Color titleBarForeground;
    @Resource Color titleBarBorder;
    @Resource Font titleBarFont;
    
    private JDialog dialog;    
 
    private final JLabel titleBarLabel;
    private final JLabel headerLine1;
    private final JLabel headerLine2;
    
    private int currentPage;

    private final JXButton continueButton;
    
    private Action continueAction = new AbstractAction(I18n.tr("Continue")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            next();
        }
    };
    
    private JButton backButton;
    private Action backAction = new AbstractAction(I18n.tr("Go back")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            back();
        }
    };
    
    private JXButton finishButton;    
    private Action finishAction = new AbstractAction(I18n.tr("Finish")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            finish();
        }
    };
    
    private List<WizardPage> pageList;
    
    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    public Wizard(SetupComponentDecorator decorator){
        super(new BorderLayout());
        
        GuiUtils.assignResources(this);
        
        setBackground(background);
        setBorder(new LineBorder(border, 3));
        
        pageList = new ArrayList<WizardPage>();
        
        continueButton = new JXButton(continueAction);
        decorator.decorateGreenButton(continueButton);
        
        backButton = new HyperLinkButton((String)backAction.getValue(Action.NAME), backAction);
        
        finishButton = new JXButton(finishAction);
        decorator.decorateGreenButton(finishButton);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(getBackground());
        
        titleBarLabel = new JLabel();
        titleBarLabel.setOpaque(true);
        titleBarLabel.setFont(titleBarFont);
        titleBarLabel.setBackground(titleBarBackground);
        titleBarLabel.setForeground(titleBarForeground);
        titleBarLabel.setBorder(new LineBorder(titleBarBorder,3));
        
        headerLine1 = new JLabel();
        decorator.decorateHeadingText(headerLine1);
        
        headerLine2 = new JLabel();
        decorator.decorateNormalText(headerLine2);
        
        JXPanel headerBar = new JXPanel(new MigLayout("insets 14, gap 0, fill"));
        decorator.decorateSetupHeader(headerBar);

        headerBar.add(titleBarLabel, "dock north, growx");
        headerBar.add(headerLine1, "growx, wrap");
        headerBar.add(headerLine2, "growx");

        // TODO: Redo this layout
        JPanel bottomBar = new JPanel(new BorderLayout());
        JPanel bottomBarInner = new JPanel(new FlowLayout());
        bottomBarInner.add(backButton);
        bottomBarInner.add(continueButton);
        bottomBarInner.add(finishButton);
        bottomBar.add(bottomBarInner, BorderLayout.EAST);
        
        add(headerBar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }
    
    public void addPage(WizardPage page){
        pageList.add(page);
        mainPanel.add(page, pageList.indexOf(page) + "");
    }
    
    public int getPageCount(){
        return pageList.size();
    }
    
    public void showDialogIfNeeded(Frame owner){
        if (getPageCount() > 0) {
            setCurrentPage(0);

            dialog = new LimeJDialog(owner, true);
            
            dialog.setPreferredSize(new Dimension(650, 490));
            dialog.setMinimumSize(dialog.getPreferredSize());
            dialog.setMaximumSize(dialog.getPreferredSize());
            
            dialog.setUndecorated(true);
            dialog.add(this);
            dialog.pack();
            dialog.setLocationRelativeTo(owner);
            dialog.setVisible(true);
        }
    }
    
    protected void finish() {
        for (WizardPage page : pageList) {
            page.applySettings();
        }
        if(dialog != null){
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
        }
    }

    private void back() {
        setCurrentPage(currentPage - 1);
    }

    private void updateTitle(WizardPage page) {
        if (pageList.size() > 1) {
            titleBarLabel.setText(I18n.tr("Setup - step {0} of {1}", currentPage + 1, pageList.size()));
        } else {
            titleBarLabel.setText(I18n.tr("Setup"));
        }
        
        headerLine1.setText(page.getLine1());
        headerLine2.setText(page.getLine2());
    }
    
    private void next(){
        setCurrentPage(currentPage + 1);
    }
    
    public void setCurrentPage(int step){
        currentPage = step;
        cardLayout.show(mainPanel, currentPage + "");
        finishButton.setVisible(currentPage == pageList.size() - 1);
        continueButton.setVisible(!finishButton.isVisible());
        backButton.setVisible(currentPage != 0);
        updateTitle(pageList.get(currentPage));
    }
}

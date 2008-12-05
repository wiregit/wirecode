package org.limewire.ui.swing.wizard;

import java.awt.CardLayout;
import java.awt.Color;
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
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class Wizard extends JPanel{
    
    @Resource Color background;
    @Resource Color border;
    @Resource Color titleBarBackground;
    @Resource Color titleBarForeground;
    @Resource Color titleBarBorder;
    @Resource Font titleBarFont;
    
    private JDialog dialog;    
 
    private JLabel titleLabel;
    
    private int currentPage;

    private JXButton continueButton;
    
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
        super(new MigLayout("nogrid, insets 4"));
        
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
        
        titleLabel = new JLabel();
        titleLabel.setOpaque(true);
        titleLabel.setFont(titleBarFont);
        titleLabel.setBackground(titleBarBackground);
        titleLabel.setForeground(titleBarForeground);
        titleLabel.setBorder(new LineBorder(titleBarBorder,3));
        
        add(titleLabel, "dock north");
        add(mainPanel, "push, wrap");
        
        add(backButton, "tag back");
        add(continueButton, "tag next, hidemode 3");
        add(finishButton, "tag next, hidemode 3");
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

    private void updateTitle() {
        if (pageList.size() > 1) {
            titleLabel.setText(I18n.tr("Setup - step {0} of {1}", currentPage + 1, pageList.size()));
        } else {
            titleLabel.setText(I18n.tr("Setup"));
        }
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
        updateTitle();
    }

}

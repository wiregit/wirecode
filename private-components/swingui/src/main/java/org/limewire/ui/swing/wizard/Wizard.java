package org.limewire.ui.swing.wizard;

import java.awt.CardLayout;
import java.awt.Color;
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

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.util.I18n;


import net.miginfocom.swing.MigLayout;

public class Wizard extends JPanel{
    
    private JDialog dialog;    
 
    private JLabel titleLabel;
    
    private int currentPage;

    private JButton continueButton;    
    private Action continueAction = new AbstractAction(I18n.tr("Continue")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            next();
        }
    };
    
    private JButton backButton;
    private Action backAction = new AbstractAction(I18n.tr("<Go back")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            back();
        }
    };
    
    private JButton finishButton;    
    private Action finishAction = new AbstractAction(I18n.tr("Finish")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            finish();
        }
    };
    
    private List<WizardPage> pageList;
    
    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    public Wizard(){
        super(new MigLayout("nogrid"));
        
        setBackground(Color.WHITE);
        setBorder(new LineBorder(Color.BLACK));
        
        pageList = new ArrayList<WizardPage>();
        
        continueButton = new JButton(continueAction);
        continueButton.setBackground(getBackground());
        backButton = new HyperLinkButton((String)backAction.getValue(Action.NAME), backAction);
        finishButton = new JButton(finishAction);
        finishButton.setBackground(getBackground());
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        titleLabel = new JLabel();
        titleLabel.setOpaque(true);
        titleLabel.setBackground(Color.LIGHT_GRAY);
        titleLabel.setBorder(new LineBorder(Color.BLACK));
        
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
            dialog = new JDialog(owner, true);
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

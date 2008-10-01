package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

public class SearchBar extends JXPanel {
    @Resource
    private Icon buttonIcon;

    public SearchBar(JComboBox comboBox, JComponent textField) {
        //TODO:proper sizing and colors
        super(new FlowLayout(FlowLayout.LEFT, 5, 2));
        GuiUtils.assignResources(this);
        comboBox.setPreferredSize(new Dimension(104, 18));
        textField.setPreferredSize(new Dimension(242, 18));
        add(comboBox);
        add(textField);

        SearchComboUI comboUI = new SearchComboUI();
        comboBox.setUI(comboUI);
        comboUI.init(buttonIcon);
        comboBox.setOpaque(false);
        textField.setOpaque(false);
        setOpaque(false);
        setBackgroundPainter(new SearchBarPainter(comboBox, textField));
    }

    private static class SearchBarPainter extends AbstractPainter<JXPanel> {
        // TODO move colors to properties
        private Color leftColor = Color.decode("#bfd8e9");
        private Color rightColor = Color.WHITE;
        
        private JComponent leftComponent;

        //private JComponent rightComponent;

        public SearchBarPainter(JComponent leftComponent, JComponent rightComponent) {
            this.leftComponent = leftComponent;
           // this.rightComponent = rightComponent;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdesktop.swingx.painter.AbstractPainter#doPaint(java.awt.Graphics2D,
         *      java.lang.Object, int, int)
         */
        @Override
        protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
            Area rightShape = new Area(
                    new RoundRectangle2D.Float(leftComponent.getWidth() + 5, 0, object.getWidth() - leftComponent.getWidth() - 5, 
                            object.getHeight(), 20, object.getHeight()));
            
            Area leftArea = new Area(new RoundRectangle2D.Float(0, 0, object.getWidth(), object.getHeight(), 20, object.getHeight()));
            leftArea.subtract(rightShape);
            
            g.setColor(leftColor);
            g.fill(leftArea);
            g.setColor(rightColor);
            g.fill(rightShape);
        }
    }
    
    private static class SearchComboUI extends BasicComboBoxUI{
//      TODO move to properties
        private Color leftColor = Color.decode("#bfd8e9");
        
        public void init(Icon buttonIcon){
            //TODO this isn't working - need to look at BasicComboBoxUI
            arrowButton.setMargin(new Insets(0, 0, 0, 0));
            arrowButton.setBorderPainted(false);
            arrowButton.setContentAreaFilled(false);
            arrowButton.setFocusPainted(false);
            arrowButton.setRolloverEnabled(true);
            arrowButton.setHideActionText(true);
            arrowButton.setBorder(null);
            arrowButton.setOpaque(false);
            arrowButton.setIcon(buttonIcon);
            
            if(editor instanceof JComponent){
                JComponent comp = (JComponent)editor;
                comp.setBorder(new EmptyBorder(0,0,0,0));
                comp.setOpaque(false);
            }
            
            comboBox.setOpaque(false);
            comboBox.setBorder(new EmptyBorder(0,0,0,0));
            comboBox.setBackground(leftColor);
        }
        
    }
}

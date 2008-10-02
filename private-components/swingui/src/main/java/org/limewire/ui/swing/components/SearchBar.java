package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;


public class SearchBar extends JXPanel {
    //TODO: move to properties file
    private static final int ARC_WIDTH = 20;
    private static final int GAP = 5;
    @Resource
    private Icon buttonIcon;

    public SearchBar(JComboBox comboBox, JComponent textField) {
        //TODO:proper sizing and colors
        super(new MigLayout("ins 0, gapx 0, gapy 0"));
        GuiUtils.assignResources(this);
        comboBox.setPreferredSize(new Dimension(104 - ARC_WIDTH, 22));
        textField.setPreferredSize(new Dimension(242 - GAP, 22));
       // setPreferredSize(new Dimension(346, 22));
        add(comboBox, "gapbefore " + ARC_WIDTH);
        add(textField, "gapbefore " + GAP);

        textField.setFont(textField.getFont().deriveFont(12));
        SearchComboUI comboUI = new SearchComboUI(buttonIcon);
        comboBox.setUI(comboUI);
        comboUI.init();
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
            Shape oldClip = g.getClip();
            Area rightShape = new Area(
                    new RoundRectangle2D.Float(leftComponent.getWidth() + ARC_WIDTH, 0, object.getWidth() - leftComponent.getWidth() - ARC_WIDTH, 
                            object.getHeight(), ARC_WIDTH, object.getHeight()));
            
            Area leftArea = new Area(new RoundRectangle2D.Float(0, 0, object.getWidth(), object.getHeight(), ARC_WIDTH, object.getHeight()));
            leftArea.subtract(rightShape);
            
            g.setColor(leftColor);
            g.fill(leftArea);
            g.setColor(rightColor);
            g.fill(rightShape);
            //get rid of artifacts on left side of right shape.  drawing full right shape would leave artifacts on right side.
            g.setClip(rightShape.getBounds().x, rightShape.getBounds().y, ARC_WIDTH, object.getHeight());
            g.draw(rightShape);
            g.setClip(oldClip);
        }
    }
    
    private static class SearchComboUI extends BasicComboBoxUI{
//      TODO move to properties
        private Color leftColor = Color.decode("#bfd8e9");
        private Icon buttonIcon;
        
        public SearchComboUI(Icon buttonIcon){
            this.buttonIcon = buttonIcon;
        }
        
        public void init(){
            if(editor instanceof JComponent){
                JComponent comp = (JComponent)editor;
                comp.setBorder(new EmptyBorder(0,0,0,0));
                comp.setOpaque(false);
            }
            
            comboBox.setOpaque(false);
            comboBox.setBorder(new EmptyBorder(0,0,0,0));
            comboBox.setBackground(leftColor);
            
//            ListCellRenderer renderer = comboBox.getRenderer();
//            if (renderer instanceof JLabel){
//                ((JLabel)renderer).setHorizontalAlignment(SwingConstants.RIGHT);
//            }
        }
        
        @Override
        protected JButton createArrowButton() {
            JButton button = new IconButton(buttonIcon);
            button.setName("ComboBox.arrowButton");
            return button;
        }
        
        @Override
        public void paintCurrentValue(Graphics g,Rectangle bounds,boolean hasFocus) {
            super.paintCurrentValue(g, bounds, false);
        }
    }
}

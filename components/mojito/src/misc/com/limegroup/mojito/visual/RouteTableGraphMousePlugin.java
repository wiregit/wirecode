package com.limegroup.mojito.visual;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;

import edu.uci.ics.jung.visualization.PickedState;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;

public class RouteTableGraphMousePlugin extends AbstractGraphMousePlugin
            implements MouseListener{

    public RouteTableGraphMousePlugin() {
        super(InputEvent.BUTTON1_DOWN_MASK);
    }

    public void mouseClicked(MouseEvent e) {
        //we only want double clicks
        if(e.getClickCount() < 2) {
            return;
        }
        
        VisualizationViewer vv = (VisualizationViewer)e.getSource();
        PickedState pickedState = vv.getPickedState();
        if(pickedState == null ) {
            return;
        }
        
        Set vSet = pickedState.getPickedVertices();
        if(vSet.isEmpty()) {
            return;
        }
        Object o = vSet.iterator().next();
        if(o == null || !(o instanceof BucketVertex)) {
            return;
        }
        BucketVertex bucketVertex = (BucketVertex)o;
        
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

}

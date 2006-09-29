package com.limegroup.mojito.visual;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.visual.BinaryEdge.EdgeType;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.PickableEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.visualization.DefaultGraphLabelRenderer;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.contrib.TreeLayout;
import edu.uci.ics.jung.visualization.control.CrossoverScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ViewScalingGraphMousePlugin;

public class RouteTableVisualizer implements RouteTableGraphCallback{
    
    private JPanel graphComponent;
    
    private JTextArea txtArea;
    
    private VisualizationViewer vv;
    
    private RouteTableGraph routeTableGraph;
    
    public static RouteTableVisualizer show(Context context) {
        final RouteTableVisualizer viz = new RouteTableVisualizer(context);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final JFrame jf = new JFrame();
                jf.getContentPane().add (viz.getComponent());
                jf.pack();
                jf.addWindowListener(new WindowListener() {
                    public void windowActivated(WindowEvent e) {}
                    public void windowClosed(WindowEvent e) {}
                    public void windowClosing(WindowEvent e) {
                        viz.stop();
                    }
                    public void windowDeactivated(WindowEvent e) {}
                    public void windowDeiconified(WindowEvent e) {}
                    public void windowIconified(WindowEvent e) {}
                    public void windowOpened(WindowEvent e) {}
                });
                jf.setVisible(true);
            }
        });
        return viz;
    }
    
    public RouteTableVisualizer(Context dht) {
        //TODO: change constructor
        RouteTableImpl routeTable = (RouteTableImpl)dht.getRouteTable();
        routeTableGraph = new BucketGraph(routeTable, this);
        init();
    }
    
    private void init() {
        //now update the graph with the route table data
        routeTableGraph.populateGraph();

        //create layout
        Layout layout = new TreeLayout(routeTableGraph.getTree());
        //render graph
        PluggableRenderer pr = new PluggableRenderer();
        //vertex
        pr.setVertexPaintFunction(new RouteTableVertexPaintFunction(
                pr, 
                Color.black, 
                Color.white,
                Color.blue,
                Color.yellow,
                Color.red));
        pr.setVertexShapeFunction(new RouteTableVertexShapeFunction());
        VertexStringer vertStringer = new VertexStringer() {
            public String getLabel(ArchetypeVertex v) {
                if(v instanceof BucketVertex) {
                    return v.toString();
                }
                else return "";
            }
            
        };
        pr.setVertexStringer(vertStringer);
        //edge
        EdgeStringer edgeStringer = new EdgeStringer(){
            public String getLabel(ArchetypeEdge e) {
                if(!(e instanceof BinaryEdge)) {
                    return e.toString();
                }
                BinaryEdge be = (BinaryEdge)e;
                return (be.getType().equals(EdgeType.LEFT)?"0":"1");
            }
        };
        pr.setEdgeStringer(edgeStringer);
        pr.setEdgePaintFunction(new PickableEdgePaintFunction(pr, Color.black, Color.cyan));
        pr.setEdgeShapeFunction(new EdgeShape.Line()); 
        pr.setGraphLabelRenderer(new DefaultGraphLabelRenderer(Color.cyan, Color.cyan));

        //create JPanel
        vv =  new VisualizationViewer(layout, pr, new Dimension(400,400));
        vv.setPickSupport(new ShapePickSupport());
        vv.setBackground(Color.white);
        // add a listener for ToolTips
        vv.setToolTipFunction(new RouteTableToolTipFunction());

        PluggableGraphMouse graphMouse = new PluggableGraphMouse();
        graphMouse.add(new PickingGraphMousePlugin());
        graphMouse.add(new ViewScalingGraphMousePlugin());
        graphMouse.add(new CrossoverScalingGraphMousePlugin());
        graphMouse.add(new RouteTableGraphMousePlugin());
        vv.setGraphMouse(graphMouse);
        
        //create south panel
        txtArea = new JTextArea();
        JScrollPane pane = new JScrollPane(txtArea);
        
        //create main compononent
        JPanel graphPanel = new GraphZoomScrollPane(vv);
        graphComponent = new JPanel(new BorderLayout());
        graphComponent.add(graphPanel, BorderLayout.CENTER);
        graphComponent.add(pane, BorderLayout.SOUTH);
        
        
    }
    
    public synchronized void handleGraphLayoutUpdated() {
        vv.setGraphLayout(new TreeLayout(routeTableGraph.getTree()));
        vv.invalidate();
        vv.revalidate();
        vv.repaint();
    }
    
    public synchronized void handleGraphInfoUpdated() {
        txtArea.setText(routeTableGraph.getGraphInfo());        
    }

    public Component getComponent() {
        return graphComponent;
    }
    
    public void stop() {
        routeTableGraph.deregister();
    }
}

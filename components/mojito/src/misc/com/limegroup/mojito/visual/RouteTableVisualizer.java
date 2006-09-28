package com.limegroup.mojito.visual;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.Timer;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.routing.impl.Bucket;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.visual.BinaryEdge.EdgeType;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.DefaultToolTipFunction;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.PickableEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.impl.SparseTree;
import edu.uci.ics.jung.utils.UserData;
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

public class RouteTableVisualizer {
    
    /**
     * the graph
     */
    private RootableSparseTree graph;
    
    private RouteTableImpl routeTable;
    
    private Vertex root;
    
    private Component graphComponent;
    
    Timer timer;
    
    private VisualizationViewer vv;
    
    public static RouteTableVisualizer show(Context context) {
        final RouteTableVisualizer viz = new RouteTableVisualizer(context);
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
        return viz;
    }
    
    public RouteTableVisualizer(Context dht) {
        this.routeTable = (RouteTableImpl)dht.getRouteTable();
        init();
        timer = scheduleTimerTask();
        timer.start();
    }
    
    private Timer scheduleTimerTask() {
        Timer timer = new Timer(1000, new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                updateGraph();
                vv.setGraphLayout(new TreeLayout(graph));
                vv.invalidate();
                vv.revalidate();
                vv.repaint();
            }
        });
        return timer;
    }
    
    private void init() {
        List<Bucket> buckets = new ArrayList<Bucket>(routeTable.getBuckets());
        //create new sparse tree graph
        root = new BucketVertex(buckets.get(0), true);
        graph = new RootableSparseTree(root);
        
        updateGraph();
        PluggableRenderer pr = new PluggableRenderer();
        pr.setVertexPaintFunction(new RouteTableVertexPaintFunction(
                pr, 
                Color.black, 
                Color.white,
                Color.blue,
                Color.yellow,
                Color.red));
        
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
        pr.setGraphLabelRenderer(new DefaultGraphLabelRenderer(Color.cyan, Color.cyan));

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

        Layout layout = new TreeLayout(graph);

        vv =  new VisualizationViewer(layout, pr, new Dimension(400,400));
        vv.setPickSupport(new ShapePickSupport());
        pr.setEdgeShapeFunction(new EdgeShape.Line()); 
        vv.setBackground(Color.white);

        // add a listener for ToolTips
        vv.setToolTipFunction(new DefaultToolTipFunction());

        PluggableGraphMouse graphMouse = new PluggableGraphMouse();
        graphMouse.add(new PickingGraphMousePlugin());
        graphMouse.add(new ViewScalingGraphMousePlugin());
        graphMouse.add(new CrossoverScalingGraphMousePlugin());

        vv.setGraphMouse(graphMouse);
        graphComponent = new GraphZoomScrollPane(vv);
    }
    
    public Component getComponent() {
        return graphComponent;
    }
    
    public synchronized void updateGraph() {
        List<Bucket> buckets = new ArrayList<Bucket>(routeTable.getBuckets());
        int count = buckets.size();
        
        if(count < 2) {
            root = new BucketVertex(buckets.get(0), true);
            graph.newRoot(root);
            return;
        } else {
            root = new InteriorNodeVertex();
            graph.newRoot(root);
        }
        
        //TODO: optimization -- or not?
        //BucketUtils.sortByDepth(buckets);
        
        Bucket currentBucket;
        InteriorNodeVertex previousVertex;
        for(Iterator it = buckets.iterator(); it.hasNext();) {
            currentBucket = (Bucket) it.next();
            
            int depth = currentBucket.getDepth();
            previousVertex = (InteriorNodeVertex)root;
            for(int i=1; i < depth ; i++) {
                if(currentBucket.getBucketID().isBitSet(i-1)) {
                    if(previousVertex.getRightChild() == null) {
                        previousVertex = createInteriorNode(previousVertex, EdgeType.RIGHT);
                        
                    } else if(previousVertex.getRightChild() instanceof BucketVertex) {
                        break;
                    } else {
                        previousVertex = (InteriorNodeVertex)previousVertex.getRightChild();
                    }
                } else {
                    if(previousVertex.getLeftChild() == null) {
                        previousVertex = createInteriorNode(previousVertex, EdgeType.LEFT);
                        
                    } else if(previousVertex.getLeftChild() instanceof BucketVertex) {
                        break;
                    } else {
                        previousVertex = (InteriorNodeVertex)previousVertex.getLeftChild();
                    }
                }
            }
            //now add the bucket
            if(currentBucket.getBucketID().isBitSet(depth-1)) {
                createBucketVertex(currentBucket, previousVertex, EdgeType.RIGHT);
            } else {
                createBucketVertex(currentBucket, previousVertex, EdgeType.LEFT);
            }
            it.remove();
        }
    }
    
    private InteriorNodeVertex createInteriorNode(InteriorNodeVertex previousVertex, EdgeType type) {
        InteriorNodeVertex vertex = new InteriorNodeVertex();
        graph.addVertex(vertex);
        graph.addEdge(new BinaryEdge(previousVertex, vertex, type));
        return vertex;
    }
    
    private BucketVertex createBucketVertex(Bucket bucket, InteriorNodeVertex predecessor, EdgeType type) {
        boolean isLocalBucket = bucket.contains(routeTable.getLocalNode().getNodeID());
        BucketVertex bv = new BucketVertex(bucket, isLocalBucket);
        graph.addVertex(bv);
        graph.addEdge(new BinaryEdge(predecessor, bv, type));
        return bv;
    }
    
    public void stop() {
        timer.stop();
    }
    
    private class RootableSparseTree extends SparseTree {
        public RootableSparseTree(Vertex root) {
            super(root);
        }
        
        /**
         * Clears the tree and add this <tt>Vertex</tt>
         * as the new root
         */
        public synchronized void newRoot(Vertex root) {
            removeAllEdges();
            removeAllVertices();
            this.mRoot = root;
            addVertex( root );
            mRoot.setUserDatum(SPARSE_ROOT_KEY, SPARSE_ROOT_KEY, UserData.SHARED);
            mRoot.setUserDatum(IN_TREE_KEY, IN_TREE_KEY, UserData.SHARED);
        }
    }
}

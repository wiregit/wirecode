package com.limegroup.mojito.visual;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.routing.impl.Bucket;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.visual.BinaryEdge.EdgeType;

import edu.uci.ics.jung.graph.Edge;

public class BucketGraph extends RouteTableGraph {
    
    public BucketGraph(RouteTableImpl routeTable, RouteTableGraphCallback callback) {
        super(routeTable, callback);
        List<Bucket> buckets = new ArrayList<Bucket>(routeTable.getBuckets());
        //create new sparse tree graph with one bucket
        root = new BucketVertex(buckets.get(0), true);
        tree = new RootableSparseTree(root);
    }

    @Override
    public void populateGraph() {
        List<Bucket> buckets = new ArrayList<Bucket>(routeTable.getBuckets());
        int count = buckets.size();
        
        if(count < 2) {
            root = new BucketVertex(buckets.get(0), true);
            tree.newRoot(root);
            return;
        } else {
            root = new InteriorNodeVertex();
            tree.newRoot(root);
        }
        
        //TODO: optimization -- or not?
        //BucketUtils.sortByDepth(buckets);
        Bucket currentBucket;
        for(Iterator it = buckets.iterator(); it.hasNext();) {
            currentBucket = (Bucket) it.next();
            updateGraphBucket(currentBucket);
            it.remove();
        }
    }
    
    @Override
    public String getGraphInfo() {
        String rtString = routeTable.toString();
        return rtString.substring(rtString.indexOf("Total"));
    }

    private void updateGraphBucket(Bucket bucket) {
        InteriorNodeVertex InteriorNode = getVertexForBucket(bucket);
        
        //now add the bucket
        if(bucket.getBucketID().isBitSet(bucket.getDepth()-1)) {
            createBucketVertex(bucket, InteriorNode, EdgeType.RIGHT);
        } else {
            createBucketVertex(bucket, InteriorNode, EdgeType.LEFT);
        }
    }
    
    private InteriorNodeVertex splitBucket(BucketVertex vertex, boolean isLeftChild) {
        InteriorNodeVertex predecessor = 
            (InteriorNodeVertex)vertex.getPredecessors().iterator().next(); 
        tree.removeEdge((Edge)vertex.getInEdges().iterator().next());
        tree.removeVertex(vertex);
        EdgeType type = (isLeftChild?EdgeType.LEFT:EdgeType.RIGHT);
        return createInteriorNode(predecessor, type);
    }
    
    private InteriorNodeVertex getVertexForBucket(Bucket bucket) {
        int depth = bucket.getDepth();

        InteriorNodeVertex vertex = (InteriorNodeVertex)root;
        for(int i=1; i < depth ; i++) {
            if(bucket.getBucketID().isBitSet(i-1)) {
                if(vertex.getRightChild() == null) {
                    vertex = createInteriorNode(vertex, EdgeType.RIGHT);
                    
                } else if(vertex.getRightChild() instanceof BucketVertex) {
                    //we have found a bucket along this bucket path
                    //--> split it in order to be able to insert new bucket
                    BucketVertex bv = (BucketVertex)vertex.getRightChild();
                    vertex = splitBucket(bv, false);
                } else {
                    vertex = (InteriorNodeVertex)vertex.getRightChild();
                }
            } else {
                if(vertex.getLeftChild() == null) {
                    vertex = createInteriorNode(vertex, EdgeType.LEFT);
                    
                } else if(vertex.getLeftChild() instanceof BucketVertex) {
                    BucketVertex bv = (BucketVertex)vertex.getLeftChild();
                    vertex = splitBucket(bv, true);
                } else {
                    vertex = (InteriorNodeVertex)vertex.getLeftChild();
                }
            }
        }
        return vertex;
    }
    
    private InteriorNodeVertex createInteriorNode(InteriorNodeVertex previousVertex, EdgeType type) {
        InteriorNodeVertex vertex = new InteriorNodeVertex();
        tree.addVertex(vertex);
        tree.addEdge(new BinaryEdge(previousVertex, vertex, type));
        return vertex;
    }
    
    private BucketVertex createBucketVertex(Bucket bucket, InteriorNodeVertex predecessor, EdgeType type) {
        boolean isLocalBucket = bucket.contains(routeTable.getLocalNode().getNodeID());
        BucketVertex bv = new BucketVertex(bucket, isLocalBucket);
        tree.addVertex(bv);
        tree.addEdge(new BinaryEdge(predecessor, bv, type));
        return bv;
    }

    public void add(Bucket bucket, Contact node) {
        callback.handleGraphInfoUpdated();
    }

    public void check(Bucket bucket, Contact existing, Contact node) {
    }

    public void clear() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                populateGraph();
                callback.handleGraphLayoutUpdated();
            }
        });
    }

    public void remove(Bucket bucket, Contact node) {
    }

    public void replace(Bucket bucket, Contact existing, Contact node) {
    }

    public void split(Bucket bucket, final Bucket left, final Bucket right) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //are we splitting the root bucket
                if(left.getDepth() == 1) {
                    root = new InteriorNodeVertex();
                    tree.newRoot(root);
                }
                updateGraphBucket(left);
                updateGraphBucket(right);
                callback.handleGraphLayoutUpdated();
            }
        });
    }

    public void update(Bucket bucket, Contact existing, Contact node) {
    }

}

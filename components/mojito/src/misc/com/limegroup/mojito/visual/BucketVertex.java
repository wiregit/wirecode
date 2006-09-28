package com.limegroup.mojito.visual;

import com.limegroup.mojito.routing.impl.Bucket;

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;

class BucketVertex extends DirectedSparseVertex{

    private Bucket bucket;
    private boolean isLocalBucket;
    
    public BucketVertex(Bucket bucket, boolean isLocalBucket) {
        super();
        this.bucket = bucket;
        this.isLocalBucket = isLocalBucket;
    }
    
    @Override
    protected void addNeighbor_internal(Edge e, Vertex v) {
        super.addNeighbor_internal(e, v);
        DirectedEdge de = (DirectedEdge) e;
        if(de.getSource() == this) {
            throw new IllegalStateException("A Bucket Vertex is always a tree leaf");
        }
    }
    
    public boolean isLocalBucket() {
        return isLocalBucket;
    }
    
    public Bucket getBucket() {
        return bucket;
    }

    @Override
    public String toString() {
        return bucket.getBucketID().toBinString().substring(0, bucket.getDepth());
    }
}

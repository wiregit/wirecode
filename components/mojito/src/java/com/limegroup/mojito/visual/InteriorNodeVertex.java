package com.limegroup.mojito.visual;

import java.util.LinkedHashSet;
import java.util.Set;

import com.limegroup.mojito.visual.BinaryEdge.EdgeType;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;

class InteriorNodeVertex extends DirectedSparseVertex {
    
    private Vertex leftChild;
    private Vertex rightChild;
    
    public InteriorNodeVertex() {
        super();
    }

    @Override
    protected void addNeighbor_internal(Edge e, Vertex v) {
        super.addNeighbor_internal(e, v);

        if (! (e instanceof BinaryEdge))
            throw new IllegalArgumentException("This vertex " + 
                    "implementation only accepts binary edges");

        BinaryEdge be = (BinaryEdge) e;
        if(this == be.getSource()) {
            if(be.getType().equals(EdgeType.LEFT)) {
                if(leftChild == null) {
                    leftChild = v;
                } else {
                    throw new IllegalArgumentException("This vertex " + 
                    "already has a left child!");
                }
            } else {
                if(rightChild == null) {
                    rightChild = v;
                } else {
                    throw new IllegalArgumentException("This vertex " + 
                    "already has a right child!");
                }
            }
        }
    }
    
    @Override
    public Set getSuccessors() {
        Set<Vertex> res = new LinkedHashSet<Vertex>();
        res.add(leftChild);
        res.add(rightChild);
        return res;
    }

    public Vertex getLeftChild() {
        return leftChild;
    }

    public Vertex getRightChild() {
        return rightChild;
    }
    
}

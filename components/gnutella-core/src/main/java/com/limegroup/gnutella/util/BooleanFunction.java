package com.limegroup.gnutella.util;

/**
 * A view over two <tt>BitField</tt> instances that represents a boolean function.
 * This class itself is a BitField so several may be chained
 * to form complex functions.
 */
public class BooleanFunction implements BitField {
	public enum Operation {
		AND, OR, NOT, XOR
	}
	
	private final BitField a, b;
	private final Operation op;
	
	/**
	 * creates the specified boolean operatio over the bitfield parameters
	 */
	public BooleanFunction(BitField a, Operation op, BitField b) {
		if (op == Operation.NOT)
			assert b == a;
		assert a.maxSize() == b.maxSize() && op != null;
		this.a = a;
		this.b = b;
		this.op = op;
	}
	
	/**
	 * Creates a view inverse of the provided view.
	 */
	public BooleanFunction(BitField notted) {
		this(notted, Operation.NOT, notted);
	}

	public int cardinality() {
		int ret = 0;
		for (int i = 0; i < maxSize(); i++) {
			if (get(i))
				ret++;
		}
		return ret;
	}

	public boolean get(int i) {
		boolean ab = a.get(i);
		boolean bb = b.get(i);
		switch(op) {
		case AND : return ab & bb;
		case OR : return ab | bb;
		case NOT : return !ab;
		case XOR : return ab ^ bb;
		}
		throw new IllegalStateException();
	}

	public int nextClearBit(int i) {
		if (i >= maxSize())
			throw new IndexOutOfBoundsException(i+">"+maxSize());
		
		int ai, bi;
		switch(op) {
		case NOT :
			return a.nextSetBit(i);
		case AND :
			ai = a.nextClearBit(i);
			bi = b.nextClearBit(i);
			if (ai == -1 && bi == -1)
				return -1;
			if (ai == -1)
				return bi;
			if (bi == -1)
				return ai;
			return Math.min(ai, bi);
		
		case OR : 
			for (int index = i; index < maxSize();) {
				ai = a.nextClearBit(index);
				bi = b.nextClearBit(index);
				if (ai == -1 || bi == -1)
					return -1;
				if (ai == bi)
					return ai;
				index = Math.max(ai, bi);
			}
			return -1;
		
		case XOR :
			for (int index = i; index < maxSize(); index++) {
				if (a.get(index) == b.get(index))
					return index;
			}
			return -1;
		}
		throw new IllegalStateException();
		
	}

	public int nextSetBit(int i) {
		if (i >= maxSize())
			throw new IndexOutOfBoundsException(i+">"+maxSize());
		
		switch(op) {
		case NOT :
			return a.nextClearBit(i);
		
		case AND :
			for (int index = i; index < maxSize();) {
				int ai = a.nextSetBit(index);
				int bi = b.nextSetBit(index);
				if (ai == -1 || bi == -1)
					return -1;
				if (ai == bi)
					return ai;
				index = Math.max(ai, bi);
			}
			return -1;
		
		case OR :
			int ai = a.nextSetBit(i);
			int bi = b.nextSetBit(i);
			if (ai == -1 && bi == -1)
				return -1;
			if (ai == -1)
				return bi;
			if (bi == -1)
				return ai;
			return Math.min(ai, bi);
			
		case XOR :
			for (int index = i; index < maxSize(); index++) {
				if (a.get(index) != b.get(index))
					return index;
			}
			return -1;
		}
		throw new IllegalStateException();
	}
	
	public int maxSize() {
		return a.maxSize();
	}
	
}

package org.limewire.collection;

public class NotView implements BitField {

	private final BitField bf; 
	public NotView(BitField bf) {
		this.bf = bf;
	}
	public int cardinality() {
		return bf.maxSize() - bf.cardinality();
	}

	public boolean get(int i) {
		return !bf.get(i);
	}

	public int maxSize() {
		return bf.maxSize();
	}

	public int nextClearBit(int i) {
		return bf.nextSetBit(i);
	}

	public int nextSetBit(int i) {
		return bf.nextClearBit(i);
	}

}

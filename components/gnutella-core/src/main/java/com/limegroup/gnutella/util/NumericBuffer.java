package com.limegroup.gnutella.util;

public class NumericBuffer<T extends Number> extends Buffer<T> {

	public NumericBuffer(int size) {
		super(size);
	}

	public NumericBuffer(Buffer<? extends T> other) {
		super(other);
	}
	
	@SuppressWarnings("unchecked")
	protected T[] createArray(int size) {
		return (T[])new Number[size + 1];
	}
	
	public double average() {
		double result = 0;
		int num = 0;
		for (int i = 0; i < buf.length; i++) {
			if (buf[i] == null)
				continue;
			result = result + buf[i].doubleValue();
			num++;
		}
		return result / num;
	}

}

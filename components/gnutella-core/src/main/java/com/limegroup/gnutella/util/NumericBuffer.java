package com.limegroup.gnutella.util;

public class NumericBuffer<T extends Number> extends Buffer<T> {

	public NumericBuffer(int size) {
		super(size);
	}

	public NumericBuffer(Buffer<T> other) {
		super(other);
	}
	
	@SuppressWarnings("unchecked")
	protected void initialize() {
		if (buf == null)
			buf = (T[])new Number[size + 1];
	}
	
	public double average() {
		double result = 0;
		int num = 0;
		for (T n : buf) {
			if (n != null) {
				result = result + n.doubleValue();
				num++;
			}
		}
		return result / num;
	}

}

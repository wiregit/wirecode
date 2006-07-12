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
	
	/**
	 * @return the average of the elements in this buffer with the
	 * best accuracy possible - double if the elements are float or double
	 * and long otherwise.  
	 */
	public Number average() {
		Number sum = sum();
		if (sum instanceof Double)
			return sum().doubleValue() / size();
		else
			return sum().longValue() / size();
	}
	
	/**
	 * @return the sum of the elements in this buffer with the
	 * best accuracy possible - double if the elements are float or double
	 * and long otherwise.  
	 */
	public Number sum() {
		if (isEmpty())
			return new Integer(0);
		Number n = first();
		if (n instanceof Float || n instanceof Double)
			return doubleSum();
		else
			return longSum();
	}
	
	private double doubleSum() {
		double ret = 0;
		for (Number n : buf) {
			if (n != null) 
				ret += n.doubleValue();
		}
		return ret;
	}
	
	private long longSum() {
		long ret = 0;
		for (Number n : buf) {
			if (n != null)
				ret += n.longValue();
		}
		return ret;
	}
}

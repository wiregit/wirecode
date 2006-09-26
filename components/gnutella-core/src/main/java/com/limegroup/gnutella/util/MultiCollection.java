package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiCollection<Type> extends MultiIterable<Type> implements Collection<Type> {
	
	private final Iterable<Collection<? extends Type>> collections;

	public MultiCollection(Collection<? extends Type>... collections) {
		super((Iterable<? extends Type>[])collections);
		this.collections = new ArrayList<Collection<? extends Type>>(collections.length);
		for (Collection<? extends Type> o : collections)
			((List<Collection<? extends Type>>)this.collections).add(o);
	}
	
	public boolean add(Object arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection arg0) {
		throw new UnsupportedOperationException();	
	}

	public void clear() {
		for (Collection c : collections)
			c.clear();
	}

	public boolean contains(Object arg0) {
		for (Collection c : collections) {
			if (c.contains(arg0))
				return true;
		}
		return false;
	}

	public boolean containsAll(Collection arg0) {
		for (Object o : arg0) {
			if (contains(o))
				return true;
		}
		return false;
	}

	public boolean isEmpty() {
		for (Collection c : collections) {
			if (!c.isEmpty())
				return false;
		}
		return true;
	}

	public boolean remove(Object arg0) {
		for (Collection c : collections) {
			if (c.remove(arg0))
				return true;
		}
		return false;
	}

	public boolean removeAll(Collection arg0) {
		boolean ret = false;
		for (Object o : arg0) {
			if (remove(o))
				ret = true;
		}
		return ret;
	}

	public boolean retainAll(Collection<?> arg0) {
		boolean ret = false;
		for (Collection<? extends Type> c : collections) {
			if (c.retainAll(arg0))
				ret = true;
		}
		return ret;
	}

	public int size() {
		int ret = 0;
		for (Collection c : collections) 
			ret += c.size();
		return ret;
	}

	@SuppressWarnings("unchecked")
	public Object[] toArray() {
		List t = new ArrayList(size());
		for (Collection c : collections) {
			t.addAll(c);
		}
		return t.toArray();
	}

	@SuppressWarnings("unchecked")
	public <B>B[] toArray(B[] a) {
		List<B> t = new ArrayList<B>(size());
		for (Collection c : collections) {
			t.addAll(c);
		}
		return t.toArray(a);
	}
}

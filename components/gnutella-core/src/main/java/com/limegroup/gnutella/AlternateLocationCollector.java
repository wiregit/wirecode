package com.limegroup.gnutella;

public interface AlternateLocationCollector extends HTTPHeaderValue {

	public void addAlternateLocation(AlternateLocation al);

	public void addAlternateLocationCollection(AlternateLocationCollection alc);

	public boolean hasAlternateLocations();
}

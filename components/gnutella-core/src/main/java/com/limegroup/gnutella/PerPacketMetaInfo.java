/**
 * auth: rsoule
 * file: PerPacketMetaInfo
 * desc: This class is a wrapper around the meta data
 *       that would be included once per packet
 * 
 */
package com.limegroup.gnutella;

public class PerPacketMetaInfo {

	private byte[] _data;  // the meta info as an array of bytes
	private String _meta;  // the meta info in string form
	private int _size;  // the number of bytes in this 

	public PerPacketMetaInfo(String meta) {
		_meta = meta;
		_data = _meta.getBytes();
	}

	public PerPacketMetaInfo(byte[] data) {
		_data = data;
		_meta = new String(_data);
	}
	
	public byte[] getMetaInfoAsBytes() {
		return _data;
	}

	public String getMetaInfoAsString() {
		return _meta;
	}

	public int getSize() {
		return _data.length;
	}

	public void print() {
		System.out.println("The Meta info is: " + _meta);
	}

}

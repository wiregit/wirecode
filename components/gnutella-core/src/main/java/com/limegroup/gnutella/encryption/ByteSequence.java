package com.limegroup.gnutella.encryption;

public class ByteSequence {

    private int _i;
    private int _j;
    private int[] _sBox;

    
    public ByteSequence(byte[] key) {
        initializeSBox(asIntsFrom0To255(key));
    }
    
    byte next() {
        _i = (_i + 1) % 256;
        _j = (_j + _sBox[_i]) % 256;
        int sBoxI = _sBox[_i];
        int sBoxJ = _sBox[_j];
        _sBox[_i] = sBoxJ;
        _sBox[_j] = sBoxI;
        int t = (_sBox[_i] + _sBox[_j]) % 256;
        return (byte) _sBox[t];
    }
    
    private void initializeSBox(int[] key) {
        int[] sBox = new int[256];
        int[] kBox = new int[256];
        for(int i = 0; i < 256; i++) {
            sBox[i] = i;
        }
        for(int i = 0; i < 256; i++) {
            kBox[i] = key[i % key.length];
        }
        int j = 0;
        for(int i = 0; i < 256; i++) {
            j = (j + sBox[i] + kBox[i]) % 256;
            int sBoxI = sBox[i];
            int sBoxJ = sBox[j];
            sBox[i] = sBoxJ;
            sBox[j] = sBoxI;
        }
        _sBox=sBox;
    }

    //converts byte array to equivalent int array
    private int[] asIntsFrom0To255(byte[] data) {
        int[] result = new int[data.length];
        for(int i = 0; i < data.length; i++) {
            result[i] = (int)data[i] & 0x000000FF;
        }
        return result;        
    }
}

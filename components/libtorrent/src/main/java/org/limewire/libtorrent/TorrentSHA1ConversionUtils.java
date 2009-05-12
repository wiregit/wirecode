package org.limewire.libtorrent;


/**
 * Temp class for misc. homeless static functions
 */
public class TorrentSHA1ConversionUtils {

    public static String toHexString(byte[] block) {
        StringBuffer hexString = new StringBuffer(block.length * 2);
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
                'e', 'f' };

        int high = 0;
        int low = 0;
        for (byte b : block) {
            high = ((b & 0xf0) >> 4);
            low = b & 0x0f;
            hexString.append(hexChars[high]);
            hexString.append(hexChars[low]);
        }

        return hexString.toString();
    }

    public static byte[] fromHexString(String hexString) {
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hexString.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}

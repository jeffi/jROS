package edu.unc.cs.robotics.ros.network;

import java.nio.ByteBuffer;
import java.util.Arrays;

class HexDump {
    private HexDump() {}

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String hexDump(ByteBuffer buf) {
        //            1         2         3         4         5         6         7
        // 0 12345678901234567890123456789012345678901234567890123456789012345678901234567
        // \t01234567  00 11 22 33 44 55 66 77  88 99 aa bb cc dd ee ff  01234567 89abcdef
        char[] line = new char[78];
        StringBuilder dump = new StringBuilder();

        final int pos = buf.position();
        final int n = buf.limit() - pos;
        for (int i = 0 ; i < n ; ++i) {
            final int b = buf.get(pos + i) & 0xff;
            final int k = i%16;
            if (k == 0) {
                if (i > 0) {
                    dump.append(line).append('\n');
                }
                Arrays.fill(line, ' ');
                line[0] = '\t';
                for (int j = 1 ; j <= 8 ; ++j) {
                    line[j] = HEX[i >>> (8 - j)*4 & 0xf];
                }
            }

            int o = k/8;
            line[11 + o + k*3] = HEX[b >>> 4];
            line[12 + o + k*3] = HEX[b & 0xf];
            line[61 + o + k] = (' ' <= b && b <= '~') ? (char)b : '.';
        }

        return dump.append(line, 0, 61 + n%16 + n%16/8).toString();
    }
}

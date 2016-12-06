package edu.unc.cs.robotics.ros.network;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import junit.framework.TestCase;

public class TCPROSProtocolTest extends TestCase {
    enum BufferAllocator implements IntFunction<ByteBuffer> {
        DIRECT {
            @Override
            public ByteBuffer apply(int value) {
                return ByteBuffer.allocateDirect(value);
            }
        },
        HEAP {
            @Override
            public ByteBuffer apply(int value) {
                return ByteBuffer.allocate(value);
            }
        },
        HEAP_SLICE {
            @Override
            public ByteBuffer apply(int value) {
                ByteBuffer buf = ByteBuffer.allocate(value + 2);
                buf.limit(value+1).position(1);
                return buf.slice();
            }
        }
    }

    public void testWikiExample() throws Exception {
        String wikiExample = "b0 00 00 00\n" +
                             "   20 00 00 00\n" +
                             "      6d 65 73 73 61 67 65 5f 64 65 66 69 6e 69 74 69 6f 6e 3d 73 74 72 69 6e 67\n" +
                             "      20 64 61 74 61 0a 0a\n" +
                             "   25 00 00 00\n" +
                             "      63 61 6c 6c 65 72 69 64 3d 2f 72 6f 73 74 6f 70 69 63 5f 34 37 36 37 5f 31\n" +
                             "      33 31 36 39 31 32 37 34 31 35 35 37\n" +
                             "   0a 00 00 00\n" +
                             "      6c 61 74 63 68 69 6e 67 3d 31\n" +
                             "   27 00 00 00\n" +
                             "      6d 64 35 73 75 6d 3d 39 39 32 63 65 38 61 31 36 38 37 63 65 63 38 63 38 62\n" +
                             "      64 38 38 33 65 63 37 33 63 61 34 31 64 31\n" +
                             "   0e 00 00 00\n" +
                             "      74 6f 70 69 63 3d 2f 63 68 61 74 74 65 72\n" +
                             "   14 00 00 00\n" +
                             "      74 79 70 65 3d 73 74 64 5f 6d 73 67 73 2f 53 74 72 69 6e 67\n" +
                             "09 00 00 00\n" +
                             "   05 00 00 00\n" +
                             "      68 65 6c 6c 6f";
        String[] hex = wikiExample.split("\\s+");

        String msg1str = wikiExample;


        byte[] data = new byte[hex.length + msg1str.length() + 8];
        for (int i=0 ; i<hex.length ; ++i) {
            data[i] = (byte)Integer.parseInt(hex[i], 16);
        }
        ByteBuffer input = ByteBuffer.wrap(data).order(NetworkServer.ROS_BYTE_ORDER);
        input.position(hex.length);
        input.putInt(msg1str.length() + 4);
        input.putInt(msg1str.length());
        Charset.forName("US-ASCII").newEncoder().encode(CharBuffer.wrap(msg1str), input, true);

        // System.out.println("DATA LENGTH = "+data.length);
        for (BufferAllocator allocator : BufferAllocator.values()) {
            for (int bufSize = data.length; bufSize > 0; --bufSize) {
                // System.out.println("BUFFER SIZE = " + bufSize);
                ByteBuffer buf = allocator.apply(bufSize).order(NetworkServer.ROS_BYTE_ORDER);
                TestDelegate delegate = new TestDelegate(
                    "message_definition", "string data\n\n",
                    "callerid", "/rostopic_4767_1316912741557",
                    "latching", "1",
                    "md5sum", "992ce8a1687cec8c8bd883ec73ca41d1",
                    "topic", "/chatter",
                    "type", "std_msgs/String"
                );
                TCPROSProtocol protocol = new TCPROSProtocol(delegate, 1);
                for (int i = 0; i <= data.length; i += bufSize) {
                    buf.put(data, i, Math.min(bufSize, data.length - i)).flip();
                    protocol.process(buf);
                    assertEquals(0, buf.remaining());
                    buf.clear();
                }

                assertTrue(delegate.headersDone);
                assertEquals(2, delegate.messages.size());
                ByteBuffer msg0 = delegate.messages.get(0);
                assertEquals(9, msg0.remaining());
                assertEquals(5, msg0.getInt());
                assertEquals('h', msg0.get());
                assertEquals('e', msg0.get());
                assertEquals('l', msg0.get());
                assertEquals('l', msg0.get());
                assertEquals('o', msg0.get());
                assertEquals(0, msg0.remaining());

                ByteBuffer msg1 = delegate.messages.get(1);
                assertEquals(msg1str.length()+4, msg1.remaining());
                assertEquals(msg1str.length(), msg1.getInt());
                for (int i=0, n=msg1str.length() ; i<n ; ++i) {
                    assertEquals(msg1str.charAt(i), (char)msg1.get());
                }
                assertEquals(0, msg1.remaining());
            }
        }

//        ByteBuffer buf = ByteBuffer.wrap(data).order(ROS_BYTE_ORDER);
//        TestDelegate delegate = new TestDelegate(
//            "message_definition", "string data\n\n",
//            "callerid", "/rostopic_4767_1316912741557",
//            "latching", "1",
//            "md5sum", "992ce8a1687cec8c8bd883ec73ca41d1",
//            "topic", "/chatter",
//            "type", "std_msgs/String"
//        );
//        TCPROSProtocol protocol = new TCPROSProtocol(delegate);
//
//        protocol.process(buf);
//
//        assertEquals(1, delegate.messages.size());
//        ByteBuffer msg0 = delegate.messages.get(0);
//        assertEquals(9, msg0.remaining());
//        assertEquals(5, msg0.getInt());
//        assertEquals('h', msg0.get());
//        assertEquals('e', msg0.get());
//        assertEquals('l', msg0.get());
//        assertEquals('l', msg0.get());
//        assertEquals('o', msg0.get());
    }

    private static class TestDelegate implements TCPROSProtocol.Delegate {
        String[] expectedHeaders;
        int _headerNo;
        boolean headersDone;
        List<ByteBuffer> messages = new ArrayList<>();

        TestDelegate(String... expectedHeaders) {
            assertEquals(0, expectedHeaders.length & 1);
            this.expectedHeaders = expectedHeaders;
        }

        @Override
        public void headerRecv(String name, String value) {
            assertTrue(_headerNo < expectedHeaders.length/2);
            assertEquals(expectedHeaders[_headerNo*2], name);
            assertEquals(expectedHeaders[_headerNo*2+1], value);
            _headerNo++;
        }

        @Override
        public void headersDone() {
            assertEquals(expectedHeaders.length/2, _headerNo);
            this.headersDone = true;
        }

        @Override
        public void messageRecv(ByteBuffer buf) {
            ByteBuffer copy = ByteBuffer.allocate(buf.remaining()).order(NetworkServer.ROS_BYTE_ORDER);
            copy.put(buf).flip();
            messages.add(copy);
        }
    }
}
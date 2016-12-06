package edu.unc.cs.robotics.ros.protocol;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import edu.unc.cs.robotics.ros.msg.ByteBufferDeserializer;
import edu.unc.cs.robotics.ros.msg.JointState;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@see http://wiki.ros.org/ROS/TCPROS}
 */
public class TCPROSMessageProtocol<M extends Message> implements Protocol {

    static final Logger _log = LoggerFactory.getLogger(TCPROSMessageProtocol.class);

    /**
     * Protocol states.
     */
    private enum State {
        /**
         * Initial state.  The protocol is looking for a header size.
         */
        INITIAL,
        /**
         * Processing the header.  Looking for header field size.
         */
        HEADER,
        /**
         * Processing a header field.  Looking for _packetSize bytes that
         * make up a single header.
         */
        FIELD,
        /**
         * Processing the message stream.  Looking for a message size.
         */
        STREAM,
        /**
         * Processing a message.  Waiting for _packetSize message bytes,
         * then it will process the message and loop to STREAM state.
         */
        MESSAGE,
        /**
         * The protocol encountered an unrecoverable error.  Any future
         * calls to process will generate a ProtocolException.
         */
        ERROR
    }

    /**
     * The current protocol state.
     */
    private State _state = State.INITIAL;

    /**
     * The number of bytes in the packet or header field to parse.
     */
    private int _packetSize;

    /**
     * The remaining bytes in the header.
     */
    private int _headerSize;

    /**
     * Buffer used by processField to convert bytes to header strings.
     */
    private StringBuilder _str = new StringBuilder();

    /**
     * Headers are parsed and placed into this map.  The implementation
     * uses a LinkedHashMap to preserve header order for iteration.
     */
    private final Map<String,String> _headers = new LinkedHashMap<>();

    private final Function<MessageDeserializer, ? extends M> _messageParser;
    private final Consumer<? super M> _messageConsumer;

    public TCPROSMessageProtocol(Function<MessageDeserializer, ? extends M> messageParser,
                                 Consumer<? super M> messageConsumer)
    {
        _messageParser = messageParser;
        _messageConsumer = messageConsumer;
    }

    /**
     * Creates a ProtocolException and sets the state to ERROR.
     *
     * @param message the error message.
     * @return a ProtocolException
     */
    private ProtocolException error(String message) {
        _state = State.ERROR;
        return new ProtocolException(message);
    }

    @Override
    public void process(ByteBuffer buf) throws ProtocolException {
        // Not sure why, but TCPROS's byte order is little-endian
        // contrary to standard network byte order. (e.g., RFC 1700)
        if (buf.order() != ByteOrder.LITTLE_ENDIAN)
            throw new IllegalArgumentException(
                "TCPROS buffers must NOT be in network byte order.");

        if (!buf.hasRemaining())
            throw new IllegalArgumentException(
                "Process called with empty buffer");

        for (;;) {
            switch (_state) {
            case INITIAL:
                // initial state, looking for 4-byte integer the
                // specifies the total header size.
                if (buf.remaining() < 4)
                    return;
                _headerSize = buf.getInt();
                _state = State.HEADER;
                _log.debug("Header size: "+_headerSize);
                // fall through
            case HEADER:
                // In the header state we're looking for a 4-byte
                // integer that determines the length of the header
                // field that follows.
                if (buf.remaining() < 4)
                    return;
                if ((_packetSize = buf.getInt()) < 0)
                    throw error("invalid packet size");
                if ((_headerSize -= _packetSize + 4) < 0)
                    throw error("header overrun");
                _state = State.FIELD;
                // fall through
            case FIELD:
                // Now looking for _packetSize bytes that make up
                // the header field.  The name and value are separated
                // byte an '=' byte.
                if (buf.remaining() < _packetSize)
                    return;
                // field is all in buffer now, parse it out.
                processField(buf, buf.position() + _packetSize);

                // if there are more header bytes left, switch
                // to HEADER state and loop again to parse the
                // next header.
                if (_headerSize > 0) {
                    _state = State.HEADER;
                    continue;
                }
                _state = State.STREAM;
                _log.debug("Headers complete.");
                // fall through
            case STREAM:
                // Once in the stream state, the header are done.
                // We're looping through messages, looking for the
                // message's packet size (a 4-byte int) each time.
                if (buf.remaining() < 4)
                    return;
                _packetSize = buf.getInt();
                _state = State.MESSAGE;
                // fall through
            case MESSAGE:
                // wait for the full packet to be available.
                if (buf.remaining() < _packetSize)
                    return;
                // process the message
                processMessage(buf, buf.position() + _packetSize);

                // and loop to the next message.
                _state = State.STREAM;
                continue;
            case ERROR:
                throw error("protocol in error state");
            }
        }
    }

    private void processField(ByteBuffer buf, int limit) throws ProtocolException {
        final int oldLimit = buf.limit();
        buf.limit(limit);
        for (byte b ; buf.hasRemaining() && (b = buf.get()) != '=' ; )
            _str.append((char)(b & 0xff));
        String name = _str.toString();
        if (_headers.containsKey(name))
            throw error("Header '"+name+"' defined multiple times");
        _str.setLength(0);
        _str.ensureCapacity(buf.remaining());
        while (buf.hasRemaining())
            _str.append((char)(buf.get() & 0xff));
        String value = _str.toString();
        _str.setLength(0);
        _log.debug("Header: '"+name+"' = '"+value+"'");
        _headers.put(name, value);
        buf.limit(oldLimit);
    }

    protected void processMessage(ByteBuffer buf, int limit) throws ProtocolException {
        final int oldLimit = buf.limit();
        buf.limit(limit);
        M msg = _messageParser.apply(new ByteBufferDeserializer(buf));
//        System.out.println(msg);
        _messageConsumer.accept(msg);
        buf.limit(oldLimit);
    }

    public static void main(String[] args) throws IOException, ProtocolException {
        try (FileInputStream in = new FileInputStream("/Users/jeffi/School/projects/cloudplan/tcpros-output.raw")) {
            TCPROSMessageProtocol<JointState> protocol = new TCPROSMessageProtocol<>(
                JointState::new, System.out::println);
            ByteBuffer buf = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
            for (int n ; (n = in.read(buf.array(), buf.position(), buf.remaining())) != -1 ; ) {
                buf.position(buf.position() + n);
                buf.flip();
                protocol.process(buf);
                if (buf.position() == 0) {
                    // if position == 0, then the protocol did not
                    // read anything from the buffer.  This means that
                    // it needs to buffer more data before it can process
                    // the packet.
                    buf = ByteBuffer.allocate(buf.capacity()*2).order(ByteOrder.LITTLE_ENDIAN).put(buf);
                } else {
                    System.out.println("Compacting " + buf.remaining());
                    buf.compact();
                }
            }
        }
    }
}

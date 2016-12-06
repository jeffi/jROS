package edu.unc.cs.robotics.ros.topic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.unc.cs.robotics.ros.msg.ByteBufferSerializer;
import edu.unc.cs.robotics.ros.msg.ByteCountSerializer;
import edu.unc.cs.robotics.ros.msg.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializedMessage<M extends Message> {
    private static final Logger LOG = LoggerFactory.getLogger(SerializedMessage.class);

    private final int _seqNo;
    private final M _message;
    private ByteBuffer _buffer;

    SerializedMessage(int seqNo, M message) {
        _seqNo = seqNo;
        _message = message;
    }

    public synchronized ByteBuffer buffer() {
        if (_buffer == null) {
            ByteCountSerializer byteCountSerializer = new ByteCountSerializer();
            _message.serialize(byteCountSerializer);
            int size = byteCountSerializer.getByteCount();
            _buffer = ByteBuffer.allocate(size + 4).order(ByteOrder.LITTLE_ENDIAN);
            _buffer.putInt(size);
            _message.serialize(new ByteBufferSerializer(_seqNo, _buffer));
            assert !_buffer.hasRemaining() : "serialization byte count mismatch";
            _buffer.flip();
            assert _buffer.remaining() == size + 4;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Serialized "+_message.getClass().getName()+" to "+(size+4)+" bytes");
            }
        }

        return _buffer.duplicate();
    }
}

package edu.unc.cs.robotics.ros.protocol;

import java.nio.ByteBuffer;

/**
 * A stateful protocol handler.
 */
public interface Protocol {
    /**
     * Processes bytes in the protocol.  The protocol will process
     * as many bytes as possible, and leave unprocessed bytes
     * remaining in the buffer.  The caller should ensure that if
     * the protocol requires more bytes than the buffer can hold,
     * that it will allocate a new buffer before calling this
     * method again.
     *
     * @param buf the buffer to process.
     * @throws ProtocolException if the buffer contains a sequence
     * of bytes that is invalid according to the underlying protocol.
     *
     * @throws IllegalArgumentException if the buffer is is in
     * the wrong byte order, or if the buffer is called with an
     * empty buffer.
     */
    void process(ByteBuffer buf) throws ProtocolException;
}

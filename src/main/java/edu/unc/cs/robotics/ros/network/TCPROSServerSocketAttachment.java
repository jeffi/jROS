package edu.unc.cs.robotics.ros.network;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

class TCPROSServerSocketAttachment implements SelectorAttachment {
    private final NetworkServer _server;

    TCPROSServerSocketAttachment(NetworkServer server) throws ClosedChannelException {
        _server = server;
    }

    @Override
    public void acceptable(SelectionKey key) throws IOException {
        if (!key.isAcceptable()) {
            return;
        }

        ServerSocketChannel serverChannel = (ServerSocketChannel)key.channel();
        SocketChannel acceptedChannel = serverChannel.accept();
        NetworkServer.LOG.info("accepted connection from {}", acceptedChannel.getRemoteAddress());
        acceptedChannel.configureBlocking(false);
        TCPROSPublisherSelectorAttachment att = new TCPROSPublisherSelectorAttachment(_server, acceptedChannel);
        _server.register(acceptedChannel, SelectionKey.OP_READ, att);
    }
}

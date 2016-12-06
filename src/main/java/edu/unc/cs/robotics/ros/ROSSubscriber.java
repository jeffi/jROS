package edu.unc.cs.robotics.ros;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.table.AbstractTableModel;

import edu.unc.cs.robotics.ros.msg.EndpointState;
import edu.unc.cs.robotics.ros.msg.JointState;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.protocol.ProtocolException;
import edu.unc.cs.robotics.ros.protocol.TCPROSMessageProtocol;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jeffi on 3/10/16.
 */
public class ROSSubscriber<M extends Message> {

    static final Logger _log = LoggerFactory.getLogger(ROSSubscriber.class);

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final String _masterURI = "http://baxter:11311/";
    private final String _callerId = "/script";
    private final String _callerApi;
    private final CloseableHttpAsyncClient _httpClient;

    private final Function<MessageDeserializer, ? extends M> _parser;
    private final Consumer<? super M> _consumer;

    public ROSSubscriber(Function<MessageDeserializer, ? extends M> parser,
                         Consumer<? super M> consumer)
        throws UnknownHostException
    {
        _httpClient = HttpAsyncClients.createDefault();
        _httpClient.start();
        // http://robot.local:57360/

        _callerApi = "http://" + InetAddress.getLocalHost().getHostAddress() + ":11311/";
        _parser = parser;
        _consumer = consumer;
    }

    MethodResponse makeCall(String uri, String call) throws ExecutionException, InterruptedException, IOException {
        HttpPost post = new HttpPost(uri);
        post.setEntity(new StringEntity(call, UTF8));
        HttpResponse httpResponse = _httpClient.execute(post, null).get();
        HttpEntity entity = httpResponse.getEntity();
        return new MethodResponse(entity.getContent());
    }

    public void getPublishedTopics(String subgraph) throws InterruptedException, ExecutionException, IOException {
        MethodResponse response = makeCall(
            _masterURI,
            new MethodCallBuilder("getPublishedTopics")
                .addParam(_callerId)
                .addParam(subgraph)
                .build());
        System.out.println(response);
    }

    public List<String> registerSubscriber(String topic, String topicType) throws InterruptedException,
        ExecutionException, IOException
    {
        MethodResponse response = makeCall(
            _masterURI,
            new MethodCallBuilder("registerSubscriber")
                .addParam(_callerId)
                .addParam(topic)
                .addParam(topicType)
                .addParam(_callerApi)
                .build());

        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>)response.result;
        if ((int)result.get(0) != 1) {
            return null;
        }

        // This is actually constructed as List<Object>, but
        // should only be populated with Strings.
        @SuppressWarnings("unchecked")
        List<String> endPoints = (List<String>)result.get(2);

        return endPoints.stream()
            .map(s -> s.replace("://robot.local:", "://baxter:"))
            .collect(Collectors.toList());
    }

    /**
     *
     * @param uri
     * @param topic
     * @param protocols this must be in the form { { protocol, param1, param2, ... }, { proto2, param1, param2, ...}, ...}
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException
     */
    public ProtocolParam requestTopic(String uri, String topic, String[] ... protocols)
        throws InterruptedException, ExecutionException, IOException
    {
        MethodResponse response = makeCall(uri, new MethodCallBuilder("requestTopic")
            .addParam(_callerId)
            .addParam(topic)
            .addParam((Object[])protocols)
            .build());

        System.out.println(response);

        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>)response.result;

        if (1 != (int)result.get(0)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Object> params = (List<Object>)result.get(2);

        return new ProtocolParam(
            (String)params.get(0), // e.g. "TCPROS"
            ((String)params.get(1)).replace("robot.local", "baxter"), // TODO
            (int)params.get(2)); // port
    }

    private void connect(ProtocolParam param, String topic, String messageType, String md5sum, String definition)
        throws IOException, ProtocolException
    {
        if (!"TCPROS".equals(param.name)) {
            throw new IllegalArgumentException("Unsupported protocol: " + param.name);
        }

        List<ByteBuffer> headers = new ArrayList<>();
        ByteBuffer totalSizeBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        headers.add(totalSizeBuf);
        int totalSize = addHeader(headers, "message_definition", definition)
            + addHeader(headers, "callerid", _callerId)
            + addHeader(headers, "topic", topic)
            + addHeader(headers, "md5sum", md5sum)
            + addHeader(headers, "type", messageType);

        totalSizeBuf.putInt(totalSize).flip();

        TCPROSMessageProtocol<M> protocol = new TCPROSMessageProtocol<>(_parser, _consumer);

        try (SocketChannel channel = SocketChannel.open(new InetSocketAddress(param.host, param.port))) {
            _log.debug("TCPROS connected");

            // Perform a gathering write of all the headers.
            channel.write(headers.toArray(new ByteBuffer[headers.size()]));

            _log.debug("Sent headers: "+totalSize+" bytes");

            long count = 0;
            ByteBuffer buf = ByteBuffer.allocate(1024*4).order(ByteOrder.LITTLE_ENDIAN);
            while (channel.read(buf) != -1) {
                int remaining = buf.remaining();
                buf.flip();
                count += buf.remaining();
                protocol.process(buf);
                if (buf.position() == 0 && remaining == 0) {
                    // if position == 0, the protocol could not process anything.
                    // if remaining == 0, there's no space left in the buffer,
                    // we need to grow it to avoid repeated looping w/o data.
                    buf = ByteBuffer.allocate(buf.capacity()*2).order(ByteOrder.LITTLE_ENDIAN).put(buf);
                    System.out.println("Grow buffer: " + buf.capacity());
                } else {
//                    System.out.println("Compacting: "+buf.remaining());
                    buf.compact();
                }
            }
        }
    }

    /**
     * Creates and adds a ByteBuffer with a TCPROS header field in it.
     * The header field has the format:
     *
     * <pre>
     *     [4-bytes] Packet length = length(name) + 1 + length(value)
     *     [n-bytes] name
     *     [1-byte]  '='
     *     [v-bytes] value
     * </pre>
     *
     * @param headers the headers list where the header is to be added
     * @param name the header name
     * @param value the header value
     * @return the packet length
     */
    private int addHeader(List<ByteBuffer> headers, String name, String value) {
        int len = name.length() + 1 + value.length();
        ByteBuffer buf = ByteBuffer.allocate(4 + len).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(len);
        buf.put(name.getBytes(US_ASCII));
        buf.put((byte)'=');
        buf.put(value.getBytes(US_ASCII));
        buf.flip();
        if (buf.limit() != len + 4) {
            throw new AssertionError();
        }
        headers.add(buf);
        return len + 4;
    }

    public static ROSSubscriber<JointState> jointState(Consumer<? super JointState> consumer) throws IOException,
        InterruptedException, ExecutionException, ProtocolException {
        ROSSubscriber<JointState> subscriber = new ROSSubscriber<>(JointState::new, consumer);
        String topic = "/robot/joint_states";
        String messageType = JointState.DATATYPE;
        List<String> endpoints = subscriber.registerSubscriber(topic, messageType);
        _log.debug(topic+" endpoints: "+endpoints);
        ProtocolParam param = subscriber.requestTopic(endpoints.get(0), topic, new String[]{ "TCPROS" });
        _log.debug(topic+" protocol params: "+param);

        Thread thread = new Thread(() -> {
            try {
                subscriber.connect(param, topic, messageType, JointState.MD5SUM, JointState.DEFINITION);
            } catch (IOException | ProtocolException e) {
                _log.error("Error in TCPROS connection", e);
            }
        });
        thread.start();
        return subscriber;
    }

    public static ROSSubscriber<EndpointState> endpointState(
        String side, Consumer<? super EndpointState> consumer)
        throws IOException, InterruptedException, ExecutionException
    {
        if (!"left".equals(side) && !"right".equals(side))
            throw new IllegalArgumentException("side must be 'left' or 'right'");

        ROSSubscriber<EndpointState> subscriber = new ROSSubscriber<>(EndpointState::new, consumer);
        String topic = "/robot/limb/" + side + "/endpoint_state";
        String messageType = EndpointState.DATATYPE;
        List<String> endpoints = subscriber.registerSubscriber(topic, messageType);
        ProtocolParam param = subscriber.requestTopic(endpoints.get(0), topic, new String[]{ "TCPROS" });

        Thread thread = new Thread(() -> {
            try {
                subscriber.connect(param, topic, messageType, EndpointState.MD5SUM, EndpointState.DEFINITION);
            } catch (IOException | ProtocolException e) {
                _log.error("Error in TCPROS connection", e);
            }
        });
        thread.start();
        return subscriber;
    }

    static class JointTableModel extends AbstractTableModel {

        private JointState _state;
        private NumberFormat _numberFormatter = DecimalFormat.getNumberInstance();
        {
            _numberFormatter.setMaximumFractionDigits(3);
            _numberFormatter.setMinimumFractionDigits(3);
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return "name";
            case 1:
                return "position";
            case 2:
                return "velocity";
            default:
                return super.getColumnName(column);
            }
        }

        @Override
        public int getRowCount() {
            return _state == null ? 0 : _state.name.length;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return _state.name[rowIndex];
            case 1:
                return _numberFormatter.format(_state.position[rowIndex] * 180/Math.PI);
            case 2:
                return _numberFormatter.format(_state.velocity[rowIndex] * 180/Math.PI);
            default:
                return rowIndex+","+columnIndex;
            }
        }

        void setState(JointState state) {
            System.out.println(state);
            _state = state;
            fireTableDataChanged();
        }
    }
}

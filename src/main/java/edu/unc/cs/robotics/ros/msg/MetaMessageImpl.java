package edu.unc.cs.robotics.ros.msg;

import java.util.function.Function;

/**
 * Created by jeffi on 6/30/16.
 */
public class MetaMessageImpl<M extends Message> implements MetaMessage<M> {

    private final String _dataType;
    private final String _md5sum;
    private final String _messageDefinition;
    private final Function<? super MessageDeserializer, ? extends M> _deserializer;

    public MetaMessageImpl(
        String dataType,
        String md5sum,
        String messageDefinition,
        Function<? super MessageDeserializer, ? extends M> deserializer)
    {
        _dataType = dataType;
        _md5sum = md5sum;
        _messageDefinition = messageDefinition;
        _deserializer = deserializer;
    }

    @Override
    public String getDataType() {
        return _dataType;
    }

    @Override
    public String getMd5sum() {
        return _md5sum;
    }

    @Override
    public String getMessageDefinition() {
        return _messageDefinition;
    }

    @Override
    public M deserialize(MessageDeserializer buf) {
        return _deserializer.apply(buf);
    }
}

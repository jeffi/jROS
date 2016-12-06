package edu.unc.cs.robotics.ros.xmlrpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@Singleton
public class XmlrpcParser {
    private final SAXParserFactory _parserFactory;

    @Inject
    XmlrpcParser() {
        _parserFactory = SAXParserFactory.newInstance();
        _parserFactory.setNamespaceAware(false);
        _parserFactory.setValidating(false);
        _parserFactory.setXIncludeAware(false);
    }

    public MethodCall parseMethodCall(InputSource inputSource) throws ParserConfigurationException, SAXException,
        IOException
    {
        return (MethodCall)parse(inputSource, new MethodCallRootState());
    }

    public MethodResponse parseMethodResponse(InputSource inputSource) throws ParserConfigurationException,
        SAXException, IOException
    {
        return (MethodResponse)parse(inputSource, new MethodResponseRootState());
    }

    private Object parse(InputSource inputSource, final RootState rootState)
        throws SAXException, IOException, ParserConfigurationException
    {
        _parserFactory.newSAXParser().parse(inputSource, new DefaultHandler() {
            State _state = rootState;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException
            {
                State nextState = _state.transition(qName);
                nextState._parent = _state;
                _state = nextState;
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                _state.pop();
                _state = _state._parent;
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                _state.characters(ch, start, length);
            }
        });
        return rootState._value;
    }


    private static abstract class State {
        State _parent;

        State transition(String qName) throws SAXException {
            throw new SAXException("unexpected tag: " + qName);
        }

        void characters(char[] chars, int off, int len) {
        }

        void pop() throws SAXException {
        }

        void setName(String name) {
        }

        void setValue(Object value) {
        }

        void put(String name, Object value) {
        }
    }

    private static abstract class RootState extends State {
        Object _value;

        @Override
        void setValue(Object value) {
            _value = value;
        }
    }

    private static class MethodCallRootState extends RootState {
        @Override
        State transition(String qName) throws SAXException {
            if ("methodCall".equals(qName)) {
                return new MethodCallState();
            } else {
                return super.transition(qName);
            }
        }

    }

    public static class MethodResponseRootState extends RootState {
        @Override
        State transition(String qName) throws SAXException {
            if ("methodResponse".equals(qName)) {
                return new MethodResponseState();
            } else {
                return super.transition(qName);
            }
        }
    }

    private static class MethodCallState extends State {
        String _methodName;
        Object[] _params;

        @Override
        State transition(String qName) throws SAXException {
            switch (qName) {
            case "methodName": return new NameState();
            case "params": return new ParamsState();
            default: return super.transition(qName);
            }
        }

        @Override
        void setName(String name) {
            _methodName = name;
        }

        @Override
        void setValue(Object value) {
            _params = (Object[])value;
        }

        @Override
        void pop() {
            _parent.setValue(new MethodCall(_methodName, _params));
        }
    }

    private static class MethodResponseState extends State {
        Object[] _params;
        Object _fault;

        @Override
        State transition(String qName) throws SAXException {
            switch (qName) {
            case "params": return new ParamsState();
            case "fault" : return new FaultState();
            default: return super.transition(qName);
            }
        }

        @Override
        void setValue(Object value) {
            _params = (Object[])value;
        }

        void setFault(Object fault) {
            _fault = fault;
        }

        @Override
        void pop() throws SAXException {
            _parent.setValue(new MethodResponse(_params, _fault));
        }
    }

    private static class FaultState extends State {
        @Override
        State transition(String qName) throws SAXException {
            if ("value".equals(qName)) {
                return new ValueState();
            } else {
                return super.transition(qName);
            }
        }

        @Override
        void setValue(Object value) {
            ((MethodResponseState)_parent).setFault(value);
        }
    }

    private static abstract class TextBufferingState extends State {
        StringBuilder _text = new StringBuilder();
        @Override
        void characters(char[] chars, int off, int len) {
            _text.append(chars, off, len);
        }
    }

    private static class NameState extends TextBufferingState {
        @Override
        void pop() {
            _parent.setName(_text.toString());
        }
    }

    private static class AbstractArrayState extends State {
        private List<Object> _data = new ArrayList<>();

        @Override
        void setValue(Object value) {
            _data.add(value);
        }

        @Override
        void pop() throws SAXException {
            Object[] array = new Object[_data.size()];
            _data.toArray(array);
            _parent.setValue(array);
        }
    }

    private static class ParamsState extends AbstractArrayState {
        @Override
        State transition(String qName) throws SAXException {
            if ("param".equals(qName)) {
                return new ParamState();
            } else {
                return super.transition(qName);
            }
        }
    }

    private static class ParamState extends State {
        @Override
        State transition(String qName) throws SAXException {
            if ("value".equals(qName)) {
                return new ValueState();
            } else {
                return super.transition(qName);
            }
        }

        @Override
        void setValue(Object value) {
            _parent.setValue(value);
        }
    }

    private static class ValueState extends TextBufferingState {
        Object _value;

        @Override
        State transition(String qName) throws SAXException {
            switch (qName) {
            case "i4":
            case "int":
                return new IntState();
            case "boolean":
                return new BooleanState();
            case "string":
                return new StringState();
            case "double":
                return new DoubleState();
            case "array":
                return new ArrayState();
            case "struct":
                return new StructState();
            case "dateTime.iso8601":
                throw new SAXException("dateTime.iso8601 not supported, implement me!");
            case "base64":
                throw new SAXException("base64 not supported, implement me!");
            default:
                return super.transition(qName);
            }
        }

        @Override
        void setValue(Object value) {
            _value = value;
        }

        @Override
        void pop() throws SAXException {
            // note: it is possible to get a response with
            // <value></value>, which results in a empty value.
            // We also see:
            // <value>foo</value>

            _parent.setValue(_value == null ? _text.toString() : _value);
        }
    }

    private static abstract class ValueSubState extends TextBufferingState {
        @Override
        void pop() throws SAXException {
            try {
                _parent.setValue(parseValue(_text.toString()));
            } catch (IllegalArgumentException ex) {
                throw new SAXException("invalue value: " + _text);
            }
        }

        abstract Object parseValue(String text);
    }

    private static class IntState extends ValueSubState {
        @Override
        Object parseValue(String text) {
            return Integer.parseInt(text);
        }
    }

    private static class BooleanState extends ValueSubState {
        @Override
        Object parseValue(String text) {
            switch (text) {
            case "0":
            case "false":
                return false;
            case "1":
            case "true":
                return true;
            default:
                throw new IllegalArgumentException("invalid boolean");
            }
        }
    }

    private static class StringState extends ValueSubState {
        @Override
        Object parseValue(String text) {
            return text;
        }
    }

    private static class DoubleState extends ValueSubState {
        @Override
        Object parseValue(String text) {
            return Double.parseDouble(text);
        }
    }

    private static class ArrayState extends State {
        @Override
        State transition(String qName) throws SAXException {
            if ("data".equals(qName)) {
                return new DataState();
            } else {
                return super.transition(qName);
            }
        }

        @Override
        void setValue(Object value) {
            _parent.setValue(value);
        }
    }

    private static class DataState extends AbstractArrayState {
        @Override
        State transition(String qName) throws SAXException {
            if ("value".equals(qName)) {
                return new ValueState();
            } else {
                return super.transition(qName);
            }
        }
    }

    private static class StructState extends State {
        private Map<String,Object> _map = new LinkedHashMap<>();

        @Override
        State transition(String qName) throws SAXException {
            if ("member".equals(qName)) {
                return new MemberState();
            } else {
                return super.transition(qName);
            }
        }

        @Override
        void put(String name, Object value) {
            _map.put(name, value);
        }

        @Override
        void pop() throws SAXException {
            _parent.setValue(_map);
        }
    }

    private static class MemberState extends State {
        String _name;
        Object _value;

        @Override
        State transition(String qName) throws SAXException {
            switch (qName) {
            case "name": return new NameState();
            case "value": return new ValueState();
            default: return super.transition(qName);
            }
        }

        @Override
        void setName(String name) {
            _name = name;
        }

        @Override
        void setValue(Object value) {
            _value = value;
        }

        @Override
        void pop() throws SAXException {
            _parent.put(_name, _value);
        }
    }
}

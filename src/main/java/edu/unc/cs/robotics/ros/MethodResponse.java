package edu.unc.cs.robotics.ros;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Created by jeffi on 3/11/16.
 */
public class MethodResponse {

    enum ParseState {
        START, METHOD_RESPONSE, PARAMS, PARAM, VALUE, INT, STRING, BOOLEAN, DOUBLE, ISO8601, BASE64, ARRAY, STRUCT, MEMBER,
        ARRAY_DATA, ARRAY_VALUE, MEMBER_NAME, MEMBER_VALUE, FAULT, FAULT_VALUE, FAULT_STRUCT,
    }

    static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();
    static {
        SAX_PARSER_FACTORY.setNamespaceAware(false);
        SAX_PARSER_FACTORY.setValidating(false);
        SAX_PARSER_FACTORY.setXIncludeAware(false);
        try {
            SAX_PARSER_FACTORY.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            e.printStackTrace();
        }
    }


    public Object result;
    public Fault fault;

    public MethodResponse(InputStream in) {
        this(new InputSource(in));
    }

    public MethodResponse(Reader reader) {
        this(new InputSource(reader));
    }

    public MethodResponse(InputSource source) {
        try {
            SAX_PARSER_FACTORY.newSAXParser().parse(source, new DefaultHandler() {
                final Deque<ParseState> _stateStack = new ArrayDeque<>();
                final Deque<Object> _dataStack = new ArrayDeque<>();
                ParseState _state = ParseState.START;
                final StringBuilder _text = new StringBuilder();
                Object _value;

                void expect(String expected, String actual) throws SAXException {
                    if (!expected.equals(actual)) {
                        throw new SAXException("Expected <" + expected + ">, found <" + actual + ">");
                    }
                }

                @Override
                public void startElement(
                    String uri,
                    String localName,
                    String qName,
                    Attributes attributes) throws SAXException
                {
                    _text.setLength(0);
                    _stateStack.push(_state);

                    switch (_state) {
                    case START:
                        expect("methodResponse", qName);
                        _state = ParseState.METHOD_RESPONSE;
                        break;
                    case METHOD_RESPONSE:
                        if ("params".equals(qName)) {
                            _state = ParseState.PARAMS;
                        } else if ("fault".equals(qName)) {
                            _state = ParseState.FAULT;
                        } else {
                            throw new SAXException("unexpected tag: <" + qName + ">");
                        }
                        break;
                    case PARAMS:
                        expect("param", qName);
                        _state = ParseState.PARAM;
                        break;
                    case PARAM:
                        expect("value", qName);
                        _state = ParseState.VALUE;
                        break;
                    case VALUE:
                    case ARRAY_VALUE:
                    case MEMBER_VALUE:
                        _value = null;
                        if ("i4".equals(qName) || "int".equals(qName)) {
                            _state = ParseState.INT;
                        } else if ("string".equals(qName)) {
                            _state = ParseState.STRING;
                        } else if ("boolean".equals(qName)) {
                            _state = ParseState.BOOLEAN;
                        } else if ("double".equals(qName)) {
                            _state = ParseState.DOUBLE;
                        } else if ("dateTime.iso8601".equals(qName)) {
                            _state = ParseState.ISO8601;
                        } else if ("base64".equals(qName)) {
                            _state = ParseState.BASE64;
                        } else if ("array".equals(qName)) {
                            _dataStack.push(new ArrayList());
                            _state = ParseState.ARRAY;
                        } else if ("struct".equals(qName)) {
                            _dataStack.push(new LinkedHashMap());
                            _state = ParseState.STRUCT;
                        } else {
                            throw new SAXException("unexpected value type <" + qName + ">");
                        }
                        break;
                    case INT:
                    case STRING:
                    case BOOLEAN:
                    case DOUBLE:
                    case ISO8601:
                    case BASE64:
                        throw new SAXException("unexpected tag <" + qName + "> while processing " + _state);
                    case STRUCT:
                    case FAULT_STRUCT:
                        expect("member", qName);
                        _dataStack.push(new Object[2]);
                        _state = ParseState.MEMBER;
                        break;
                    case MEMBER:
                        if ("name".equalsIgnoreCase(qName)) {
                            _state = ParseState.MEMBER_NAME;
                        } else if ("value".equalsIgnoreCase(qName)) {
                            _state = ParseState.MEMBER_VALUE;
                        } else {
                            throw new SAXException("unexpected tag <" + qName + "> while processing <member>");
                        }
                        break;
                    case ARRAY:
                        expect("data", qName);
                        _state = ParseState.ARRAY_DATA;
                        break;
                    case ARRAY_DATA:
                        expect("value", qName);
                        _state = ParseState.ARRAY_VALUE;
                        break;
                    case FAULT:
                        expect("value", qName);
                        _state = ParseState.FAULT_VALUE;
                        break;
                    case FAULT_VALUE:
                        expect("struct", qName);
                        _dataStack.push(new HashMap());
                        _state = ParseState.FAULT_STRUCT;
                        break;
                    default:
                        throw new AssertionError("Entered unimplemented state: " + _state);
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    String text = _text.toString();
                    switch (_state) {
                    case PARAM:
                        MethodResponse.this.result = _value;
                        _value = null;
                        break;
                    case VALUE:
                        if (_value == null) {
                            _value = text;
                        }
                        break;
                    case ARRAY_VALUE:
                        if (_value == null) {
                            _value = text;
                        }
                        @SuppressWarnings("unchecked")
                        List<Object> array = (List<Object>)_dataStack.peek();
                        array.add(_value);
                        _value = null;
                        break;
                    case ARRAY:
                        _value = _dataStack.pop();
                        break;
                    case STRUCT:
                        _value = _dataStack.pop();
                        break;
                    case FAULT_STRUCT:
                        @SuppressWarnings("unchecked")
                        Map<String,Object> faultMap = (Map<String,Object>)_dataStack.pop();
                        MethodResponse.this.fault = new Fault(
                            (int)faultMap.get("faultCode"),
                            (String)faultMap.get("faultString"));
                        break;
                    case INT:
                        _value = Integer.parseInt(text);
                        break;
                    case BOOLEAN:
                        if ("1".equals(text) || "true".equals(text)) {
                            _value = Boolean.TRUE;
                        } else if ("0".equals(text) || "false".equals(text)) {
                            _value = Boolean.FALSE;
                        } else {
                            throw new SAXException("encountered '" + text + "' when expecting a boolean");
                        }
                        break;
                    case STRING:
                        _value = text;
                        break;
                    case DOUBLE:
                        _value = new Double(text);
                        break;
                    case MEMBER_NAME:
                        ((Object[])_dataStack.peek())[0] = text;
                        break;
                    case MEMBER_VALUE:
                        ((Object[])_dataStack.peek())[1] = _value;
                        break;
                    case MEMBER:
                        Object[] pair = (Object[])_dataStack.pop();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> struct = (Map<String, Object>)_dataStack.peek();
                        struct.put((String)pair[0], pair[1]);
                        break;
                    }

                    _state = _stateStack.pop();
                    _text.setLength(0);
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    _text.append(ch, start, length);
                }
            });

        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString() {
        return "MethodResponse{" +
            "result=" + result +
            ", fault=" + fault +
            '}';
    }

    public static void main(String[] args) {
        String test = "<?xml version='1.0'?>\n" +
                      "<methodResponse>\n" +
                      "<params>\n" +
                      "<param>\n" +
                      "<value><array><data>\n" +
                      "<value><int>1</int></value>\n" +
                      "<value><string>Subscribed to [/robot/limb/left/endpoint_state]</string></value>\n" +
                      "<value><array><data>\n" +
                      "<value><string>http://robot.local:49820/</string></value>\n" +
                      "</data></array></value>\n" +
                      "</data></array></value>\n" +
                      "</param>\n" +
                      "</params>\n" +
                      "</methodResponse>";

        String test2 = "<?xml version=\"1.0\"?>\n" +
                       "<methodResponse><params><param>\n" +
                       "\t<value><array><data><value><i4>0</i4></value><value>robot config incorrect, &apos;/robot_config/jcb_joint_config/left_s1/dual_piecewise_linear_spring_model/max_hysteresis&apos; not type double: error occurs at line 305, file /var/tmp/portage/sci-electronics/rethink-rsdk-1.1.1.171/work/rethink-rsdk-1.1.1.171/stacks/MotorControl/MotorControlPlugins/BasePlugin.h</value><value><i4>0</i4></value></data></array></value>\n" +
                       "</param></params></methodResponse>";

        String test3 = "<?xml version=\"1.0\"?>\n" +
                       "<methodResponse>\n" +
                       "   <fault>\n" +
                       "      <value>\n" +
                       "         <struct>\n" +
                       "            <member>\n" +
                       "               <name>faultCode</name>\n" +
                       "               <value><int>4</int></value>\n" +
                       "               </member>\n" +
                       "            <member>\n" +
                       "               <name>faultString</name>\n" +
                       "               <value><string>Too many parameters.</string></value>\n" +
                       "               </member>\n" +
                       "            </struct>\n" +
                       "         </value>\n" +
                       "      </fault>\n" +
                       "   </methodResponse>";

        System.out.println(new MethodResponse(new StringReader(test3)).fault);
    }
}

package edu.unc.cs.robotics.ros.xml;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by jeffi on 3/10/16.
 */
@XmlType(name="value")
public class Value {
    @XmlElements({
        @XmlElement(type = String.class, name = "string"),
        @XmlElement(type = Integer.class, name = "i4"),
        @XmlElement(type = Integer.class, name = "int"),
        @XmlElement(type = Double.class, name = "double"),
        @XmlElement(type = Boolean.class, name = "boolean"),
        @XmlElement(type = Array.class, name = "array")
    })
    public Object value;

    public Value() {}

    public Value(String value) {
        this.value = value;
    }

    public Value(int value) {
        this.value = value;
    }

    public Value(double value) {
        this.value = value;
    }

    public Value(boolean value) {
        this.value = value;
    }

    public Value(String[] strings) {
        this.value = new Array(strings);
    }

    public Value(Object javaValue) {
        if (javaValue instanceof Object[]) {
            this.value = new Array((Object[])javaValue);
        } else {
            this.value = javaValue;
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public Array asArray() {
        return (Array)value;
    }

    public String asString() {
        return (String)value;
    }

    public Object toJavaObject() {
        if (value instanceof Array) {
            final List<Value> data = ((Array)this.value).data;
            final int n = data.size();
            Object[] array = new Object[n];
            for (int i=0 ; i<n ; ++i) {
                array[i] = data.get(i).toJavaObject();
            }
            return array;
        } else {
            return value;
        }
    }
}

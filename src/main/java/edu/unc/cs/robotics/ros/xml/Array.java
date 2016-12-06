package edu.unc.cs.robotics.ros.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by jeffi on 3/10/16.
 */
@XmlType(name="array")
public class Array {
    @XmlElementWrapper(name="data")
    @XmlElement(name="value")
    public List<Value> data = new ArrayList<>();

    public Array() {
    }

    public Array(Object[] array) {
        for (Object value : array) {
            data.add(new Value(value));
        }
    }

    public Value get(int index) {
        return data.get(index);
    }

    @Override
    public String toString() {
        return "Array{" +
            "data=" + data +
            '}';
    }
}

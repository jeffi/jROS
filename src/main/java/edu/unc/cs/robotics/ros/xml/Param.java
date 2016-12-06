package edu.unc.cs.robotics.ros.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by jeffi on 3/10/16.
 */
@XmlType(name="param")
public class Param {
    @XmlElement(name="value")
    public Value value;

    public Param() {
    }

    public Param(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Param{" +
            "value=" + value +
            '}';
    }
}

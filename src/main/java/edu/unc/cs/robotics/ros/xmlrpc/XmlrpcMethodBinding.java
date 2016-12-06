package edu.unc.cs.robotics.ros.xmlrpc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface XmlrpcMethodBinding {
    String value() default "##default";
}

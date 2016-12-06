package edu.unc.cs.robotics.ros.msg;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public interface MetaMessage<M extends Message> {
    String getDataType();
    String getMd5sum();
    String getMessageDefinition();

    M deserialize(MessageDeserializer buf);

    static <M extends Message> MetaMessage<M> forClass(Class<M> cls) {
        MessageSpec spec = cls.getAnnotation(MessageSpec.class);
        if (spec == null) {
            throw new IllegalArgumentException("missing MessageSpec annotation");
        }

        if (Modifier.isAbstract(cls.getModifiers())) {
            throw new IllegalArgumentException(cls.getName() + " cannot be abstract");
        }

        if (!Modifier.isPublic(cls.getModifiers())) {
            throw new IllegalArgumentException(cls.getName() + " must be public");
        }

        Constructor<M> ctor;
        try {
            ctor = cls.getConstructor(MessageDeserializer.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("missing deserialization constructor");
        }

        return new MetaMessage<M>() {
            @Override
            public String getDataType() {
                return spec.type();
            }

            @Override
            public String getMd5sum() {
                return spec.md5sum();
            }

            @Override
            public String getMessageDefinition() {
                return spec.definition();
            }

            @Override
            public M deserialize(MessageDeserializer buf) {
                try {
                    return ctor.newInstance(buf);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new AssertionError("should not happen, this is checked before MetaMessage is created", e);
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException)e.getCause();
                    } else if (e.getCause() instanceof Error) {
                        throw (Error)e.getCause();
                    } else {
                        throw new AssertionError("constructor should not throw an unchecked exception", e);
                    }
                }
            }
        };
    }
}

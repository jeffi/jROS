package edu.unc.cs.robotics.ros.xmlrpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XmlrpcBinding {
    private static final Logger LOG = LoggerFactory.getLogger(XmlrpcBinding.class);

    private static final Set<Type> VALID_SERIALIZATION_TYPES = new HashSet<>(Arrays.asList(
        String.class,
        Number.class, // OK since it can accept int/double
        Integer.class,
        Integer.TYPE,
        Double.class,
        Double.TYPE,
        Boolean.class,
        Boolean.TYPE,
        Object.class,
        Object[].class,
        Date.class
    ));

    private final Map<String, XmlrpcMethod> _methodMap = new HashMap<>();

    private static class ReflectedMethod implements XmlrpcMethod {
        final Object _object;
        final Method _method;

        ReflectedMethod(Object object, Method method) {
            _object = object;
            _method = method;
        }

        Object getObject() {
            return _object;
        }

        @Override
        public Object invoke(Object... params) throws XmlrpcException {
            try {
                return _method.invoke(_object, params);
            } catch (IllegalAccessException e) {
                // we checked it was public when creating the binding
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getTargetException();
                if (targetException instanceof XmlrpcException) {
                    throw (XmlrpcException)targetException;
                } else if (targetException instanceof RuntimeException) {
                    throw (RuntimeException)targetException;
                } else {
                    throw new AssertionError("unexpected exception", e);
                }
            }
        }
    }

    public XmlrpcMethod get(String methodName) {
        synchronized (_methodMap) {
            return _methodMap.get(methodName);
        }
    }

    public void bind(Object obj) {
        synchronized (_methodMap) {
            bind(obj, obj.getClass());
        }
    }

    public void unbind(Object obj) {
        synchronized (_methodMap) {
            Iterator<XmlrpcMethod> it = _methodMap.values().iterator();
            while (it.hasNext()) {
                XmlrpcMethod method = it.next();

                if (method instanceof ReflectedMethod &&
                    ((ReflectedMethod)method).getObject() == obj)
                {
                    it.remove();
                }
            }
        }
    }

    private void bind(Object obj, Class<?> cls) {
        Class<?> superclass = cls.getSuperclass();
        if (superclass != null) {
            bind(obj, superclass);
        }

    outer:
        for (Method method : cls.getDeclaredMethods()) {
            XmlrpcMethodBinding ann = method.getAnnotation(XmlrpcMethodBinding.class);
            if (ann == null) {
                continue;
            }

            if (!Modifier.isPublic(method.getModifiers())) {
                LOG.error("annotated method is not public: "+
                    method.getDeclaringClass().getName() + "." +
                    method.getName());
                continue;
            }

            for (Class<?> exType : method.getExceptionTypes()) {
                if (!XmlrpcException.class.isAssignableFrom(exType) &&
                    !RuntimeException.class.isAssignableFrom(exType)) {

                    LOG.error("annotated method throws incompatible exception: "+
                        exType.getName());
                    continue outer;
                }
            }

            for (Type type : method.getGenericParameterTypes()) {
                if (!isValidSerializationType(type)) {
                    LOG.error("annotated method has an invalid parameter type: " +
                        type);
                }
            }

            if (!isValidSerializationType(method.getGenericReturnType())) {
                LOG.error("annotated method has an invalid return type: "+
                    method.getGenericReturnType());
            }

            String bindName = ann.value();
            if ("##default".equals(bindName)) { // == probably works here too.
                bindName = method.getName();
            }

            if (_methodMap.containsKey(bindName)) {
                LOG.warn("binding for "+bindName+" already exists, overwriting");
            }

            LOG.debug("binding "+method.getDeclaringClass().getName()+"."+
                method.getName()+" as "+bindName);
            _methodMap.put(bindName, new ReflectedMethod(obj, method));
        }
    }

    private boolean isValidSerializationType(Type type) {
        if (VALID_SERIALIZATION_TYPES.contains(type)) {
            return true;
        }

        if (type instanceof Class) {
            return Map.class.isAssignableFrom((Class<?>)type);
        }

        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType)type).getRawType();
            if (!(rawType instanceof Class)) {
                return false;
            }
            if (!Map.class.isAssignableFrom((Class)rawType)) {
                return false;
            }

            Type[] typeParameters = ((ParameterizedType)type).getActualTypeArguments();

            if (typeParameters.length != 2) {
                return false;
            }

            if (String.class != typeParameters[0]) {
                return false;
            }

            if (Object.class != typeParameters[1]) {
                return false;
            }

            return true;
        }

        return false;
    }
}

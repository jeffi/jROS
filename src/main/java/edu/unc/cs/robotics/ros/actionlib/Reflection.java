package edu.unc.cs.robotics.ros.actionlib;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.actionlib.ActionGoal;

class Reflection {
    private Reflection() {}

    public static ParameterizedType findGenericSuperclass(Type type, Class<?> rawSuperclassType) {
        if (rawSuperclassType.isInterface()) {
            throw new IllegalArgumentException(
                "this method does not handle interfaces");
        }

        TypeVariable<? extends Class<?>>[] typeParameters = rawSuperclassType.getTypeParameters();
        if (typeParameters.length == 0) {
            throw new IllegalArgumentException(
                rawSuperclassType.getName()+" is not a generic type");
        }

        do {
            while (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType)type;
                type = pType.getRawType();
                if (type == rawSuperclassType) {
                    return pType;
                }
            }
            if (!(type instanceof Class)) {
                throw new IllegalArgumentException(
                    "don't know how to handle type hierarchy for " + rawSuperclassType.getName());
            }
        } while ((type = ((Class)type).getGenericSuperclass()) != null);

        throw new IllegalArgumentException(
            "argument does not extend "+rawSuperclassType.getName());
    }

    @SuppressWarnings("unchecked")
    public static <G extends Message> Class<G> getGoalClass(Class<? extends ActionGoal<G>> actionGoal) {
        ParameterizedType pType = findGenericSuperclass(actionGoal, ActionGoal.class);
        Type type = pType.getActualTypeArguments()[0];
        if (!(type instanceof Class)) {
            throw new IllegalArgumentException(
                "goal class cannot have type parameters");
        }
        // this is an unchecked cast.  However we know that the type is correct
        // since we're determining what <G> is in this lookup
        return (Class<G>)type;
    }
}

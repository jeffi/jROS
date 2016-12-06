package edu.unc.cs.robotics.ros;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * A ROS name.  See http://wiki.ros.org/Names.  Name instances
 * are interned in the JVM so that there is only one instance
 * per actual string name.  This has the side effect of allowing
 * (a == b) to be true for instances of name.
 *
 * <p>There are two ways of creating a name.  The static
 * Name.create(String) method and the .resolve(String) member method.</p>
 *
 * <p>Names can be global ("/foo"), relative ("foo"), or private ("~foo").</p>
 */
public final class Name {
    // The description on http://wiki.ros.org/Names is a little
    // off on this point.  According to its rules, the following
    // are valid names:
    //   ~////
    //   ~/_
    //   ~/8
    // but the following are invalid:
    //   _
    //   8
    // meaning you can create a name that cannot be resolved
    // relatively.
    //
    // My interpretation here is that a valid part of a name
    // is [a-zA-Z][0-9a-zA-Z_]+
    //
    // And "/" can only be present as a separator between
    // or at the start of a name.
    //
    // Additionally it seems like the possible intent was to
    // only allow _ as a separator, but we'll leave it so that
    // _ suffix and sequences are allowed (e.g. "a_" and "a___b")
    private static final Pattern VALID_NAME = Pattern.compile(
        "[~/]?[a-zA-Z][0-9a-zA-Z_]*(/[a-zA-Z][0-9a-zA-Z_]*)*");

    private static boolean isValidName(String name) {
        return "/".equals(name) || VALID_NAME.matcher(name).matches();
    }

    private static class Ref extends WeakReference<Name> {
        String name;
        public Ref(Name referent, ReferenceQueue<? super Name> q) {
            super(referent, q);
            this.name = referent.toString();
        }
    }
    private static final ConcurrentHashMap<String, Ref> INTERN_MAP = new ConcurrentHashMap<>();
    private static final ReferenceQueue<Name> REF_QUEUE = new ReferenceQueue<>();

    private final String _path;

    private static Name intern(String name) {
        assert isValidName(name);

        // clean up the intern map from any GC'd names
        for (Ref ref; (ref = (Ref)REF_QUEUE.poll()) != null ; ) {
            INTERN_MAP.remove(ref.name, ref);
        }

        final Name newName;
        final Ref newRef;

        Ref oldRef = INTERN_MAP.get(name);
        Name oldName;

        // quick path, either the intern map does not contain
        // the name, and we add it, or it does contain the name
        // and we return it.  We also handle the case where the
        // there is a reference in the map, but it was GC'd.
        if (oldRef == null) {
            newName = new Name(name);
            newRef = new Ref(newName, REF_QUEUE);
            if ((oldRef = INTERN_MAP.putIfAbsent(name, newRef)) == null) {
                return newName;
            } else if ((oldName = oldRef.get()) != null) {
                return oldName;
            }
        } else {
            if ((oldName = oldRef.get()) != null) {
                return oldName;
            }
            newName = new Name(name);
            newRef = new Ref(newName, REF_QUEUE);
        }

        // slow path. at this point we have oldRef which is not
        // null and without a referent.  We also have newRef
        // which is valid, and we're attempting to replace the
        // stale entry in the intern map.  If replacement fails
        // it means another thread inserted a (possibly stale)
        // reference.
        for (;;) {
            if (INTERN_MAP.replace(name, oldRef, newRef) ||
                (oldRef = INTERN_MAP.putIfAbsent(name, newRef)) == null) {
                return newName;
            } else if ((oldName = oldRef.get()) != null) {
                return oldName;
            }
        }
    }

    private Name(String path) {
        _path = path;
    }

    public static Name create(String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("invalid name: " + name);
        }

        return intern(name);
    }

    public boolean isRelative() {
        return !isGlobal() && !isPrivate();
    }

    public boolean isGlobal() {
        return _path.charAt(0) == '/';
    }

    public boolean isPrivate() {
        return _path.charAt(0) == '~';
    }

    public Name resolve(Name name) {
        if (name.isGlobal()) {
            // shortcut interning path
            return name;
        } else {
            return resolve(name._path);
        }
    }

    public Name resolveNS(String name) {
        if (!isGlobal()) {
            throw new IllegalArgumentException("cannot resolve relative to a non-global name");
        }

        if (!isValidName(name)) {
            throw new IllegalArgumentException("invalid name: " + name);
        }

        if (name.charAt(0) == '~') {
            throw new IllegalArgumentException("cannot resolve ~ names with resolveNS");
        }

        if (name.charAt(0) == '/') {
            return intern(name);
        }

        if (_path.isEmpty()) {
            return intern(name);
        } else if ("/".equals(_path)) {
            return intern("/" + name);
        } else {
            assert !_path.endsWith("/");
            return intern(_path + "/" + name);
        }
    }

    public Name resolve(String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("invalid name: " + name);
        }

        if (name.charAt(0) == '/') {
            // global name, just return directly
            return intern(name);
        }

        if (name.charAt(0) == '~') {
            // private name
            // [/node1].resolve("~bar") -> "/node1/bar"

            if (_path.charAt(_path.length()-1) == '/') {
                return intern(_path + name.substring(1));
            } else {
                return intern(_path + '/' + name);
            }
        }

        // only thing that remains is a "base" or "relative" name

        // [/node1].resolve("bar") -> [/bar]
        // [/wg/node2].resolve("bar") -> [/wg/bar]
        // [/wg/node3].resolve("foo/bar") -> [/wg/foo/bar]
        // [~private/node4].resolve("bar") -> [~private/bar]

        // find the last index
        int i = _path.lastIndexOf('/');
        if (i != -1) {
            // this is a global name, append the resolved name
            // to everything before the last '/' in this.
            return intern(_path.substring(0, i + 1) + name);
        }

        // else this is a private or base name
        // [~private].resolve("bar") -> "~bar"
        // [base].resolve("bar") -> "bar"

        if (isPrivate()) {
            return intern("~" + name);
        } else {
            return intern(name);
        }
    }

    // Note: since all names are interned, we do not have to
    // implement .equals or .hashCode.  The default identity
    // based version will do the job.

    @Override
    public String toString() {
        return _path;
    }
}

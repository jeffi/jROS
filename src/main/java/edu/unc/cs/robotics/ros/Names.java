package edu.unc.cs.robotics.ros;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Names {
    private static final Logger LOG = LoggerFactory.getLogger(Names.class);
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile(
        "[/~A-Za-z][/_a-zA-Z0-9]*");

    final Map<String,String> _remappings;
    final Map<String,String> _unresolvedRemappings;
    final String _namespace;
    final String _name;


    public Names(String name, Map<String,String> remappings, boolean anonymousName) {
        _remappings = new HashMap<>();
        _unresolvedRemappings = new HashMap<>();

        String namespace = System.getenv("ROS_NAMESPACE");
        boolean disableAnon = false;

        if (remappings.containsKey("__name")) {
            name = remappings.get("__name");
            disableAnon = true;
        }
        if (remappings.containsKey("__ns")) {
            namespace = remappings.get("__ns");
        }

        if (namespace == null || namespace.isEmpty()) {
            namespace = "/";
        }

        if (!"/".equals(namespace)) {
            namespace = "/" + namespace;
        }

        if (!validate(namespace)) {
            throw new IllegalArgumentException("invalid namespace: " + namespace);
        }

        _namespace = namespace;

        // this is names::init(remappings)
        for (Map.Entry<String,String> entry : remappings.entrySet()) {
            String key = entry.getKey();
            if (!key.isEmpty() && key.charAt(0) != '_' && !key.equals(name)) {
                String resolvedKey = resolve(key, false);
                String value = entry.getValue();
                String resolvedValue = resolve(value, false);
                _remappings.put(resolvedKey, resolvedValue);
                _unresolvedRemappings.put(key, value);
            }
        }


        if (name.indexOf('/') != -1) {
            throw new IllegalArgumentException("node names cannot contain /");
        }

        if (name.indexOf('~') != -1) {
            throw new IllegalArgumentException("node names cannot contain ~");
        }

        name = resolve(namespace, name, true);

        if (anonymousName && !disableAnon) {
            name = Long.toString(System.currentTimeMillis());
        }

        _name = name;
        LOG.info("configuring with namespace={}, name={}", namespace, name);
    }

    String resolve(String name, boolean remap) {
        return resolve(_namespace, name, remap);
    }

    private String resolve(String ns, String name, boolean remap) {
        if (!validate(name)) {
            throw new IllegalArgumentException("invalid name: " + name);
        }

        if (name.isEmpty()) {
            if (ns.isEmpty()) {
                return "/";
            }
            if (ns.charAt(0) == '/') {
                return ns;
            }
            return append("/", ns);
        }

        if (name.charAt(0) == '~') {
            name = append(_name, name.substring(1));
        }
        if (name.charAt(0) != '/') {
            name = append("/", append(ns, name));
        }

        name = clean(name);

        if (remap) {
            name = remap(name);
        }

        return name;
    }

    private String remap(String name) {
        String resolved = resolve(name, false);
        String remap = _remappings.get(resolved);
        return remap != null ? remap : name;
    }

    String append(String a, String b) {
        return clean(a + "/" + b);
    }

    static String clean(String s) {
        s = s.replaceAll("//+", "/");
        return s.endsWith("/") ? s.substring(0, s.length()-1) : s;
    }

    static boolean validate(String name) {
        return VALID_NAME_PATTERN.matcher(name).matches();
    }

    public String getName() {
        return _name;
    }

    public String getNamespace() {
        return _namespace;
    }
}

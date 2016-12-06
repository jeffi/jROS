package edu.unc.cs.robotics.ros;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HostNameMap {
    private static final Logger LOG = LoggerFactory.getLogger(HostNameMap.class);

    private final Map<String, String> _map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public HostNameMap addMapping(String from, String to) {
        if (_map.containsKey(to)) {
            throw new IllegalArgumentException("cyclic mapping detected");
        }

        _map.put(from, to);
        return this;
    }

    public String remap(String host) {
        String remap = _map.get(host);
        return remap != null ? remap : host;
    }

    public URI remap(URI uri) {
        final String host = _map.get(uri.getHost());
        if (host == null) {
            return uri;
        }

        try {
            final URI remapped = new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                host,
                uri.getPort(),
                uri.getPath(),
                uri.getQuery(),
                uri.getFragment());

            LOG.debug("Remapped {} -> {}", uri, remapped);

            return remapped;
        } catch (URISyntaxException e) {
            LOG.warn("failed to remap host in "+uri, e);
            return uri;
        }
    }
}

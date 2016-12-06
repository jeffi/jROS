package edu.unc.cs.robotics.ros;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class LookupHostBindingService implements HostBindingService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupHostBindingService.class);

    private String _host;

    @Override
    public synchronized void start() {
        try {
            Enumeration<NetworkInterface> netifs = NetworkInterface.getNetworkInterfaces();

            InetAddress bestAddr = null;

        netifsLoop:
            while (netifs.hasMoreElements()) {
                NetworkInterface netif = netifs.nextElement();
                if (!netif.isUp()) {
                    LOG.debug("skipping down interface: {}", netif);
                    continue;
                }

                Enumeration<InetAddress> addrs = netif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();

                    // use the first non-loopback ipv4 address
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        bestAddr = addr;
                        break netifsLoop;
                    }

                    if (bestAddr == null ||
                        // prefer non-loopback
                        !addr.isLoopbackAddress() && bestAddr.isLoopbackAddress() ||
                        // prefer ipv4
                        addr instanceof Inet4Address && !(bestAddr instanceof Inet4Address))
                    {
                        bestAddr = addr;
                    }
                }
            }

            if (bestAddr == null) {
                throw new IllegalStateException("could not find an available local internet address");
            }


            _host = bestAddr.getHostAddress();

            if (bestAddr.isLoopbackAddress()) {
                LOG.warn("using LOOPBACK address {}", _host);
            } else {
                LOG.info("using address {}", _host);
            }


        } catch (SocketException e) {
            // TODO: make Service.start() throws Exception
            // let caller deal with it
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() {
        _host = null;
    }

    @Override
    public String host() {
        return _host;
    }
}

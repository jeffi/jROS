package edu.unc.cs.robotics.ros;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ROS time is stored in nanoseconds precision.  This utility class provides
 * a class for dealing with ROS time.  Both time and duration are stored in
 * the same way.  Time is stored with the same epoch as Java methods, so there
 * is just one utility class for both.
 *
 * <p>In ROS CPP, time is stored as a seconds + nanoseconds pair, each in an
 * unsigned 32-bit integer.  In this version, time is stored as a single 64-bit
 * quantity in nanoseconds.  This has an advantage of making math easier, and
 * also extending the time at which overflow will occur.</p>
 *
 * <p>With 32-bits for seconds, there is approximately 136 years before overflow
 * (with time=0 at 1970, that means overflow in ~2106).  With 64-bits for nanoseconds,
 * there is approximately 584 years before overflow (meaning overflow will happen
 * in the year 2554)</p>
 */
public final class ROSTime {
    private static final Logger LOG = LoggerFactory.getLogger(ROSTime.class);

    private ROSTime() {}

    interface ROSClock {
        long get();
    }

    /**
     * Normalize the version string to be zero-padded.
     * This allows for String.compareTo on versions
     *
     * @param verStr the version string to normalize
     * @return the normalized version string
     */
    private static String normVer(String verStr) {
        return verStr.replaceAll("\\d+", "0000000$0")
            .replaceAll("0+(\\d{8})", "$1");
    }

    /**
     * System.currentTimeMillis() version of time stamps.
     * This is available in all versions of java, however
     * it only has millisecond precision, whereas nanosecond
     * precision is desired.
     */
    private static class CurrentTimeMillisImpl implements ROSClock {
        {
            LOG.debug("using System.currentTimeMillis for time stamps");
        }
        @Override
        public long get() {
            return System.currentTimeMillis()*1_000_000L;
        }
    }

    /**
     * java.time.Clock based version of time stamps.  This
     * is available in java 1.8+, but will not have more
     * precision than System.currentTimeMillis() until 1.9
     */
    private static class JavaTimeClockImpl implements ROSClock {
        {
            LOG.debug("using java.time.Clock for time stamps");
        }
        private final Clock _clock = Clock.systemUTC();
        @Override
        public long get() {
            final Instant instant = _clock.instant();
            final long sec = instant.getEpochSecond();
            final int nsec = instant.getNano();
            return sec * 1_000_000_000L + nsec;
        }
    }

    // This is a possible route to get nanoseconds into the
    // time stamp.  However it has a few problems that need to be
    // worked out:
    // 1. System.currentTimeMillis() can drift slowly to sync with NTP
    //    whereas System.nanoTime() will monotonically increase.
    //    Thus you can have more or less than 1,000,000 nanoseconds
    //    between each milliseconds.
    // 2. System.currentTimeMillis() can drift drastically with a
    //    manual system clock change.
    // 3. System.nanoTime() is possibly NOT thread-safe, thus we might need
    //    to track with a thread-local (have not confirmed this to be true)
    private static class NanoTrackingImpl implements ROSClock {
        private final long _startTimeNanos = System.currentTimeMillis()*1_000_000L - System.nanoTime();

        public long get() {
            return _startTimeNanos + System.nanoTime();
        }

        /**
         * Returns the current time in milliseconds.  This is a convenience method
         * that simply computes {@code get() / 1_000_000L}.  Thus it uses the
         * same time source and is consistent with {@link #get()}.
         *
         * @return current time in milliseconds.
         */
        public long getMillis() {
            return get() / 1_000_000L;
        }
    }

    private static final ROSClock INSTANCE = normVer("1.9")
        .compareTo(normVer(System.getProperty("java.version"))) >= 0
        ? new CurrentTimeMillisImpl()
        : new JavaTimeClockImpl();

    /**
     * Returns the current time stamp.
     *
     * @return the current time stamp.
     */
    public static long stamp() {
        return INSTANCE.get();
    }

    /**
     * Converts the given time to a ros time.
     *
     * @param duration the duration in the specified timeUnit
     * @param timeUnit the timeUnit for the duration
     * @return ros time.
     */
    public static long convertFrom(long duration, TimeUnit timeUnit) {
        return timeUnit.toNanos(duration);
    }

    /**
     * Converts a ROS time to the specified unit
     *
     * @param rosTime time in ROS time
     * @param timeUnit the unit to convert to
     * @return the time in the timeUnit
     */
    public static long convertTo(long rosTime, TimeUnit timeUnit) {
        return timeUnit.convert(rosTime, TimeUnit.NANOSECONDS);
    }

    public static long convertFromMillis(long ms) {
        return convertFrom(ms, TimeUnit.MILLISECONDS);
    }

    /**
     * Converts time in seconds to ROS time.
     *
     * @param seconds the time in seconds
     * @return ROS time
     */
    public static long convertFromSeconds(double seconds) {
        return (long)(seconds * 1e9);
    }

    /**
     * Converts from a (seconds, nano-seconds) pair into a single long.
     *
     * @param sec the seconds
     * @param nsec the nano-seconds
     * @return the ros time.
     */
    public static long convertFromSecNSec(int sec, int nsec) {
        return sec * 1_000_000_000L + nsec;
    }

//    public static void main(String ... args) throws InterruptedException {
//        CurrentTimeMillisImpl currentTimeMillis = new CurrentTimeMillisImpl();
//        NanoTrackingImpl nanoTracking = new NanoTrackingImpl();
//
//        // this loop showed a drift of ~3 ms by the end.
//        for (int i=0 ; i<100 ; ++i) {
//            long a = currentTimeMillis.get();
//            long b = nanoTracking.get();
//            long c = Math.abs(a - b);
//
//            System.out.printf("%d.%09d  %d.%09d  drift=%c%d.%09d%n",
//                a / 1_000_000_000L,
//                a % 1_000_000_000L,
//                b / 1_000_000_000L,
//                b % 1_000_000_000L,
//                a < b ? '-' : '+',
//                c / 1_000_000_000L,
//                c % 1_000_000_000L);
//
//            Thread.sleep(1000);
//        }
//    }
}

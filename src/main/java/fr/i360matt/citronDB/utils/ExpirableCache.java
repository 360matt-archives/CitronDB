package fr.i360matt.citronDB.utils;


import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * This class makes it possible to establish a cache system which can expire with the defined time.
 * It is from Vivekananthan but it was remade by me (360matt)
 *
 * @author Vivekananthan M
 * https://github.com/vivekjustthink/WeakConcurrentHashMap
 * https://stackoverflow.com/questions/3802370/java-time-based-map-cache-with-expiring-keys
 *
 * @author 360matt ( reformat - github.com/360matt )
 *
 * @param <K> Key Type
 * @param <V> Value Type
 */
public class ExpirableCache<K, V> extends ConcurrentHashMap<K, V> {

    public static final ExpirableCache<Class<?>, Object> types = new ExpirableCache<>(10_000);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private final Map<K, Long> timeMap = new ConcurrentHashMap<>();
    private long expiryInMillis = 1000;

    public ExpirableCache () {
        startTask();
    }

    public ExpirableCache (final long expiryInMillis) {
        this.expiryInMillis = expiryInMillis;
        startTask();
    }

    @Override
    public final V put (final K key, final V value) {
        final Date date = new Date();
        timeMap.put(key, date.getTime());
        return super.put(key, value);
    }

    @Override
    public final void putAll (final Map<? extends K, ? extends V> m) {
        for (final Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public final V putIfAbsent(final K key, final V value) {
        if (!containsKey(key))
            return put(key, value);
        else
            return get(key);
    }

    private void startTask () {
        executor.scheduleAtFixedRate(() -> {
            final long currentTime = System.currentTimeMillis();

            /*
            for (final Entry<K, Long> entry  : timeMap.entrySet()) {
                if (currentTime > (entry.getValue() + expiryInMillis)) {
                    remove(entry.getKey());
                    timeMap.remove(entry.getKey());
                }
            }
             */

            final Iterator<Entry<K, Long>> iter = timeMap.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<K, Long> entry = iter.next();
                if (currentTime > (entry.getValue() + expiryInMillis)) {
                    remove(entry.getKey());
                    iter.remove();
                }
            }

        }, expiryInMillis / 2, expiryInMillis / 2, TimeUnit.MILLISECONDS);
    }

    public final void quitMap () {
        executor.shutdownNow();
        clear();
        timeMap.clear();
    }

    public final boolean isAlive () {
        return !executor.isShutdown();
    }
}
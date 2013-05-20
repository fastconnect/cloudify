package org.cloudifysource.dsl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {

    private int maxSize;

    public MaxSizeHashMap(final int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}

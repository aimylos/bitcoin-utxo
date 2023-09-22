package Blockchainj.Util;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * SortedLinkedHashMap
 *
 * This class is used to created a presorted LinkedHashMap.
 * This class can be used to create a SortedMap from a presorted Link efficiently.
 *
 */

public class SortedLinkedHashMap<K,V> extends LinkedHashMap<K,V> implements SortedMap<K,V> {

    public SortedLinkedHashMap() {
        super();
    }

    public SortedLinkedHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public SortedLinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public SortedLinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }

    public SortedLinkedHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }


    @Override
    public Comparator<? super K> comparator() {
        return null;
    }

    @Override
    public SortedMap<K,V> tailMap(K fromKey) {
        return new SortedLinkedHashMap<>();
    }

    @Override
    public K firstKey() {
        return null;
    }

    @Override
    public SortedMap<K,V> subMap(K fromKey, K toKey) {
        return new SortedLinkedHashMap<>();
    }

    @Override
    public K lastKey() {
        return null;
    }

    @Override
    public SortedMap<K,V> headMap(K toKey) {
        return new SortedLinkedHashMap<>();
    }
}

package com.smarttask.datastructures;

/**
 * Custom hash map with separate chaining (replacement for HashMap).
 * Used to map userId -> in-memory task structures quickly.
 */
public class SimpleMap<K, V> {

    private static class Entry<K, V> {
        private final K key;
        private V value;
        private Entry<K, V> next;

        private Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Entry<K, V>[] buckets;
    private int size;

    @SuppressWarnings("unchecked")
    public SimpleMap() {
        this.buckets = (Entry<K, V>[]) new Entry[16];
        this.size = 0;
    }

    public void put(K key, V value) {
        int index = indexFor(key);
        Entry<K, V> current = buckets[index];
        while (current != null) {
            if (equals(current.key, key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        Entry<K, V> created = new Entry<>(key, value);
        created.next = buckets[index];
        buckets[index] = created;
        size++;
        if (size > buckets.length * 0.75) {
            resize();
        }
    }

    public V get(K key) {
        int index = indexFor(key);
        Entry<K, V> current = buckets[index];
        while (current != null) {
            if (equals(current.key, key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public V getOrDefault(K key, V defaultValue) {
        V value = get(key);
        return value == null ? defaultValue : value;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public V remove(K key) {
        int index = indexFor(key);
        Entry<K, V> current = buckets[index];
        Entry<K, V> previous = null;
        while (current != null) {
            if (equals(current.key, key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return current.value;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    public int size() {
        return size;
    }

    public DynamicArray<K> keys() {
        DynamicArray<K> keys = new DynamicArray<>();
        for (Entry<K, V> bucket : buckets) {
            Entry<K, V> current = bucket;
            while (current != null) {
                keys.add(current.key);
                current = current.next;
            }
        }
        return keys;
    }

    private int indexFor(K key) {
        int hash = key == null ? 0 : key.hashCode();
        return Math.floorMod(hash, buckets.length);
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] old = buckets;
        buckets = (Entry<K, V>[]) new Entry[old.length * 2];
        size = 0;
        for (Entry<K, V> bucket : old) {
            Entry<K, V> current = bucket;
            while (current != null) {
                put(current.key, current.value);
                current = current.next;
            }
        }
    }

    private boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}

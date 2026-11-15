package com.smarttask.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Custom resizable array (replacement for ArrayList).
 * Grows by doubling capacity when full.
 */
public class DynamicArray<T> implements Iterable<T> {

    private Object[] data;
    private int size;

    public DynamicArray() {
        this(8);
    }

    public DynamicArray(int capacity) {
        if (capacity < 1) {
            capacity = 8;
        }
        this.data = new Object[capacity];
        this.size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(T value) {
        ensureCapacity(size + 1);
        data[size++] = value;
    }

    public void addAll(DynamicArray<T> other) {
        if (other == null) {
            return;
        }
        for (int i = 0; i < other.size; i++) {
            add(other.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndex(index);
        return (T) data[index];
    }

    public void set(int index, T value) {
        checkIndex(index);
        data[index] = value;
    }

    @SuppressWarnings("unchecked")
    public T removeAt(int index) {
        checkIndex(index);
        T removed = (T) data[index];
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
        }
        data[--size] = null;
        return removed;
    }

    public boolean remove(T value) {
        int index = indexOf(value);
        if (index < 0) {
            return false;
        }
        removeAt(index);
        return true;
    }

    public int indexOf(T value) {
        for (int i = 0; i < size; i++) {
            if (equals(data[i], value)) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(T value) {
        return indexOf(value) >= 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            data[i] = null;
        }
        size = 0;
    }

    public DynamicArray<T> copy() {
        DynamicArray<T> copy = new DynamicArray<>(Math.max(8, size));
        for (int i = 0; i < size; i++) {
            copy.add(get(i));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    public T[] toArray(T[] array) {
        if (array.length < size) {
            array = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), size);
        }
        for (int i = 0; i < size; i++) {
            array[i] = (T) data[i];
        }
        if (array.length > size) {
            array[size] = null;
        }
        return array;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= data.length) {
            return;
        }
        int newCapacity = Math.max(data.length * 2, minCapacity);
        Object[] grown = new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            grown[i] = data[i];
        }
        data = grown;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " size " + size);
        }
    }

    private boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return (T) data[cursor++];
            }
        };
    }
}

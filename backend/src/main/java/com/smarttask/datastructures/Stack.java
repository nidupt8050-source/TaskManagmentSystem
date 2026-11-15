package com.smarttask.datastructures;

import java.util.EmptyStackException;

/**
 * Custom LIFO stack built on DynamicArray.
 * Used for undoing the most recent task status change.
 */
public class Stack<T> {

    private final DynamicArray<T> data = new DynamicArray<>();

    public void push(T value) {
        data.add(value);
    }

    public T pop() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return data.removeAt(data.size() - 1);
    }

    public T peek() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return data.get(data.size() - 1);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public int size() {
        return data.size();
    }

    public void clear() {
        data.clear();
    }
}

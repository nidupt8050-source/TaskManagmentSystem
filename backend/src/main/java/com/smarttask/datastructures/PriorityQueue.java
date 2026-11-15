package com.smarttask.datastructures;

import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Custom binary-heap priority queue (min-heap by default via comparator).
 * Used to surface earliest task deadlines first.
 */
public class PriorityQueue<T> {

    private final DynamicArray<T> heap = new DynamicArray<>();
    private final Comparator<T> comparator;

    public PriorityQueue(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    public void offer(T value) {
        heap.add(value);
        siftUp(heap.size() - 1);
    }

    public T poll() {
        if (isEmpty()) {
            throw new NoSuchElementException("Priority queue is empty");
        }
        T root = heap.get(0);
        T last = heap.removeAt(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            siftDown(0);
        }
        return root;
    }

    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Priority queue is empty");
        }
        return heap.get(0);
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    public int size() {
        return heap.size();
    }

    public PriorityQueue<T> copy() {
        PriorityQueue<T> copy = new PriorityQueue<>(comparator);
        for (int i = 0; i < heap.size(); i++) {
            copy.heap.add(heap.get(i));
        }
        return copy;
    }

    public void clear() {
        heap.clear();
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (comparator.compare(heap.get(index), heap.get(parent)) >= 0) {
                break;
            }
            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown(int index) {
        int size = heap.size();
        while (true) {
            int left = index * 2 + 1;
            int right = left + 1;
            int smallest = index;

            if (left < size && comparator.compare(heap.get(left), heap.get(smallest)) < 0) {
                smallest = left;
            }
            if (right < size && comparator.compare(heap.get(right), heap.get(smallest)) < 0) {
                smallest = right;
            }
            if (smallest == index) {
                break;
            }
            swap(index, smallest);
            index = smallest;
        }
    }

    private void swap(int i, int j) {
        T temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}

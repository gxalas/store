package com.example.pdfreader.Helpers;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObservableQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final CopyOnWriteArrayList<QueueListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void addListener(QueueListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(QueueListener<T> listener) {
        listeners.remove(listener);
    }

    public void add(T element) {
        queue.add(element);
        fireElementAdded(element);
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }
    public int size(){
        return queue.size();
    }
    public void clear(){
        fireElementRemoved(null);
        queue.clear();
    }

    public T poll() {
        T element = queue.poll();
        if (element != null) {
            fireElementRemoved(element);
        }
        return element;
    }
    public List<T> toList(){
        return queue.stream().toList();
    }
    public void remove(T element){
        queue.remove(element);
        fireElementRemoved(element);
    }

    private void fireElementAdded(T element) {
        for (QueueListener<T> listener : listeners) {
            listener.elementAdded(element);
        }
    }

    private void fireElementRemoved(T element) {
        for (QueueListener<T> listener : listeners) {
            listener.elementRemoved(element);
        }
    }


    public interface QueueListener<T> {
        void elementAdded(T element);
        void elementRemoved(T element);
    }
}


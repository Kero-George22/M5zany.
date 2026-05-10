package com.smartstock.util;

import com.smartstock.model.PhantomEntity;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GenericRepository<T extends PhantomEntity> {
    private final Map<Integer, T> storage = new HashMap<>();
    private int nextId = 1;

    public T add(T entity) {
        entity.setId(nextId++);
        storage.put(entity.getId(), entity);
        return entity;
    }

    public T getById(int id) {
        return storage.get(id);
    }

    public List<T> getAll() {
        return List.copyOf(storage.values());
    }

    public List<T> find(Predicate<T> predicate) {
        return storage.values().stream().filter(predicate).collect(Collectors.toUnmodifiableList());
    }

    public boolean update(T entity) {
        if (entity == null || !storage.containsKey(entity.getId())) {
            return false;
        }
        storage.put(entity.getId(), entity);
        return true;
    }

    public boolean remove(int id) {
        return storage.remove(id) != null;
    }

    public boolean remove(T entity) {
        if (entity == null) return false;
        return storage.remove(entity.getId()) != null;
    }

    public int count() {
        return storage.size();
    }

    public void clear() {
        storage.clear();
        nextId = 1;
    }
}

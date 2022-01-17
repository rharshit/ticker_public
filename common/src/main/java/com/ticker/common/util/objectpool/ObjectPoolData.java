package com.ticker.common.util.objectpool;

import lombok.Data;

import java.util.Objects;


@Data
public class ObjectPoolData<D> {
    private boolean valid = false;
    private boolean idle = true;

    private long lastUsed;
    private D object;


    public ObjectPoolData(D object) {
        this.object = object;
        this.lastUsed = System.currentTimeMillis();
        this.valid = true;
    }

    public void destroy(D object) {

    }

    public void destroy() {
        this.object = null;
        this.valid = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectPoolData<?> that = (ObjectPoolData<?>) o;
        return Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }
}

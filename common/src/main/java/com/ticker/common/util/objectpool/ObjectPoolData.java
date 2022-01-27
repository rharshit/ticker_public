package com.ticker.common.util.objectpool;

import lombok.Data;

import java.util.Objects;


@Data
public abstract class ObjectPoolData<T> {
    private boolean valid = false;
    private boolean idle = true;

    private long lastUsed;
    private T object;

    public ObjectPoolData() {
        this.object = createObject();
        this.lastUsed = System.currentTimeMillis();
        this.valid = true;
    }

    protected void setObject(Object object) {
        this.object = (T) object;
    }

    public abstract T createObject();

    public abstract void destroyObject(T object);

    public void destroy() {
        this.valid = false;
        destroyObject(this.object);
        this.object = null;
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

    public boolean equalsObject(Object o) {
        if (this.getObject() == o) return true;
        if (o == null || getObject().getClass() != o.getClass()) return false;
        return Objects.equals(object, o);
    }
}

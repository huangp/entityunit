package com.github.huangp.entityunit.maker;

public interface Maker<T> {

    /**
     * @return a made value
     */
    T value();

}

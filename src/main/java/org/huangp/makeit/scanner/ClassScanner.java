package org.huangp.makeit.scanner;

public interface ClassScanner
{
    Iterable<EntityClass> scan(Class clazz);
}

package org.huangp.scanner;

public interface ClassScanner
{
    Iterable<EntityClass> scan(Class clazz);
}

package com.github.huangp.entityunit.entity;

import com.github.huangp.entities.Category;
import com.github.huangp.entities.LineItem;
import com.github.huangp.entities.Person;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;

/**
 * @author Patrick Huang
 */
public class EntityCleanerTest {
    @Mock
    private EntityManager entityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Ignore("not yet implemented")
    public void canDeleteTableInOrder() {
        EntityCleaner.deleteAll(entityManager, Category.class, LineItem.class, Person.class);
    }
}

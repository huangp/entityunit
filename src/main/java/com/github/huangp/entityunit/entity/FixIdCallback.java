package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.HasAnnotationPredicate;
import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.EntityManager;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.github.huangp.entityunit.util.ClassUtil.findEntity;

/**
 * <b>NOTE:</b> Altering primary key is not supported in JPA specification.
 * <p/>
 * However in some occasion this is necessary.
 * i.e. some web app and framework keeps the entity id in session and if you delete and make a new object with different
 * id in your Selenium test it will fail.
 * <p/>
 * The trick is to after hibernate persisted the entity and have a generated id, we alter it by using query.
 * The persistence context is polluted and can't be trusted. Therefore the entity will be detached first and re-loaded
 * again.
 * <p/>
 * There is no guarantee referential constraints are defined with "on update cascade", therefore <b>this callback only
 * supports entity with no association.</b>
 *
 * @author Patrick Huang
 * @see <a href="http://stackoverflow.com/questions/734461/hibernate-alter-identifier-primary-key/2217064#2217064">stack overflow entry</a>
 */
@RequiredArgsConstructor
@Slf4j
public class FixIdCallback extends AbstractNoOpCallback {
    // TODO reformat and provide some example
    private static final String NOT_EMPTY_ASSOCIATION_ERROR =
            "Found not empty or null associations [%s]. Fix Id only works on no association entity. You can make it with fix id first and add the association manually";

    private final Class<?> entityType;
    private final Serializable wantedIdValue;

    @Override
    public Iterable<Object> afterPersist(EntityManager entityManager, Iterable<Object> persisted) {

        Object entity = findEntity(persisted, entityType);

        final Settable identityField = ClassUtil.getIdentityField(entity);
        Serializable generatedIdValue = getGeneratedId(entity, identityField);
        if (generatedIdValue.equals(wantedIdValue))
        {
           return persisted;
        }
        checkPrecondition(entity);

        // TODO consider entity name mapping and id column mapping
        String tableName = entityType.getSimpleName();
        String idColumnName = identityField.getSimpleName();
        String sqlString = String.format("update %s set %s=%s where %s=%s", tableName, idColumnName, wantedIdValue, idColumnName, generatedIdValue);
        log.info("query to update generated id: {}", sqlString);

        int affectedRow = entityManager.createQuery(sqlString).executeUpdate();
        log.debug("update generated id affected row: {}", affectedRow);

        // set the updated value back to entity
        entityManager.detach(entity); // remove it from current persistence context
        // regain the entity back
        Object updated = entityManager.find(entityType, wantedIdValue);
        List<Object> toReturn = Lists.newArrayList(persisted);
        int index = Iterables.indexOf(persisted, Predicates.instanceOf(entityType));
        toReturn.set(index, updated);
        return toReturn;
    }

    private void checkPrecondition(Object entity) {
        try {
            EntityClass entityClass = EntityClass.from(entityType, ScanOption.IncludeOneToOne);

            Iterable<Method> oneToManyGetters = entityClass.getContainingEntitiesGetterMethods();
            Iterable<Method> manyToManyGetters = entityClass.getManyToManyMethods();
            Iterable<Method> oneToOneGetters = getOneToOneGetters(entityClass);
            Iterable<Method> associations = Iterables.concat(oneToManyGetters, manyToManyGetters, oneToOneGetters);

            for (Method method : associations) {
                Object result = method.invoke(entity);
                if (result == null) {
                    continue;
                }
                log.debug("method [{}] result: {}", method.getName(), result);
                if (ClassUtil.isCollection(result.getClass())) {
                    Preconditions.checkState((result == null) || ((Collection) result).isEmpty(), NOT_EMPTY_ASSOCIATION_ERROR, method.getName());
                } else if (ClassUtil.isMap(result.getClass())) {
                    Preconditions.checkState((result == null) || ((Map) result).isEmpty(), NOT_EMPTY_ASSOCIATION_ERROR, method.getName());
                } else {
                    Preconditions.checkState(result == null, NOT_EMPTY_ASSOCIATION_ERROR, method.getName());
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static Iterable<Method> getOneToOneGetters(EntityClass entityClass) {
        Iterable<Settable> oneToOnes = Iterables.filter(entityClass.getElements(), HasAnnotationPredicate.has(OneToOne.class));
        return Iterables.transform(oneToOnes, new Function<Settable, Method>() {

            @Override
            public Method apply(Settable input) {
                return input.getterMethod();
            }
        });
    }

    private static Serializable getGeneratedId(Object entity, Settable identityField) {
        try {
            return (Serializable) identityField.getterMethod().invoke(entity);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

}

package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.HasAnnotationPredicate;
import com.github.huangp.entityunit.util.Settable;
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
            "Found not empty or null associations [%s]. Fix Id only works on no association entity. You can make it with fix id first and add the association manually later";

    private final Class<?> entityType;
    private final Serializable wantedIdValue;

    @Override
    public Iterable<Object> afterPersist(EntityManager entityManager, Iterable<Object> persisted) {

        Object entity = findEntity(persisted, entityType);

        Settable identityField = ClassUtil.getIdentityField(entity);
        Serializable generatedIdValue = identityField.valueIn(entity);
        if (generatedIdValue.equals(wantedIdValue))
        {
           return persisted;
        }
        checkPrecondition(entity);

        String entityName = ClassUtil.getEntityName(entityType);
        String idColumnName = identityField.getSimpleName();
        String queryString = String.format("update %s set %s=%s where %s=%s", entityName, idColumnName, wantedIdValue, idColumnName, generatedIdValue);
        log.info("query to update generated id: {}", queryString);

        int affectedRow = entityManager.createQuery(queryString).executeUpdate();
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

            Iterable<Settable> oneToManyGetters = entityClass.getContainingEntitiesElements();
            Iterable<Settable> manyToManyGetters = entityClass.getManyToMany();
            Iterable<Settable> oneToOneGetters = getOneToOneGetters(entityClass);
            Iterable<Settable> associations = Iterables.concat(oneToManyGetters, manyToManyGetters, oneToOneGetters);

            for (Settable settable : associations) {
                Object result = settable.valueIn(entity);
                if (result == null) {
                    continue;
                }
                log.debug("referenced entity association [{}] result: {}", settable.getSimpleName(), result);
                if (ClassUtil.isCollection(result.getClass())) {
                    Preconditions.checkState((result == null) || ((Collection) result).isEmpty(),
                            NOT_EMPTY_ASSOCIATION_ERROR, settable.fullyQualifiedName());
                } else if (ClassUtil.isMap(result.getClass())) {
                    Preconditions.checkState((result == null) || ((Map) result).isEmpty(),
                            NOT_EMPTY_ASSOCIATION_ERROR, settable.fullyQualifiedName());
                } else {
                    Preconditions.checkState(result == null, NOT_EMPTY_ASSOCIATION_ERROR,
                            settable.fullyQualifiedName());
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static Iterable<Settable> getOneToOneGetters(EntityClass entityClass) {
        return Iterables.filter(entityClass.getElements(), HasAnnotationPredicate.has(OneToOne.class));
    }

}

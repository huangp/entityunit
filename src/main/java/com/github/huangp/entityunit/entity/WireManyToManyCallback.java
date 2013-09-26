package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.util.ClassUtil;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import org.jodah.typetools.TypeResolver;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * By default the EntityMaker will not wire many to many associations. This callback is provided to take care of it.
 * <p/>
 * Example:
 * <pre>
 * <code>
 *
 * {@literal @}Entity
 * class Account {
 *     {@literal @}ManyToMany(targetEntity = Role.class)
 *     {@literal @}JoinTable(name = "AccountMembership", joinColumns = @JoinColumn(name = "accountId"), inverseJoinColumns = @JoinColumn(name = "memberOf"))
 *     public Set<Role> getRoles() {
 *         return roles;
 *     }
 *
 * }
 *
 * {@literal @}Entity
 * class Role {
 * }
 *
 * // assuming role is already in database
 * Role role = entityManager.find(Role.class, 1L);
 * Account account = maker.makeAndPersist(entityManager, HAccount.class, new WireManyToManyCallback(HAccount.class, role));
 *
 * assertThat(account.getRoles(), Matchers.containsInAnyOrder(role));
 *
 * </code>
 * </pre>
 *
 * @author Patrick Huang
 */
@RequiredArgsConstructor
public class WireManyToManyCallback extends AbstractNoOpCallback {
    private final Class typeToFind;
    private final Object objectToWire;

    @Override
    public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted) {
        Object target = ClassUtil.findEntity(toBePersisted, typeToFind);
        addManyToMany(target, objectToWire);
        addManyToMany(objectToWire, target);
        return toBePersisted;
    }

    private static void addManyToMany(Object manyOwner, final Object manyElement) {
        EntityClass oneEntityClass = EntityClass.from(manyOwner.getClass());

        Iterable<Method> manyToManyGetters = oneEntityClass.getManyToManyMethods();

        Optional<Method> methodFound = Iterables.tryFind(manyToManyGetters, new Predicate<Method>() {
            @Override
            public boolean apply(Method input) {
                Class<?> genericType = TypeResolver.resolveRawArgument(input.getGenericReturnType(), Collection.class);
                return genericType.isInstance(manyElement);
            }
        });
        if (methodFound.isPresent()) {
            Collection collection = ClassUtil.invokeGetter(manyOwner, methodFound.get(), Collection.class);
            if (collection != null) {
                collection.add(manyElement);
            }
        }
    }
}

package com.github.huangp.entityunit.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import javax.persistence.EntityManager;

import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor
@Slf4j
public class FixIdCallback extends AbstractNoOpCallback
{
   private final Class<?> entityType;
   private final Serializable wantedIdValue;

   @Override
   public Iterable<Object> afterPersist(EntityManager entityManager, Iterable<Object> persisted)
   {
      Object entity = ClassUtil.findEntity(persisted, entityType);
      final Settable identityField = ClassUtil.getIdentityField(entity);
      Serializable generatedIdValue = getGeneratedId(entity, identityField);

      // TODO consider entity name mapping and id column mapping
      String tableName = entityType.getSimpleName();
      String idColumnName = identityField.getSimpleName();
      String sqlString = String.format("update %s set %s=%s where %s=%s", tableName, idColumnName, wantedIdValue, idColumnName, generatedIdValue);
      log.info("query to update generated id: {}", sqlString);

      int affectedRow = entityManager.createNativeQuery(sqlString).executeUpdate();
      log.debug("update generated id affected row: {}", affectedRow);

      // set the updated value back to entity
      entityManager.detach(entity); // otherwise it won't allow us  to modify id field
      Field idField = Iterables.find(ClassUtil.getAllDeclaredFields(entityType), new Predicate<Field>()
      {
         @Override
         public boolean apply(Field input)
         {
            return input.getName().equals(identityField.getSimpleName());
         }
      });
      idField.setAccessible(true);
      try
      {
         idField.set(entity, wantedIdValue);
      }
      catch (IllegalAccessException e)
      {
         throw Throwables.propagate(e);
      }
      // regain the entity back
      Object updated = entityManager.find(entityType, wantedIdValue);
      List<Object> toReturn = Lists.newArrayList(persisted);
      int index = Iterables.indexOf(persisted, Predicates.instanceOf(entityType));
      toReturn.set(index, updated);
      return toReturn;
   }

   private static Serializable getGeneratedId(Object entity, Settable identityField)
   {
      try
      {
         return (Serializable) identityField.getterMethod().invoke(entity);
      }
      catch (Exception e)
      {
         throw Throwables.propagate(e);
      }
   }

}

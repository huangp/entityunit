package com.github.huangp.entityunit.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import javax.persistence.EntityManager;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.Settable;
import com.github.huangp.entityunit.util.SettableField;
import com.google.common.base.Throwables;

import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor
public class FixIdCallback implements EntityMaker.Callback
{
   private final Class<?> entityType;
   private final Serializable id;

   @Override
   public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted)
   {
      Object entity = ClassUtil.findEntity(toBePersisted, entityType);
//      ClassUtil.setValue(ClassUtil.getIdentityField(entity), entity, id);
//      persist(id, entityManager, entity);
      entityManager.persist(entity);
      try
      {
         System.out.println(ClassUtil.getIdentityField(entity).getterMethod().invoke(entity));
      } catch (IllegalAccessException e)
      {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (InvocationTargetException e)
      {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      return toBePersisted;
   }

   private static <T> void persist(final Serializable id, EntityManager entityManager, T entity)
   {
      SessionImplementor session = (SessionImplementor) entityManager.getDelegate();
      EntityPersister persister = session.getEntityPersister(entity.getClass().getName(), entity);
      IdentifierProperty ip = persister.getEntityMetamodel().getIdentifierProperty();

      Settable identifierAssignedByInsert = getIdentifierPropertySettable();
      ClassUtil.setValue(identifierAssignedByInsert, ip, false);

      IdentifierValue backupUnsavedValue = setUnsavedValue(ip, IdentifierValue.ANY);

//      entityManager.getTransaction().begin();
      entityManager.persist(entity);
//      entityManager.getTransaction().commit();

      // restore the backuped unsavedValue
      setUnsavedValue(ip, backupUnsavedValue);
   }

   private static Settable getIdentifierPropertySettable()
   {
      try
      {
         return SettableField.from(IdentifierProperty.class, IdentifierProperty.class.getDeclaredField("identifierAssignedByInsert"));
      }
      catch (NoSuchFieldException e)
      {
         throw Throwables.propagate(e);
      }
   }

   private static IdentifierValue setUnsavedValue(IdentifierProperty ip, IdentifierValue newUnsavedValue)
   {
      try
      {
         IdentifierValue backup = ip.getUnsavedValue();
         Field f = ip.getClass().getDeclaredField("unsavedValue");
         f.setAccessible(true);
         f.set(ip, newUnsavedValue);
         return backup;
      }
      catch (Exception e)
      {
         throw Throwables.propagate(e);
      }
   }
}

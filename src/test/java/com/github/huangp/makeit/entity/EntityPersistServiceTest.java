package com.github.huangp.makeit.entity;

import java.util.List;
import java.util.Queue;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hamcrest.Matchers;
import com.github.huangp.entities.Category;
import com.github.huangp.entities.LineItem;
import com.github.huangp.entities.Person;
import com.github.huangp.makeit.maker.FixedValueMaker;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zanata.common.LocaleId;
import org.zanata.model.HLocale;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlow;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class EntityPersistServiceTest
{
   private static EntityManagerFactory emFactory;
   private EntityPersistService service;
   private EntityManager entityManager;
   @Mock(answer = Answers.RETURNS_DEEP_STUBS)
   private EntityManager mockEntityManager;
   private CopyCallback copyCallback;

   @BeforeClass
   public static void setupEmFactory()
   {
      emFactory = Persistence.createEntityManagerFactory("zanataTestDatasourcePU", null);
   }

   @Before
   public void setUp()
   {
      MockitoAnnotations.initMocks(this);
      service = EntityPersistServiceBuilder.builder().build();
      entityManager = emFactory.createEntityManager();
      copyCallback = new CopyCallback();
   }

   @After
   public void tearDown()
   {
      entityManager.close();
   }

   @Test
   public void canGetEntitiesInQueue()
   {
      service.makeAndPersist(mockEntityManager, HProjectIteration.class, copyCallback);

      // first element in queue should be project
      HProject hProject = copyCallback.getFromCopy(0);
      assertThat(hProject, Matchers.notNullValue());

      // second element should be iteration
      HProjectIteration hProjectIteration = copyCallback.getFromCopy(1);

      assertThat(hProjectIteration.getProject(), Matchers.sameInstance(hProject));
      assertThat(hProjectIteration.getProject().getProjectIterations(), Matchers.contains(hProjectIteration));
   }

   @Test
   public void testInReal()
   {
      HProjectIteration hProjectIteration = service.makeAndPersist(entityManager, HProjectIteration.class);

      Long numOfIteration = entityManager.createQuery("select count(*) from HProjectIteration", Long.class).getSingleResult();
      Long numOfProject = entityManager.createQuery("select count(*) from HProject", Long.class).getSingleResult();

      log.info("result {}, {}", numOfIteration, numOfProject);
      assertThat(numOfIteration, Matchers.equalTo(1L));
      assertThat(numOfProject, Matchers.equalTo(1L));
      assertThat(hProjectIteration.getId(), Matchers.notNullValue());
      assertThat(hProjectIteration.getProject().getId(), Matchers.notNullValue());

      service.deleteAll(entityManager, Lists.<Class>newArrayList(HProjectIteration.class, HProject.class));
   }

   @Test
   public void canSetPreferredValue()
   {
      service = EntityPersistServiceBuilder.builder()
            .addConstructorParameterMaker(HLocale.class, 0, new FixedValueMaker<LocaleId>(LocaleId.DE))
            .addFieldOrPropertyMaker(HLocale.class, "enabledByDefault", FixedValueMaker.ALWAYS_TRUE_MAKER)
            .addFieldOrPropertyMaker(HLocale.class, "active", FixedValueMaker.ALWAYS_TRUE_MAKER)
      .build();

      service.makeAndPersist(mockEntityManager, HLocale.class, copyCallback);
      HLocale locale = copyCallback.getFromCopy(0);

      assertThat(locale.getLocaleId(), Matchers.equalTo(LocaleId.DE)); //override default value
      assertThat(locale.isActive(), Matchers.equalTo(true)); //override default value
      assertThat(locale.isEnabledByDefault(), Matchers.equalTo(true)); //override default value
   }

   @Test
   public void canMakeUltimateObjectTreeAndDeleteAll()
   {

      service.makeAndPersist(entityManager, HTextFlow.class, copyCallback);

      HTextFlow textFlow = entityManager.createQuery("from HTextFlow", HTextFlow.class).getSingleResult();
      log.info("persisted text flow {}", textFlow);
      assertThat(textFlow.getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getProject(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getProject().getId(), Matchers.notNullValue());


      List<Object> entities = Lists.reverse(Lists.newArrayList(copyCallback.getCopy()));
      List<Class> classes = Lists.transform(entities, new Function<Object, Class>()
      {
         @Override
         public Class apply(Object input)
         {
            return input.getClass();
         }
      });
      log.debug("about to delete: {}", classes);
      service.deleteAll(entityManager, classes);

   }

   @Test
   public void testWithMixedAccessType()
   {
      service.makeAndPersist(mockEntityManager, LineItem.class, copyCallback);
      List<Object> result = copyCallback.getCopy();
      assertThat(result, Matchers.iterableWithSize(3));

      Person person = copyCallback.getFromCopy(0);
      Category category = copyCallback.getFromCopy(1);
      LineItem lineItem = copyCallback.getFromCopy(2);

      assertThat(lineItem.getCategory(), Matchers.sameInstance(category));
      assertThat(lineItem.getOwner(), Matchers.sameInstance(person));
      assertThat(category.getLineItems(), Matchers.contains(lineItem));
   }

   @Test
   public void willNotInheritContext()
   {
      service = EntityPersistServiceBuilder.builder()
            .addConstructorParameterMaker(HLocale.class, 0, new FixedValueMaker<LocaleId>(LocaleId.DE))
            .build();

      service.makeAndPersist(mockEntityManager, HLocale.class, copyCallback);

      assertThat(copyCallback.<HLocale>getFromCopy(0).getLocaleId(), Matchers.equalTo(LocaleId.DE));

      // re-create service will override previous set up
      service = EntityPersistServiceBuilder.builder()
            .addConstructorParameterMaker(HLocale.class, 0, new FixedValueMaker<LocaleId>(LocaleId.FR))
            .build();

      service.makeAndPersist(mockEntityManager, HLocale.class, copyCallback);
      assertThat(copyCallback.<HLocale>getFromCopy(0).getLocaleId(), Matchers.equalTo(LocaleId.FR));
   }



   static class CopyCallback implements EntityPersistService.Callback
   {
      @Getter
      private List<Object> copy;

      @Override
      public Queue<Object> beforePersist(Queue<Object> toBePersisted)
      {
         copy = ImmutableList.copyOf(toBePersisted);
         return toBePersisted;
      }

      <T> T getFromCopy(int index)
      {
         return (T) copy.get(index);
      }

   }
}

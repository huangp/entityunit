package com.github.huangp.makeit.entity;

import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import lombok.extern.slf4j.Slf4j;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zanata.common.LocaleId;
import org.zanata.model.HAccount;
import org.zanata.model.HAccountRole;
import org.zanata.model.HLocale;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlow;

import com.github.huangp.entities.Category;
import com.github.huangp.entities.LineItem;
import com.github.huangp.entities.Person;
import com.github.huangp.makeit.maker.FixedValueMaker;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

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
   private TakeCopyCallback copyCallback;

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
      copyCallback = new TakeCopyCallback();
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
      HProject hProject = copyCallback.getByIndex(0);
      assertThat(hProject, Matchers.notNullValue());

      // second element should be iteration
      HProjectIteration hProjectIteration = copyCallback.getByIndex(1);

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

      service.deleteAll(entityManager, Lists.<Class> newArrayList(HProjectIteration.class, HProject.class));
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
      HLocale locale = copyCallback.getByIndex(0);

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

      Category category = copyCallback.getByIndex(0);
      Person person = copyCallback.getByIndex(1);
      LineItem lineItem = copyCallback.getByIndex(2);

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

      assertThat(copyCallback.getByType(HLocale.class).getLocaleId(), Matchers.equalTo(LocaleId.DE));

      // re-create service will override previous set up
      service = EntityPersistServiceBuilder.builder()
            .addConstructorParameterMaker(HLocale.class, 0, new FixedValueMaker<LocaleId>(LocaleId.FR))
            .build();

      service.makeAndPersist(mockEntityManager, HLocale.class, copyCallback);
      assertThat(copyCallback.getByType(HLocale.class).getLocaleId(), Matchers.equalTo(LocaleId.FR));
   }

   @Test
   public void canWireManyToManyRelationship()
   {
      service = EntityPersistServiceBuilder.builder().build();

      HAccountRole hAccountRole = service.makeAndPersist(mockEntityManager, HAccountRole.class);

      HPerson hPerson = EntityPersistServiceBuilder.builder()
            .includeOptionalOneToOne()
            .build().makeAndPersist(mockEntityManager, HPerson.class, new WireManyToManyCallback(HAccount.class, hAccountRole));

      assertThat(hPerson.getAccount().getRoles(), Matchers.contains(hAccountRole));
   }
   
   @Test
   public void canDeleteWithExclusion() throws Exception
   {
      Category one = service.makeAndPersist(entityManager, Category.class);
      Category two = service.makeAndPersist(entityManager, Category.class);

      log.info("category 1: {}", one);
      log.info("category 2: {}", two);

      service.deleteAllExcept(entityManager, Lists.<Class>newArrayList(Category.class), two);

      List<Category> result = entityManager.createQuery("from Category", Category.class).getResultList();
      log.info("result: {}", result);

      assertThat(result, Matchers.hasSize(1));
      assertThat(result.get(0), Matchers.equalTo(two));
   }

//   @Test
//   public void canNotReuseId()
//   {
//      HProject one = service.makeAndPersist(entityManager, HProject.class);
//
//      log.info("1: {}", one);
//
//      service.deleteAllExcept(entityManager, Lists.<Class>newArrayList(HProject.class));
//
//      HProject two = service.makeAndPersist(entityManager, HProject.class);
//      List<HProject> result = entityManager.createQuery("from HProject", HProject.class).getResultList();
//      log.info("result: {}", result);
//      log.info("2: {}", two);
//
//      assertThat(result, Matchers.hasSize(1));
//      assertThat(result.get(0).getId(), Matchers.equalTo(2L));
//   }

}

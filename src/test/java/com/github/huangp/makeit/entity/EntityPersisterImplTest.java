package com.github.huangp.makeit.entity;

import static org.hamcrest.MatcherAssert.assertThat;

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
import org.zanata.common.ActivityType;
import org.zanata.common.ContentType;
import org.zanata.common.LocaleId;
import org.zanata.model.Activity;
import org.zanata.model.HAccount;
import org.zanata.model.HAccountRole;
import org.zanata.model.HDocument;
import org.zanata.model.HGlossaryEntry;
import org.zanata.model.HGlossaryTerm;
import org.zanata.model.HLocale;
import org.zanata.model.HLocaleMember;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;

import com.github.huangp.entities.Category;
import com.github.huangp.entities.LineItem;
import com.github.huangp.entities.Person;
import com.github.huangp.makeit.maker.FixedValueMaker;
import com.github.huangp.makeit.maker.RangeValuesMaker;
import com.google.common.collect.Lists;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class EntityPersisterImplTest
{
   private static EntityManagerFactory emFactory;
   private EntityPersister service;
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
      service = EntityPersisterBuilder.builder().build();
      entityManager = emFactory.createEntityManager();
      EntityCleaner.deleteAll(entityManager, Lists.<Class> newArrayList(
            // simple test entities
            LineItem.class, Category.class, Person.class,
            // zanata stuff
            Activity.class,
            HGlossaryEntry.class, HGlossaryTerm.class,
            HTextFlowTarget.class, HTextFlow.class, HDocument.class,
            HLocaleMember.class, HLocale.class,
            HProjectIteration.class, HProject.class,
            HPerson.class, HAccount.class
            ));
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

      EntityPersisterImplTest.log.info("result {}, {}", numOfIteration, numOfProject);
      assertThat(numOfIteration, Matchers.equalTo(1L));
      assertThat(numOfProject, Matchers.equalTo(1L));
      assertThat(hProjectIteration.getId(), Matchers.notNullValue());
      assertThat(hProjectIteration.getProject().getId(), Matchers.notNullValue());

      EntityCleaner.deleteAll(entityManager, Lists.<Class> newArrayList(HProjectIteration.class, HProject.class));
   }

   @Test
   public void canSetPreferredValue()
   {
      service = EntityPersisterBuilder.builder()
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
   public void canMakeUltimateObjectTree()
   {

      service.makeAndPersist(entityManager, HTextFlowTarget.class, copyCallback);

      HTextFlowTarget textFlowTarget = entityManager.createQuery("from HTextFlowTarget", HTextFlowTarget.class).getSingleResult();
      EntityPersisterImplTest.log.info("persisted text flow target {}", textFlowTarget);
      assertThat(textFlowTarget.getId(), Matchers.notNullValue());

      textFlowTarget.setContents("new content"); // so that it will insert a history entry
      entityManager.getTransaction().begin();
      entityManager.persist(textFlowTarget);
      entityManager.getTransaction().commit();

      assertThat(textFlowTarget.getHistory(), Matchers.hasKey(0));

      HTextFlow textFlow = textFlowTarget.getTextFlow();
      assertThat(textFlow.getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getProject(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getProject().getId(), Matchers.notNullValue());
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
      service = EntityPersisterBuilder.builder()
            .addConstructorParameterMaker(HLocale.class, 0, new FixedValueMaker<LocaleId>(LocaleId.DE))
            .build();

      service.makeAndPersist(mockEntityManager, HLocale.class, copyCallback);

      assertThat(copyCallback.getByType(HLocale.class).getLocaleId(), Matchers.equalTo(LocaleId.DE));

      // re-create service will override previous set up
      service = EntityPersisterBuilder.builder()
            .addConstructorParameterMaker(HLocale.class, 0, new FixedValueMaker<LocaleId>(LocaleId.FR))
            .build();

      service.makeAndPersist(mockEntityManager, HLocale.class, copyCallback);
      assertThat(copyCallback.getByType(HLocale.class).getLocaleId(), Matchers.equalTo(LocaleId.FR));
   }

   @Test
   public void canWireManyToManyRelationship()
   {
      service = EntityPersisterBuilder.builder().build();

      HAccountRole hAccountRole = service.makeAndPersist(mockEntityManager, HAccountRole.class);

      HPerson hPerson = EntityPersisterBuilder.builder()
            .includeOptionalOneToOne()
            .build().makeAndPersist(mockEntityManager, HPerson.class, new WireManyToManyCallback(HAccount.class, hAccountRole));

      assertThat(hPerson.getAccount().getRoles(), Matchers.contains(hAccountRole));
   }

   @Test
   public void canDeleteWithExclusion() throws Exception
   {
      Category one = service.makeAndPersist(entityManager, Category.class);
      Category two = service.makeAndPersist(entityManager, Category.class);

      EntityPersisterImplTest.log.info("category 1: {}", one);
      EntityPersisterImplTest.log.info("category 2: {}", two);

      EntityCleaner.deleteAllExcept(entityManager, Lists.<Class> newArrayList(Category.class), two);

      List<Category> result = entityManager.createQuery("from Category", Category.class).getResultList();
      EntityPersisterImplTest.log.info("result: {}", result);

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

   @Test
   public void canMakeEntityUsesInterfaceAsParameterType()
   {
      service.makeAndPersist(entityManager, HTextFlowTarget.class, copyCallback);

      EntityPersister activityPersistService = EntityPersisterBuilder.builder()
            .reuseEntities(copyCallback.getCopy())
            //   public Activity(HPerson actor, IsEntityWithType context, IsEntityWithType target, ActivityType activityType,
            .addConstructorParameterMaker(Activity.class, 1,
                  FixedValueMaker.fix(copyCallback.getByType(HProjectIteration.class)))
            .addConstructorParameterMaker(Activity.class, 2,
                  RangeValuesMaker.cycle(copyCallback.getByType(HDocument.class), copyCallback.getByType(HTextFlowTarget.class)))
            .addConstructorParameterMaker(Activity.class, 3,
                  RangeValuesMaker.cycle(ActivityType.UPLOAD_SOURCE_DOCUMENT, ActivityType.UPDATE_TRANSLATION,
                        ActivityType.UPLOAD_TRANSLATION_DOCUMENT, ActivityType.REVIEWED_TRANSLATION))
            .build();
      activityPersistService.makeAndPersist(entityManager, Activity.class);
      activityPersistService.makeAndPersist(entityManager, Activity.class);
      activityPersistService.makeAndPersist(entityManager, Activity.class);
      activityPersistService.makeAndPersist(entityManager, Activity.class);
      activityPersistService.makeAndPersist(entityManager, Activity.class);
      activityPersistService.makeAndPersist(entityManager, Activity.class);

      List<Activity> result = entityManager.createQuery("from Activity", Activity.class).getResultList();

      assertThat(result, Matchers.hasSize(6));
   }

   @Test
   public void canMakeMultipleEntitiesOfSameType()
   {
      // 3 documents
      HDocument document1 = service.makeAndPersist(entityManager, HDocument.class);
      HDocument document2 = service.makeAndPersist(entityManager, HDocument.class);
      HDocument document3 = service.makeAndPersist(entityManager, HDocument.class);

      // 4 targets
      EntityPersisterBuilder.builder()
            .reuseEntity(document1)
            .build().makeAndPersist(entityManager, HTextFlowTarget.class);
      EntityPersisterBuilder.builder()
            .reuseEntity(document2)
            .build().makeAndPersist(entityManager, HTextFlowTarget.class);
      EntityPersisterBuilder.builder()
            .reuseEntity(document3)
            .build().makeAndPersist(entityManager, HTextFlowTarget.class);
      EntityPersisterBuilder.builder()
            .reuseEntity(document1)
            .build().makeAndPersist(entityManager, HTextFlowTarget.class);

      List<HTextFlowTarget> result = entityManager.createQuery("from HTextFlowTarget order by id", HTextFlowTarget.class).getResultList();

      EntityPersisterImplTest.log.info("result {}", result);
      assertThat(result, Matchers.hasSize(4));
      assertThat(result.get(0).getTextFlow().getDocument(), Matchers.equalTo(document1));
      assertThat(result.get(1).getTextFlow().getDocument(), Matchers.equalTo(document2));
      assertThat(result.get(2).getTextFlow().getDocument(), Matchers.equalTo(document3));
      assertThat(result.get(3).getTextFlow().getDocument(), Matchers.equalTo(document1));
   }

   @Test
   public void canSetIndexColumn()
   {
      service.makeAndPersist(entityManager, LineItem.class);
      service.makeAndPersist(entityManager, LineItem.class);

      entityManager.clear();
      List<LineItem> result = entityManager.createQuery("from LineItem it order by it.number", LineItem.class).getResultList();

      assertThat(result, Matchers.hasSize(2));
      EntityPersisterImplTest.log.info("result {}", result);
      assertThat(result.get(0).getNumber(), Matchers.equalTo(0));
      assertThat(result.get(1).getNumber(), Matchers.equalTo(1));
   }

   @Test
   public void testFixNameAndSlug() 
   {
      EntityPersister service = EntityPersisterBuilder.builder()
            // project
            .addFieldOrPropertyMaker(HProject.class, "slug", FixedValueMaker.fix("about-fedora"))
            .addFieldOrPropertyMaker(HProject.class, "name", FixedValueMaker.fix("about fedora"))
            // iteration
            .addFieldOrPropertyMaker(HProjectIteration.class, "slug", FixedValueMaker.fix("master"))
            // document
            // public HDocument(String docId, String name, String path, ContentType contentType, HLocale locale)
            .addConstructorParameterMaker(HDocument.class, 0, FixedValueMaker.fix("About_Fedora"))
            .addConstructorParameterMaker(HDocument.class, 1, FixedValueMaker.fix("About_Fedora"))
            .addConstructorParameterMaker(HDocument.class, 2, FixedValueMaker.EMPTY_STRING_MAKER)
            .addConstructorParameterMaker(HDocument.class, 3, FixedValueMaker.fix(ContentType.PO))
            .build();

      HDocument result = service.makeAndPersist(entityManager, HDocument.class, copyCallback);

      assertThat(result.getDocId(), Matchers.equalTo("About_Fedora"));
      assertThat(result.getName(), Matchers.equalTo("About_Fedora"));
      assertThat(result.getPath(), Matchers.equalTo(""));
      assertThat(result.getContentType(), Matchers.equalTo(ContentType.PO));
      assertThat(result.getProjectIteration().getSlug(), Matchers.equalTo("master"));
      assertThat(result.getProjectIteration().getProject().getSlug(), Matchers.equalTo("about-fedora"));
      assertThat(result.getProjectIteration().getProject().getName(), Matchers.equalTo("about fedora"));
   }

}

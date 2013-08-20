package org.huangp.makeit.entity;

import java.util.List;
import java.util.Queue;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hamcrest.Matchers;
import org.huangp.entities.Category;
import org.huangp.entities.LineItem;
import org.huangp.entities.Person;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlow;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

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

   @BeforeClass
   public static void setupEmFactory()
   {
      emFactory = Persistence.createEntityManagerFactory("zanataTestDatasourcePU", null);
   }

   @Before
   public void setUp()
   {
      service = new EntityPersistServiceImpl();
      entityManager = emFactory.createEntityManager();
   }

   @After
   public void tearDown()
   {
      entityManager.close();
   }

   @Test
   public void canGetEntitiesInQueue()
   {
      Queue<Object> result = service.getRequiredEntitiesFor(HProjectIteration.class);

      // first element in queue should be project
      assertThat(result.peek(), Matchers.notNullValue());
      assertThat(result.peek(), Matchers.instanceOf(HProject.class));
      HProject hProject = (HProject) result.poll();

      // second element should be iteration
      assertThat(result.peek(), Matchers.instanceOf(HProjectIteration.class));
      HProjectIteration hProjectIteration = (HProjectIteration) result.poll();

      assertThat(hProjectIteration.getProject(), Matchers.sameInstance(hProject));
      assertThat(hProjectIteration.getProject().getProjectIterations(), Matchers.contains(hProjectIteration));
   }

   @Test
   public void testInReal()
   {
      Queue<Object> queue = service.getRequiredEntitiesFor(HProjectIteration.class);

      HProject project = (HProject) queue.peek();
      assertThat(project.getId(), Matchers.nullValue());
      assertThat(project.getProjectIterations().get(0).getId(), Matchers.nullValue());
      log.info("project {}", project);

      service.persistInOrder(entityManager, queue);

      Long numOfIteration = entityManager.createQuery("select count(*) from HProjectIteration", Long.class).getSingleResult();
      Long numOfProject = entityManager.createQuery("select count(*) from HProject", Long.class).getSingleResult();

      log.info("result {}, {}", numOfIteration, numOfProject);
      assertThat(project.getId(), Matchers.notNullValue());
      assertThat(project.getProjectIterations().get(0).getId(), Matchers.notNullValue());
   }

   @Test
   public void canMakeUltimateObjectTreeAndDeleteAll()
   {
      Queue<Object> queue = service.getRequiredEntitiesFor(HTextFlow.class);

      service.persistInOrder(entityManager, queue);

      HTextFlow textFlow = entityManager.createQuery("from HTextFlow", HTextFlow.class).getSingleResult();
      log.info("persisted text flow {}", textFlow);
      assertThat(textFlow.getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getId(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getProject(), Matchers.notNullValue());
      assertThat(textFlow.getDocument().getProjectIteration().getProject().getId(), Matchers.notNullValue());

      List<Object> entities = Lists.reverse(Lists.newArrayList(queue));
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
      Queue<Object> queue = service.getRequiredEntitiesFor(LineItem.class);

      assertThat(queue, Matchers.iterableWithSize(3));

      Person person = (Person) queue.poll();
      Category category = (Category) queue.poll();
      LineItem lineItem = (LineItem) queue.poll();

      assertThat(lineItem.getCategory(), Matchers.sameInstance(category));
      assertThat(lineItem.getOwner(), Matchers.sameInstance(person));
      assertThat(category.getLineItems(), Matchers.contains(lineItem));

   }
}

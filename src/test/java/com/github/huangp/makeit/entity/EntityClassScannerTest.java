package com.github.huangp.makeit.entity;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.zanata.model.HAccount;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class EntityClassScannerTest
{
   private EntityClassScanner scanner;

   @Before
   public void setUp() throws Exception
   {
      scanner = new EntityClassScanner();
   }

   @Test
   public void testScan() throws Exception
   {
      Iterable<EntityClass> dependents = scanner.scan(HTextFlowTarget.class);

      List<EntityClass> result = Lists.newArrayList(dependents);

      printNice(result);
      assertThat(result, Matchers.hasSize(10));
      List<Class> types = Lists.transform(result, new Function<EntityClass, Class>()
      {
         @Override
         public Class apply(EntityClass input)
         {
            return input.getType();
         }
      });
      assertThat(types, Matchers.<Class> contains(
            HProject.class,
            HPerson.class,
            HLocale.class,
            HProjectIteration.class,
            HDocument.class,
            HPerson.class,
            HLocale.class,
            HPerson.class,
            HTextFlow.class,
            HPerson.class));
   }

   private void printNice(List<EntityClass> result)
   {
      log.info("============ result ==============");
      for (EntityClass entityClass : result)
      {
         log.info("{}", entityClass);
      }
      log.info("============ result ==============");
   }


   @Test
   public void canScanOptionalOneToOne()
   {
      scanner = new EntityClassScanner(ScanOption.IncludeOneToOne);

      Iterable<EntityClass> dependents = scanner.scan(HPerson.class);

      List<EntityClass> result = Lists.newArrayList(dependents);

      printNice(result);
      assertThat(result, Matchers.hasSize(1));
      assertThat(result.get(0).getType(), Matchers.<Class>equalTo(HAccount.class));
   }

   @Test
   public void scanResultIsCached()
   {
      // scan twice
      Iterable<EntityClass> dependents = scanner.scan(HTextFlowTarget.class);
      scanner.scan(HTextFlowTarget.class);

      List<EntityClass> result = Lists.newArrayList(dependents);

      assertThat(result, Matchers.hasSize(10));
   }
}

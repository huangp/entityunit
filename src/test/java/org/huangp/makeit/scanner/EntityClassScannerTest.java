package org.huangp.makeit.scanner;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
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

      log.info("result: {}", result);
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
            HPerson.class,
            HProject.class,
            HProjectIteration.class,
            HLocale.class,
            HPerson.class,
            HDocument.class,
            HTextFlow.class,
            HPerson.class,
            HLocale.class,
            HPerson.class));
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

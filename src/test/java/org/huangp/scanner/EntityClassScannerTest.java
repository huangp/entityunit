package org.huangp.scanner;

import java.util.List;

import org.hamcrest.Matchers;
import org.huangp.scanner.EntityClass;
import org.huangp.scanner.EntityClassScanner;
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

import lombok.extern.slf4j.Slf4j;
import static org.hamcrest.MatcherAssert.*;

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

      EntityClassScannerTest.log.info("result: {}", result);
      assertThat(result, Matchers.hasSize(10));
      List<Class> types = Lists.transform(result, new Function<EntityClass, Class>()
      {
         @Override
         public Class apply(EntityClass input)
         {
            return input.getType();
         }
      });
      assertThat(types, Matchers.<Class>contains(
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
}

package org.huangp.maker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.huangp.maker.ScalarValueMakerFactory.FACTORY;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.huangp.holder.BeanValueHolderImpl;
import org.huangp.util.Settable;
import org.junit.Before;
import org.junit.Test;
import org.zanata.common.ProjectType;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.StatusCount;
import com.google.common.base.Optional;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class ScalarValueMakerFactoryTest
{

   private Optional<Settable> settableOptional = Optional.absent();

   @Before
   public void setUp() throws Exception
   {
      BeanValueHolderImpl.HOLDER.clear();
   }

   @Test
   public void canGetNumberMaker()
   {
      assertThat(FACTORY.from(Integer.class, settableOptional), Matchers.instanceOf(NumberMaker.class));
      assertThat(FACTORY.from(Long.class, settableOptional), Matchers.instanceOf(NumberMaker.class));
      assertThat(FACTORY.from(Short.class, settableOptional), Matchers.instanceOf(NumberMaker.class));
   }

   @Test
   public void canGetBeanMaker()
   {
      assertThat(FACTORY.from(StatusCount.class, settableOptional), Matchers.instanceOf(BeanMaker.class));
   }

   @Test
   public void reuseEntityOrNull()
   {
      assertThat(FACTORY.from(HCopyTransOptions.class, settableOptional).value(), Matchers.nullValue());

      // if holder has value it will return
      HCopyTransOptions bean = new HCopyTransOptions();
      BeanValueHolderImpl.HOLDER.putIfNotNull(HCopyTransOptions.class, bean);
      assertThat(FACTORY.from(HCopyTransOptions.class, settableOptional).value(), Matchers.<Object>sameInstance(bean));
   }

   @Test
   public void canGetNullMakerForArrayAndCollection()
   {
      assertThat(FACTORY.from(new Integer[] {}.getClass(), settableOptional), Matchers.instanceOf(NullMaker.class));
      assertThat(FACTORY.from(Collection.class, settableOptional), Matchers.instanceOf(NullMaker.class));
      assertThat(FACTORY.from(List.class, settableOptional), Matchers.instanceOf(NullMaker.class));
      assertThat(FACTORY.from(Map.class, settableOptional), Matchers.instanceOf(NullMaker.class));
   }

   @Test
   public void canGetEnumMaker()
   {
      assertThat(FACTORY.from(ProjectType.class, settableOptional), Matchers.instanceOf(EnumMaker.class));
   }
}

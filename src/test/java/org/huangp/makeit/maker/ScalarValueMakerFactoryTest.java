package org.huangp.makeit.maker;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.huangp.entities.Person;
import org.huangp.makeit.holder.BeanValueHolderImpl;
import org.huangp.makeit.util.Settable;
import org.huangp.makeit.util.SettableProperty;
import org.junit.Before;
import org.junit.Test;
import org.zanata.common.ProjectType;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.StatusCount;
import com.google.common.base.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.huangp.makeit.maker.ScalarValueMakerFactory.FACTORY;

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

   @Test
   public void canRegisterPreferredValueMakerAndUseIt() throws Exception
   {
       PreferredValueMakersRegistry.registry().add("org.huangp.entities.Person#name", new FixedValueMaker<Object>("admin"));

      Maker<String> maker = FACTORY.from(SettableProperty.from(Person.class, Person.class.getMethod("getName")));

      String name = maker.value();
      assertThat(name, Matchers.equalTo("admin"));
   }
}

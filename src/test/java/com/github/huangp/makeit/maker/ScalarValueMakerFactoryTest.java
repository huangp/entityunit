package com.github.huangp.makeit.maker;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import com.github.huangp.entities.Person;
import com.github.huangp.makeit.entity.MakeContext;
import com.github.huangp.makeit.holder.BeanValueHolder;
import com.github.huangp.makeit.util.Settable;
import com.github.huangp.makeit.util.SettableParameter;
import com.github.huangp.makeit.util.SettableProperty;
import org.junit.Before;
import org.junit.Test;
import org.zanata.common.ProjectType;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HLocale;
import org.zanata.model.StatusCount;
import com.google.common.base.Optional;
import com.google.common.reflect.Invokable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang
 */
@Slf4j
public class ScalarValueMakerFactoryTest
{

   private Optional<Settable> settableOptional = Optional.absent();
   private ScalarValueMakerFactory factory;
   private BeanValueHolder holder;
   private PreferredValueMakersRegistry registry;

   @Before
   public void setUp() throws Exception
   {

      holder = new BeanValueHolder();
      registry = new PreferredValueMakersRegistry();
      MakeContext context = new MakeContext(holder, registry);
      factory = new ScalarValueMakerFactory(context);
   }

   @Test
   public void canGetNumberMaker()
   {
      assertThat(factory.from(Integer.class, settableOptional), Matchers.instanceOf(NumberMaker.class));
      assertThat(factory.from(Long.class, settableOptional), Matchers.instanceOf(NumberMaker.class));
      assertThat(factory.from(Short.class, settableOptional), Matchers.instanceOf(NumberMaker.class));
   }

   @Test
   public void canGetBeanMaker()
   {
      assertThat(factory.from(StatusCount.class, settableOptional), Matchers.instanceOf(BeanMaker.class));
   }

   @Test
   public void reuseEntityOrNull()
   {
      assertThat(factory.from(HCopyTransOptions.class, settableOptional).value(), Matchers.nullValue());

      // if holder has value it will return
      HCopyTransOptions bean = new HCopyTransOptions();
      holder.putIfNotNull(HCopyTransOptions.class, bean);
      assertThat(factory.from(HCopyTransOptions.class, settableOptional).value(), Matchers.<Object>sameInstance(bean));
   }

   @Test
   public void canGetNullMakerForArrayAndCollection()
   {
      assertThat(factory.from(new Integer[]{}.getClass(), settableOptional), Matchers.instanceOf(NullMaker.class));
      assertThat(factory.from(Collection.class, settableOptional), Matchers.instanceOf(NullMaker.class));
      assertThat(factory.from(List.class, settableOptional), Matchers.instanceOf(NullMaker.class));
      assertThat(factory.from(Map.class, settableOptional), Matchers.instanceOf(NullMaker.class));
   }

   @Test
   public void canGetEnumMaker()
   {
      assertThat(factory.from(ProjectType.class, settableOptional), Matchers.instanceOf(EnumMaker.class));
   }

   @Test
   public void canRegisterPreferredValueMakerAndUseIt() throws Exception
   {
      registry.add(Matchers.equalTo("com.github.huangp.entities.Person - name"), new FixedValueMaker<String>("admin"));
      Maker<String> maker = factory.from(SettableProperty.from(Person.class, Person.class.getMethod("getName")));

      String name = maker.value();
      assertThat(name, Matchers.equalTo("admin"));
   }

   @Test
   public void preferredValueMakerWorksOnConstructor() throws NoSuchMethodException
   {
      registry.addConstructorParameterMaker(TestClass.class, 0, new FixedValueMaker<String>("constructor"));
      Settable settableParam = SettableParameter.from(TestClass.class,
            Invokable.from(TestClass.class.getConstructor(String.class)).getParameters().get(0));

      log.debug("settable parameter: {}", settableParam.fullyQualifiedName());
      Maker<String> maker = factory.from(settableParam);

      String name = maker.value();
      assertThat(name, Matchers.equalTo("constructor"));
   }

   @Test
   public void primitiveTypeCanBeSet() throws NoSuchMethodException
   {

      registry.add(Matchers.containsString("enabledByDefault"), new FixedValueMaker<Boolean>(true));

      Maker<Boolean> enabledByDefaultMaker = factory.from(SettableProperty.from(HLocale.class, HLocale.class.getMethod("isEnabledByDefault")));
      Maker<Boolean> activeMaker = factory.from(SettableProperty.from(HLocale.class, HLocale.class.getMethod("isActive")));

      assertThat(enabledByDefaultMaker.value(), Matchers.is(true));
      assertThat(activeMaker.value(), Matchers.is(false));
   }

   @RequiredArgsConstructor
   private static class TestClass
   {
      private final String name;
   }
}

package com.github.huangp.entityunit.maker;

import com.github.huangp.entities.Person;
import com.github.huangp.entityunit.entity.MakeContext;
import com.github.huangp.entityunit.holder.BeanValueHolder;
import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.Settable;
import com.github.huangp.entityunit.util.SettableProperty;
import lombok.Delegate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.zanata.common.ProjectType;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HLocale;
import org.zanata.model.StatusCount;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang
 */
@Slf4j
public class ScalarValueMakerFactoryTest {
    private ScalarValueMakerFactory factory;
    private BeanValueHolder holder;
    private PreferredValueMakersRegistry registry;

    @Before
    public void setUp() throws Exception {

        holder = new BeanValueHolder();
        registry = new PreferredValueMakersRegistry();
        MakeContext context = new MakeContext(holder, registry);
        factory = new ScalarValueMakerFactory(context);
    }

    @Test
    public void canGetNumberMaker() {
        assertThat(factory.from(new FakeSettable(Integer.class)), Matchers.instanceOf(NumberMaker.class));
        assertThat(factory.from(new FakeSettable(Long.class)), Matchers.instanceOf(NumberMaker.class));
        assertThat(factory.from(new FakeSettable(Short.class)), Matchers.instanceOf(NumberMaker.class));
    }

    @Test
    public void canGetBeanMaker() {
        assertThat(factory.from(new FakeSettable(StatusCount.class)), Matchers.instanceOf(BeanMaker.class));
    }

    @Test
    public void reuseEntityOrNull() {
        assertThat(factory.from(new FakeSettable(HCopyTransOptions.class)).value(), Matchers.nullValue());

        // if holder has value it will return
        HCopyTransOptions bean = new HCopyTransOptions();
        holder.putIfNotNull(HCopyTransOptions.class, bean);
        assertThat(factory.from(new FakeSettable(HCopyTransOptions.class)).value(), Matchers.<Object>sameInstance(bean));
    }

    @Test
    public void canGetNullMakerForArrayAndCollection() {
        assertThat(factory.from(new FakeSettable(new Integer[]{}.getClass())), Matchers.instanceOf(NullMaker.class));
        assertThat(factory.from(new FakeSettable(Collection.class)), Matchers.instanceOf(NullMaker.class));
        assertThat(factory.from(new FakeSettable(List.class)), Matchers.instanceOf(NullMaker.class));
        assertThat(factory.from(new FakeSettable(Map.class)), Matchers.instanceOf(NullMaker.class));
    }

    @Test
    public void canGetEnumMaker() {
        assertThat(factory.from(new FakeSettable(ProjectType.class)), Matchers.instanceOf(EnumMaker.class));
    }

    @Test
    public void canRegisterPreferredValueMakerAndUseIt() throws Exception {
        registry.add(Matchers.equalTo("com.github.huangp.entities.Person - name"), FixedValueMaker.fix("admin"));
        Maker<String> maker = factory.from(SettableProperty.from(Person.class, new PropertyDescriptor("name", Person.class)));

        String name = maker.value();
        assertThat(name, Matchers.equalTo("admin"));
    }

    @Test
    public void preferredValueMakerWorksOnConstructor() throws NoSuchMethodException {
        registry.addConstructorParameterMaker(TestClass.class, 0, FixedValueMaker.fix("constructor"));
        Constructor<TestClass> constructor = TestClass.class.getConstructor(String.class);
        Settable settableParam = ClassUtil.getConstructorParameters(constructor, TestClass.class).get(0);

        log.debug("settable parameter: {}", settableParam.fullyQualifiedName());
        Maker<String> maker = factory.from(settableParam);

        String name = maker.value();
        assertThat(name, Matchers.equalTo("constructor"));
    }

    @Test
    public void primitiveTypeCanBeSet() throws NoSuchMethodException, IntrospectionException {

        registry.add(Matchers.containsString("enabledByDefault"), FixedValueMaker.ALWAYS_TRUE_MAKER);

        Maker<Boolean> enabledByDefaultMaker = factory.from(SettableProperty.from(HLocale.class, new PropertyDescriptor("enabledByDefault", HLocale.class)));
        Maker<Boolean> activeMaker = factory.from(SettableProperty.from(HLocale.class, new PropertyDescriptor("active", HLocale.class)));

        assertThat(enabledByDefaultMaker.value(), Matchers.is(true));
        assertThat(activeMaker.value(), Matchers.is(false));
    }

    @RequiredArgsConstructor
    private static class TestClass {
        private final String name;
    }

    @RequiredArgsConstructor
    private static class FakeSettable implements Settable {
        @Delegate
        private final Class delegate;


        @Override
        public Type getType() {
            return delegate;
        }

        @Override
        public String fullyQualifiedName() {
            return delegate + ".null";
        }

        @Override
        public <T> T valueIn(Object ownerInstance) {
            return null;
        }
    }
}

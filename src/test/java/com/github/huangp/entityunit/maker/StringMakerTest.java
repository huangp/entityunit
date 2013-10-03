package com.github.huangp.entityunit.maker;

import com.github.huangp.entities.Person;
import com.github.huangp.entityunit.util.Settable;
import com.github.huangp.entityunit.util.SettableField;
import com.github.huangp.entityunit.util.SettableProperty;
import com.google.common.base.Optional;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.zanata.model.HAccount;
import org.zanata.model.HPerson;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang
 */
public class StringMakerTest {

    @Test
    public void canMakeRandomString() throws IntrospectionException {
        StringMaker maker = StringMaker.from(SettableProperty.from(Person.class, new PropertyDescriptor("name", Person.class)));

        String value = maker.value();

        assertThat(value, Matchers.notNullValue());
        assertThat(value.length(), Matchers.equalTo(StringMaker.DEFAULT_MAX));
    }

    @Test
    public void canMakeStringWithEmailConstraint() throws NoSuchMethodException, IntrospectionException {

        StringMaker maker = StringMaker.from(SettableProperty.from(HPerson.class, new PropertyDescriptor("email", HPerson.class)));

        String value = maker.value();

        assertThat(value, Matchers.endsWith("@nowhere.org"));
    }

    @Test
    public void canMakeStringWithSizeLimit() throws NoSuchMethodException, IntrospectionException {
        StringMaker maker = StringMaker.from(SettableProperty.from(HAccount.class, new PropertyDescriptor("apiKey", HAccount.class)));

        String value = maker.value();

        assertThat(value.length(), Matchers.equalTo(32));
    }
}

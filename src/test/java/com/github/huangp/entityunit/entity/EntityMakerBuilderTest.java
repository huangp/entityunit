package com.github.huangp.entityunit.entity;

import com.github.huangp.entities.Category;
import com.github.huangp.entities.LineItem;
import com.github.huangp.entityunit.holder.BeanValueHolder;
import com.github.huangp.entityunit.maker.FixedValueMaker;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zanata.common.LocaleId;
import org.zanata.model.HLocale;
import org.zanata.model.HTextFlow;

import javax.persistence.EntityManager;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang
 */
public class EntityMakerBuilderTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityManager em;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void canReuseEntity() {
        Category existEntity = new Category();
        EntityMaker service = EntityMakerBuilder.builder()
                .reuseEntity(existEntity)
                .build();

        LineItem result = service.makeAndPersist(em, LineItem.class);

        assertThat(result.getCategory(), Matchers.sameInstance(existEntity));
    }

    @Test
    public void canReuseMakeContext() {

        EntityMaker service = EntityMakerBuilder.builder()
                .includeOptionalOneToOne()
                .addConstructorParameterMaker(HLocale.class, 0, FixedValueMaker.fix(LocaleId.DE))
                .build();

        // make something
        HLocale hLocale = service.makeAndPersist(em, HLocale.class);
        assertThat(hLocale.getLocaleId(), Matchers.equalTo(LocaleId.DE));
        BeanValueHolder beans = service.exportCopyOfBeans();

        service = EntityMakerBuilder.builder()
                .reuseObjects(beans)
                .build();

        // make another
        HTextFlow hTextFlow = service.makeAndPersist(em, HTextFlow.class);

        assertThat(hTextFlow.getLocale(), Matchers.equalTo(LocaleId.DE));
        assertThat(hTextFlow.getDocument().getLocale(), Matchers.sameInstance(hLocale));

    }

}

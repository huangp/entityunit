package com.github.huangp.makeit.entity;

import javax.persistence.EntityManager;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zanata.common.LocaleId;
import org.zanata.model.HLocale;
import org.zanata.model.HTextFlow;
import com.github.huangp.entities.Category;
import com.github.huangp.entities.LineItem;
import com.github.huangp.makeit.holder.BeanValueHolder;
import com.github.huangp.makeit.maker.FixedValueMaker;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class EntityPersistServiceBuilderTest
{
   @Mock(answer = Answers.RETURNS_DEEP_STUBS)
   private EntityManager em;

   @Before
   public void setUp() throws Exception
   {
      MockitoAnnotations.initMocks(this);
   }

   @Test
   public void canReuseEntity()
   {
      Category existEntity = new Category();
      EntityPersistService service = EntityPersistServiceBuilder.builder()
            .reuseEntities(existEntity)
            .build();

      LineItem result = service.makeAndPersist(em, LineItem.class);

      assertThat(result.getCategory(), Matchers.sameInstance(existEntity));
   }

   @Test
   public void canReuseMakeContext()
   {

      EntityPersistService service = EntityPersistServiceBuilder.builder()
            .includeOptionalOneToOne()
            .addConstructorParameterMaker(HLocale.class, 0, new FixedValueMaker<LocaleId>(LocaleId.DE))
            .build();

      // make something
      HLocale hLocale = service.makeAndPersist(em, HLocale.class);
      assertThat(hLocale.getLocaleId(), Matchers.equalTo(LocaleId.DE));
      BeanValueHolder beans = service.exportCopyOfBeans();

      service = EntityPersistServiceBuilder.builder()
            .reuseObjects(beans)
            .build();

      // make another
      HTextFlow hTextFlow = service.makeAndPersist(em, HTextFlow.class);

      assertThat(hTextFlow.getLocale(), Matchers.equalTo(LocaleId.DE));
      assertThat(hTextFlow.getDocument().getLocale(), Matchers.sameInstance(hLocale));

   }

}

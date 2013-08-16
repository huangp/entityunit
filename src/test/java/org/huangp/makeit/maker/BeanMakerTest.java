package org.huangp.makeit.maker;

import javax.persistence.Entity;
import javax.persistence.Id;

import static org.hamcrest.MatcherAssert.assertThat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hamcrest.Matchers;
import org.hibernate.validator.constraints.Email;
import org.huangp.beans.Child;
import org.huangp.beans.Language;
import org.huangp.beans.Toy;
import org.huangp.makeit.holder.BeanValueHolder;
import org.huangp.makeit.holder.BeanValueHolderImpl;
import org.junit.Before;
import org.junit.Test;
import org.zanata.common.ContentType;
import org.zanata.common.EntityStatus;
import org.zanata.common.LocaleId;
import org.zanata.common.ProjectType;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HLocale;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class BeanMakerTest
{

   private BeanValueHolder holder;

   @Before
   public void setUp()
   {
      holder = new BeanValueHolderImpl();
   }

   @Test
   public void canMakeBean()
   {
      BeanMaker<HLocale> maker = new BeanMaker<HLocale>(HLocale.class, holder);

      HLocale locale = maker.value();
      log.info("result {}", locale);

      assertThat(locale.getId(), Matchers.nullValue()); // skip read only properties
      assertThat(locale.getVersionNum(), Matchers.notNullValue());
      assertThat(locale.getCreationDate(), Matchers.notNullValue());
      assertThat(locale.getCreationDate(), Matchers.notNullValue());
      assertThat(locale.getLocaleId(), Matchers.equalTo(LocaleId.EN));

   }

   @Test
   public void canMakeEntity()
   {
      BeanMaker<HProject> maker = new BeanMaker<HProject>(HProject.class, holder);

      HProject hProject = maker.value();

      log.info("result {}", hProject);

      assertThat(hProject.getId(), Matchers.nullValue()); // skip read only properties
      assertThat(hProject.getDescription(), Matchers.notNullValue());
      assertThat(hProject.getHomeContent(), Matchers.notNullValue());
      assertThat(hProject.getName(), Matchers.notNullValue());
      assertThat(hProject.getVersionNum(), Matchers.notNullValue());
      assertThat(hProject.getSourceCheckoutURL(), Matchers.notNullValue());
      assertThat(hProject.getCreationDate(), Matchers.notNullValue());
      assertThat(hProject.getStatus(), Matchers.equalTo(EntityStatus.ACTIVE)); // won't touch default value
      assertThat(hProject.getCreationDate(), Matchers.notNullValue());

   }

   @Test
   public void willNotTouchDefaultValue()
   {
      BeanMaker<HCopyTransOptions> maker = new BeanMaker<HCopyTransOptions>(HCopyTransOptions.class, holder);

      HCopyTransOptions options = maker.value();

      assertThat(options.getContextMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
      assertThat(options.getDocIdMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
      assertThat(options.getProjectMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
   }

   @Test
   public void canMakeBeanWithNestedBean()
   {

      BeanMaker<Child> maker = new BeanMaker<Child>(Child.class, holder);

      Child child = maker.value();
      log.info("result {}", child);

      assertThat(child.getId(), Matchers.nullValue()); // skip read only properties
      assertThat(child.getBestField(), Matchers.nullValue()); // skip field that is the same type of self
      assertThat(child.getParent(), Matchers.notNullValue()); // create transitive bean
      assertThat(child.getSpeaks(), Matchers.equalTo(Language.English)); // first enum value
      assertThat(child.getJob(), Matchers.equalTo("play")); // won't touch default value
      assertThat(child.getAge(), Matchers.notNullValue());
      assertThat(child.getDateOfBirth(), Matchers.notNullValue());

   }

   @Test
   public void willNotMakeNestedEntity()
   {

      BeanMaker<HProjectIteration> maker = new BeanMaker<HProjectIteration>(HProjectIteration.class, holder);

      HProjectIteration hIteration = maker.value();
      log.info("result {}", hIteration);

      assertThat(hIteration.getId(), Matchers.nullValue()); // skip read only properties
      assertThat(hIteration.getParent(), Matchers.nullValue()); // skip field that is the same type of self
      assertThat(hIteration.getProject(), Matchers.nullValue()); // transitive bean will be ignored
      assertThat(hIteration.getProjectType(), Matchers.equalTo(ProjectType.Utf8Properties)); // first enum value
      assertThat(hIteration.getSlug(), Matchers.notNullValue());
      assertThat(hIteration.getStatus(), Matchers.equalTo(EntityStatus.ACTIVE)); // won't touch default value
      assertThat(hIteration.getCreationDate(), Matchers.notNullValue());

   }

   @Test
   public void useInstanceConstantsWhenPossible()
   {
      BeanMaker<ContentType> maker = new BeanMaker<ContentType>(ContentType.class, holder);

      ContentType contentType = maker.value();

      assertThat(contentType, Matchers.sameInstance(ContentType.TextPlain));
   }

   @Test
   public void willPreferArgsConstructorIfExists()
   {
      BeanMaker<Toy> maker = new BeanMaker<Toy>(Toy.class, holder);

      Toy toy = maker.value();
      log.debug("toy: {}", toy);

      assertThat(toy.getOwner(), Matchers.notNullValue());
      assertThat(toy.getOwnerName(), Matchers.equalTo(toy.getOwner().getName()));
   }

   @Test
   public void constructorParameterWillFollowConstraint()
   {
      BeanMaker<TestEntity> maker = new BeanMaker<TestEntity>(TestEntity.class, holder);
      TestEntity testEntity = maker.value();

      assertThat(testEntity.email, Matchers.endsWith("@nowhere.org"));
   }

   @Entity
   @NoArgsConstructor
   @Data
   private static class TestEntity
   {
      @Id
      private Long id;
      private String email;

      private TestEntity(@Email String email)
      {
         this.email = email;
      }
   }
}

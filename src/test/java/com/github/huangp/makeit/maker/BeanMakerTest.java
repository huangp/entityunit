package com.github.huangp.makeit.maker;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hamcrest.Matchers;
import org.hibernate.validator.constraints.Email;
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
import com.github.huangp.beans.Child;
import com.github.huangp.beans.Language;
import com.github.huangp.beans.Toy;
import com.github.huangp.makeit.entity.MakeContext;
import com.github.huangp.makeit.holder.BeanValueHolder;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang
 */
@Slf4j
public class BeanMakerTest
{

   private MakeContext context;

   @Before
   public void setUp()
   {
      BeanValueHolder holder = new BeanValueHolder();
      PreferredValueMakersRegistry registry = new PreferredValueMakersRegistry();
      context = new MakeContext(holder, registry);
   }

   @Test
   public void canMakeBean()
   {
      BeanMaker<HLocale> maker = new BeanMaker<HLocale>(HLocale.class, context);

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
      BeanMaker<HProject> maker = new BeanMaker<HProject>(HProject.class, context);

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
      BeanMaker<HCopyTransOptions> maker = new BeanMaker<HCopyTransOptions>(HCopyTransOptions.class, context);

      HCopyTransOptions options = maker.value();

      assertThat(options.getContextMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
      assertThat(options.getDocIdMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
      assertThat(options.getProjectMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
   }

   @Test
   public void canMakeBeanWithNestedBean()
   {

      BeanMaker<Child> maker = new BeanMaker<Child>(Child.class, context);

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

      BeanMaker<HProjectIteration> maker = new BeanMaker<HProjectIteration>(HProjectIteration.class, context);

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
      BeanMaker<ContentType> maker = new BeanMaker<ContentType>(ContentType.class, context);

      ContentType contentType = maker.value();

      assertThat(contentType, Matchers.sameInstance(ContentType.TextPlain));
   }

   @Test
   public void willPreferArgsConstructorIfExists()
   {
      BeanMaker<Toy> maker = new BeanMaker<Toy>(Toy.class, context);

      Toy toy = maker.value();
      log.debug("toy: {}", toy);

      assertThat(toy.getOwner(), Matchers.notNullValue());
      assertThat(toy.getOwnerName(), Matchers.equalTo(toy.getOwner().getName()));
   }

   @Test
   public void constructorParameterWillFollowConstraint()
   {
      BeanMaker<TestEntity> maker = new BeanMaker<TestEntity>(TestEntity.class, context);
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

package org.huangp.maker;

import javax.persistence.Entity;
import javax.persistence.Id;

import static org.hamcrest.MatcherAssert.assertThat;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hamcrest.Matchers;
import org.hibernate.validator.constraints.Email;
import org.huangp.holder.BeanValueHolderImpl;
import org.junit.Before;
import org.junit.Test;
import org.zanata.common.ContentType;
import org.zanata.common.EntityStatus;
import org.zanata.common.ProjectType;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowHistory;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class SingleEntityMakerTest
{

   @Before
   public void setUp()
   {
      BeanValueHolderImpl.HOLDER.clear();
   }

   @Test
   public void canMakeEntity()
   {
      SingleEntityMaker<HProject> maker = new SingleEntityMaker<HProject>(HProject.class);

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
      SingleEntityMaker<HCopyTransOptions> maker = new SingleEntityMaker<HCopyTransOptions>(HCopyTransOptions.class);

      HCopyTransOptions options = maker.value();

      assertThat(options.getContextMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
      assertThat(options.getDocIdMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
      assertThat(options.getProjectMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
   }

   @Test
   public void willNotMakeNestedEntity()
   {

      SingleEntityMaker<HProjectIteration> maker = new SingleEntityMaker<HProjectIteration>(HProjectIteration.class);

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
      SingleEntityMaker<ContentType> maker = new SingleEntityMaker<ContentType>(ContentType.class);

      ContentType contentType = maker.value();

      assertThat(contentType, Matchers.sameInstance(ContentType.TextPlain));
   }

   @Test
   public void willPreferArgsConstructorIfExists()
   {
      // text flow that will be used as constructor parameter
      SingleEntityMaker<HTextFlow> textFlowEntityMaker = new SingleEntityMaker<HTextFlow>(HTextFlow.class);
      textFlowEntityMaker.value();

      SingleEntityMaker<HTextFlowHistory> maker = new SingleEntityMaker<HTextFlowHistory>(HTextFlowHistory.class);

      HTextFlowHistory textFlowHistory = maker.value();

      assertThat(textFlowHistory.getTextFlow(), Matchers.notNullValue());
      assertThat(textFlowHistory.getRevision(), Matchers.equalTo(textFlowHistory.getTextFlow().getRevision()));
   }

   @Test
   public void constructorParameterWillFollowConstraint()
   {
      SingleEntityMaker<TestEntity> maker = new SingleEntityMaker<TestEntity>(TestEntity.class);
      TestEntity testEntity = maker.value();

      assertThat(testEntity.email, Matchers.equalTo("nobody@nowhere.org"));
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

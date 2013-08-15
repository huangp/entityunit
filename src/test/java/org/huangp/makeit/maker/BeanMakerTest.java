package org.huangp.makeit.maker;

import static org.hamcrest.MatcherAssert.assertThat;

import lombok.extern.slf4j.Slf4j;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.zanata.common.ContentType;
import org.zanata.common.EntityStatus;
import org.zanata.common.LocaleId;
import org.zanata.common.ProjectType;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlowHistory;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class BeanMakerTest
{

   @Test
   public void canMakeBean()
   {
      BeanMaker<HLocale> maker = new BeanMaker<HLocale>(HLocale.class);

      HLocale locale = maker.value();
      log.info("result {}", locale);

      assertThat(locale.getId(), Matchers.nullValue()); // skip read only properties
      assertThat(locale.getVersionNum(), Matchers.notNullValue());
      assertThat(locale.getCreationDate(), Matchers.notNullValue());
      assertThat(locale.getCreationDate(), Matchers.notNullValue());
      assertThat(locale.getLocaleId(), Matchers.equalTo(LocaleId.EN));

   }

   @Test
   public void willNotTouchDefaultValue()
   {
      BeanMaker<HCopyTransOptions> maker = new BeanMaker<HCopyTransOptions>(HCopyTransOptions.class);

      HCopyTransOptions options = maker.value();

      assertThat(options.getContextMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
      assertThat(options.getDocIdMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
      assertThat(options.getProjectMismatchAction(), Matchers.sameInstance(HCopyTransOptions.ConditionRuleAction.REJECT));
   }

   @Test
   public void canMakeBeanWithNestedBean()
   {

      BeanMaker<HProjectIteration> maker = new BeanMaker<HProjectIteration>(HProjectIteration.class);

      HProjectIteration hIteration = maker.value();
      log.info("result {}", hIteration);

      assertThat(hIteration.getId(), Matchers.nullValue()); // skip read only properties
      assertThat(hIteration.getParent(), Matchers.nullValue()); // skip field that is the same type of self
      assertThat(hIteration.getProject(), Matchers.notNullValue()); // create transitive bean
      assertThat(hIteration.getProjectType(), Matchers.equalTo(ProjectType.Utf8Properties)); // first enum value
      assertThat(hIteration.getSlug(), Matchers.notNullValue());
      assertThat(hIteration.getStatus(), Matchers.equalTo(EntityStatus.ACTIVE)); // won't touch default value
      assertThat(hIteration.getCreationDate(), Matchers.notNullValue());

   }

   @Test
   public void useInstanceConstantsWhenPossible()
   {
      BeanMaker<ContentType> maker = new BeanMaker<ContentType>(ContentType.class);

      ContentType contentType = maker.value();

      assertThat(contentType, Matchers.sameInstance(ContentType.TextPlain));
   }

   @Test
   @Ignore("this is not an ordinary bean")
   public void canMakeUltimateObjectTree()
   {

      BeanMaker<HDocument> maker = new BeanMaker<HDocument>(HDocument.class);

      HDocument document = maker.value();
      log.info("result {}", document);

      assertThat(document.getPoHeader(), Matchers.notNullValue());
      assertThat(document.getProjectIteration(), Matchers.notNullValue());
      assertThat(document.getProjectIteration().getProject(), Matchers.notNullValue());
      assertThat(document.getRawDocument(), Matchers.notNullValue());
      assertThat(document.getLocale(), Matchers.notNullValue());
      assertThat(document.getSourceLocaleId(), Matchers.is(LocaleId.EN));
   }

   @Test
   public void willPreferArgsConstructorIfExists()
   {
      BeanMaker<HTextFlowHistory> maker = new BeanMaker<HTextFlowHistory>(HTextFlowHistory.class);

      HTextFlowHistory textFlowHistory = maker.value();

      assertThat(textFlowHistory.getTextFlow(), Matchers.notNullValue());
      assertThat(textFlowHistory.getRevision(), Matchers.equalTo(textFlowHistory.getTextFlow().getRevision()));
   }
}

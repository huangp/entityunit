package com.github.huangp.entities;

import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.validation.constraints.Size;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import lombok.Data;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Entity
@Data
@Access(AccessType.FIELD)
public class Category extends Identifier
{
   @Size(min = 5)
   private String name;

   @OneToMany(targetEntity = LineItem.class)
   private List<LineItem> lineItems = Lists.newArrayList();


   @Override
   public String toString()
   {
      return Objects.toStringHelper(this)
            .add("name", name)
            .add("lineItems", lineItems.size())
            .toString();
   }
}

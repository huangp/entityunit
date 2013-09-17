package com.github.huangp.entities;

import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.validation.constraints.Size;

import org.hibernate.annotations.IndexColumn;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Patrick Huang
 */
@Entity
@Getter
@Setter
@Access(AccessType.FIELD)
public class Category extends Identifier
{
   @Size(min = 5)
   private String name;

   @OneToMany(targetEntity = LineItem.class)
   @IndexColumn(name = "number", nullable = false)
   @JoinColumn(name = "category_id", nullable = false)
   private List<LineItem> lineItems = Lists.newArrayList();


   @Override
   public String toString()
   {
      return Objects.toStringHelper(this)
            .add("id", getId())
            .add("name", name)
            .add("lineItems", lineItems.size())
            .toString();
   }
}

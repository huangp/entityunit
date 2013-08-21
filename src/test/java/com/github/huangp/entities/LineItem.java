package com.github.huangp.entities;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.Size;

import lombok.Setter;
import lombok.ToString;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Entity
@Setter
@ToString
public class LineItem extends Identifier
{
   private String content;

   private Person owner;

   private Category category;

   @Size(min = 20)
   public String getContent()
   {
      return content;
   }

   @OneToOne(targetEntity = Person.class, optional = false)
   public Person getOwner()
   {
      return owner;
   }

   @ManyToOne(targetEntity = Category.class)
   public Category getCategory()
   {
      return category;
   }
}

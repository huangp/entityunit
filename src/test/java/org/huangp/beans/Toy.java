package org.huangp.beans;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Getter
@Setter
@ToString
public class Toy
{
   private final String ownerName;
   private String name;
   private double price;
   private final Child owner;

   public Toy(Child owner)
   {
      this.owner = owner;
      ownerName = owner.getName();
   }
}

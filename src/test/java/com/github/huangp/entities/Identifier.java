package com.github.huangp.entities;

import java.io.Serializable;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Patrick Huang
 */

@MappedSuperclass
public class Identifier implements Serializable
{
   @Setter(AccessLevel.PACKAGE)
   protected Long id;

   @Id
   @GeneratedValue
   public Long getId()
   {
      return id;
   }
}

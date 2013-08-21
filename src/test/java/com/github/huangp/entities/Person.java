package com.github.huangp.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;

import lombok.Data;


/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Entity
@Access(AccessType.FIELD)
@Data
public class Person extends Identifier
{
   @Email
   private String email;

   @Size(min = 1, max = 20)
   private String name;
}

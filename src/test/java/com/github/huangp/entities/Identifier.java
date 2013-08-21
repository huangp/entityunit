package com.github.huangp.entities;

import java.io.Serializable;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */

@MappedSuperclass
@Getter
public class Identifier implements Serializable
{
   @Id
   @GeneratedValue
   @Setter(AccessLevel.PACKAGE)
   protected Long id;

}

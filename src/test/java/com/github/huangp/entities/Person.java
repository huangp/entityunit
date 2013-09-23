package com.github.huangp.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Email;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.validation.constraints.Size;

/**
 * @author Patrick Huang
 */
@Entity
@Access(AccessType.FIELD)
@Getter
@Setter
@ToString
public class Person extends Identifier {
    @Email
    private String email;

    @Size(min = 1, max = 20)
    private String name;
}

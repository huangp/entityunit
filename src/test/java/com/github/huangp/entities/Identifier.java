package com.github.huangp.entities;

import lombok.AccessLevel;
import lombok.Setter;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * @author Patrick Huang
 */

@MappedSuperclass
public class Identifier implements Serializable {
    @Setter(AccessLevel.PACKAGE)
    protected Long id;

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }
}

package com.github.huangp.beans;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

/**
 * @author Patrick Huang
 */
@Data
public class Child {
    @Setter(AccessLevel.PROTECTED)
    private Long id;
    private String name;
    private String job = "play";
    private Integer age;
    private Parent parent;
    private Child bestField;
    private Language speaks;
    private Date dateOfBirth;

    private Set<Toy> toys;
}

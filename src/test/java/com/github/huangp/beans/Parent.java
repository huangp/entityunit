package com.github.huangp.beans;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author Patrick Huang
 */
@Data
public class Parent {
    private String name;
    private int age;
    private Date dateOfBirth;
    private Parent parent;
    private List<Child> children;
    private Language speaks;
}

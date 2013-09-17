package com.github.huangp.beans;

import java.util.Date;
import java.util.List;

import lombok.Data;

/**
* @author Patrick Huang
*/
@Data
public class Parent
{
   private String name;
   private int age;
   private Date dateOfBirth;
   private Parent parent;
   private List<Child> children;
   private Language speaks;
}

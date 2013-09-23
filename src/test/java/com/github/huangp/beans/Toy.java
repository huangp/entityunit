package com.github.huangp.beans;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Patrick Huang
 */
@Getter
@Setter
@ToString
public class Toy {
    private final String ownerName;
    private String name;
    private double price;
    private final Child owner;

    public Toy(Child owner) {
        this.owner = owner;
        ownerName = owner.getName();
    }
}

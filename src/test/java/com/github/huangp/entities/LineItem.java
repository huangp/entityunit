package com.github.huangp.entities;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.Size;

/**
 * @author Patrick Huang
 */
@Entity
@Setter
@ToString
public class LineItem extends Identifier {
    private String content;

    private Person owner;

    private Category category;

    @Setter(AccessLevel.PROTECTED)
    private Integer number;

    @Size(min = 20)
    public String getContent() {
        return content;
    }

    @OneToOne(targetEntity = Person.class, optional = false)
    @JoinColumn(name = "owner_id")
    public Person getOwner() {
        return owner;
    }

    @ManyToOne(targetEntity = Category.class)
    @JoinColumn(name = "category_id", insertable = false, updatable = false, nullable = false)
    public Category getCategory() {
        return category;
    }

    @Column(insertable = false, updatable = false, nullable = false)
    public Integer getNumber() {
        return number;
    }
}

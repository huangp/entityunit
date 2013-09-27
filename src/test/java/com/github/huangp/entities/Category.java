package com.github.huangp.entities;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.hibernate.annotations.IndexColumn;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author Patrick Huang
 */
@Entity
@Getter
@Access(AccessType.FIELD)
public class Category extends Identifier {
    @Size(min = 5)
    private String name;

    public Category() {
    }

    public Category(String name, Person categoryOwner) {
        this.name = name;
        this.categoryOwner = categoryOwner;
    }

    @OneToMany(targetEntity = LineItem.class)
    @IndexColumn(name = "number", nullable = false)
    @JoinColumn(name = "category_id", nullable = false)
    private List<LineItem> lineItems = Lists.newArrayList();

    @OneToOne
    @JoinColumn(name = "owner_id")
    private Person categoryOwner;

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", getId())
                .add("name", name)
                .add("lineItems", lineItems.size())
                .toString();
    }
}

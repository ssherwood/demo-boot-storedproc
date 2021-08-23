package io.undertree.demo.storedproc.domain;

import javax.persistence.*;

/**
 * This is a dummy entity
 */
@Entity
@Table(name = "dummy")
public class DummyEntity {

    public DummyEntity() {
    }

    public DummyEntity(Integer id, String value) {
        this.id = id;
        this.value = value;
    }

    @Id
    @GeneratedValue
    private Integer id;

    @Column
    private String value;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
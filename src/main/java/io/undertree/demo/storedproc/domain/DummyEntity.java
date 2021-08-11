package io.undertree.demo.storedproc.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * This is a dummy entity
 */
@Entity
public class DummyEntity {
    @Id
    private Integer id;
}
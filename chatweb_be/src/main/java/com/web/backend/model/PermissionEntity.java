package com.web.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "permissions")
public class PermissionEntity extends AbstractEntity<Long> implements Serializable {

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToMany(mappedBy = "permissions")
    private Set<RoleEntity> roles = new HashSet<>();
}

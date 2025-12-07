package com.web.backend.model;

import com.web.backend.common.GenderType;
import com.web.backend.common.Role;
import com.web.backend.common.UserStatus;
import jakarta.persistence.*; // Import từ jakarta.persistence
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String phone;

    @Column(name = "is_online")
    private boolean isOnline;

    @Column(name = "user_status")
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(columnDefinition = "TEXT", name = "public_key")
    private String publicKey;

    @Column(columnDefinition = "TEXT", name = "encrypted_rsa_private_key")
    private String encryptedRsaPrivateKey;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column
    private String avatar;

    @Column
    private Date birthday;

    @Enumerated(EnumType.STRING)
    private GenderType gender;

    @Column(name = "crate_at")
    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    private Date createAt;

    @Column(name = "update_at")
    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    private Date updateAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<AddressEntity> addresses = new ArrayList<>();

    public void addAddress(AddressEntity address) {
        this.addresses.add(address);
        address.setUser(this);
    }

    public void removeAddress(AddressEntity address) {
        this.addresses.remove(address);
        address.setUser(null);
    }
}
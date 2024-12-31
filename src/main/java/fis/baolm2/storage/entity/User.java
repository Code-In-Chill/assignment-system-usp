package fis.baolm2.storage.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Map;

@NamedQueries({
        @NamedQuery(name = "getUserByUserName", query = "select u from User u where u.userName = :username"),
        @NamedQuery(name = "getUserByEmail", query = "select u from User u where u.email = :email"),
        @NamedQuery(name = "getUserCount", query = "select count(u) from User u"),
        @NamedQuery(name = "getAllUsers", query = "select u from User u"),
        @NamedQuery(name = "searchForUser", query = "select u from User u where " +
                "( lower(u.userName) like :search or u.email like :search ) order by u.userName"),
        @NamedQuery(name = "searchForRole", query = "select u from User u where upper(u.roles) like upper(concat('%', :search, '%'))")
})
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "user_name", nullable = false, unique = true)
    private String userName;

    @Column(name = "password", nullable = false, unique = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @ElementCollection
    @CollectionTable(name = "user_attributes", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> attributes;

    @Column(name = "roles")
    private String roles;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, String> getEntityAttributes() {
        return attributes;
    }

    public void setEntityAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

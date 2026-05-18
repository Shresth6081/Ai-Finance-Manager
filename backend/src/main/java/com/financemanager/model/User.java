package com.financemanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;

    private String email;

    /*
    Since my password variable name is password and username variable name is username
    when spring calls getPassword(), lombok handles it for me
    if I change the name of them, it will give error until I explicitly don't implement it
    Eg.
    @Override
    public String getPassword(){
         return this.pass_name;
    }
    upon removing unique = true from username, code will work but internally Jpa will give an error
     */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // Modify if roles are needed
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

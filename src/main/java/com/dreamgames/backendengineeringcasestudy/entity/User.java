package com.dreamgames.backendengineeringcasestudy.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.dreamgames.backendengineeringcasestudy.enums.Country;
import com.dreamgames.backendengineeringcasestudy.enums.CountryEnumConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue 
    private UUID user_id;

    @NotNull
    @Size(min = 1, max = 16)
    @Pattern(regexp = "[\\x21-\\x7E]+")
    private String username;
    
    private long coins = 5000;
    private int level = 1;

    private LocalDateTime created_at;
    
    @Column
    @Convert(converter = CountryEnumConverter.class)
    private Country country;

}

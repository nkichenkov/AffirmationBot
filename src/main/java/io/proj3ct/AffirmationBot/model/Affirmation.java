package io.proj3ct.AffirmationBot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Affirmation {

    @Column(length = 25500)
    private String body;

    @Id
    private Integer id;

}
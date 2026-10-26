package org.example.quoteotd.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "viewer")
@Getter
@NoArgsConstructor
public class Viewer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(nullable = false)
    private String name;

    public Viewer(String name) {
        this.name = name;
    }

    public Viewer(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

}

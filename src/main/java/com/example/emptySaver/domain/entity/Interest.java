package com.example.emptySaver.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class Interest {
    @Id@GeneratedValue
    private Long id;

    private String interestName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Category category;
}

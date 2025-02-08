package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name="site_id", nullable=false)
    private Site site;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;
}

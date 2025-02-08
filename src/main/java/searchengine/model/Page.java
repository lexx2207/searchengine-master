package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import javax.persistence.Index;

@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList  =  "path"),
        uniqueConstraints = {@UniqueConstraint(columnNames = {"path", "site_id"})} )
@Data
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name  = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable  = false)
    private String content;
}

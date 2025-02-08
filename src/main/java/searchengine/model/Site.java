package searchengine.model;

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.*;

@Entity
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "site")
@Data
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "status", nullable = false)
    @Type(type = "pgsql_enum")
    private Status status;

    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(unique = true)
    private String url;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "site")
    private List<Page> pages = new ArrayList<>();
}

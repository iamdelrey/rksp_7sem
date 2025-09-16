package rksp.practices.pr4;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;

@Table("reading")
public class Reading {
    @Id
    private Long id;
    private String type;
    @Column("val")
    private Integer value;
    private Instant ts;

    public Reading() {}
    public Reading(String type, Integer value, Instant ts) {
        this.type = type; this.value = value; this.ts = ts;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }

    @Override public String toString() {
        return "Reading{id=%d, type=%s, value=%d, ts=%s}".formatted(id, type, value, ts);
    }
}

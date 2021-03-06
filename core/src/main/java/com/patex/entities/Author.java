package com.patex.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.patex.utils.StreamU;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
public class Author {

    static final String BOOKS_NO_SEQUENCE = "booksNoSequence";
    static final String SEQUENCES = "sequences";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.LAZY, mappedBy = "author")
    @JsonIgnore
    private List<AuthorBook> books = new ArrayList<>();

    @Lob
    private String descr;

    public Author() {
    }

    public Author(Long id, String name) {
        this.id = id;
        this.name = name;
    }


    public Author(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AuthorBook> getBooks() {
        return books;
    }

    public void setBooks(List<AuthorBook> books) {
        this.books = books;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    @JsonIgnore
    public Stream<Sequence> getSequencesStream() {
        return getBooks().stream().
                flatMap(book -> book.getBook().getSequences().stream()).
                map(BookSequence::getSequence).
                filter(StreamU.distinctByKey(Sequence::getId));
    }

    @JsonGetter(SEQUENCES)
    public List<Sequence> getSequences() {
        return getSequencesStream().collect(Collectors.toList());
    }

    @JsonGetter(BOOKS_NO_SEQUENCE)
    public List<AuthorBook> getBooksNoSequence() {
        return getBooks().stream().filter(book -> book.getBook().getSequences().isEmpty()).collect(Collectors.toList());
    }


}

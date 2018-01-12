package net.unit8.rotom.model;

import org.eclipse.jgit.lib.PersonIdent;

import java.io.Serializable;

public class Commit implements Serializable {
    private String name;
    private String email;
    private String message;

    public Commit(String name, String email, String message) {
        this.name = name;
        this.email = email;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public PersonIdent getPersonIdent() {
        return new PersonIdent(name, email);
    }

    public String getMessage() {
        return message;
    }
}

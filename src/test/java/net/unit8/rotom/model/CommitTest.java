package net.unit8.rotom.model;

import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommitTest {

    @Test
    void constructorAndGetters() {
        Commit commit = new Commit("user", "user@example.com", "initial commit");
        assertEquals("user", commit.getName());
        assertEquals("user@example.com", commit.getEmail());
        assertEquals("initial commit", commit.getMessage());
    }

    @Test
    void getPersonIdentCreatesValidIdent() {
        Commit commit = new Commit("user", "user@example.com", "msg");
        PersonIdent ident = commit.getPersonIdent();
        assertEquals("user", ident.getName());
        assertEquals("user@example.com", ident.getEmailAddress());
    }
}

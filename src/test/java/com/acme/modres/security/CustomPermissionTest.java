package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.security.BasicPermission;

/**
 * Tests for CustomPermission class.
 */
public class CustomPermissionTest {

    @Test
    void testConstructorWithName_createsInstance() {
        CustomPermission perm = new CustomPermission("test.permission");
        assertNotNull(perm);
    }

    @Test
    void testConstructorWithName_getName() {
        CustomPermission perm = new CustomPermission("test.permission");
        assertEquals("test.permission", perm.getName());
    }

    @Test
    void testConstructorWithNameAndActions_createsInstance() {
        CustomPermission perm = new CustomPermission("test.permission", "read");
        assertNotNull(perm);
    }

    @Test
    void testConstructorWithNameAndActions_getName() {
        CustomPermission perm = new CustomPermission("test.permission", "write");
        assertEquals("test.permission", perm.getName());
    }

    @Test
    void testConstructorWithNameAndActions_nullActions() {
        CustomPermission perm = new CustomPermission("test.permission", null);
        assertNotNull(perm);
        assertEquals("test.permission", perm.getName());
    }

    @Test
    void testExtendsBasicPermission() {
        CustomPermission perm = new CustomPermission("test.permission");
        assertTrue(perm instanceof BasicPermission);
    }

    @Test
    void testConstructorWithName_wildcardPermission() {
        CustomPermission perm = new CustomPermission("test.*");
        assertNotNull(perm);
        assertEquals("test.*", perm.getName());
    }

    @Test
    void testConstructorWithName_allPermission() {
        CustomPermission perm = new CustomPermission("*");
        assertNotNull(perm);
    }

    @Test
    void testImplies_samePermission() {
        CustomPermission perm1 = new CustomPermission("test.permission");
        CustomPermission perm2 = new CustomPermission("test.permission");
        assertTrue(perm1.implies(perm2));
    }

    @Test
    void testImplies_wildcardImpliesSpecific() {
        CustomPermission wildcard = new CustomPermission("test.*");
        CustomPermission specific = new CustomPermission("test.specific");
        assertTrue(wildcard.implies(specific));
    }

    @Test
    void testImplies_specificDoesNotImplyDifferent() {
        CustomPermission perm1 = new CustomPermission("test.permission1");
        CustomPermission perm2 = new CustomPermission("test.permission2");
        assertFalse(perm1.implies(perm2));
    }

    @Test
    void testGetActions_returnsEmpty() {
        CustomPermission perm = new CustomPermission("test.permission");
        assertEquals("", perm.getActions());
    }
}

package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomPermissionTest {

    @Test
    void testConstructor_withName_createsInstance() {
        CustomPermission permission = new CustomPermission("test.permission");
        assertNotNull(permission);
    }

    @Test
    void testConstructor_withNameAndActions_createsInstance() {
        CustomPermission permission = new CustomPermission("test.permission", "read,write");
        assertNotNull(permission);
    }

    @Test
    void testGetName_returnsCorrectName() {
        CustomPermission permission = new CustomPermission("my.permission");
        assertEquals("my.permission", permission.getName());
    }

    @Test
    void testConstructor_withNameAndActions_getName() {
        CustomPermission permission = new CustomPermission("my.permission", "read");
        assertEquals("my.permission", permission.getName());
    }

    @Test
    void testImplies_samePermission_returnsTrue() {
        CustomPermission p1 = new CustomPermission("test.permission");
        CustomPermission p2 = new CustomPermission("test.permission");
        assertTrue(p1.implies(p2));
    }

    @Test
    void testImplies_differentPermission_returnsFalse() {
        CustomPermission p1 = new CustomPermission("test.permission");
        CustomPermission p2 = new CustomPermission("other.permission");
        assertFalse(p1.implies(p2));
    }

    @Test
    void testConstructor_withWildcard_createsInstance() {
        CustomPermission permission = new CustomPermission("*");
        assertNotNull(permission);
    }

    @Test
    void testConstructor_withDotSeparatedName() {
        CustomPermission permission = new CustomPermission("com.acme.modres.permission");
        assertNotNull(permission);
        assertEquals("com.acme.modres.permission", permission.getName());
    }
}

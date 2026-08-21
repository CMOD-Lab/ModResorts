package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for OpMetadata class.
 */
public class OpMetadataTest {

    private OpMetadata opMetadata;

    @BeforeEach
    void setUp() {
        opMetadata = new OpMetadata();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(opMetadata);
    }

    @Test
    void testParameterizedConstructor_setsAllFields() {
        OpMetadata op = new OpMetadata("testName", "testDesc", "void", 5);
        assertEquals("testName", op.getName());
        assertEquals("testDesc", op.getDescription());
        assertEquals("void", op.getType());
        assertEquals(5, op.getImpact());
    }

    @Test
    void testParameterizedConstructor_withNullValues() {
        OpMetadata op = new OpMetadata(null, null, null, 0);
        assertNull(op.getName());
        assertNull(op.getDescription());
        assertNull(op.getType());
        assertEquals(0, op.getImpact());
    }

    @Test
    void testSetName_andGetName() {
        opMetadata.setName("increaseMaxLimit");
        assertEquals("increaseMaxLimit", opMetadata.getName());
    }

    @Test
    void testSetName_withNull() {
        opMetadata.setName(null);
        assertNull(opMetadata.getName());
    }

    @Test
    void testSetDescription_andGetDescription() {
        opMetadata.setDescription("Increase the max limit");
        assertEquals("Increase the max limit", opMetadata.getDescription());
    }

    @Test
    void testSetDescription_withNull() {
        opMetadata.setDescription(null);
        assertNull(opMetadata.getDescription());
    }

    @Test
    void testSetType_andGetType() {
        opMetadata.setType("void");
        assertEquals("void", opMetadata.getType());
    }

    @Test
    void testSetType_withNull() {
        opMetadata.setType(null);
        assertNull(opMetadata.getType());
    }

    @Test
    void testSetImpact_andGetImpact() {
        opMetadata.setImpact(10);
        assertEquals(10, opMetadata.getImpact());
    }

    @Test
    void testSetImpact_withZero() {
        opMetadata.setImpact(0);
        assertEquals(0, opMetadata.getImpact());
    }

    @Test
    void testSetImpact_withNegativeValue() {
        opMetadata.setImpact(-1);
        assertEquals(-1, opMetadata.getImpact());
    }

    @Test
    void testGetName_defaultIsNull() {
        assertNull(opMetadata.getName());
    }

    @Test
    void testGetDescription_defaultIsNull() {
        assertNull(opMetadata.getDescription());
    }

    @Test
    void testGetType_defaultIsNull() {
        assertNull(opMetadata.getType());
    }

    @Test
    void testGetImpact_defaultIsZero() {
        assertEquals(0, opMetadata.getImpact());
    }

    @Test
    void testSetAndGetAllFields() {
        opMetadata.setName("resetMaxLimit");
        opMetadata.setDescription("Reset the max limit");
        opMetadata.setType("String");
        opMetadata.setImpact(20);

        assertEquals("resetMaxLimit", opMetadata.getName());
        assertEquals("Reset the max limit", opMetadata.getDescription());
        assertEquals("String", opMetadata.getType());
        assertEquals(20, opMetadata.getImpact());
    }
}

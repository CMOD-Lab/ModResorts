package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OpMetadataTest {

    @Test
    void testDefaultConstructor_createsInstance() {
        OpMetadata op = new OpMetadata();
        assertNotNull(op);
    }

    @Test
    void testParameterizedConstructor_setsAllFields() {
        OpMetadata op = new OpMetadata("testOp", "Test description", "void", 1);
        assertEquals("testOp", op.getName());
        assertEquals("Test description", op.getDescription());
        assertEquals("void", op.getType());
        assertEquals(1, op.getImpact());
    }

    @Test
    void testSetName_andGetName() {
        OpMetadata op = new OpMetadata();
        op.setName("myOperation");
        assertEquals("myOperation", op.getName());
    }

    @Test
    void testSetDescription_andGetDescription() {
        OpMetadata op = new OpMetadata();
        op.setDescription("My description");
        assertEquals("My description", op.getDescription());
    }

    @Test
    void testSetType_andGetType() {
        OpMetadata op = new OpMetadata();
        op.setType("java.lang.String");
        assertEquals("java.lang.String", op.getType());
    }

    @Test
    void testSetImpact_andGetImpact() {
        OpMetadata op = new OpMetadata();
        op.setImpact(2);
        assertEquals(2, op.getImpact());
    }

    @Test
    void testDefaultConstructor_fieldsAreNull() {
        OpMetadata op = new OpMetadata();
        assertNull(op.getName());
        assertNull(op.getDescription());
        assertNull(op.getType());
        assertEquals(0, op.getImpact());
    }

    @Test
    void testSetName_withNull() {
        OpMetadata op = new OpMetadata();
        op.setName(null);
        assertNull(op.getName());
    }

    @Test
    void testSetDescription_withNull() {
        OpMetadata op = new OpMetadata();
        op.setDescription(null);
        assertNull(op.getDescription());
    }

    @Test
    void testSetType_withNull() {
        OpMetadata op = new OpMetadata();
        op.setType(null);
        assertNull(op.getType());
    }

    @Test
    void testSetImpact_withZero() {
        OpMetadata op = new OpMetadata();
        op.setImpact(0);
        assertEquals(0, op.getImpact());
    }

    @Test
    void testParameterizedConstructor_withNullValues() {
        OpMetadata op = new OpMetadata(null, null, null, 0);
        assertNull(op.getName());
        assertNull(op.getDescription());
        assertNull(op.getType());
        assertEquals(0, op.getImpact());
    }
}

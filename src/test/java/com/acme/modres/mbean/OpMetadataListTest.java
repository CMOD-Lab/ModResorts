package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for OpMetadataList class.
 */
public class OpMetadataListTest {

    private OpMetadataList opMetadataList;

    @BeforeEach
    void setUp() {
        opMetadataList = new OpMetadataList();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(opMetadataList);
    }

    @Test
    void testDefaultConstructor_emptyList() {
        assertNotNull(opMetadataList.getOpMetadatList());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void testAdd_singleElement() {
        OpMetadata op = new OpMetadata("op1", "desc1", "void", 1);
        opMetadataList.add(op);
        assertEquals(1, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testAdd_multipleElements() {
        OpMetadata op1 = new OpMetadata("op1", "desc1", "void", 1);
        OpMetadata op2 = new OpMetadata("op2", "desc2", "String", 2);
        opMetadataList.add(op1);
        opMetadataList.add(op2);
        assertEquals(2, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testAdd_preservesOrder() {
        OpMetadata op1 = new OpMetadata("op1", "desc1", "void", 1);
        OpMetadata op2 = new OpMetadata("op2", "desc2", "String", 2);
        opMetadataList.add(op1);
        opMetadataList.add(op2);
        assertEquals("op1", opMetadataList.getOpMetadatList().get(0).getName());
        assertEquals("op2", opMetadataList.getOpMetadatList().get(1).getName());
    }

    @Test
    void testGetOpMetadatList_returnsCorrectList() {
        OpMetadata op = new OpMetadata("testOp", "testDesc", "void", 5);
        opMetadataList.add(op);
        List<OpMetadata> list = opMetadataList.getOpMetadatList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("testOp", list.get(0).getName());
    }

    @Test
    void testSetOpMetadatList_replacesExistingList() {
        OpMetadata op1 = new OpMetadata("op1", "desc1", "void", 1);
        opMetadataList.add(op1);

        List<OpMetadata> newList = new ArrayList<>();
        OpMetadata op2 = new OpMetadata("op2", "desc2", "String", 2);
        newList.add(op2);

        opMetadataList.setOpMetadatList(newList);
        assertEquals(1, opMetadataList.getOpMetadatList().size());
        assertEquals("op2", opMetadataList.getOpMetadatList().get(0).getName());
    }

    @Test
    void testSetOpMetadatList_withEmptyList() {
        OpMetadata op1 = new OpMetadata("op1", "desc1", "void", 1);
        opMetadataList.add(op1);

        opMetadataList.setOpMetadatList(new ArrayList<>());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void testSetOpMetadatList_withNull() {
        opMetadataList.setOpMetadatList(null);
        assertNull(opMetadataList.getOpMetadatList());
    }

    @Test
    void testAdd_nullElement() {
        // Should not throw, just add null
        opMetadataList.add(null);
        assertEquals(1, opMetadataList.getOpMetadatList().size());
        assertNull(opMetadataList.getOpMetadatList().get(0));
    }
}

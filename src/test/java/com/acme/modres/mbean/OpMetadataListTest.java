package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

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
    void testAdd_singleItem() {
        OpMetadata op = new OpMetadata("op1", "desc1", "void", 1);
        opMetadataList.add(op);
        assertEquals(1, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testAdd_multipleItems() {
        opMetadataList.add(new OpMetadata("op1", "desc1", "void", 1));
        opMetadataList.add(new OpMetadata("op2", "desc2", "String", 2));
        opMetadataList.add(new OpMetadata("op3", "desc3", "int", 3));
        assertEquals(3, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testGetOpMetadatList_returnsCorrectList() {
        OpMetadata op = new OpMetadata("testOp", "Test", "void", 0);
        opMetadataList.add(op);
        List<OpMetadata> list = opMetadataList.getOpMetadatList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("testOp", list.get(0).getName());
    }

    @Test
    void testSetOpMetadatList_replacesExistingList() {
        opMetadataList.add(new OpMetadata("old", "old desc", "void", 0));

        List<OpMetadata> newList = new ArrayList<>();
        newList.add(new OpMetadata("new1", "new desc1", "void", 1));
        newList.add(new OpMetadata("new2", "new desc2", "String", 2));
        opMetadataList.setOpMetadatList(newList);

        assertEquals(2, opMetadataList.getOpMetadatList().size());
        assertEquals("new1", opMetadataList.getOpMetadatList().get(0).getName());
    }

    @Test
    void testSetOpMetadatList_withNull() {
        opMetadataList.setOpMetadatList(null);
        assertNull(opMetadataList.getOpMetadatList());
    }

    @Test
    void testAdd_preservesOrder() {
        opMetadataList.add(new OpMetadata("first", "desc", "void", 0));
        opMetadataList.add(new OpMetadata("second", "desc", "void", 0));
        opMetadataList.add(new OpMetadata("third", "desc", "void", 0));

        List<OpMetadata> list = opMetadataList.getOpMetadatList();
        assertEquals("first", list.get(0).getName());
        assertEquals("second", list.get(1).getName());
        assertEquals("third", list.get(2).getName());
    }
}

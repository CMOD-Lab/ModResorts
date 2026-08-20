package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class OpMetadataListTest {

  private OpMetadataList opMetadataList;

  @BeforeEach
  public void setUp() {
    opMetadataList = new OpMetadataList();
  }

  @Test
  public void testDefaultConstructor() {
    assertNotNull(opMetadataList);
    assertNotNull(opMetadataList.getOpMetadatList());
    assertTrue(opMetadataList.getOpMetadatList().isEmpty());
  }

  @Test
  public void testAddOpMetadata() {
    OpMetadata op1 = new OpMetadata("op1", "Operation 1", "void", 1);
    opMetadataList.add(op1);

    assertEquals(1, opMetadataList.getOpMetadatList().size());
    assertEquals(op1, opMetadataList.getOpMetadatList().get(0));
  }

  @Test
  public void testAddMultipleOpMetadata() {
    OpMetadata op1 = new OpMetadata("op1", "Operation 1", "void", 1);
    OpMetadata op2 = new OpMetadata("op2", "Operation 2", "String", 2);
    OpMetadata op3 = new OpMetadata("op3", "Operation 3", "int", 3);

    opMetadataList.add(op1);
    opMetadataList.add(op2);
    opMetadataList.add(op3);

    assertEquals(3, opMetadataList.getOpMetadatList().size());
  }

  @Test
  public void testGetOpMetadatList() {
    List<OpMetadata> list = opMetadataList.getOpMetadatList();
    assertNotNull(list);
    assertTrue(list instanceof List);
  }

  @Test
  public void testSetOpMetadatList() {
    List<OpMetadata> newList = new ArrayList<>();
    newList.add(new OpMetadata("op1", "Operation 1", "void", 1));
    newList.add(new OpMetadata("op2", "Operation 2", "String", 2));

    opMetadataList.setOpMetadatList(newList);

    assertEquals(2, opMetadataList.getOpMetadatList().size());
    assertEquals(newList, opMetadataList.getOpMetadatList());
  }

  @Test
  public void testSetOpMetadatListWithEmptyList() {
    List<OpMetadata> emptyList = new ArrayList<>();
    opMetadataList.setOpMetadatList(emptyList);

    assertTrue(opMetadataList.getOpMetadatList().isEmpty());
  }

  @Test
  public void testEmptyOpMetadataList() {
    assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    assertEquals(0, opMetadataList.getOpMetadatList().size());
  }

  @Test
  public void testAddAndSetInteraction() {
    OpMetadata op1 = new OpMetadata("op1", "Operation 1", "void", 1);
    opMetadataList.add(op1);
    assertEquals(1, opMetadataList.getOpMetadatList().size());

    List<OpMetadata> newList = new ArrayList<>();
    newList.add(new OpMetadata("op2", "Operation 2", "String", 2));
    opMetadataList.setOpMetadatList(newList);

    assertEquals(1, opMetadataList.getOpMetadatList().size());
    assertEquals("op2", opMetadataList.getOpMetadatList().get(0).getName());
  }
}

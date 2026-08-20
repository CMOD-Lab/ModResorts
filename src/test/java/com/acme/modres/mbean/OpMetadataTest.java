package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class OpMetadataTest {

  private OpMetadata opMetadata;

  @BeforeEach
  public void setUp() {
    opMetadata = new OpMetadata();
  }

  @Test
  public void testDefaultConstructor() {
    assertNotNull(opMetadata);
    assertNull(opMetadata.getName());
    assertNull(opMetadata.getDescription());
    assertNull(opMetadata.getType());
    assertEquals(0, opMetadata.getImpact());
  }

  @Test
  public void testParameterizedConstructor() {
    OpMetadata op = new OpMetadata("testOp", "Test Operation", "void", 1);
    assertEquals("testOp", op.getName());
    assertEquals("Test Operation", op.getDescription());
    assertEquals("void", op.getType());
    assertEquals(1, op.getImpact());
  }

  @Test
  public void testSetName() {
    opMetadata.setName("operationName");
    assertEquals("operationName", opMetadata.getName());
  }

  @Test
  public void testSetDescription() {
    opMetadata.setDescription("This is a test operation");
    assertEquals("This is a test operation", opMetadata.getDescription());
  }

  @Test
  public void testSetType() {
    opMetadata.setType("String");
    assertEquals("String", opMetadata.getType());
  }

  @Test
  public void testSetImpact() {
    opMetadata.setImpact(2);
    assertEquals(2, opMetadata.getImpact());
  }

  @Test
  public void testGettersAndSetters() {
    opMetadata.setName("myOperation");
    opMetadata.setDescription("My operation description");
    opMetadata.setType("int");
    opMetadata.setImpact(3);

    assertEquals("myOperation", opMetadata.getName());
    assertEquals("My operation description", opMetadata.getDescription());
    assertEquals("int", opMetadata.getType());
    assertEquals(3, opMetadata.getImpact());
  }

  @Test
  public void testSetNameWithNull() {
    opMetadata.setName(null);
    assertNull(opMetadata.getName());
  }

  @Test
  public void testSetDescriptionWithNull() {
    opMetadata.setDescription(null);
    assertNull(opMetadata.getDescription());
  }

  @Test
  public void testSetTypeWithNull() {
    opMetadata.setType(null);
    assertNull(opMetadata.getType());
  }

  @Test
  public void testImpactValues() {
    opMetadata.setImpact(0);
    assertEquals(0, opMetadata.getImpact());

    opMetadata.setImpact(-1);
    assertEquals(-1, opMetadata.getImpact());

    opMetadata.setImpact(100);
    assertEquals(100, opMetadata.getImpact());
  }
}

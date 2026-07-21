package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.management.MBeanOperationInfo;
import java.util.ArrayList;
import java.util.List;

public class DMBeanUtilsTest {

    @Test
    void testGetOps_withNullOpList_returnsNull() {
        MBeanOperationInfo[] result = DMBeanUtils.getOps(null);
        assertNull(result);
    }

    @Test
    void testGetOps_withOpListHavingNullList_returnsNull() {
        OpMetadataList opList = new OpMetadataList();
        opList.setOpMetadatList(null);
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNull(result);
    }

    @Test
    void testGetOps_withEmptyOpList_returnsNull() {
        OpMetadataList opList = new OpMetadataList();
        // empty list, numOps == 0
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNull(result);
    }

    @Test
    void testGetOps_withOneOperation_returnsArrayOfOne() {
        OpMetadataList opList = new OpMetadataList();
        OpMetadata op = new OpMetadata("testOp", "Test operation", "void", MBeanOperationInfo.ACTION);
        opList.add(op);

        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    void testGetOps_withMultipleOperations_returnsCorrectCount() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "Operation 1", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("op2", "Operation 2", "String", MBeanOperationInfo.INFO));
        opList.add(new OpMetadata("op3", "Operation 3", "int", MBeanOperationInfo.ACTION_INFO));

        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void testGetOps_operationHasCorrectName() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("myOperation", "My operation", "void", MBeanOperationInfo.ACTION));

        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        assertNotNull(result);
        assertEquals("myOperation", result[0].getName());
    }

    @Test
    void testGetOps_operationHasCorrectDescription() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op", "My description", "void", MBeanOperationInfo.ACTION));

        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        assertNotNull(result);
        assertEquals("My description", result[0].getDescription());
    }

    @Test
    void testGetOps_operationHasCorrectReturnType() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op", "desc", "java.lang.String", MBeanOperationInfo.ACTION));

        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        assertNotNull(result);
        assertEquals("java.lang.String", result[0].getReturnType());
    }
}

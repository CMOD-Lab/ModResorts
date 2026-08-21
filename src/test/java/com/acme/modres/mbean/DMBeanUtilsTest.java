package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import javax.management.MBeanOperationInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for DMBeanUtils class.
 * Note: MBeanOperationInfo impact must be one of:
 *   ACTION (1), ACTION_INFO (2), INFO (0), or UNKNOWN (3)
 */
public class DMBeanUtilsTest {

    @Test
    void testGetOps_withNullOpList_returnsNull() {
        MBeanOperationInfo[] result = DMBeanUtils.getOps(null);
        assertNull(result);
    }

    @Test
    void testGetOps_withNullInnerList_returnsNull() {
        OpMetadataList opList = new OpMetadataList();
        opList.setOpMetadatList(null);
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNull(result);
    }

    @Test
    void testGetOps_withEmptyList_returnsNull() {
        OpMetadataList opList = new OpMetadataList();
        // empty list, numOps == 0
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNull(result);
    }

    @Test
    void testGetOps_withSingleOp_returnsArrayOfOne() {
        OpMetadataList opList = new OpMetadataList();
        // Use valid impact: ACTION=1
        opList.add(new OpMetadata("increaseMaxLimit", "Increase the max limit", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    void testGetOps_withSingleOp_correctName() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("increaseMaxLimit", "Increase the max limit", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("increaseMaxLimit", result[0].getName());
    }

    @Test
    void testGetOps_withSingleOp_correctDescription() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("increaseMaxLimit", "Increase the max limit", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("Increase the max limit", result[0].getDescription());
    }

    @Test
    void testGetOps_withMultipleOps_returnsCorrectCount() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc1", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("op2", "desc2", "String", MBeanOperationInfo.INFO));
        opList.add(new OpMetadata("op3", "desc3", "int", MBeanOperationInfo.UNKNOWN));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void testGetOps_withMultipleOps_correctNames() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc1", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("op2", "desc2", "String", MBeanOperationInfo.ACTION_INFO));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("op1", result[0].getName());
        assertEquals("op2", result[1].getName());
    }

    @Test
    void testGetOps_withTwoOps_validImpact() {
        OpMetadataList opList = new OpMetadataList();
        // Use valid impact values: ACTION=1, ACTION_INFO=2
        opList.add(new OpMetadata("increaseMaxLimit", "Increase the max limit before raising alerts", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("resetMaxLimit", "Reset the max limit before raising alerts to the default value", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("increaseMaxLimit", result[0].getName());
        assertEquals("resetMaxLimit", result[1].getName());
    }

    @Test
    void testGetOps_withNullOpMetadata_handlesGracefully() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata(null, null, null, MBeanOperationInfo.UNKNOWN));
        // Should not throw
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    void testGetOps_withInfoImpact_returnsCorrectOp() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("getInfo", "Get information", "String", MBeanOperationInfo.INFO));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("getInfo", result[0].getName());
    }
}

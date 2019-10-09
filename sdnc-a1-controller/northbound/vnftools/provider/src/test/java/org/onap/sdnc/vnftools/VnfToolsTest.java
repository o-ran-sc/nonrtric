/*-
 * ============LICENSE_START=======================================================
 * openECOMP : SDN-C
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.sdnc.vnftools;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.ccsdk.sli.core.sli.SvcLogicContext;
import org.onap.ccsdk.sli.core.sli.SvcLogicException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class VnfToolsTest {
    private SvcLogicContext mockSvcLogicContext = mock(SvcLogicContext.class);

    private VnfTools vnfTools;

    @Before
    public void setUp() throws Exception {
        vnfTools = new VnfTools();
    }

    @Test
    public void testConstructor() throws Exception {
        VnfTools vTools = new VnfTools();
        Assert.assertTrue("Should have created", vTools != null);
    }

    @Test(expected = SvcLogicException.class)
    public void testCheckIfActivateReadyFailure() throws Exception {
        vnfTools.checkIfActivateReady(null, mockSvcLogicContext);
    }

    @Test
    public void testCheckIfActivateReady() throws Exception {
        String value = "testing";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(VnfTools.RETURN_KEY, value);
        vnfTools.checkIfActivateReady(parameters, mockSvcLogicContext);
        Mockito.verify(mockSvcLogicContext, times(1)).setAttribute(value, VnfTools.TRUE_STRING);
    }

    @Test(expected = SvcLogicException.class)
    public void testStringContainsFailure() throws Exception {
        vnfTools.stringContains(null, mockSvcLogicContext);
    }

    @Test
    public void testStringContains() throws Exception {
        String value = "result ctx string";
        String stringToFindValue = "testing";
        String stringToSearchValue = "testing 1234";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(VnfTools.RESULT_CTX_STRING, value);
        parameters.put(VnfTools.STRING_TO_FIND, stringToFindValue);
        parameters.put(VnfTools.STRING_TO_SEARCH, stringToSearchValue);

        vnfTools.stringContains(parameters, mockSvcLogicContext);
        Mockito.verify(mockSvcLogicContext, times(1)).setAttribute(
                value, Boolean.toString(stringToSearchValue.contains(stringToFindValue)));

        stringToFindValue = "1234";
        vnfTools.stringContains(parameters, mockSvcLogicContext);
        Mockito.verify(mockSvcLogicContext, times(2)).setAttribute(
                value, Boolean.toString(stringToSearchValue.contains(stringToFindValue)));
    }

    @Test
    public void testGenerateNameFailure() throws Exception {
        try {
            vnfTools.generateName(null, mockSvcLogicContext);
            Assert.fail("should have throw SvcLogicException");
        } catch (SvcLogicException e) {
            Assert.assertFalse("Should be validation error",
                    e.getMessage().contains("needs at least length 4 but only have"));
        }
    }

    @Test
    public void testGenerateNameFailWithShortBaseParam() throws Exception {
        String value = "return path";
        String base = "123";
        String suffix = "suffix";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(VnfTools.RETURN_PATH, value);
        parameters.put(VnfTools.BASE, base);
        parameters.put(VnfTools.SUFFIX, suffix);

        try {
            vnfTools.generateName(parameters, mockSvcLogicContext);
            Assert.fail("should have throw SvcLogicException");
        } catch (SvcLogicException e) {
            Assert.assertTrue("Should be length error",
                    e.getMessage().contains("needs at least length 4 but only have"));
        }
    }

    @Test
    public void testGenerateName() throws Exception {
        String value = "return path";
        String base = "1234567890";
        String suffix = "suffix";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(VnfTools.RETURN_PATH, value);
        parameters.put(VnfTools.BASE, base);
        parameters.put(VnfTools.SUFFIX, suffix);

        vnfTools.generateName(parameters, mockSvcLogicContext);
        String expectedValue = String.format("%s%s%s",
                base.substring(0, base.length() - 4), suffix, base.substring(base.length() - 2));
        Mockito.verify(mockSvcLogicContext, times(1)).setAttribute(value, expectedValue);
    }

    @Test
    public void testPrintContextInParamNullFailure() throws Exception {
        try {
            vnfTools.printContext(null, mockSvcLogicContext);
            Assert.fail("should have throw SvcLogicException");
        } catch(SvcLogicException e) {
            Assert.assertEquals("Should be no param error", "no parameters passed", e.getMessage());
        }
    }

    @Test
    public void testPrintContextFileNameFailure() throws Exception {
        String expectedEmessage = "printContext requires 'filename' parameter";
        Map<String, String> parameters = new HashMap<>();
        try {
            vnfTools.printContext(parameters, mockSvcLogicContext);
            Assert.fail("should have throw SvcLogicException");
        } catch(SvcLogicException e) {
            Assert.assertEquals("Should be missing filename error", expectedEmessage, e.getMessage());
        }

        parameters.put(VnfTools.FILENAME, "");
        try {
            vnfTools.printContext(parameters, mockSvcLogicContext);
            Assert.fail("should have throw SvcLogicException");
        } catch(SvcLogicException e) {
            Assert.assertEquals("Should still be missing filename error", expectedEmessage, e.getMessage());
        }
    }

    @Test
    public void testPrintContext() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(VnfTools.FILENAME, "target/testPrintContext.out");
        vnfTools.printContext(parameters, mockSvcLogicContext);
    }

    @Test
    public void testGetArrayLengthInvalidInt() throws Exception {
        String key = "abc";
        Mockito.doReturn("efg").when(mockSvcLogicContext).getAttribute(key);
        int result = VnfTools.getArrayLength(mockSvcLogicContext, key);
        Assert.assertEquals("Should return 0 for string value", 0, result);
    }

    @Test
    public void testGetArrayLength() throws Exception {
        String key = "abc";
        String value = "234";
        Mockito.doReturn(value).when(mockSvcLogicContext).getAttribute(key);
        int result = VnfTools.getArrayLength(mockSvcLogicContext, key);
        Assert.assertEquals("Should return the value int", Integer.parseInt(value), result);
    }

    @Test
    public void testGetArrayLengthWithDebugInvalidInt() throws Exception {
        String key = "abc";
        Mockito.doReturn("efg").when(mockSvcLogicContext).getAttribute(key);
        int result = VnfTools.getArrayLength(mockSvcLogicContext, key, "debug");
        Assert.assertEquals("Should return 0 for string value", 0, result);
    }

    @Test
    public void testGetArrayLengthWithDebug() throws Exception {
        String key = "abc";
        String value = "234";
        Mockito.doReturn(value).when(mockSvcLogicContext).getAttribute(key);
        int result = VnfTools.getArrayLength(mockSvcLogicContext, key, "debug");
        Assert.assertEquals("Should return the value int", Integer.parseInt(value), result);
    }

}
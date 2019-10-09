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

import org.onap.ccsdk.sli.core.sli.SvcLogicContext;
import org.onap.ccsdk.sli.core.sli.SvcLogicException;
import org.onap.ccsdk.sli.core.sli.SvcLogicJavaPlugin;
import org.onap.ccsdk.sli.core.slipluginutils.SliPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class VnfTools implements SvcLogicJavaPlugin {
    static final String BASE = "base";
    static final String FILENAME = "filename";
    static final String RESULT_CTX_STRING = "result_ctx_string";
    static final String RETURN_KEY = "return-key";
    static final String RETURN_PATH = "return-path";
    static final String STRING_TO_FIND = "string_to_find";
    static final String STRING_TO_SEARCH = "string_to_search";
    static final String SUFFIX = "suffix";
    static final String TRUE_STRING = "true";

    private static final Logger LOG = LoggerFactory.getLogger(VnfTools.class);

    public VnfTools() {

    }

    public void checkIfActivateReady(Map<String, String> parameters, SvcLogicContext ctx) throws SvcLogicException {
        LOG.debug("Checking if enough data is available to send the NCS Activate request...");

        SliPluginUtils.checkParameters(parameters, new String[]{RETURN_KEY}, LOG);
        setIfNotNull(parameters.get(RETURN_KEY), TRUE_STRING, ctx);
    }

    /**
     * DG node performs a java String.contains(String) and writes true or false
     * to a key in context memory.
     * @param parameters HashMap in context memory must contain the following:
     * <table border='1'>
     * <thead>
     *     <th>Key</th>
     *     <th>Description</th>
     * </thead>
     * <tbody>
     *     <tr>
     *         <td>string_to_search</td>
     *         <td>String to perform java String.contains(String) on</td>
     *     </tr>
     *  <tr>
     *         <td>string_to_find</td>
     *         <td>String to find in the string_to_search</td>
     *     </tr>
     *  <tr>
     *         <td>result_ctx_string</td>
     *         <td>Context memory key to write the result ("true" or "false") to</td>
     *     </tr>
     * </tbody>
     * </table>
     * @param ctx Reference to context memory
     * @throws SvcLogicException when passed in parameter is not valid
     */
    public void stringContains(Map<String, String> parameters, SvcLogicContext ctx) throws SvcLogicException {
        SliPluginUtils.checkParameters(
                parameters, new String[]{STRING_TO_SEARCH, STRING_TO_FIND, RESULT_CTX_STRING}, LOG);
        setIfNotNull(parameters.get(RESULT_CTX_STRING),
                Boolean.toString(parameters.get(STRING_TO_SEARCH).contains(parameters.get(STRING_TO_FIND))),
                ctx);
    }


    public void generateName(Map<String, String> parameters, SvcLogicContext ctx) throws SvcLogicException {
        LOG.debug("generateName");

        SliPluginUtils.checkParameters(parameters, new String[]{BASE, SUFFIX, RETURN_PATH}, LOG);

        String base = parameters.get(BASE);
        int baseLength = base.length();
        if (baseLength < 4) {
            String errorMessage = String.format("Parameter(%s) needs at least length 4 but only have %d",
                    BASE, baseLength);
            LOG.error(errorMessage);
            throw new SvcLogicException(errorMessage);
        }

        setIfNotNull(parameters.get(RETURN_PATH), String.format("%s%s%s",
                base.substring(0, baseLength - 4), parameters.get(SUFFIX), base.substring(baseLength - 2)),
                ctx);
    }

    private void setIfNotNull(String property, String value, SvcLogicContext ctx) {
        if (property != null && value != null) {
            LOG.debug("Setting ", property, " to ",  value);
            ctx.setAttribute(property, value);
        }
    }

    public void printContext(Map<String, String> parameters, SvcLogicContext ctx) throws SvcLogicException {
        if (parameters == null) {
            throw new SvcLogicException("no parameters passed");
        }

        String fileName = parameters.get(FILENAME);

        if ((fileName == null) || (fileName.length() == 0)) {
            throw new SvcLogicException("printContext requires 'filename' parameter");
        }

        PrintStream pstr = null;

        try (FileOutputStream fileStream = new FileOutputStream(new File(fileName), true)){
            pstr = new PrintStream(fileStream);
        } catch (IOException e1) {
            LOG.error("FileOutputStream close exception: ", e1);
        }
        catch (Exception e) {
            throw new SvcLogicException("Cannot open file " + fileName, e);
        } finally {
            if (pstr != null) {
                pstr.println("#######################################");
                for (String attr : ctx.getAttributeKeySet()) {
                    pstr.println(attr + " = " + ctx.getAttribute(attr));
                }

                pstr.flush();
                pstr.close();
            }
        }

    }

    static int getArrayLength(SvcLogicContext ctx, String key) {
        String value = ctx.getAttribute(key);
        try {
            return Integer.parseInt(value);
        } catch( NumberFormatException e ) {
            LOG.debug(String.format("Ctx contained key(%s) value(%s) is not integer", key, value));
        }

        return 0;
    }

    static int getArrayLength(SvcLogicContext ctx, String key, String debug) {
        try {
            return Integer.parseInt(ctx.getAttribute(key));
        } catch( NumberFormatException e ) {
            LOG.debug(debug);
        }

        return 0;
    }
}

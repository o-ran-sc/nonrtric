/* ============LICENSE_START=======================================================
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

package com.onap.sdnc.testapi.model;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import com.onap.sdnc.reports.model.ValidationTestType;


public class ValidationTest {
	
	private String validationType="network";	
	private String typeId="1";
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void TestValidation() {	    
	    ValidationTestType validationTestType=new ValidationTestType();
		    validationTestType.setTypeId(typeId);
		    validationTestType.setValidationType(validationType);
		
		    assertEquals(validationTestType.getTypeId(), "1"); 
		    assertEquals(validationTestType.getValidationType(), "network");
	}
}

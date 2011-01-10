/*
 * Copyright 2011 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl2.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.module.external.kc.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.kuali.kfs.module.external.kc.businessobject.BudgetCategoryDTO;
import org.kuali.kfs.module.external.kc.dto.HashMapElement;

/**
 * This class was generated by Apache CXF 2.2.10
 * Fri Jan 07 09:07:37 HST 2011
 * Generated source version: 2.2.10
 * 
 */
 
@WebService(targetNamespace = "KC", name = "budgetCategoryService")
public interface BudgetCategoryService {
    public static final List <String> KC_BA_ALLOWABLE_CRITERIA_PARAMETERS = Arrays.asList("unitName","unitNumber","parentUnitNumber","organizationId");

    List lookupBudgetCategories(Map<String, String> searchCriteria);
}
/*
 * Copyright 2010 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.module.external.kc.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.kuali.kfs.integration.kc.businessobject.BudgetCategoryDTO;
import org.kuali.kfs.module.external.kc.KcConstants;
import org.kuali.kfs.module.external.kc.dto.BudgetAdjustmentCreationStatusDTO;
import org.kuali.kfs.module.external.kc.dto.BudgetAdjustmentParametersDTO;
import org.kuali.kfs.module.external.kc.dto.HashMapElement;
import org.kuali.kfs.module.external.kc.dto.KcObjectCode;

@WebService(name = KcConstants.BudgetAdjustmentService.WEB_SERVICE_NAME, 
            targetNamespace = KcConstants.MODULE_TARGET_NAMESPACE)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL, 
             parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public interface BudgetAdjustmentService {

    public BudgetAdjustmentCreationStatusDTO createBudgetAdjustment(
            @WebParam(name="budgetAdjustmentParametersDTO")BudgetAdjustmentParametersDTO budgetAdjustmentParametersDTO);
    
    public KcObjectCode getObjectCode( @WebParam(name="universityFiscalYear") String universityFiscalYear,
            @WebParam(name="chartOfAccountsCode") String chartOfAccountsCode,
            @WebParam(name="financialObjectCode") String financialObjectCode);
  
    public List<KcObjectCode> lookupObjectCodes( @WebParam(name="searchCriteria") java.util.List <HashMapElement> searchCriteria);

}

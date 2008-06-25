/*
 * Copyright 2006-2007 The Kuali Foundation.
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
package org.kuali.kfs.module.bc.document.web.struts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.kuali.core.authorization.AuthorizationType;
import org.kuali.core.exceptions.AuthorizationException;
import org.kuali.core.exceptions.ModuleAuthorizationException;
import org.kuali.core.service.BusinessObjectService;
import org.kuali.core.service.KualiConfigurationService;
import org.kuali.core.service.KualiModuleService;
import org.kuali.core.util.GlobalVariables;
import org.kuali.core.util.UrlFactory;
import org.kuali.core.web.struts.form.KualiForm;
import org.kuali.kfs.module.bc.BCConstants;
import org.kuali.kfs.module.bc.BCKeyConstants;
import org.kuali.kfs.module.bc.businessobject.PendingBudgetConstructionAppointmentFunding;
import org.kuali.kfs.module.bc.businessobject.SalarySettingExpansion;
import org.kuali.kfs.module.bc.document.service.SalarySettingService;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.context.SpringContext;

public class QuickSalarySettingAction extends SalarySettingBaseAction {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(QuickSalarySettingAction.class);

    /**
     * @see org.kuali.core.web.struts.action.KualiAction#refresh(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward refresh(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        QuickSalarySettingForm salarySettingForm = (QuickSalarySettingForm) form;
        String refreshCaller = request.getParameter(KFSConstants.REFRESH_CALLER);

        if (StringUtils.equals(BCConstants.POSITION_SALARY_SETTING_REFRESH_CALLER, refreshCaller)) {
            // TODO do things specific to returning from Position Salary Setting
        }

        if (StringUtils.equals(BCConstants.INCUMBENT_SALARY_SETTING_REFRESH_CALLER, refreshCaller)) {
            // TODO do things specific to returning from Position Salary Setting
        }

        salarySettingForm.populateBCAFLines();

        SalarySettingExpansion salarySettingExpansion = salarySettingForm.getSalarySettingExpansion();
        salarySettingExpansion.refresh();

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    public ActionForward addIncumbent(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {

        QuickSalarySettingForm tForm = (QuickSalarySettingForm) form;
        GlobalVariables.getErrorMap().putError(KFSConstants.GLOBAL_MESSAGES, KFSKeyConstants.ERROR_UNIMPLEMENTED, "Add Incumbent");

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    public ActionForward addPosition(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {

        QuickSalarySettingForm tForm = (QuickSalarySettingForm) form;
        GlobalVariables.getErrorMap().putError(KFSConstants.GLOBAL_MESSAGES, KFSKeyConstants.ERROR_UNIMPLEMENTED, "Add Position");

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * @see org.kuali.kfs.module.bc.document.web.struts.SalarySettingBaseAction#loadExpansionScreen(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward loadExpansionScreen(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        QuickSalarySettingForm salarySettingForm = (QuickSalarySettingForm) form;

        // use the passed url parms to get the record from DB
        Map<String, Object> keyMap = salarySettingForm.getKeyMapOfSalarySettingItem();

        SalarySettingExpansion salarySettingExpansion = (SalarySettingExpansion) businessObjectService.findByPrimaryKey(SalarySettingExpansion.class, keyMap);

        if (salarySettingExpansion == null) {
            // TODO need to figure out what to do (if anything) under edit and view mode cases
            // probably nothing, the create new by incumbent or position links would still be shown in edit mode
        }

        salarySettingForm.setSalarySettingExpansion(salarySettingExpansion);

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * perform salary setting by incumbent with the specified funding line
     */
    public ActionForward performIncumbentSalarySetting(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String salarySettingURL = this.buildDetailSalarySettingURL(mapping, form, request, BCConstants.INCUMBENT_SALARY_SETTING_ACTION);

        return new ActionForward(salarySettingURL, true);
    }

    /**
     * perform salary setting by position with the specified funding line
     */
    public ActionForward performPositionSalarySetting(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String salarySettingURL = this.buildDetailSalarySettingURL(mapping, form, request, BCConstants.POSITION_SALARY_SETTING_ACTION);

        return new ActionForward(salarySettingURL, true);
    }

    /**
     * perform salary setting by position with the specified funding line
     */
    public ActionForward toggleAdjustmentMeasurement(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        QuickSalarySettingForm salarySettingForm = (QuickSalarySettingForm) form;

        boolean currentStatus = salarySettingForm.isHideAdjustmentMeasurement();
        salarySettingForm.setHideAdjustmentMeasurement(!currentStatus);

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * save the changes for salary setting
     */
    public ActionForward save(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        QuickSalarySettingForm salarySettingForm = (QuickSalarySettingForm) form;
        SalarySettingExpansion salarySettingExpansion = salarySettingForm.getSalarySettingExpansion();

        salarySettingService.saveSalarySetting(salarySettingExpansion);
        salarySettingExpansion.refresh();

        GlobalVariables.getMessageList().add(BCKeyConstants.MESSAGE_SALARY_SETTING_SAVED);
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    // build the URL for the specified salary setting method
    private String buildDetailSalarySettingURL(ActionMapping mapping, ActionForm form, HttpServletRequest request, String salarySettingAction) {
        QuickSalarySettingForm salarySettingForm = (QuickSalarySettingForm) form;
        SalarySettingExpansion salarySettingExpansion = salarySettingForm.getSalarySettingExpansion();

        Map<String, String> salarySettingMethodAction = this.getSalarySettingMethodActionInfo();

        int indexOfSelectedLine = this.getSelectedLine(request);
        List<PendingBudgetConstructionAppointmentFunding> appointmentFundings = salarySettingExpansion.getPendingBudgetConstructionAppointmentFunding();
        PendingBudgetConstructionAppointmentFunding appointmentFunding = appointmentFundings.get(indexOfSelectedLine);

        // get the base action
        String basePath = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(KFSConstants.APPLICATION_URL_KEY);
        String baseAction = basePath + "/" + salarySettingAction;
        String methodToCall = salarySettingMethodAction.get(salarySettingAction);

        // build the query strings with the information of the selected line
        Properties parameters = new Properties();
        parameters.put(KFSConstants.DISPATCH_REQUEST_PARAMETER, methodToCall);
        parameters.put(KFSConstants.BACK_LOCATION, basePath + mapping.getPath() + ".do");
        parameters.put(KFSPropertyConstants.UNIVERSITY_FISCAL_YEAR, appointmentFunding.getUniversityFiscalYear().toString());
        parameters.put(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE, appointmentFunding.getChartOfAccountsCode());
        parameters.put(KFSPropertyConstants.ACCOUNT_NUMBER, appointmentFunding.getAccountNumber());
        parameters.put(KFSPropertyConstants.SUB_ACCOUNT_NUMBER, appointmentFunding.getSubAccountNumber());
        parameters.put(KFSPropertyConstants.FINANCIAL_OBJECT_CODE, appointmentFunding.getFinancialObjectCode());
        parameters.put(KFSPropertyConstants.FINANCIAL_SUB_OBJECT_CODE, appointmentFunding.getFinancialSubObjectCode());
        parameters.put(KFSPropertyConstants.POSITION_NUMBER, appointmentFunding.getPositionNumber());
        parameters.put(KFSPropertyConstants.EMPLID, appointmentFunding.getEmplid());
        parameters.put(BCConstants.BUDGET_BY_ACCOUNT_MODE, Boolean.TRUE.toString());
        parameters.put(KFSConstants.ADD_LINE_METHOD, Boolean.FALSE.toString());

        // anchor, if it exists
        if (form instanceof KualiForm && StringUtils.isNotEmpty(salarySettingForm.getAnchor())) {
            parameters.put(BCConstants.RETURN_ANCHOR, salarySettingForm.getAnchor());
        }

        // the form object is retrieved and removed upon return by KualiRequestProcessor.processActionForm()
        parameters.put(BCConstants.RETURN_FORM_KEY, GlobalVariables.getUserSession().addObject(form, BCConstants.FORMKEY_PREFIX));

        return UrlFactory.parameterizeUrl(baseAction, parameters);
    }

    // get the pairs of the method and action of salary settings
    private Map<String, String> getSalarySettingMethodActionInfo() {
        Map<String, String> salarySettingMethodAction = new HashMap<String, String>();
        salarySettingMethodAction.put(BCConstants.INCUMBENT_SALARY_SETTING_ACTION, BCConstants.INCUMBENT_SALARY_SETTING_METHOD);
        salarySettingMethodAction.put(BCConstants.POSITION_SALARY_SETTING_ACTION, BCConstants.POSITION_SALARY_SETTING_METHOD);

        return salarySettingMethodAction;
    }
}

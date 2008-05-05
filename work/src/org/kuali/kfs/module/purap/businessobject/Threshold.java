/*
 * Copyright 2008 The Kuali Foundation.
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
package org.kuali.module.purap.bo;

import java.util.LinkedHashMap;

import org.kuali.core.bo.PersistableBusinessObjectBase;
import org.kuali.core.util.KualiDecimal;
import org.kuali.kfs.bo.Options;
import org.kuali.kfs.context.SpringContext;
import org.kuali.module.chart.bo.AcctType;
import org.kuali.module.chart.bo.Chart;
import org.kuali.module.chart.bo.ObjectCode;
import org.kuali.module.chart.bo.Org;
import org.kuali.module.chart.bo.SubFundGroup;
import org.kuali.module.financial.service.UniversityDateService;
import org.kuali.module.vendor.bo.CommodityCode;
import org.kuali.module.vendor.bo.VendorDetail;
import org.kuali.module.vendor.bo.VendorHeader;

public class Threshold extends PersistableBusinessObjectBase {

    private Integer thresholdIdentifier;
    private String chartOfAccountsCode;
    private String accountTypeCode;
    private String subFundGroupCode;
    private String financialObjectCode;
    private String organizationCode;
    private KualiDecimal thresholdAmount;
    private String purchasingCommodityCode;
    
    private Integer vendorHeaderGeneratedIdentifier;
    private Integer vendorDetailAssignedIdentifier;

    private boolean active;
    
    private Chart chart;
    private AcctType accountType;
    private SubFundGroup subFundGroup;
    private ObjectCode financialObject;
    private Org organization;
    private VendorDetail vendorDetail;
    private CommodityCode commodityCode;
    private VendorDetail tempVendorDetail;
    
    public Threshold(){
    }
    
    @Override
    protected LinkedHashMap toStringMapper() {
        
        LinkedHashMap returnMap = new LinkedHashMap();
        
        returnMap.put("thresholdIdentifier",thresholdIdentifier);
        returnMap.put("chartOfAccountsCode",chartOfAccountsCode);
        returnMap.put("accountTypeCode",accountTypeCode);
        returnMap.put("subFundGroupCode",subFundGroupCode);
        returnMap.put("financialObjectCode",financialObjectCode);
        returnMap.put("organizationCode",organizationCode);
        returnMap.put("vendorHeaderGeneratedIdentifier",vendorHeaderGeneratedIdentifier);
        returnMap.put("thresholdAmount",thresholdAmount);
        returnMap.put("active",active);
        
        return returnMap;
    }

    public AcctType getAccountType() {
        return accountType;
    }

    public void setAccountType(AcctType accountType) {
        this.accountType = accountType;
    }

    public String getAccountTypeCode() {
        return accountTypeCode;
    }

    public void setAccountTypeCode(String accountTypeCode) {
        this.accountTypeCode = accountTypeCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Chart getChart() {
        return chart;
    }

    public void setChart(Chart chart) {
        this.chart = chart;
    }

    public String getChartOfAccountsCode() {
        return chartOfAccountsCode;
    }

    public void setChartOfAccountsCode(String chartOfAccountsCode) {
        this.chartOfAccountsCode = chartOfAccountsCode;
    }

    public ObjectCode getFinancialObject() {
        return financialObject;
    }

    public void setFinancialObject(ObjectCode financialObject) {
        this.financialObject = financialObject;
    }

    public String getFinancialObjectCode() {
        return financialObjectCode;
    }

    public void setFinancialObjectCode(String financialObjectCode) {
        this.financialObjectCode = financialObjectCode;
    }

    public Org getOrganization() {
        return organization;
    }

    public void setOrganization(Org organization) {
        this.organization = organization;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public SubFundGroup getSubFundGroup() {
        return subFundGroup;
    }

    public void setSubFundGroup(SubFundGroup subFundGroup) {
        this.subFundGroup = subFundGroup;
    }

    public String getSubFundGroupCode() {
        return subFundGroupCode;
    }

    public void setSubFundGroupCode(String subFundGroupCode) {
        this.subFundGroupCode = subFundGroupCode;
    }

    public Integer getThresholdIdentifier() {
        return thresholdIdentifier;
    }

    public void setThresholdIdentifier(Integer thresholdIdentifier) {
        this.thresholdIdentifier = thresholdIdentifier;
    }

    public Integer getVendorHeaderGeneratedIdentifier() {
        return vendorHeaderGeneratedIdentifier;
    }

    public void setVendorHeaderGeneratedIdentifier(Integer vendorHeaderGeneratedIdentifier) {
        this.vendorHeaderGeneratedIdentifier = vendorHeaderGeneratedIdentifier;
    }

    public KualiDecimal getThresholdAmount() {
        return thresholdAmount;
    }

    public void setThresholdAmount(KualiDecimal thresholdAmount) {
        this.thresholdAmount = thresholdAmount;
    }

    public VendorDetail getVendorDetail() {
        return vendorDetail;
    }

    public void setVendorDetail(VendorDetail vendorDetail) {
        this.vendorDetail = vendorDetail;
    }

    public String getVendorNumber() {
        // using the code from the VendorDetail to generate the vendor number
        if (tempVendorDetail == null){
            tempVendorDetail = new VendorDetail();
        }
        
        tempVendorDetail.setVendorHeaderGeneratedIdentifier(vendorHeaderGeneratedIdentifier);
        tempVendorDetail.setVendorDetailAssignedIdentifier(vendorDetailAssignedIdentifier);
        return tempVendorDetail.getVendorNumber();
    }

    public void setVendorNumber(String vendorNumber) {
        // using the code from the VendorDetail to set the 2 component fields of the vendor number
        if (tempVendorDetail == null){
            tempVendorDetail = new VendorDetail();
        }
        
        tempVendorDetail.setVendorNumber(vendorNumber);
        setVendorHeaderGeneratedIdentifier(tempVendorDetail.getVendorHeaderGeneratedIdentifier());
        setVendorDetailAssignedIdentifier(tempVendorDetail.getVendorDetailAssignedIdentifier());
    }

    public Integer getVendorDetailAssignedIdentifier() {
        return vendorDetailAssignedIdentifier;
    }

    public void setVendorDetailAssignedIdentifier(Integer vendorDetailAssignedIdentifier) {
        this.vendorDetailAssignedIdentifier = vendorDetailAssignedIdentifier;
    }
    
    public String getPurchasingCommodityCode() {
        return purchasingCommodityCode;
    }

    public void setPurchasingCommodityCode(String purchasingCommodityCode) {
        this.purchasingCommodityCode = purchasingCommodityCode;
    }

    public CommodityCode getCommodityCode() {
        return commodityCode;
    }

    public void setCommodityCode(CommodityCode commodityCode) {
        this.commodityCode = commodityCode;
    }
    
}

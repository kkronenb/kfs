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
package org.kuali.kfs.module.purap.document.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.module.purap.businessobject.PurchaseOrderItem;
import org.kuali.kfs.module.purap.dataaccess.B2BDao;
import org.kuali.kfs.module.purap.document.PurchaseOrderDocument;
import org.kuali.kfs.module.purap.document.RequisitionDocument;
import org.kuali.kfs.module.purap.document.service.B2BPurchaseOrderService;
import org.kuali.kfs.module.purap.document.service.RequisitionService;
import org.kuali.kfs.module.purap.exception.B2BConnectionException;
import org.kuali.kfs.module.purap.exception.CxmlParseError;
import org.kuali.kfs.module.purap.util.cxml.B2BParserHelper;
import org.kuali.kfs.module.purap.util.cxml.PurchaseOrderResponse;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.service.ParameterService;
import org.kuali.kfs.vnd.businessobject.ContractManager;
import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kns.service.DateTimeService;
import org.kuali.rice.kns.service.KualiConfigurationService;
import org.kuali.rice.kns.util.ObjectUtils;
import org.kuali.rice.kns.workflow.service.KualiWorkflowDocument;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class B2BPurchaseOrderServiceImpl implements B2BPurchaseOrderService {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(B2BPurchaseOrderServiceImpl.class);

    private B2BDao b2bDao;
    private RequisitionService requisitionService;
    private ParameterService parameterService;

    // FIXME do we really need this?
    private org.kuali.rice.kim.service.PersonService personService;


    /**
     * @see org.kuali.kfs.module.purap.document.service.B2BPurchaseOrderService#sendPurchaseOrder(org.kuali.kfs.module.purap.document.PurchaseOrderDocument)
     */
    public String sendPurchaseOrder(PurchaseOrderDocument purchaseOrder) {
        /*
         * IMPORTANT DESIGN NOTE: We need the contract manager's name, phone number, and e-mail address. B2B orders that don�t
         * qualify to become APO's will have contract managers on the PO, and the ones that DO become APO's will not. We decided to
         * always get the contract manager from the B2B contract associated with the order, and for B2B orders to ignore the
         * contract manager field on the PO. We pull the name and phone number from the contract manager table and get the e-mail
         * address from the user data.
         */

        ContractManager contractManager = purchaseOrder.getVendorContract().getContractManager();
        String contractManagerEmail = getContractManagerEmail(contractManager);

        String vendorDuns = purchaseOrder.getVendorDetail().getVendorDunsNumber();

        RequisitionDocument r = requisitionService.getRequisitionById(purchaseOrder.getRequisitionIdentifier());
        KualiWorkflowDocument reqWorkflowDoc = r.getDocumentHeader().getWorkflowDocument();

//        String password = parameterService.getParameterValue(ParameterConstants.PURCHASING_DOCUMENT.class, PurapParameterConstants.B2BParameters.PO_PASSWORD);
//        String punchoutUrl = parameterService.getParameterValue(ParameterConstants.PURCHASING_DOCUMENT.class, PurapParameterConstants.B2BParameters.PO_URL);
        String password = "p01mport";
        String punchoutUrl = "http://sciwmtest.sciquest.com/invoke/wm.tn/receive";
        LOG.debug("sendPurchaseOrder(): punchoutUrl is " + punchoutUrl);

        String validateErrors = verifyCxmlPOData(purchaseOrder, reqWorkflowDoc.getInitiatorNetworkId(), password, contractManager, contractManagerEmail, vendorDuns);
        if (StringUtils.isEmpty(validateErrors)) {
            return validateErrors;
        }

        StringBuffer transmitErrors = new StringBuffer();

        try {
            LOG.debug("sendPurchaseOrder() Generating cxml");
            String cxml = getCxml(purchaseOrder, reqWorkflowDoc.getInitiatorNetworkId(), password, contractManager, contractManagerEmail, vendorDuns);

            LOG.info("sendPurchaseOrder() Sending cxml\n" + cxml);
            String responseCxml = b2bDao.sendPunchOutRequest(cxml, punchoutUrl);

            LOG.info("sendPurchaseOrder(): Response cXML for po number " + purchaseOrder.getPurapDocumentIdentifier() + ":" + responseCxml);

            PurchaseOrderResponse poResponse = B2BParserHelper.getInstance().parsePurchaseOrderResponse(responseCxml);
            String statusText = poResponse.getStatusText();
            LOG.debug("sendPurchaseOrder(): statusText is " + statusText);
            if ((ObjectUtils.isNull(statusText)) || (!"success".equalsIgnoreCase(statusText.trim()))) {
                LOG.error("sendPurchaseOrder(): PO cXML for po number " + purchaseOrder.getPurapDocumentIdentifier() + " failed sending to vendor: " + statusText);
                transmitErrors.append("Unable to send Purchase Order: " + statusText);

                // find any additional error messages that might have been sent
                List errorMessages = poResponse.getPOResponseErrorMessages();
                if (ObjectUtils.isNotNull(errorMessages) && !errorMessages.isEmpty()) {
                    for (Iterator iter = errorMessages.iterator(); iter.hasNext();) {
                        String errorMessage = (String) iter.next();
                        if (ObjectUtils.isNotNull(errorMessage)) {
                            LOG.error("sendPurchaseOrder(): Error message for po number " + purchaseOrder.getPurapDocumentIdentifier() + ": " + errorMessage);
                            transmitErrors.append("Error sending Purchase Order: " + errorMessage);
                        }
                    }
                }
            }
        }
        catch (B2BConnectionException e) {
            LOG.error("sendPurchaseOrder() Error connecting to b2b", e);
            transmitErrors.append("Connection to vendor failed.");
        }
        catch (CxmlParseError e) {
            LOG.error("sendPurchaseOrder() Error Parsing", e);
            transmitErrors.append("Unable to read cxml returned from vendor.");
        }
        catch (Throwable e) {
            LOG.error("sendPurchaseOrder() Unknown Error", e);
            transmitErrors.append("Unexpected error occurred while attempting to transmit Purchase Order.");
        }

        return transmitErrors.toString();
    }

    /**
     * @see org.kuali.kfs.module.purap.document.service.B2BPurchaseOrderService#getCxml(org.kuali.kfs.module.purap.document.PurchaseOrderDocument,
     *      org.kuali.rice.kim.bo.Person, java.lang.String, org.kuali.kfs.vnd.businessobject.ContractManager,
     *      java.lang.String, java.lang.String)
     */
    public String getCxml(PurchaseOrderDocument purchaseOrder, String requisitionInitiatorId, String password, ContractManager contractManager, String contractManagerEmail, String vendorDuns) {

        StringBuffer cxml = new StringBuffer();
        Date d = SpringContext.getBean(DateTimeService.class).getCurrentDate();
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss.sss");

        cxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        cxml.append("<!DOCTYPE cXML SYSTEM \"http://xml.cXML.org/schemas/cXML/1.2.019/cXML.dtd\">\n");
        // payloadID - can be whatever you would like it to be. Just make it unique.
        cxml.append("<cXML payloadID=\"test@kuali.org\" timestamp=\"").append(date.format(d)).append("T").append(time.format(d)).append("+03:00").append("\" xml:lang=\"en-US\">\n");
        cxml.append("  <Header>\n");
        cxml.append("    <From>\n");
        cxml.append("      <Credential domain=\"NetworkUserId\">\n");
        cxml.append("        <Identity>").append(requisitionInitiatorId.toUpperCase()).append("</Identity>\n");
        cxml.append("      </Credential>\n");
        cxml.append("    </From>\n");
        cxml.append("    <To>\n");
        cxml.append("      <Credential domain=\"DUNS\">\n");
        cxml.append("        <Identity>").append(vendorDuns).append("</Identity>\n");
        cxml.append("      </Credential>\n");
        cxml.append("    </To>\n");
        cxml.append("    <Sender>\n");
        cxml.append("      <Credential domain=\"NetworkUserId\">\n");
        cxml.append("        <Identity>KFS</Identity>\n");
        cxml.append("        <SharedSecret>").append(password).append("</SharedSecret>\n");
        cxml.append("      </Credential>\n");
        cxml.append("      <UserAgent>Ariba.com Network V1.0</UserAgent>\n");
        cxml.append("    </Sender>\n");
        cxml.append("  </Header>\n");
        // set deployment mode to test if not in production
        if (isProduction()) {
            cxml.append("  <Request>\n");
        } else {
            cxml.append("  <Request deploymentMode=\"test\">\n");
        }
        cxml.append("    <OrderRequest>\n");
        cxml.append("      <OrderRequestHeader orderID=\"").append(purchaseOrder.getPurapDocumentIdentifier()).append("\" orderDate=\"").append(date.format(d)).append("\" type=\"new\">\n");
        cxml.append("        <Total>\n");
        cxml.append("          <Money currency=\"USD\">").append(purchaseOrder.getTotalDollarAmount()).append("</Money>\n");
        cxml.append("        </Total>\n");
        cxml.append("        <ShipTo>\n");
        cxml.append("          <Address>\n");
        cxml.append("            <Name xml:lang=\"en\">Kuali</Name>\n");
        cxml.append("            <PostalAddress name=\"defaul\">\n");
        cxml.append("              <DeliverTo>").append(purchaseOrder.getDeliveryToName().trim()).append("</DeliverTo>\n");
        if (ObjectUtils.isNull(purchaseOrder.getInstitutionContactEmailAddress())) {
            cxml.append("              <DeliverTo><![CDATA[").append(purchaseOrder.getDeliveryToEmailAddress()).append("]]></DeliverTo>\n");
        }
        else {
            cxml.append("              <DeliverTo><![CDATA[").append(purchaseOrder.getInstitutionContactEmailAddress()).append("]]></DeliverTo>\n");
        }
        if (ObjectUtils.isNotNull(purchaseOrder.getDeliveryBuildingCode())) {
            cxml.append("              <DeliverTo><![CDATA[").append(purchaseOrder.getDeliveryBuildingCode()).append("]]></DeliverTo>\n");
        }
        cxml.append("              <Street><![CDATA[").append(purchaseOrder.getDeliveryBuildingLine1Address().trim()).append("]]></Street>\n");
        if (!StringUtils.isEmpty(purchaseOrder.getDeliveryBuildingLine2Address())) {
            cxml.append("              <Street><![CDATA[").append(purchaseOrder.getDeliveryBuildingLine2Address().trim()).append("]]></Street>\n");
        }
        if (!StringUtils.isEmpty(purchaseOrder.getDeliveryBuildingRoomNumber())) {
            cxml.append("              <Street><![CDATA[").append(purchaseOrder.getDeliveryBuildingRoomNumber().trim()).append("]]></Street>\n");
        }
        cxml.append("              <City><![CDATA[").append(purchaseOrder.getDeliveryCityName().trim()).append("]]></City>\n");
        cxml.append("              <State>").append(purchaseOrder.getDeliveryStateCode()).append("</State>\n");
        cxml.append("              <PostalCode>").append(purchaseOrder.getDeliveryPostalCode()).append("</PostalCode>\n");
        cxml.append("              <Country isoCountryCode=\"").append(purchaseOrder.getDeliveryCountryCode()).append("\">").append(purchaseOrder.getDeliveryCountryName()).append("</Country>\n");
        cxml.append("            </PostalAddress>\n");
        cxml.append("          </Address>\n");
        cxml.append("        </ShipTo>\n");
        cxml.append("        <BillTo>\n");
        cxml.append("          <Address>\n");
        cxml.append("            <Name xml:lang=\"en\"><![CDATA[").append(purchaseOrder.getBillingName().trim()).append("]]></Name>\n");
        cxml.append("            <PostalAddress name=\"defaul\">\n");
        cxml.append("              <Street><![CDATA[").append(purchaseOrder.getBillingLine1Address().trim()).append("]]></Street>\n");
        if (!StringUtils.isEmpty(purchaseOrder.getBillingLine2Address())) {
            cxml.append("              <Street><![CDATA[").append(purchaseOrder.getBillingLine2Address().trim()).append("]]></Street>\n");
        }
        cxml.append("              <City><![CDATA[").append(purchaseOrder.getBillingCityName().trim()).append("]]></City>\n");
        cxml.append("              <State>").append(purchaseOrder.getBillingStateCode()).append("</State>\n");
        cxml.append("              <PostalCode>").append(purchaseOrder.getBillingPostalCode()).append("</PostalCode>\n");
        cxml.append("              <Country isoCountryCode=\"").append(purchaseOrder.getBillingCountryCode()).append("\">").append(purchaseOrder.getBillingCountryName()).append("</Country>\n");
        cxml.append("            </PostalAddress>\n");
        cxml.append("          </Address>\n");
        cxml.append("        </BillTo>\n");
        cxml.append("        <Tax>\n");
        cxml.append("          <Money currency=\"USD\">").append(purchaseOrder.getTotalTaxAmount()).append("</Money>\n");
        cxml.append("          <Description xml:lang=\"en\">").append("tax description").append("</Description>\n");
        cxml.append("        </Tax>\n");
        cxml.append("      </OrderRequestHeader>\n");
        
        for (Object tmpPoi : purchaseOrder.getItems()) {
            PurchaseOrderItem poi = (PurchaseOrderItem) tmpPoi;

            cxml.append("      <ItemOut quantity=\"").append(poi.getItemQuantity()).append("\" lineNumber=\"").append(poi.getItemLineNumber()).append("\">\n");
            cxml.append("        <ItemID>\n");
            cxml.append("          <SupplierPartID><![CDATA[").append(poi.getItemCatalogNumber()).append("]]></SupplierPartID>\n");
            if (ObjectUtils.isNotNull(poi.getItemAuxiliaryPartIdentifier())) {
                cxml.append("          <SupplierPartAuxiliaryID><![CDATA[").append(poi.getItemAuxiliaryPartIdentifier()).append("]]></SupplierPartAuxiliaryID>\n");
            }
            cxml.append("        </ItemID>\n");
            cxml.append("        <ItemDetail>\n");
            cxml.append("          <UnitPrice>\n");
            cxml.append("            <Money currency=\"USD\">").append(poi.getItemUnitPrice()).append("</Money>\n");
            cxml.append("          </UnitPrice>\n");
            cxml.append("          <Description xml:lang=\"en\"><![CDATA[").append(poi.getItemDescription()).append("]]></Description>\n"); // Required.
            cxml.append("          <UnitOfMeasure><![CDATA[").append(poi.getItemUnitOfMeasureCode()).append("]]></UnitOfMeasure>\n");
            cxml.append("          <Classification domain=\"UNSPSC\"></Classification>\n");
            if (poi.getExternalOrganizationB2bProductTypeName().equals("Punchout")) {
                cxml.append("          <ManufacturerPartID></ManufacturerPartID>\n");
            }
            else {
                cxml.append("          <ManufacturerPartID>").append(poi.getExternalOrganizationB2bProductReferenceNumber()).append("</ManufacturerPartID>\n");
            }
            cxml.append("          <ManufacturerName>").append(poi.getExternalOrganizationB2bProductTypeName()).append("</ManufacturerName>\n");
            cxml.append("        </ItemDetail>\n");
            cxml.append("      </ItemOut>\n");
        }

        cxml.append("    </OrderRequest>\n");
        cxml.append("  </Request>\n");
        cxml.append("</cXML>");

        LOG.debug("getCxml(): cXML for po number " + purchaseOrder.getPurapDocumentIdentifier() + ":\n" + cxml.toString());

        return cxml.toString();
    }

    /**
     * @see org.kuali.kfs.module.purap.document.service.B2BPurchaseOrderService#verifyCxmlPOData(org.kuali.kfs.module.purap.document.PurchaseOrderDocument,
     *      org.kuali.rice.kim.bo.Person, java.lang.String, org.kuali.kfs.vnd.businessobject.ContractManager,
     *      java.lang.String, java.lang.String)
     */
    public String verifyCxmlPOData(PurchaseOrderDocument purchaseOrder, String requisitionInitiatorId, String password, ContractManager contractManager, String contractManagerEmail, String vendorDuns) {
        StringBuffer errors = new StringBuffer();

        if (ObjectUtils.isNull(purchaseOrder)) {
            LOG.error("verifyCxmlPOData()  The Purchase Order is null.");
            errors.append("Error occurred retrieving Purchase Order\n");
            return errors.toString();
        }
        if (ObjectUtils.isNull(contractManager)) {
            LOG.error("verifyCxmlPOData()  The contractManager is null.");
            errors.append("Error occurred retrieving Contract Manager\n");
            return errors.toString();
        }
        if (StringUtils.isEmpty(password)) {
            LOG.error("verifyCxmlPOData()  The B2B PO password is required for the cXML PO but is missing.");
            errors.append("Missing Data: B2B PO password\n");
        }
        if (ObjectUtils.isNull(purchaseOrder.getPurapDocumentIdentifier())) {
            LOG.error("verifyCxmlPOData()  The purchase order Id is required for the cXML PO but is missing.");
            errors.append("Missing Data: Purchase Order ID\n");
        }
        if (StringUtils.isEmpty(requisitionInitiatorId)) {
            LOG.error("verifyCxmlPOData()  The requisition initiator Network Id is required for the cXML PO but is missing.");
            errors.append("Missing Data: Requisition Initiator NetworkId\n");
        }
        if (ObjectUtils.isNull(purchaseOrder.getPurchaseOrderCreateTimestamp())) {
            LOG.error("verifyCxmlPOData()  The PO create date is required for the cXML PO but is null.");
            errors.append("Create Date\n");
        }
        if (StringUtils.isEmpty(contractManager.getContractManagerPhoneNumber())) {
            LOG.error("verifyCxmlPOData()  The contract manager phone number is required for the cXML PO but is missing.");
            errors.append("Missing Data: Contract Manager Phone Number\n");
        }
        if (StringUtils.isEmpty(contractManager.getContractManagerName())) {
            LOG.error("verifyCxmlPOData()  The contract manager name is required for the cXML PO but is missing.");
            errors.append("Missing Data: Contract Manager Name\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryCampusCode())) {
            LOG.error("verifyCxmlPOData()  The Delivery Campus Code is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery Campus Code\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getBillingName())) {
            LOG.error("verifyCxmlPOData()  The Delivery Billing Name is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery Billing Name\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getBillingLine1Address())) {
            LOG.error("verifyCxmlPOData()  The Billing Line 1 Address is required for the cXML PO but is missing.");
            errors.append("Missing Data: Billing Line 1 Address\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getBillingLine2Address())) {
            LOG.error("verifyCxmlPOData()  The Billing Line 2 Address is required for the cXML PO but is missing.");
            errors.append("Missing Data: Billing Line 2 Address\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getBillingCityName())) {
            LOG.error("verifyCxmlPOData()  The Billing Address City Name is required for the cXML PO but is missing.");
            errors.append("Missing Data: Billing Address City Name\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getBillingStateCode())) {
            LOG.error("verifyCxmlPOData()  The Billing Address State Code is required for the cXML PO but is missing.");
            errors.append("Missing Data: Billing Address State Code\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getBillingPostalCode())) {
            LOG.error("verifyCxmlPOData()  The Billing Address Postal Code is required for the cXML PO but is missing.");
            errors.append("Missing Data: Billing Address Postal Code\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryToName())) {
            LOG.error("verifyCxmlPOData()  The Delivery To Name is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery To Name\n");
        }
        if (StringUtils.isEmpty(contractManagerEmail)) {
            LOG.error("verifyCxmlPOData()  The Contract Manager Email is required for the cXML PO but is missing.");
            errors.append("Missing Data: Contract Manager Email\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryToEmailAddress())) {
            LOG.error("verifyCxmlPOData()  The Requesting Person Email Address is required for the cXML PO but is missing.");
            errors.append("Missing Data: Requesting Person Email Address\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryToPhoneNumber())) {
            LOG.error("verifyCxmlPOData()  The Requesting Person Phone Number is required for the cXML PO but is missing.");
            errors.append("Missing Data: Requesting Person Phone Number\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryBuildingCode())) {
            LOG.error("verifyCxmlPOData()  The Delivery Building Code is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery Building Code\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryBuildingLine1Address())) {
            LOG.error("verifyCxmlPOData()  The Delivery Line 1 Address is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery Line 1 Address\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryToName())) {
            LOG.error("verifyCxmlPOData()  The Delivery To Name is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery To Name\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryCityName())) {
            LOG.error("verifyCxmlPOData()  The Delivery City Name is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery City Name\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryStateCode())) {
            LOG.error("verifyCxmlPOData()  The Delivery State is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery State\n");
        }
        if (StringUtils.isEmpty(purchaseOrder.getDeliveryPostalCode())) {
            LOG.error("verifyCxmlPOData()  The Delivery Postal Code is required for the cXML PO but is missing.");
            errors.append("Missing Data: Delivery Postal Code\n");
        }
        // FIXME (hjs) Commented out because this is being hard-coded as US.
        // if (StringUtils.isEmpty(purchaseOrder.getDeliveryCountryCode())) {
        // LOG.error("verifyCxmlPOData() The Delivery Country is required for the cXML PO but is missing.");
        // errors.append("Missing Data: Delivery Country\n");
        // }

        // verify item data
        List detailList = purchaseOrder.getItems();
        for (Iterator iter = detailList.iterator(); iter.hasNext();) {
            PurchaseOrderItem poi = (PurchaseOrderItem) iter.next();
            if (ObjectUtils.isNotNull(poi.getItemType()) && poi.getItemType().isLineItemIndicator()) {
                if (ObjectUtils.isNull(poi.getItemLineNumber())) {
                    LOG.error("verifyCxmlPOData()  The Item Line Number is required for the cXML PO but is missing.");
                    errors.append("Missing Data: Item Line Number\n");
                }
                if (StringUtils.isEmpty(poi.getItemCatalogNumber())) {
                    LOG.error("verifyCxmlPOData()  The Catalog Number for item number " + poi.getItemLineNumber() + " is required for the cXML PO but is missing.");
                    errors.append("Missing Data: Item#" + poi.getItemLineNumber() + " - Catalog Number\n");
                }
                if (StringUtils.isEmpty(poi.getItemDescription())) {
                    LOG.error("verifyCxmlPOData()  The Description for item number " + poi.getItemLineNumber() + " is required for the cXML PO but is missing.");
                    errors.append("Missing Data: Item#" + poi.getItemLineNumber() + " - Description\n");
                }
                if (StringUtils.isEmpty(poi.getItemUnitOfMeasureCode())) {
                    LOG.error("verifyCxmlPOData()  The Unit Of Measure Code for item number " + poi.getItemLineNumber() + " is required for the cXML PO but is missing.");
                    errors.append("Missing Data: Item#" + poi.getItemLineNumber() + " - Unit Of Measure\n");
                }
                if (StringUtils.isEmpty(poi.getExternalOrganizationB2bProductReferenceNumber())) {
                    LOG.error("verifyCxmlPOData()  The External Org B2B Product Reference Number for item number " + poi.getItemLineNumber() + " is required for the cXML PO but is missing.");
                    errors.append("Missing Data: Item#" + poi.getItemLineNumber() + " - External Org B2B Product Reference Number\n");
                }
                if (StringUtils.isEmpty(poi.getExternalOrganizationB2bProductTypeName())) {
                    LOG.error("verifyCxmlPOData()  The External Org B2B Product Type Name for item number " + poi.getItemLineNumber() + " is required for the cXML PO but is missing.");
                    errors.append("Missing Data: Item#" + poi.getItemLineNumber() + " - External Org B2B Product Type Name\n");
                }
                if (poi.getItemQuantity() == null) {
                    LOG.error("verifyCxmlPOData()  The Order Quantity for item number " + poi.getItemLineNumber() + " is required for the cXML PO but is missing.");
                    errors.append("Missing Data: Item#" + poi.getItemLineNumber() + " - Order Quantity\n");
                }
                if (poi.getItemUnitPrice() == null) {
                    LOG.error("verifyCxmlPOData()  The Unit Price for item number " + poi.getItemLineNumber() + " is required for the cXML PO but is missing.");
                    errors.append("Missing Data: Item#" + poi.getItemLineNumber() + " - Unit Price\n");
                }
            }
        } // end item looping

        return errors.toString();
    } 

    /**
     * Retrieve the Contract Manager's email
     */
    private String getContractManagerEmail(ContractManager cm) {
        Person contractManager = personService.getPerson(cm.getContractManagerUserIdentifier());
        if (contractManager == null) {
            LOG.error("getContractManagerEmail(): returning null.");
            return null;
        } else {
            return contractManager.getEmailAddress();
        }
    }

    public void setPersonService(org.kuali.rice.kim.service.PersonService personService) {
        this.personService = personService;
    }

    public void setRequisitionService(RequisitionService requisitionService) {
        this.requisitionService = requisitionService;
    }

    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public void setB2bDao(B2BDao b2bDao) {
        this.b2bDao = b2bDao;
    }

    /**
     * Throws an exception if running on production
     */
    private boolean isProduction() {
        KualiConfigurationService kualiConfigurationService = SpringContext.getBean(KualiConfigurationService.class);

        return StringUtils.equals(kualiConfigurationService.getPropertyString(KFSConstants.PROD_ENVIRONMENT_CODE_KEY), kualiConfigurationService.getPropertyString(KFSConstants.ENVIRONMENT_KEY));
    }

}


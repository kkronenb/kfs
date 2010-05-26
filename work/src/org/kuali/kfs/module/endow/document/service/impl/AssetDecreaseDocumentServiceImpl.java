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
package org.kuali.kfs.module.endow.document.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.module.endow.EndowConstants;
import org.kuali.kfs.module.endow.businessobject.EndowmentTransactionLine;
import org.kuali.kfs.module.endow.businessobject.EndowmentTransactionSecurity;
import org.kuali.kfs.module.endow.businessobject.EndowmentTransactionTaxLotLine;
import org.kuali.kfs.module.endow.businessobject.HoldingTaxLot;
import org.kuali.kfs.module.endow.businessobject.Security;
import org.kuali.kfs.module.endow.document.AssetDecreaseDocument;
import org.kuali.kfs.module.endow.document.service.AssetDecreaseDocumentService;
import org.kuali.kfs.module.endow.document.service.HoldingTaxLotService;
import org.kuali.kfs.module.endow.document.service.KEMService;
import org.kuali.kfs.module.endow.document.service.SecurityService;
import org.kuali.kfs.module.endow.util.KEMCalculationRoundingHelper;
import org.kuali.kfs.sys.service.impl.KfsParameterConstants;
import org.kuali.rice.kns.service.ParameterService;
import org.kuali.rice.kns.util.KualiInteger;
import org.kuali.rice.kns.util.ObjectUtils;

public class AssetDecreaseDocumentServiceImpl implements AssetDecreaseDocumentService {

    private HoldingTaxLotService taxLotService;
    private SecurityService securityService;
    private KEMService kemService;
    private ParameterService parameterService;


    /**
     * @see org.kuali.kfs.module.endow.document.service.AssetDecreaseDocumentService#updateTransactionLineTaxLots(boolean,
     *      org.kuali.kfs.module.endow.document.AssetDecreaseDocument,
     *      org.kuali.kfs.module.endow.businessobject.EndowmentTransactionLine)
     */
    public void updateTransactionLineTaxLots(boolean isUpdate, AssetDecreaseDocument assetDecreaseDocument, EndowmentTransactionLine transLine) {

        EndowmentTransactionSecurity endowmentTransactionSecurity = assetDecreaseDocument.getSourceTransactionSecurity();
        String accountingMethod = parameterService.getParameterValue(KfsParameterConstants.ENDOWMENT_ALL.class, EndowConstants.EndowmentSystemParameter.TAX_LOTS_ACCOUNTING_METHOD);
        Security security = securityService.getByPrimaryKey(endowmentTransactionSecurity.getSecurityID());

        if (ObjectUtils.isNotNull(security)) {
            if (EndowConstants.TaxLotsAccountingMethodOptions.AVERAGE_BALANCE.equalsIgnoreCase(accountingMethod) || (EndowConstants.TaxLotsAccountingMethodOptions.FIFO.equalsIgnoreCase(accountingMethod) && !security.getClassCode().isTaxLotIndicator())) {
                if (EndowConstants.TransactionSubTypeCode.CASH.equalsIgnoreCase(assetDecreaseDocument.getTransactionSubTypeCode())) {
                    updateTaxLotsForAccountingMethodAverageBalance(true,isUpdate, assetDecreaseDocument, endowmentTransactionSecurity, transLine);
                }
                if (EndowConstants.TransactionSubTypeCode.NON_CASH.equalsIgnoreCase(assetDecreaseDocument.getTransactionSubTypeCode())) {
                    updateTaxLotsForAccountingMethodAverageBalance(false,isUpdate, assetDecreaseDocument, endowmentTransactionSecurity, transLine);
                }
            }

            if ((EndowConstants.TaxLotsAccountingMethodOptions.FIFO.equalsIgnoreCase(accountingMethod) || EndowConstants.TaxLotsAccountingMethodOptions.LIFO.equalsIgnoreCase(accountingMethod)) && security.getClassCode().isTaxLotIndicator()) {
                boolean isFIFO = EndowConstants.TaxLotsAccountingMethodOptions.FIFO.equalsIgnoreCase(accountingMethod);

                if (EndowConstants.TransactionSubTypeCode.CASH.equalsIgnoreCase(assetDecreaseDocument.getTransactionSubTypeCode())) {
                    updateTaxLotsForAccountingMethodFIFOorLIFO(true,isUpdate, isFIFO, assetDecreaseDocument, endowmentTransactionSecurity, transLine);
                }
                if (EndowConstants.TransactionSubTypeCode.NON_CASH.equalsIgnoreCase(assetDecreaseDocument.getTransactionSubTypeCode())) {
                    updateTaxLotsForAccountingMethodFIFOorLIFO(false, isUpdate,isFIFO, assetDecreaseDocument, endowmentTransactionSecurity, transLine);
                }
            }
        }

    }


    /**
     * Updates the tax lots for the transaction line in the case the accounting method is Average Balance.
     * 
     * @param isSubTypeCash
     * @param assetDecreaseDocument
     * @param endowmentTransactionSecurity
     * @param transLine
     */
    private void updateTaxLotsForAccountingMethodAverageBalance(boolean isSubTypeCash, boolean isUpdate, AssetDecreaseDocument assetDecreaseDocument, EndowmentTransactionSecurity endowmentTransactionSecurity, EndowmentTransactionLine transLine) {

        BigDecimal transactionUnits = transLine.getTransactionUnits().bigDecimalValue();
        BigDecimal totalTaxLotsUnits = BigDecimal.ZERO;
        BigDecimal transactionAmount = BigDecimal.ZERO;
        BigDecimal perUnitValue = BigDecimal.ZERO;

        List<HoldingTaxLot> holdingTaxLots = new ArrayList<HoldingTaxLot>();

        if (!isUpdate) {
            transLine.getTaxLotLines().clear();
            holdingTaxLots = taxLotService.getAllTaxLots(transLine.getKemid(), endowmentTransactionSecurity.getSecurityID(), endowmentTransactionSecurity.getRegistrationCode(), transLine.getTransactionIPIndicatorCode());
        }
        else {
            List<EndowmentTransactionTaxLotLine> existingTransactionLines = transLine.getTaxLotLines();
            for (EndowmentTransactionTaxLotLine endowmentTransactionTaxLotLine : existingTransactionLines) {
                HoldingTaxLot holdingTaxLot = taxLotService.getByPrimaryKey(transLine.getKemid(), endowmentTransactionSecurity.getSecurityID(), endowmentTransactionSecurity.getRegistrationCode(), endowmentTransactionTaxLotLine.getTransactionHoldingLotNumber(), transLine.getTransactionIPIndicatorCode());

                if (ObjectUtils.isNotNull(holdingTaxLot)) {
                    holdingTaxLots.add(holdingTaxLot);
                }
            }

            transLine.getTaxLotLines().clear();
        }

        Map<KualiInteger, EndowmentTransactionTaxLotLine> decreaseHoldingTaxLots = new HashMap<KualiInteger, EndowmentTransactionTaxLotLine>();
        if (isSubTypeCash) {
            transactionAmount = transLine.getTransactionAmount().bigDecimalValue();
            // 1. Calculate per unit value
            perUnitValue = KEMCalculationRoundingHelper.divide(transactionAmount, transactionUnits, 5);
        }

        if (holdingTaxLots != null && holdingTaxLots.size() > 0) {
            // compute the total number of units for tax lots
            for (HoldingTaxLot holdingTaxLot : holdingTaxLots) {
                totalTaxLotsUnits = totalTaxLotsUnits.add(holdingTaxLot.getUnits());

            }

            boolean keepIntegers = true;
            for (HoldingTaxLot holdingTaxLot : holdingTaxLots) {
                EndowmentTransactionTaxLotLine taxLotLine = new EndowmentTransactionTaxLotLine();
                taxLotLine.setDocumentLineNumber(transLine.getTransactionLineNumber());
                // 2. Calculate percentage each lot contains of the total units
                BigDecimal percentage = KEMCalculationRoundingHelper.divide(transLine.getTransactionUnits().bigDecimalValue(), totalTaxLotsUnits, 5);

                // 3. Calculate the number of units to be transacted in each lot
                // check if percentage and tax lot units are integers
                BigDecimal lotUnits = BigDecimal.ZERO;
                try {
                    int lotUnitsInt = holdingTaxLot.getUnits().intValueExact();
                }
                catch (ArithmeticException ex) {
                    keepIntegers = false;

                }
                lotUnits = KEMCalculationRoundingHelper.multiply(percentage, holdingTaxLot.getUnits(), 5);
                taxLotLine.setLotUnits(lotUnits);

                // 5. Calculate original unit value for each tax lot
                BigDecimal originalUnitValue = KEMCalculationRoundingHelper.divide(holdingTaxLot.getCost(), holdingTaxLot.getUnits(), 5);

                // 6. Calculate original cost
                BigDecimal originalCost = KEMCalculationRoundingHelper.multiply(lotUnits, originalUnitValue, 2);
                taxLotLine.setLotHoldingCost(originalCost);

                if (isSubTypeCash) {

                    // 4. Calculate the value received for units sold in each tax lot
                    BigDecimal valueReceived = KEMCalculationRoundingHelper.multiply(lotUnits, perUnitValue, 2);

                    // 7. Calculate Gain or loss
                    calculateGainLoss(holdingTaxLot, taxLotLine, valueReceived, originalCost);
                }

                // set tax lot line lot number and acquired date
                taxLotLine.setTransactionHoldingLotNumber(holdingTaxLot.getLotNumber().intValue());
                taxLotLine.setLotAcquiredDate(holdingTaxLot.getAcquiredDate());

                // add the new tax lot line to the transaction line tax lots
                transLine.getTaxLotLines().add(taxLotLine);
            }

            // Adjust the number of units if the total is different from the transaction line units
            adjustUnitsNumberForAverageBalance(transLine, keepIntegers);

        }
    }

    /**
     * Calculates Gain or Loss for the tax lot line and determines if it's long term or short term
     * 
     * @param holdingTaxLot
     * @param taxLotLine
     * @param valueReceived
     * @param originalCost
     */
    private void calculateGainLoss(HoldingTaxLot holdingTaxLot, EndowmentTransactionTaxLotLine taxLotLine, BigDecimal valueReceived, BigDecimal originalCost) {
        BigDecimal gainOrLoss = valueReceived.subtract(originalCost);
        gainOrLoss = gainOrLoss.setScale(2, BigDecimal.ROUND_HALF_UP);

        // Determine if short or long term gain/loss
        Date currentDate = kemService.getCurrentDate();
        Date acquiredDate = holdingTaxLot.getAcquiredDate();

        Calendar calendarAcquiredDate = Calendar.getInstance();
        calendarAcquiredDate.setTime(acquiredDate);
        calendarAcquiredDate.add(Calendar.MONTH, 6);

        if (calendarAcquiredDate.getTime().before(currentDate)) {
            // long term gain/loss
            taxLotLine.setLotLongTermGainLoss(gainOrLoss);
        }
        // short term gain/loss
        else {
            taxLotLine.setLotShortTermGainLoss(gainOrLoss);
        }
    }

    /**
     * Adjusts the number of units if the total is different from the transaction line units.
     * 
     * @param transLine
     * @param keepIntegers
     */
    private void adjustUnitsNumberForAverageBalance(EndowmentTransactionLine transLine, boolean keepIntegers) {
        // Adjust the number of units if the total is different from the transaction line units
        BigDecimal totalComputedTaxLotUnits = BigDecimal.ZERO;
        EndowmentTransactionTaxLotLine oldestTaxLotLine = null;

        if (transLine.getTaxLotLines() != null && transLine.getTaxLotLines().size() > 0) {
            for (EndowmentTransactionTaxLotLine taxLotLine : transLine.getTaxLotLines()) {
                BigDecimal lotUnits = taxLotLine.getLotUnits();

                // IF all original units per lot are integers (no decimal values), the result is rounded to the nearest
                // integer and stored with the five decimals as zero. If the original units are not all integers, then the
                // value is rounded to five decimals and stored as the five decimal values.
                if (keepIntegers) {
                    lotUnits = lotUnits.setScale(0, BigDecimal.ROUND_HALF_UP);
                    lotUnits = lotUnits.setScale(5);
                }

                // calculate the total number of units to be decreased
                totalComputedTaxLotUnits = totalComputedTaxLotUnits.add(lotUnits);

                // keep the tax lot with the oldest acquired date so that we can adjust the units for that one in case the
                // number of units needs and adjustment
                if (oldestTaxLotLine != null) {
                    if (oldestTaxLotLine.getLotAcquiredDate().after(taxLotLine.getLotAcquiredDate())) {
                        oldestTaxLotLine = taxLotLine;
                    }
                }
                else {
                    oldestTaxLotLine = taxLotLine;
                }
            }
        }

        if (totalComputedTaxLotUnits.compareTo(transLine.getTransactionUnits().bigDecimalValue()) == -1) {
            BigDecimal difUnits = transLine.getTransactionUnits().bigDecimalValue().subtract(totalComputedTaxLotUnits);
            oldestTaxLotLine.setLotUnits(oldestTaxLotLine.getLotUnits().add(difUnits));
        }
    }


    /**
     * Updates the tax lots for the transaction line in the case the accounting method is FIFO or LIFO.
     * 
     * @param isSubTypeCash
     * @param isFIFO
     * @param assetDecreaseDocument
     * @param endowmentTransactionSecurity
     * @param transLine
     */
    private void updateTaxLotsForAccountingMethodFIFOorLIFO(boolean isSubTypeCash, boolean isUpdate, boolean isFIFO, AssetDecreaseDocument assetDecreaseDocument, EndowmentTransactionSecurity endowmentTransactionSecurity, EndowmentTransactionLine transLine) {
        BigDecimal transactionUnits = transLine.getTransactionUnits().bigDecimalValue();
        BigDecimal transactionAmount = BigDecimal.ZERO;
        BigDecimal perUnitVal = BigDecimal.ZERO;
        List<HoldingTaxLot> holdingTaxLots = new ArrayList<HoldingTaxLot>();

        if (!isUpdate) {
            transLine.getTaxLotLines().clear();
            holdingTaxLots = taxLotService.getAllTaxLotsOrderByAcquiredDate(transLine.getKemid(), endowmentTransactionSecurity.getSecurityID(), endowmentTransactionSecurity.getRegistrationCode(), transLine.getTransactionIPIndicatorCode(), isFIFO);
        }
        else {
            List<EndowmentTransactionTaxLotLine> existingTransactionLines = transLine.getTaxLotLines();
            for (EndowmentTransactionTaxLotLine endowmentTransactionTaxLotLine : existingTransactionLines) {
                HoldingTaxLot holdingTaxLot = taxLotService.getByPrimaryKey(transLine.getKemid(), endowmentTransactionSecurity.getSecurityID(), endowmentTransactionSecurity.getRegistrationCode(), endowmentTransactionTaxLotLine.getTransactionHoldingLotNumber(), transLine.getTransactionIPIndicatorCode());

                if (ObjectUtils.isNotNull(holdingTaxLot)) {
                    holdingTaxLots.add(holdingTaxLot);
                }
            }
            transLine.getTaxLotLines().clear();
        }

        Map<KualiInteger, EndowmentTransactionTaxLotLine> decreaseHoldingTaxLots = new HashMap<KualiInteger, EndowmentTransactionTaxLotLine>();

        if (isSubTypeCash) {
            transactionAmount = transLine.getTransactionAmount().bigDecimalValue();
            // 1. Compute per unit value
            perUnitVal = KEMCalculationRoundingHelper.divide(transactionAmount, transactionUnits, 5);
        }

        BigDecimal remainingUnits = transactionUnits;

        if (holdingTaxLots != null && holdingTaxLots.size() > 0) {
            // compute the total number of units for tax lots
            for (HoldingTaxLot holdingTaxLot : holdingTaxLots) {
                EndowmentTransactionTaxLotLine taxLotLine = new EndowmentTransactionTaxLotLine();
                taxLotLine.setDocumentLineNumber(transLine.getTransactionLineNumber());

                // 2. Set the lot units

                // if transaction units is greater than the holding tax lot units then take all the holding tax lot units
                if (remainingUnits.compareTo(holdingTaxLot.getUnits()) == 1) {
                    taxLotLine.setLotUnits(holdingTaxLot.getUnits());
                    remainingUnits = remainingUnits.subtract(holdingTaxLot.getUnits());
                }
                // if transaction units is less than the holding tax lot units then take just the remaining number of units
                else {
                    taxLotLine.setLotUnits(remainingUnits);
                    remainingUnits = BigDecimal.ZERO;
                }

                // 4. Calculate the original unit value: cost/units
                BigDecimal originalUnitVal = KEMCalculationRoundingHelper.divide(holdingTaxLot.getCost(), holdingTaxLot.getUnits(), 5);

                // 5. Calculate original cost
                BigDecimal originalCost = KEMCalculationRoundingHelper.multiply(originalUnitVal, taxLotLine.getLotUnits(), 2);

                // set the tax lot holding cost
                taxLotLine.setLotHoldingCost(originalCost);

                // if sub type cash then the
                if (isSubTypeCash) {
                    // 3. Calculate the value received for the units sold and round to 2 decimals
                    BigDecimal receivedValue = KEMCalculationRoundingHelper.multiply(perUnitVal, taxLotLine.getLotUnits(), 2);
                    // 6. Calculate the gain or loss
                    calculateGainLoss(holdingTaxLot, taxLotLine, receivedValue, originalCost);
                }

                // set the lot number
                taxLotLine.setTransactionHoldingLotNumber(holdingTaxLot.getLotNumber().intValue());
                // set the lot acquired date
                taxLotLine.setLotAcquiredDate(holdingTaxLot.getAcquiredDate());

                transLine.getTaxLotLines().add(taxLotLine);

                if (remainingUnits.compareTo(BigDecimal.ZERO) == 0) {
                    break;
                }
            }
        }
    }

    /**
     * Gets the taxLotService.
     * 
     * @return taxLotService
     */
    public HoldingTaxLotService getTaxLotService() {
        return taxLotService;
    }

    /**
     * Sets the taxLotService.
     * 
     * @param taxLotService
     */
    public void setTaxLotService(HoldingTaxLotService taxLotService) {
        this.taxLotService = taxLotService;
    }

    /**
     * Gets the securityService.
     * 
     * @return securityService
     */
    public SecurityService getSecurityService() {
        return securityService;
    }

    /**
     * Sets the securityService.
     * 
     * @param securityService
     */
    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Gets the kemService.
     * 
     * @return kemService
     */
    public KEMService getKemService() {
        return kemService;
    }

    /**
     * Sets the kemService.
     * 
     * @param kemService
     */
    public void setKemService(KEMService kemService) {
        this.kemService = kemService;
    }

    /**
     * Gets the parameterService.
     * 
     * @return parameterService
     */
    public ParameterService getParameterService() {
        return parameterService;
    }

    /**
     * Sets the parameterService.
     * 
     * @param parameterService
     */
    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

}

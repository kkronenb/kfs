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
package org.kuali.module.financial.document.validation.impl;

import static org.kuali.kfs.KFSConstants.AMOUNT_PROPERTY_NAME;
import static org.kuali.kfs.KFSConstants.BALANCE_TYPE_BASE_BUDGET;
import static org.kuali.kfs.KFSConstants.BALANCE_TYPE_BUDGET_STATISTICS;
import static org.kuali.kfs.KFSConstants.BALANCE_TYPE_CURRENT_BUDGET;
import static org.kuali.kfs.KFSConstants.BALANCE_TYPE_MONTHLY_BUDGET;
import static org.kuali.kfs.KFSConstants.CREDIT_AMOUNT_PROPERTY_NAME;
import static org.kuali.kfs.KFSConstants.DEBIT_AMOUNT_PROPERTY_NAME;
import static org.kuali.kfs.KFSConstants.GL_DEBIT_CODE;
import static org.kuali.kfs.KFSConstants.JOURNAL_LINE_HELPER_PROPERTY_NAME;
import static org.kuali.kfs.KFSConstants.NEW_SOURCE_ACCT_LINE_PROPERTY_NAME;
import static org.kuali.kfs.KFSConstants.SQUARE_BRACKET_LEFT;
import static org.kuali.kfs.KFSConstants.SQUARE_BRACKET_RIGHT;
import static org.kuali.kfs.KFSConstants.VOUCHER_LINE_HELPER_CREDIT_PROPERTY_NAME;
import static org.kuali.kfs.KFSConstants.VOUCHER_LINE_HELPER_DEBIT_PROPERTY_NAME;
import static org.kuali.kfs.KFSKeyConstants.ERROR_ZERO_AMOUNT;
import static org.kuali.kfs.KFSKeyConstants.ERROR_ZERO_OR_NEGATIVE_AMOUNT;
import static org.kuali.kfs.KFSKeyConstants.JournalVoucher.ERROR_NEGATIVE_NON_BUDGET_AMOUNTS;
import static org.kuali.kfs.KFSPropertyConstants.BALANCE_TYPE;

import org.apache.commons.lang.StringUtils;
import org.kuali.core.util.GlobalVariables;
import org.kuali.core.util.KualiDecimal;
import org.kuali.kfs.bo.AccountingLine;
import org.kuali.kfs.rule.event.AttributedDocumentEvent;
import org.kuali.kfs.validation.GenericValidation;
import org.kuali.module.financial.document.JournalVoucherDocument;

/**
 * The Journal Voucher's version of the accounting line amount validation
 */
public class JournalVoucherAccountingLineAmountValidation extends GenericValidation {
    private JournalVoucherDocument journalVoucherForValidation;
    private AccountingLine accountingLineForValidation;

    /**
     * Accounting lines for Journal Vouchers can be positive or negative, just not "$0.00".  
     * 
     * Additionally, accounting lines cannot have negative dollar amounts if the balance type of the 
     * journal voucher allows for general ledger pending entry offset generation or the balance type 
     * is not a budget type code.
     * @see org.kuali.kfs.validation.Validation#validate(org.kuali.kfs.rule.event.AttributedDocumentEvent)
     */
    public boolean validate(AttributedDocumentEvent event) {
        KualiDecimal amount = getAccountingLineForValidation().getAmount();

        getJournalVoucherForValidation().refreshReferenceObject(BALANCE_TYPE);

        if (getJournalVoucherForValidation().getBalanceType().isFinancialOffsetGenerationIndicator()) {
            // check for negative or zero amounts
            if (amount.isZero()) { // if 0
                GlobalVariables.getErrorMap().putErrorWithoutFullErrorPath(buildErrorMapKeyPathForDebitCreditAmount(true), ERROR_ZERO_OR_NEGATIVE_AMOUNT, "an accounting line");
                GlobalVariables.getErrorMap().putErrorWithoutFullErrorPath(buildErrorMapKeyPathForDebitCreditAmount(false), ERROR_ZERO_OR_NEGATIVE_AMOUNT, "an accounting line");

                return false;
            }
            else if (amount.isNegative()) { // entered a negative number
                String debitCreditCode = getAccountingLineForValidation().getDebitCreditCode();
                if (StringUtils.isNotBlank(debitCreditCode) && GL_DEBIT_CODE.equals(debitCreditCode)) {
                    GlobalVariables.getErrorMap().putErrorWithoutFullErrorPath(buildErrorMapKeyPathForDebitCreditAmount(true), ERROR_ZERO_OR_NEGATIVE_AMOUNT, "an accounting line");
                }
                else {
                    GlobalVariables.getErrorMap().putErrorWithoutFullErrorPath(buildErrorMapKeyPathForDebitCreditAmount(false), ERROR_ZERO_OR_NEGATIVE_AMOUNT, "an accounting line");
                }

                return false;
            }
        }
        else {
            // Check for zero amounts
            if (amount.isZero()) { // amount == 0
                GlobalVariables.getErrorMap().putError(AMOUNT_PROPERTY_NAME, ERROR_ZERO_AMOUNT, "an accounting line");
                return false;
            }
            else if (amount.isNegative()) {
                if (!getAccountingLineForValidation().getBalanceTypeCode().equals(BALANCE_TYPE_BASE_BUDGET) && !getAccountingLineForValidation().getBalanceTypeCode().equals(BALANCE_TYPE_CURRENT_BUDGET) && !getAccountingLineForValidation().getBalanceTypeCode().equals(BALANCE_TYPE_MONTHLY_BUDGET) && !getAccountingLineForValidation().getBalanceTypeCode().equals(BALANCE_TYPE_BUDGET_STATISTICS)) {
                    GlobalVariables.getErrorMap().putError(AMOUNT_PROPERTY_NAME, ERROR_NEGATIVE_NON_BUDGET_AMOUNTS);
                }
            }
        }

        return true;
    }
    
    /**
     * This method looks at the current full key path that exists in the ErrorMap structure to determine how to build 
     * the error map for the special journal voucher credit and debit fields since they don't conform to the standard 
     * pattern of accounting lines.
     * 
     * The error map key path is also dependent on whether or not the accounting line containing an error is a new 
     * accounting line or an existing line that is being updated.  This determination is made by searching for 
     * NEW_SOURCE_ACCT_LINE_PROPERTY_NAME in the error path of the global error map.
     * 
     * @param isDebit Identifies whether or not the line we are returning an error path for is a debit accounting line or not.
     * @return The full error map key path for the appropriate amount type.
     */
    private String buildErrorMapKeyPathForDebitCreditAmount(boolean isDebit) {
        // determine if we are looking at a new line add or an update
        boolean isNewLineAdd = GlobalVariables.getErrorMap().getErrorPath().contains(NEW_SOURCE_ACCT_LINE_PROPERTY_NAME);
        isNewLineAdd |= GlobalVariables.getErrorMap().getErrorPath().contains(NEW_SOURCE_ACCT_LINE_PROPERTY_NAME);

        if (isNewLineAdd) {
            return isDebit ? DEBIT_AMOUNT_PROPERTY_NAME : CREDIT_AMOUNT_PROPERTY_NAME;
        }
        else {
            String index = StringUtils.substringBetween(GlobalVariables.getErrorMap().getKeyPath("", true), SQUARE_BRACKET_LEFT, SQUARE_BRACKET_RIGHT);
            String indexWithParams = SQUARE_BRACKET_LEFT + index + SQUARE_BRACKET_RIGHT;
            return isDebit ? (JOURNAL_LINE_HELPER_PROPERTY_NAME + indexWithParams + VOUCHER_LINE_HELPER_DEBIT_PROPERTY_NAME) : (JOURNAL_LINE_HELPER_PROPERTY_NAME + indexWithParams + VOUCHER_LINE_HELPER_CREDIT_PROPERTY_NAME);
        }
    }

    /**
     * Gets the accountingLineForValidation attribute. 
     * @return Returns the accountingLineForValidation.
     */
    public AccountingLine getAccountingLineForValidation() {
        return accountingLineForValidation;
    }

    /**
     * Sets the accountingLineForValidation attribute value.
     * @param accountingLineForValidation The accountingLineForValidation to set.
     */
    public void setAccountingLineForValidation(AccountingLine accountingLineForValidation) {
        this.accountingLineForValidation = accountingLineForValidation;
    }

    /**
     * Gets the journalVoucherForValidation attribute. 
     * @return Returns the journalVoucherForValidation.
     */
    public JournalVoucherDocument getJournalVoucherForValidation() {
        return journalVoucherForValidation;
    }

    /**
     * Sets the journalVoucherForValidation attribute value.
     * @param journalVoucherForValidation The journalVoucherForValidation to set.
     */
    public void setJournalVoucherForValidation(JournalVoucherDocument journalVoucherForValidation) {
        this.journalVoucherForValidation = journalVoucherForValidation;
    }
}

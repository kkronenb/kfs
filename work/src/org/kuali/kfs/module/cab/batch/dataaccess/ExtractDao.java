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
package org.kuali.kfs.module.cab.batch.dataaccess;

import java.util.Collection;

import org.kuali.kfs.gl.businessobject.Entry;
import org.kuali.kfs.module.cab.businessobject.BatchParameters;
import org.kuali.kfs.module.purap.businessobject.CreditMemoAccountHistory;
import org.kuali.kfs.module.purap.businessobject.PaymentRequestAccountHistory;
import org.kuali.kfs.module.purap.businessobject.PurchaseOrderAccount;
import org.kuali.kfs.module.purap.businessobject.PurchaseOrderItem;
import org.kuali.kfs.sys.businessobject.GeneralLedgerPendingEntry;

public interface ExtractDao {
    /**
     * This method implementation should find all GL Entries matching the parameters listed in the batch parameters
     * 
     * @param batchParameters Batch Parameters
     * @return GL Entries
     */
    Collection<Entry> findMatchingGLEntries(BatchParameters batchParameters);

    /**
     * This method implementation should find all pending GL entries created by Payment Requests (PREQ) or Credit Memos (CM)
     * 
     * @param batchParameters Batch Parameters
     * @return Pending GL Entries
     */
    Collection<GeneralLedgerPendingEntry> findPurapPendingGLEntries(BatchParameters batchParameters);

    /**
     * This method implementation should find valid Credit Memo account line changes as per the batch parameters
     * 
     * @param batchParameters Batch Parameters
     * @return List of Credit memo account history records
     */
    Collection<CreditMemoAccountHistory> findCreditMemoAccountHistory(BatchParameters batchParameters);

    /**
     * This method implementation should find valid Payment Request account line changes as per the batch parameters
     * 
     * @param batchParameters Batch Parameters
     * @return List of Payment Request account history records
     */
    Collection<PaymentRequestAccountHistory> findPaymentRequestAccountHistory(BatchParameters batchParameters);

    /**
     * This method implementation should retrieve all eligible pretaggable PO account lines from Purchasing module
     * 
     * @param batchParameters Batch Parameters
     * @return List of pre-taggable purchase order account lines
     */
    Collection<PurchaseOrderAccount> findPreTaggablePOAccounts(BatchParameters batchParameters);

}

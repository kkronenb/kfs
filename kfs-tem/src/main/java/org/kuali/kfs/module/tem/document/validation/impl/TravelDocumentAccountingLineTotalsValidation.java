/*
 * The Kuali Financial System, a comprehensive financial management system for higher education.
 * 
 * Copyright 2005-2014 The Kuali Foundation
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kuali.kfs.module.tem.document.validation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.kuali.kfs.module.tem.document.TravelDocument;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.kfs.sys.KfsAuthorizationConstants;
import org.kuali.kfs.sys.document.AccountingDocument;
import org.kuali.kfs.sys.document.validation.event.AttributedDocumentEvent;
import org.kuali.kfs.sys.document.validation.impl.AccountingLineGroupTotalsUnchangedValidation;
import org.kuali.rice.core.web.format.CurrencyFormatter;
import org.kuali.rice.kim.api.identity.Person;
import org.kuali.rice.kns.document.authorization.TransactionalDocumentAuthorizer;
import org.kuali.rice.kns.document.authorization.TransactionalDocumentPresentationController;
import org.kuali.rice.kns.service.DocumentHelperService;
import org.kuali.rice.krad.util.GlobalVariables;

/**
 * Mostly disallows totals on document from changing, but if user can change wire transfer or foreign draft, allows totals to change
 */
public class TravelDocumentAccountingLineTotalsValidation extends AccountingLineGroupTotalsUnchangedValidation {
    protected static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(TravelDocumentAccountingLineTotalsValidation.class);

    protected DocumentHelperService documentHelperService;

    /**
     * @see org.kuali.kfs.sys.document.validation.impl.AccountingLineGroupTotalsUnchangedValidation#validate(org.kuali.kfs.sys.document.validation.event.AttributedDocumentEvent)
     */
    @Override
    public boolean validate(AttributedDocumentEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("validate start");
        }

        final TravelDocument travelDoc = (TravelDocument) event.getDocument();
        final Person financialSystemUser = GlobalVariables.getUserSession().getPerson();
        final Set<String> currentEditModes = getEditModesFromDocument(travelDoc, financialSystemUser);

        // amounts can only decrease
        if (hasRequiredEditMode(currentEditModes, getCandidateEditModes())) {
            //users in foreign or wire workgroup can increase or decrease amounts because of currency conversion
            List<String> foreignDraftAndWireTransferEditModes = getForeignDraftAndWireTransferEditModes();
            if (!this.hasRequiredEditMode(currentEditModes, foreignDraftAndWireTransferEditModes)) {
                TravelDocument persistedDocument = (TravelDocument) retrievePersistedDocument(travelDoc);
                if (persistedDocument == null) {
                    handleNonExistentDocumentWhenApproving(travelDoc);
                    return true;
                }
                // KFSMI- 5183
                if (persistedDocument.getDocumentHeader().getWorkflowDocument().isSaved() && persistedDocument.getTotalAccountLineAmount().isZero()) {
                    return true;
                }

                // check total cannot decrease
                if (!persistedDocument.getDocumentHeader().getWorkflowDocument().isCompletionRequested() && persistedDocument.getTotalAccountLineAmount().isLessThan(travelDoc.getTotalAccountLineAmount())) {
                    final String persistedTotal = (String) new CurrencyFormatter().format(persistedDocument.getTotalAccountLineAmount());
                    final String currentTotal = (String) new CurrencyFormatter().format(travelDoc.getTotalAccountLineAmount());
                    GlobalVariables.getMessageMap().putError(KFSConstants.SOURCE_ACCOUNTING_LINE_ERRORS, KFSKeyConstants.ERROR_DOCUMENT_SINGLE_ACCOUNTING_LINE_SECTION_TOTAL_CHANGED, new String[] { persistedTotal, currentTotal });
                    return false;
                }
            }

            return true;
        }

        return super.validate(event);
    }

    /**
     * determine whether the give user has permission to any edit mode defined in the given candidate edit modes
     *
     * @param currentEditModes the edit modes currently available to the given user on the document
     * @param candidateEditEditModes the given candidate edit modes
     * @return true if the give user has permission to any edit mode defined in the given candidate edit modes; otherwise, false
     */
    protected boolean hasRequiredEditMode(Set<String> currentEditModes, List<String> candidateEditModes) {
        for (String editMode : candidateEditModes) {
            if (currentEditModes.contains(editMode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the current edit modes from the document
     * @param accountingDocument the document to find edit modes of
     * @param financialSystemUser the user requesting the edit modes
     * @return the Set of current edit modes
     */
    protected Set<String> getEditModesFromDocument(AccountingDocument accountingDocument, Person financialSystemUser) {
        final TransactionalDocumentAuthorizer documentAuthorizer = (TransactionalDocumentAuthorizer) getDocumentHelperService().getDocumentAuthorizer(accountingDocument);
        final TransactionalDocumentPresentationController presentationController = (TransactionalDocumentPresentationController) getDocumentHelperService().getDocumentPresentationController(accountingDocument);

        final Set<String> presentationControllerEditModes = presentationController.getEditModes(accountingDocument);
        final Set<String> editModes = documentAuthorizer.getEditModes(accountingDocument, financialSystemUser, presentationControllerEditModes);

        return editModes;
    }

    /**
     * define the possibly desired edit modes
     *
     * @return the possibly desired edit modes
     */
    protected List<String> getCandidateEditModes() {
        List<String> candidateEdiModes = new ArrayList<String>();
        candidateEdiModes.add(KfsAuthorizationConstants.TransactionalEditMode.FRN_ENTRY);
        candidateEdiModes.add(KfsAuthorizationConstants.TransactionalEditMode.WIRE_ENTRY);

        return candidateEdiModes;
    }

    /**
     * get foreign draft And wire transfer edit mode names, as well as tax if the payee is a non-resident alien
     * @param dvDocument the document we're validating
     * @return foreign draft And wire transfer edit mode names
     */
    protected List<String> getForeignDraftAndWireTransferEditModes() {
        List<String> foreignDraftAndWireTransferEditModes = new ArrayList<String>();
        foreignDraftAndWireTransferEditModes.add(KfsAuthorizationConstants.TransactionalEditMode.FRN_ENTRY);
        foreignDraftAndWireTransferEditModes.add(KfsAuthorizationConstants.TransactionalEditMode.WIRE_ENTRY);

        return foreignDraftAndWireTransferEditModes;
    }

    public DocumentHelperService getDocumentHelperService() {
        return documentHelperService;
    }

    public void setDocumentHelperService(DocumentHelperService documentHelperService) {
        this.documentHelperService = documentHelperService;
    }

}

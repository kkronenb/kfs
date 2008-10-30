/*
 * Copyright 2007 The Kuali Foundation.
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
/*
 * Created on Jul 8, 2004
 *
 */
package org.kuali.kfs.pdp.businessobject;

import java.sql.Timestamp;
import java.util.LinkedHashMap;

import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.businessobject.TimestampedBusinessObjectBase;
import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kns.util.KualiDecimal;
import org.kuali.rice.kns.util.KualiInteger;

public class Batch extends TimestampedBusinessObjectBase {

    private KualiInteger id; // PMT_BATCH_ID

    private KualiInteger customerId;
    private CustomerProfile customerProfile; // CUST_ID

    private String paymentFileName; // PMT_FL_NM
    private Timestamp customerFileCreateTimestamp; // CUST_FL_CRTN_TS
    private KualiInteger paymentCount; // PMT_CNT
    private KualiDecimal paymentTotalAmount; // PMT_TOT_AMT
    private Person submiterUser;
    private String submiterUserId; // SBMTR_USR_ID
    private Timestamp fileProcessTimestamp; // FL_PROC_TS
    
    public Batch() {
        super();
    }

    /**
     * @hibernate.property column="FL_PROC_TS"
     */
    public Timestamp getFileProcessTimestamp() {
        return fileProcessTimestamp;
    }

    /**
     * @return
     * @hibernate.id column="PMT_BATCH_ID" generator-class="sequence"
     * @hibernate.generator-param name="sequence" value="PDP.PDP_PMT_BATCH_ID_SEQ"
     */
    public KualiInteger getId() {
        return id;
    }

    /**
     * @return
     * @hibernate.property column="CUST_FL_CRTN_TS"
     */
    public Timestamp getCustomerFileCreateTimestamp() {
        return customerFileCreateTimestamp;
    }

    /**
     * @return
     * @hibernate.property column="PMT_CNT"
     */
    public KualiInteger getPaymentCount() {
        return paymentCount;
    }

    /**
     * @return
     * @hibernate.property column="PMT_FL_NM" lenght="30"
     */
    public String getPaymentFileName() {
        return paymentFileName;
    }

    /**
     * @return
     * @hibernate.property column="PMT_TOT_AMT"
     */
    public KualiDecimal getPaymentTotalAmount() {
        return paymentTotalAmount;
    }

    /**
     * @return
     * @hibernate.many-to-one column="CUST_ID" class="edu.iu.uis.pdp.bo.CustomerProfile"
     */
    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    /**
     * @param string
     */
    public void setCustomerFileCreateTimestamp(Timestamp t) {
        customerFileCreateTimestamp = t;
    }

    /**
     * @param timestamp
     */
    public void setFileProcessTimestamp(Timestamp timestamp) {
        fileProcessTimestamp = timestamp;
    }

    /**
     * @param integer
     */
    public void setId(KualiInteger integer) {
        id = integer;
    }

    /**
     * @param integer
     */
    public void setPaymentCount(KualiInteger integer) {
        paymentCount = integer;
    }

    /**
     * @param string
     */
    public void setPaymentFileName(String string) {
        paymentFileName = string;
    }

    /**
     * @param decimal
     */
    public void setPaymentTotalAmount(KualiDecimal decimal) {
        paymentTotalAmount = decimal;
    }

    /**
     * @param integer
     */
    public void setCustomerProfile(CustomerProfile cp) {
        customerProfile = cp;
    }

    public Person getSubmiterUser() {
        return submiterUser;
    }

    public void setSubmiterUser(Person s) {
        if (s != null) {
            this.submiterUserId = s.getPrincipalId();
        }
        else {
            this.submiterUserId = null;
        }
        this.submiterUser = s;
    }

    /**
     * @hibernate.property column="SBMTR_USR_ID" length="11" not-null="true"
     * @return Returns the submiterUserId.
     */
    public String getSubmiterUserId() {
        return submiterUserId;
    }

    /**
     * @param submiterUserId The submiterUserId to set.
     */
    public void setSubmiterUserId(String submiterUserId) {
        this.submiterUserId = submiterUserId;
    }


    public void updateUser(org.kuali.rice.kim.service.PersonService userService) {
        Person u = userService.getPerson(submiterUserId);
        if (u == null) {
            setSubmiterUser(null);
        }
        else {
            setSubmiterUser(u);
        }
    }

    /**
     * @see org.kuali.rice.kns.bo.BusinessObjectBase#toStringMapper()
     */
    @Override
    protected LinkedHashMap toStringMapper() {
        LinkedHashMap m = new LinkedHashMap();
        
        m.put(KFSPropertyConstants.ID, this.id);

        return m;
    }
   
}


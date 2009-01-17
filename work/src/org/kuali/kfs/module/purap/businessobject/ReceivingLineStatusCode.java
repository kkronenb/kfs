/*
 * Copyright 2009 The Kuali Foundation.
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
package org.kuali.kfs.module.purap.businessobject;

import java.util.LinkedHashMap;
import org.kuali.rice.kns.bo.PersistableBusinessObjectBase;

public class ReceivingLineStatusCode extends PersistableBusinessObjectBase {

    private  String receivingLineStatusCode;
    private  String receivingLineStatusDescription;
    
    public String getReceivingLineStatusCode() {
        return receivingLineStatusCode;
    }

    public void setReceivingLineStatusCode(String receivingLineStatusCode) {
        this.receivingLineStatusCode = receivingLineStatusCode;
    }

    public String getReceivingLineStatusDescription() {
        return receivingLineStatusDescription;
    }

    public void setReceivingLineStatusDescription(String receivingLineStatusDescription) {
        this.receivingLineStatusDescription = receivingLineStatusDescription;
    }

    protected LinkedHashMap toStringMapper() {
            LinkedHashMap m = new LinkedHashMap();
            m.put("receivingLineStatusCode", this.receivingLineStatusCode);
            return m;
        }
}

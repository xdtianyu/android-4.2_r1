/*
* Copyright (C) 2012 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.motorolamobility.preflighting.checkers.androidmarketfilters;

import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

public class CertificateExpiredCondition extends Condition
{

    private ValidationManagerConfiguration valManagerConfig;

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        this.valManagerConfig = valManagerConfig;
        Calendar date = GregorianCalendar.getInstance();
        date.clear();
        date.set(2033, Calendar.OCTOBER, 22);
        List<Certificate> certList = data.getCertificateChain();
        if (certList != null)
        {
            for (Certificate cert : certList)
            {
                if (cert instanceof X509Certificate)
                {
                    X509Certificate x509Cert = (X509Certificate) cert;
                    try
                    {
                        x509Cert.checkValidity(date.getTime());
                    }
                    catch (CertificateExpiredException ce)
                    {
                        // exception means certificate expired
                        addValidationResult(
                                results,
                                getId(),
                                CheckerNLS
                                        .bind(CheckerNLS.AndroidMarketFiltersChecker_certificatePeriodExpired_Issue,
                                                x509Cert.getNotAfter()),
                                CheckerNLS.AndroidMarketFiltersChecker_certificatePeriodExpired_Suggestion);
                    }
                    catch (CertificateNotYetValidException e)
                    {
                        //certificate did not expire yet, but it is not valid until 22 October 2033
                        addValidationResult(
                                results,
                                getId(),
                                CheckerNLS
                                        .bind(CheckerNLS.AndroidMarketFiltersChecker_certificatePeriodNotYeatValid_Issue,
                                                x509Cert.getNotBefore()),
                                CheckerNLS.AndroidMarketFiltersChecker_certificatePeriodExpired_Suggestion);
                    }
                }
                else
                {
                    PreflightingLogger.error(CertificateExpiredCondition.class,
                            "Unrecognized certificate type"); //$NON-NLS-1$
                }
            }
        }
    }

    private void addValidationResult(ValidationResult results, String id, String issueDescription,
            String quickFixSuggestion)
    {
        ValidationResultData resultData = new ValidationResultData();
        resultData.setSeverity(getSeverityLevel());
        resultData.setConditionID(getId());
        resultData.setIssueDescription(issueDescription);
        resultData.setQuickFixSuggestion(quickFixSuggestion);
        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                valManagerConfig));
        results.addValidationResult(resultData);

    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        return new CanExecuteConditionStatus(IStatus.OK, PreflightingCorePlugin.PLUGIN_ID, "",
                getId());
    }

}

package com.bank.corebanking_prod.feature.ledger.service;

import com.bank.corebanking_prod.feature.ledger.model.dto.ReconciliationReportDto;

public interface LedgerReconciliationService {

    ReconciliationReportDto reconcileCustomerDeposits();
}

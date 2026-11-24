package com.bank.corebanking_prod.feature.account.service;

import com.bank.corebanking_prod.feature.account.repository.AccountNumberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class IbanGeneratorService {

    private static final long MAX_ACCOUNT_NUMBER = 9_999_999_999_999_999L;
    private static final String TURKEY_COUNTRY_CODE_NUMERIC = "2927";

    private final AccountNumberRepository accountNumberRepository;
    private final String bankCode;

    public IbanGeneratorService(
            AccountNumberRepository accountNumberRepository,
            @Value("${banking.iban.bank-code:00000}") String bankCode) {
        this.accountNumberRepository = accountNumberRepository;
        this.bankCode = bankCode;
    }

    /**
     * Türkiye IBAN biçiminde, MOD-97 kontrol basamağı hesaplanmış bir test IBAN'ı üretir.
     * Banka kodu gerçek bir bankaya ait olmalıdır; varsayılan 00000 yalnızca geliştirme
     * ve test ortamları içindir.
     */
    public String generate() {
        validateBankCode();

        long accountNumberValue = accountNumberRepository.getNextAccountNumber();
        if (accountNumberValue < 0 || accountNumberValue > MAX_ACCOUNT_NUMBER) {
            throw new IllegalStateException(
                    "Account number sequence exceeded the 16-digit account number range."
            );
        }

        String accountNumber = String.format(Locale.ROOT, "%016d", accountNumberValue);

        // Türkiye BBAN yapısı: 5 haneli banka kodu + 1 rezerv hane + 16 haneli hesap no.
        String bban = bankCode + "0" + accountNumber;

        // IBAN kontrol basamağı hesaplanırken TR00, sayısal olarak 292700 biçiminde sona eklenir.
        int remainder = calculateMod97(bban + TURKEY_COUNTRY_CODE_NUMERIC + "00");
        int checkDigits = 98 - remainder;

        return String.format(Locale.ROOT, "TR%02d%s", checkDigits, bban);
    }

    public boolean isValid(String iban) {
        if (iban == null || !iban.matches("TR[0-9]{24}")) {
            return false;
        }

        // TR ülke kodunu 2927'ye çevirip kontrol basamaklarını sona taşı.
        String rearranged = iban.substring(4)
                + TURKEY_COUNTRY_CODE_NUMERIC
                + iban.substring(2, 4);

        return calculateMod97(rearranged) == 1;
    }

    private void validateBankCode() {
        if (bankCode == null || !bankCode.matches("[0-9]{5}")) {
            throw new IllegalArgumentException(
                    "Bank code must contain exactly 5 numeric digits."
            );
        }
    }

    private int calculateMod97(String numericValue) {
        int remainder = 0;

        for (int i = 0; i < numericValue.length(); i++) {
            char current = numericValue.charAt(i);
            if (current < '0' || current > '9') {
                throw new IllegalArgumentException("MOD-97 input must contain only digits.");
            }
            remainder = (remainder * 10 + (current - '0')) % 97;
        }

        return remainder;
    }
}

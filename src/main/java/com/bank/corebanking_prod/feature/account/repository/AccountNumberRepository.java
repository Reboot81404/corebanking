package com.bank.corebanking_prod.feature.account.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountNumberRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountNumberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * PostgreSQL sequence'inden bir sonraki benzersiz hesap numarasını alır.
     * Sequence değerleri transaction rollback durumunda geri alınmaz; numara
     * aralarında boşluk oluşması normaldir.
     */
    public long getNextAccountNumber() {
        Long nextValue = jdbcTemplate.queryForObject(
                "SELECT nextval('account_number_seq')",
                Long.class
        );

        if (nextValue == null) {
            throw new IllegalStateException("Account number sequence returned no value.");
        }

        return nextValue;
    }
}

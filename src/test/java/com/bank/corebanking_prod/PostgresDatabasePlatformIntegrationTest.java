package com.bank.corebanking_prod;

import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.repository.AccountNumberRepository;
import com.bank.corebanking_prod.feature.account.repository.AccountRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SPEC-003: Database Platform Baseline PostgreSQL Entegrasyon Testi.
 * Gerçek PostgreSQL (192.168.1.221:5432) üzerinde Flyway migration,
 * sequence ve Hibernate validate doğrulaması yapar.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=${DB_URL:jdbc:postgresql://192.168.1.221:5432/corebanking_prod}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=${DB_USERNAME:postgres}",
        "spring.datasource.password=${DB_PASSWORD:postgres}",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PostgresDatabasePlatformIntegrationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired
    private AccountNumberRepository accountNumberRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("PostgreSQL üzerinde Flyway migration başarıyla uygulanmalı ve baseline doğrulanmalı")
    void shouldVerifyFlywayMigrationOnPostgres() {
        assertNotNull(flyway, "Flyway bean yüklü olmalıdır");

        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertTrue(appliedMigrations.length > 0, "En az bir Flyway migration uygulanmış veya baseline edilmiş olmalıdır");

        MigrationInfo current = flyway.info().current();
        assertNotNull(current, "Mevcut şema versiyonu null olamaz");
        assertTrue(Integer.parseInt(current.getVersion().getVersion()) >= 1, "En az versiyon 1 veya üzeri aktif olmalıdır");
    }

    @Test
    @DisplayName("PostgreSQL üzerinde account_number_seq başarıyla çağrılabilmeli")
    void shouldGenerateAccountNumberFromPostgresSequence() {
        Long nextVal = accountNumberRepository.getNextAccountNumber();
        assertNotNull(nextVal);
        assertTrue(nextVal >= 100000000000000L, "Sequence değeri 100000000000000'den büyük veya eşit olmalı");
    }

    @Test
    @Transactional
    @DisplayName("PostgreSQL üzerinde findByIbanForUpdate PESSIMISTIC_WRITE lock sorgusu başarıyla çalışmalı")
    void shouldExecuteLockQueryOnPostgres() {
        // Var olmayan bir IBAN ile de olsa PostgreSQL'in 'SELECT ... FOR UPDATE' sorgusunu çalıştırmasını doğrular
        Optional<AccountEntity> result = accountRepository.findByIbanForUpdate("TR000000000000000000000000");
        assertTrue(result.isEmpty(), "Olmayan IBAN için empty dönmeli, SQL hatası vermemeli");
    }
}

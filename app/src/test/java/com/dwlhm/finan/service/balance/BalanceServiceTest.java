package com.dwlhm.finan.service.balance;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.dao.WalletBalanceDao;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BalanceServiceTest {

    private FakeTransactionDao transactionDao;
    private FakeWalletBalanceDao walletBalanceDao;
    private BalanceService balanceService;

    @Before
    public void setUp() {
        transactionDao = new FakeTransactionDao();
        walletBalanceDao = new FakeWalletBalanceDao();
        balanceService = new BalanceService(transactionDao, walletBalanceDao);
    }

    @Test
    public void apply_expense_decreases_cached_balance() {
        walletBalanceDao.setCachedBalance(1L, 100_000L);
        balanceService.applyTransaction(expense(1L, 25_000L));
        assertEquals(75_000L, walletBalanceDao.getCachedBalance(1L));
    }

    @Test
    public void apply_income_increases_cached_balance() {
        walletBalanceDao.setCachedBalance(1L, 50_000L);
        balanceService.applyTransaction(income(1L, 30_000L));
        assertEquals(80_000L, walletBalanceDao.getCachedBalance(1L));
    }

    @Test
    public void recalculate_from_transactions() {
        transactionDao.add(expense(1L, 10_000L));
        transactionDao.add(income(1L, 50_000L));
        transactionDao.add(expense(1L, 5_000L));
        long balance = balanceService.recalculate(1L);
        assertEquals(35_000L, balance);
        assertEquals(35_000L, walletBalanceDao.getCachedBalance(1L));
    }

    private static Transaction expense(long walletId, long amount) {
        return new Transaction(0L, amount, TransactionType.EXPENSE, walletId, 1L, 1L, null);
    }

    private static Transaction income(long walletId, long amount) {
        return new Transaction(0L, amount, TransactionType.INCOME, walletId, 1L, 1L, null);
    }

    private static final class FakeTransactionDao implements TransactionGateway {
        private final List<Transaction> transactions = new ArrayList<>();

        void add(Transaction t) {
            transactions.add(t);
        }

        @Override
        public long insert(Transaction transaction) {
            transactions.add(transaction);
            return transactions.size();
        }

        @Override
        public void update(Transaction transaction) {
        }

        @Override
        public void delete(long transactionId) {
        }

        @Override
        public Transaction findById(long transactionId) {
            return null;
        }

        @Override
        public Transaction findLast() {
            return transactions.isEmpty() ? null : transactions.get(transactions.size() - 1);
        }

        @Override
        public List<Transaction> findRecent(int limit) {
            return transactions;
        }

        @Override
        public List<Transaction> findHistory(
                Long walletId,
                Long categoryId,
                TransactionType type,
                Long startInclusiveMillis,
                Long endExclusiveMillis,
                boolean oldestFirst
        ) {
            return new ArrayList<>(transactions);
        }

        @Override
        public com.dwlhm.finan.domain.model.PageResult<
                        Transaction, com.dwlhm.finan.domain.model.HistoryPageCursor>
                findHistoryPage(
                Long walletId,
                Long categoryId,
                TransactionType type,
                Long startInclusiveMillis,
                Long endExclusiveMillis,
                boolean oldestFirst,
                com.dwlhm.finan.domain.model.HistoryPageCursor cursor,
                int limit) {
            return new com.dwlhm.finan.domain.model.PageResult<>(
                    new ArrayList<>(transactions), false, null);
        }

        @Override
        public com.dwlhm.finan.domain.model.HistoryTotals findHistoryTotals(
                Long walletId,
                Long categoryId,
                TransactionType type,
                Long startInclusiveMillis,
                Long endExclusiveMillis) {
            return new com.dwlhm.finan.domain.model.HistoryTotals(transactions.size(), 0L, 0L);
        }

        @Override
        public List<Transaction> findByWalletId(long walletId) {
            List<Transaction> result = new ArrayList<>();
            for (Transaction t : transactions) {
                if (t.getWalletId() == walletId) {
                    result.add(t);
                }
            }
            return result;
        }

        @Override
        public List<Transaction> findAll() {
            return new ArrayList<>(transactions);
        }
    }

    private static final class FakeWalletBalanceDao implements WalletBalanceDao {
        private final Map<Long, Long> balances = new HashMap<>();

        @Override
        public long getCachedBalance(long walletId) {
            return balances.getOrDefault(walletId, 0L);
        }

        @Override
        public void setCachedBalance(long walletId, long balanceMinor) {
            balances.put(walletId, balanceMinor);
        }
    }
}

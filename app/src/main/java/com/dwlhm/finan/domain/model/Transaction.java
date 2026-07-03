package com.dwlhm.finan.domain.model;

public class Transaction {

    private long id;
    private long amountMinor;
    private TransactionType type;
    private long walletId;
    private long categoryId;
    private long occurredAt;
    private String note;
    private Long transferId;
    private CashFlowActivity cashFlowActivity = CashFlowActivity.UNCLASSIFIED;
    private boolean cashFlowActivityOverridden;

    public Transaction(
            long id,
            long amountMinor,
            TransactionType type,
            long walletId,
            long categoryId,
            long occurredAt,
            String note
    ) {
        this.id = id;
        this.amountMinor = amountMinor;
        this.type = type;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.occurredAt = occurredAt;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public long getWalletId() {
        return walletId;
    }

    public void setWalletId(long walletId) {
        this.walletId = walletId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public long getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(long occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public CashFlowActivity getCashFlowActivity() {
        return cashFlowActivity;
    }

    public void setCashFlowActivity(CashFlowActivity cashFlowActivity) {
        this.cashFlowActivity =
                cashFlowActivity == null ? CashFlowActivity.UNCLASSIFIED : cashFlowActivity;
    }

    public boolean isCashFlowActivityOverridden() {
        return cashFlowActivityOverridden;
    }

    public void setCashFlowActivityOverridden(boolean overridden) {
        cashFlowActivityOverridden = overridden;
    }
}

package com.dwlhm.finan.service.merchant;

import com.dwlhm.finan.data.dao.MerchantGateway;

public class MerchantUsageService {

  private final MerchantGateway merchantGateway;

  public MerchantUsageService(MerchantGateway merchantGateway) {
    this.merchantGateway = merchantGateway;
  }

  public void bumpUsage(long merchantId) {
    if (merchantId > 0L) {
      merchantGateway.bumpUsage(merchantId);
    }
  }
}

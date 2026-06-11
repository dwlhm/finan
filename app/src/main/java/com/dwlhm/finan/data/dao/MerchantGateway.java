package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.domain.model.Merchant;

import java.util.List;

public interface MerchantGateway {

  void bumpUsage(long merchantId);

  List<Merchant> findAll();
}

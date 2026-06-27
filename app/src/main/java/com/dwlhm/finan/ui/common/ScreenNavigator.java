package com.dwlhm.finan.ui.common;

public interface ScreenNavigator {
  void openCategories();

  void openWallets();

  void openHistoryForCategory(long categoryId);
}

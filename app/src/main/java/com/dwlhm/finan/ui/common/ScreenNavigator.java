package com.dwlhm.finan.ui.common;

public interface ScreenNavigator {
  /** Opens the transaction capture screen. */
  void openCapture();

  void openCategories();

  void openCategoriesFiltered(String classificationFilter);

  void openWallets();

  void openHistoryForCategory(long categoryId);

  void openHistoryWithFilter(String type, long startDate, long endDate, Long walletId, Long categoryId);

  void openHistoryForActivity(String activity);
}

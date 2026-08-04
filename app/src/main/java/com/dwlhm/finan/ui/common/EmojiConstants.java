package com.dwlhm.finan.ui.common;

public final class EmojiConstants {
  private EmojiConstants() {}

  public static final class EmojiCategory {
    private final String name;
    private final String icon;
    private final String[] emojis;

    public EmojiCategory(String name, String icon, String[] emojis) {
      this.name = name;
      this.icon = icon;
      this.emojis = emojis;
    }

    public String getName() {
      return name;
    }

    public String getIcon() {
      return icon;
    }

    public String[] getEmojis() {
      return emojis;
    }
  }

  public static final String[] WALLET_EMOJIS = {
    "💳", "💰", "🏦", "💵", "💎", "🏠", "🚗", "🎓",
    "✈️", "🛒", "🍔", "☕", "🎮", "👕", "💊", "🐾",
    "🎵", "📱", "💻", "🏋️"
  };

  public static final String[] CATEGORY_EMOJIS = {
    "🍔", "🚗", "🏠", "🛍️", "💡", "🏥", "🎓", "🎮", "✈️", "☕",
    "💰", "🍿", "👔", "💆", "🐾", "🎁", "🥦", "🚌", "🏋️", "🎨",
    "🍕", "🍜", "🧋", "🍎", "🍰", "🍺", "🛒", "⛽", "🛵", "🚲",
    "🎫", "🗺️", "⚡", "💧", "📶", "🔌", "🧹", "🔑", "👗", "👠",
    "💄", "💍", "🎬", "🎵", "📚", "⚽", "🎲", "💊", "💅",
    "💵", "🏦", "📈", "💼", "🧾", "📊", "🪙", "🏷️", "📱", "💻"
  };

  public static final EmojiCategory[] CATEGORIES = new EmojiCategory[] {
    new EmojiCategory("Makanan", "🍔", new String[]{
      "🍔", "☕", "🥦", "🍿", "🍕", "🍜", "🧋", "🍎", "🍰", "🍺", "🛒", "🍱", "🍩", "🍦", "🌮"
    }),
    new EmojiCategory("Transport", "🚗", new String[]{
      "🚗", "✈️", "🚌", "🚖", "🚆", "⛽", "🛵", "🚲", "🎫", "🗺️", "🚊", "⛵", "🚀"
    }),
    new EmojiCategory("Rumah", "🏠", new String[]{
      "🏠", "💡", "🏥", "🎓", "🐾", "⚡", "💧", "📶", "🔌", "🧹", "🔑", "📑", "🛠️", "🛋️"
    }),
    new EmojiCategory("Belanja", "🛍️", new String[]{
      "🛍️", "👔", "💆", "🎁", "🎨", "👗", "👠", "💄", "💍", "👓", "📦", "💳", "👕"
    }),
    new EmojiCategory("Hiburan", "🎮", new String[]{
      "🎮", "🍿", "🏋️", "🎨", "🎵", "🎬", "📚", "⚽", "🎲", "🎸", "🎤", "🎳", "⛺", "🏆"
    }),
    new EmojiCategory("Keuangan", "💰", new String[]{
      "💰", "💎", "💵", "🏦", "📈", "💼", "🧾", "📊", "🪙", "🏷️", "🤝", "💳"
    }),
    new EmojiCategory("Lainnya", "✨", new String[]{
      "💊", "💆", "💅", "📱", "💻", "⭐️", "🎯", "☀️", "🌸", "🐕", "🐈", "📍", "🚩"
    })
  };
}

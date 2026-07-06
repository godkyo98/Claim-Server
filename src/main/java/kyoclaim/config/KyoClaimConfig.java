package kyoclaim.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class KyoClaimConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "kyoclaim.json");

  // ==========================================
  // CẤU HÌNH NGÔN NGỮ LỆNH (COMMANDS & SUB-COMMANDS)
  // ==========================================
  public List<String> commandAliases = Arrays.asList("kyoclaim", "claim", "nha", "dat");
  public List<String> cmdBuy = Arrays.asList("buy", "mua");
  public List<String> cmdUpgrade = Arrays.asList("upgrade", "nangcap", "up");
  public List<String> cmdSell = Arrays.asList("sell", "ban");
  public List<String> cmdTrust = Arrays.asList("trust", "themban", "add");
  public List<String> cmdUntrust = Arrays.asList("untrust", "duoiban", "kick");
  public List<String> cmdInfo = Arrays.asList("info", "toado", "vitri");
  // THÊM DÒNG NÀY ĐỂ TẠO LỆNH DỊCH CHUYỂN:
  public List<String> cmdHome = Arrays.asList("home", "ve");

  // ==========================================
  // CẤU HÌNH THÔNG SỐ BẤT ĐỘNG SẢN
  // ==========================================
  public int baseRadius = 2;
  public int basePrice = 5000;
  public int upgradeRadiusBoost = 1;
  public int baseUpgradePrice = 20000;

  // THÊM DÒNG NÀY ĐỂ THU PHÍ DỊCH CHUYỂN:
  public int homeTeleportPrice = 1000; // Tốn 200 Xu mỗi lần bay về nhà

  private static KyoClaimConfig instance;

  public static KyoClaimConfig getInstance() {
    if (instance == null) {
      load();
    }
    return instance;
  }

  public static void load() {
    if (CONFIG_FILE.exists()) {
      try (FileReader reader = new FileReader(CONFIG_FILE)) {
        instance = GSON.fromJson(reader, KyoClaimConfig.class);
      } catch (IOException e) {
        System.err.println("[Kyo Claim] Lỗi khi đọc config! Sử dụng mặc định.");
        instance = new KyoClaimConfig();
      }
    } else {
      instance = new KyoClaimConfig();
      save();
    }
  }

  public static void save() {
    try {
      CONFIG_FILE.getParentFile().mkdirs();
      try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
        GSON.toJson(instance, writer);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
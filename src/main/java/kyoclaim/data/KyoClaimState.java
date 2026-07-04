package kyoclaim.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KyoClaimState extends SavedData {
  public final Map<UUID, ClaimData> claims = new HashMap<>();

  // 💡 FIX LỖI "NOT A STRING":
  // Ép kiểu UUID thành String khi lưu vào ổ cứng, và dịch từ String sang UUID khi nạp lên RAM
  private static final Codec<UUID> STRING_UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

  // BỘ MÃ HÓA CODEC (Trái tim của hệ thống lưu trữ)
  public static final Codec<KyoClaimState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      // Thay UUIDUtil.CODEC bằng bộ mã hóa String UUID ta vừa tạo ở trên
      Codec.unboundedMap(STRING_UUID_CODEC, ClaimData.CODEC)
          .fieldOf("claims")
          .forGetter(state -> state.claims)
  ).apply(instance, map -> {
    KyoClaimState state = new KyoClaimState();
    state.claims.putAll(map);
    return state;
  }));

  // ĐĂNG KÝ CHUẨN 26.2
  public static final SavedDataType<KyoClaimState> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath("kyoclaim", "kyoclaim_data"),
      KyoClaimState::new,
      CODEC,
      null
  );

  public KyoClaimState() {}

  public void addClaim(UUID uuid, ClaimData data) {
    claims.put(uuid, data);
    this.setDirty(); // Báo cho game biết để tự động lưu!
  }

  public void removeClaim(UUID uuid) {
    claims.remove(uuid);
    this.setDirty(); // Báo cho game biết để tự động lưu!
  }

  public ClaimData getClaim(UUID uuid) {
    return claims.get(uuid);
  }

  public static KyoClaimState getServerState(MinecraftServer server) {
    return server.getDataStorage().computeIfAbsent(TYPE);
  }
}
package kyoclaim.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KyoClaimState extends SavedData {
  public final Map<UUID, ClaimData> claims = new HashMap<>();

  // 🔥 CODEC LAI (HYBRID): Tự động nhận diện và dịch file mảng số cũ sang String mới
  private static final Codec<UUID> STRING_UUID_CODEC = Codec.withAlternative(
      Codec.STRING.xmap(UUID::fromString, UUID::toString),
      UUIDUtil.CODEC
  );

  public static final Codec<KyoClaimState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.unboundedMap(STRING_UUID_CODEC, ClaimData.CODEC)
          .fieldOf("claims")
          .forGetter(state -> state.claims)
  ).apply(instance, map -> {
    KyoClaimState state = new KyoClaimState();
    state.claims.putAll(map);
    return state;
  }));

  // FIX: Trả lại Identifier chuẩn của 26.2
  public static final SavedDataType<KyoClaimState> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath("kyoclaim", "kyoclaim_data"),
      KyoClaimState::new,
      CODEC,
      null
  );

  public KyoClaimState() {}

  // =========================================
  // FIX: PHỤC HỒI CÁC HÀM CŨ CHO LỆNH SELL/TRUST
  // =========================================
  public ClaimData getClaim(UUID uuid) {
    return claims.get(uuid);
  }

  public void removeClaim(UUID uuid) {
    claims.remove(uuid);
    this.setDirty();
  }

  public void addClaim(UUID uuid, ClaimData data) {
    claims.put(uuid, data);
    this.setDirty();
  }
}
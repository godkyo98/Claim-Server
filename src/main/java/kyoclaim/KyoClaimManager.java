package kyoclaim;

import kyoclaim.data.ClaimData;
import kyoclaim.data.KyoClaimState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class KyoClaimManager {

  // Tìm xem Block này đang thuộc đất của ai bằng cách gọi State
  public static ClaimData getClaimAt(MinecraftServer server, BlockPos pos) {
    KyoClaimState state = KyoClaimState.getServerState(server);
    for (ClaimData claim : state.claims.values()) {
      if (claim.contains(pos)) return claim;
    }
    return null;
  }

  // Quét ranh giới
  public static boolean canPlaceOrUpgrade(MinecraftServer server, BlockPos center, int radius, UUID myUuid) {
    KyoClaimState state = KyoClaimState.getServerState(server);
    for (ClaimData claim : state.claims.values()) {
      if (claim.getOwner().equals(myUuid)) continue;
      if (claim.overlapsWith(center, radius)) return false;
    }
    return true;
  }
}
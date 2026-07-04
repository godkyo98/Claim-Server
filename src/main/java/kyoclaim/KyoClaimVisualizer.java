package kyoclaim;

import kyoclaim.data.ClaimData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KyoClaimVisualizer {
  // Lưu trữ danh sách những người chơi đang bật "Máy chiếu"
  private static final Map<UUID, ActiveVisual> activeVisuals = new ConcurrentHashMap<>();

  // Hàm gọi để bật máy chiếu trong 15 giây (300 ticks)
  public static void showBorder(ServerPlayer player, ClaimData claim) {
    activeVisuals.put(player.getUUID(), new ActiveVisual(player, claim, 300));
  }

  public static void register() {
    // Hook vào sự kiện nhịp đập (Tick) của Server để giữ hạt sáng liên tục
    ServerTickEvents.END_SERVER_TICK.register(server -> {
      if (activeVisuals.isEmpty()) return;

      Iterator<Map.Entry<UUID, ActiveVisual>> it = activeVisuals.entrySet().iterator();
      while (it.hasNext()) {
        ActiveVisual visual = it.next().getValue();

        // Nếu người chơi thoát game hoặc hết thời gian 15s -> Tắt máy chiếu
        if (visual.player.isRemoved() || visual.ticksLeft <= 0) {
          it.remove();
          continue;
        }

        // Cứ mỗi 5 tick (4 lần/giây) sẽ vẽ lại viền 1 lần để tạo thành dải sáng không bao giờ tắt
        if (visual.ticksLeft % 5 == 0) {
          drawGlowingLaser(visual.player, visual.claim);
        }
        visual.ticksLeft--;
      }
    });
  }

  private static void drawGlowingLaser(ServerPlayer player, ClaimData claim) {
    ServerLevel level = player.level();
    BlockPos center = claim.getCenter();
    int r = claim.getRadius();

    // Tính 4 góc mép ngoài cùng của Block (Tránh việc viền ăn lẹm vào giữa block)
    double minX = center.getX() - r;
    double minZ = center.getZ() - r;
    double maxX = center.getX() + r + 1.0;
    double maxZ = center.getZ() + r + 1.0;

    // Vẽ ngang bụng người chơi để dễ nhìn
    double y = player.getY() + 1.0;
    int size = r * 2 + 1;

    // 1. VẼ VIỀN MẶT ĐẤT (Cạnh vuông) - Dùng lửa
    // 12 Tham số: player, particleType, longDistance, force, x, y, z, count, dx, dy, dz, speed
    for (int i = 0; i <= size; i++) {
      level.sendParticles(player, ParticleTypes.FLAME, true, true, minX + i, y, minZ, 1, 0, 0, 0, 0.0);
      level.sendParticles(player, ParticleTypes.FLAME, true, true, minX + i, y, maxZ, 1, 0, 0, 0, 0.0);
      level.sendParticles(player, ParticleTypes.FLAME, true, true, minX, y, minZ + i, 1, 0, 0, 0, 0.0);
      level.sendParticles(player, ParticleTypes.FLAME, true, true, maxX, y, minZ + i, 1, 0, 0, 0, 0.0);
    }

    // 2. VẼ 4 CỘT LASER (Pillar) Ở 4 GÓC - Dùng sao phép thuật rơi xuống
    for (int j = -2; j <= 8; j += 2) {
      level.sendParticles(player, ParticleTypes.END_ROD, true, true, minX, y + j, minZ, 1, 0, 0, 0, 0.0);
      level.sendParticles(player, ParticleTypes.END_ROD, true, true, maxX, y + j, minZ, 1, 0, 0, 0, 0.0);
      level.sendParticles(player, ParticleTypes.END_ROD, true, true, minX, y + j, maxZ, 1, 0, 0, 0, 0.0);
      level.sendParticles(player, ParticleTypes.END_ROD, true, true, maxX, y + j, maxZ, 1, 0, 0, 0, 0.0);
    }
  }

  private static class ActiveVisual {
    ServerPlayer player;
    ClaimData claim;
    int ticksLeft;

    ActiveVisual(ServerPlayer player, ClaimData claim, int ticksLeft) {
      this.player = player;
      this.claim = claim;
      this.ticksLeft = ticksLeft;
    }
  }
}
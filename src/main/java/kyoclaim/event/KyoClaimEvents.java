package kyoclaim.event;

import kyoclaim.KyoClaimManager;
import kyoclaim.data.ClaimData;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public class KyoClaimEvents {

  public static void register() {
    PlayerBlockBreakEvents.BEFORE.register((world, player, pos, blockState, blockEntity) -> {
      if (world.isClientSide()) return true;

      MinecraftServer server = world.getServer();
      ClaimData claim = KyoClaimManager.getClaimAt(server, pos);

      // FIX: Dùng hàm hasPermission thay vì chỉ so sánh Owner
      if (claim != null && !claim.hasPermission(player.getUUID())) {
        player.sendSystemMessage(Component.literal("§c[Kyo Claim] ❌ Đây là căn cứ của " + claim.getOwnerName() + "! Bạn không thể phá."));
        return false;
      }
      return true;
    });

    UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
      if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

      MinecraftServer server = world.getServer();
      ClaimData claim = KyoClaimManager.getClaimAt(server, hitResult.getBlockPos());

      // FIX: Dùng hàm hasPermission thay vì chỉ so sánh Owner
      if (claim != null && !claim.hasPermission(player.getUUID())) {
        player.sendSystemMessage(Component.literal("§c[Kyo Claim] ❌ Không được táy máy đồ trong nhà người khác!"));
        return InteractionResult.FAIL;
      }
      return InteractionResult.PASS;
    });
  }
}
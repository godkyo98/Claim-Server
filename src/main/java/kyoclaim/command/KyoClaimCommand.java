package kyoclaim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kyoclaim.KyoClaimManager;
import kyoclaim.KyoClaimVisualizer;
import kyoclaim.config.KyoClaimConfig;
import kyoclaim.data.ClaimData;
import kyoclaim.data.KyoClaimState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import kyoeconomy.api.KyoEconomyAPI;

import java.util.UUID;

public class KyoClaimCommand {

  private static int getUpgradePrice(int currentRadius) {
    KyoClaimConfig config = KyoClaimConfig.getInstance();
    int upgradeLevel = currentRadius - config.baseRadius + 1;
    return config.baseUpgradePrice * upgradeLevel;
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    KyoClaimConfig config = KyoClaimConfig.getInstance();

    // Lặp qua tất cả các tên lệnh chính (VD: kyoclaim, nha, dat)
    for (String mainAlias : config.commandAliases) {

      // Xây dựng gốc lệnh chính
      LiteralArgumentBuilder<CommandSourceStack> mainCommand = Commands.literal(mainAlias)
          .executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            ClaimData claim = KyoClaimManager.getClaimAt(server, player.blockPosition());

            if (claim == null) {
              player.sendSystemMessage(Component.literal("§a[Kyo Claim] Vùng đất này vô chủ. Dùng §b/" + mainAlias + " " + config.cmdBuy.get(0) + " §ađể cắm cờ (Giá: " + config.basePrice + " Xu)."));
            } else {
              int size = (claim.getRadius() * 2) + 1;
              player.sendSystemMessage(Component.literal("§6[Kyo Claim] Căn cứ của: §e" + claim.getOwnerName() + " §7(Kích thước: " + size + "x" + size + ")"));
              KyoClaimVisualizer.showBorder(player, claim);
            }
            return 1;
          });

      // Lặp qua các từ khóa lệnh MUA (buy, mua)
      for (String buyAlias : config.cmdBuy) {
        mainCommand.then(Commands.literal(buyAlias).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          KyoClaimState state = KyoClaimState.getServerState(server);

          if (state.getClaim(player.getUUID()) != null) {
            player.sendSystemMessage(Component.literal("§c❌ Bạn đã có một Đại bản doanh rồi! Hãy dùng lệnh nâng cấp."));
            return 0;
          }

          BlockPos center = player.blockPosition();
          if (!KyoClaimManager.canPlaceOrUpgrade(server, center, config.baseRadius, player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c❌ Vị trí này quá sát ranh giới với nhà hàng xóm!"));
            return 0;
          }

          double balance = KyoEconomyAPI.getBalanceInXu(server, player.getUUID());

          if (balance < config.basePrice) {
            player.sendSystemMessage(Component.literal("§c❌ Bạn không đủ " + config.basePrice + " Xu để mua Lõi đất!"));
            return 0;
          }

          KyoEconomyAPI.removeMoneyInXu(server, player.getUUID(), config.basePrice);

          ClaimData newClaim = new ClaimData(player.getUUID(), player.getName().getString(), center, config.baseRadius);
          state.addClaim(player.getUUID(), newClaim);

          int initialSize = (config.baseRadius * 2) + 1;
          player.sendSystemMessage(Component.literal("§a🎉 Cắm cờ thành công! Căn Cứ " + initialSize + "x" + initialSize + " của bạn đã được bảo vệ."));
          KyoClaimVisualizer.showBorder(player, newClaim);
          return 1;
        }));
      }

      // Lặp qua các từ khóa lệnh NÂNG CẤP (upgrade, nangcap, up)
      for (String upgradeAlias : config.cmdUpgrade) {
        mainCommand.then(Commands.literal(upgradeAlias).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          KyoClaimState state = KyoClaimState.getServerState(server);

          ClaimData myClaim = state.getClaim(player.getUUID());
          if (myClaim == null) {
            player.sendSystemMessage(Component.literal("§c❌ Bạn chưa có Lõi đất để nâng cấp!"));
            return 0;
          }

          int newRadius = myClaim.getRadius() + config.upgradeRadiusBoost;
          int upgradePrice = getUpgradePrice(myClaim.getRadius());

          if (!KyoClaimManager.canPlaceOrUpgrade(server, myClaim.getCenter(), newRadius, player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c❌ Không thể mở rộng thêm! Sẽ đè vào nhà hàng xóm."));
            return 0;
          }

          double balance = KyoEconomyAPI.getBalanceInXu(server, player.getUUID());

          if (balance < upgradePrice) {
            player.sendSystemMessage(Component.literal("§c❌ Phí nâng cấp lần này là " + upgradePrice + " Xu. Bạn không đủ tiền!"));
            return 0;
          }

          KyoEconomyAPI.removeMoneyInXu(server, player.getUUID(), upgradePrice);

          myClaim.addRadius(config.upgradeRadiusBoost);
          state.setDirty();

          int newSize = (myClaim.getRadius() * 2) + 1;
          player.sendSystemMessage(Component.literal("§e🌟 Keng! Căn cứ đã mở rộng thành " + newSize + "x" + newSize + " (Trừ " + upgradePrice + " Xu)"));
          KyoClaimVisualizer.showBorder(player, myClaim);
          return 1;
        }));
      }

      // Lặp qua các từ khóa lệnh BÁN (sell, ban)
      for (String sellAlias : config.cmdSell) {
        mainCommand.then(Commands.literal(sellAlias).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          KyoClaimState state = KyoClaimState.getServerState(server);

          if (state.getClaim(player.getUUID()) == null) {
            player.sendSystemMessage(Component.literal("§c❌ Bạn vô gia cư, lấy đâu ra đất mà bán?"));
            return 0;
          }

          int refund = config.basePrice;
          KyoEconomyAPI.addMoneyInXu(server, player.getUUID(), refund);

          state.removeClaim(player.getUUID());
          player.sendSystemMessage(Component.literal("§c🏚️ Bạn đã tháo dỡ Căn Cứ và thu hồi " + refund + " Xu."));
          return 1;
        }));
      }

      // Lặp qua các từ khóa lệnh THÊM BẠN (trust, themban, add)
      for (String trustAlias : config.cmdTrust) {
        mainCommand.then(Commands.literal(trustAlias)
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(context -> {
                  ServerPlayer player = context.getSource().getPlayerOrException();
                  MinecraftServer server = context.getSource().getServer();
                  KyoClaimState state = KyoClaimState.getServerState(server);

                  ClaimData myClaim = state.getClaim(player.getUUID());
                  if (myClaim == null) {
                    player.sendSystemMessage(Component.literal("§c❌ Bạn chưa có Căn Cứ để chia sẻ!"));
                    return 0;
                  }

                  var profiles = GameProfileArgument.getGameProfiles(context, "player");
                  if (profiles.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§c❌ Không tìm thấy người chơi này!"));
                    return 0;
                  }

                  var targetProfile = profiles.iterator().next();
                  UUID targetUuid = targetProfile.id();

                  if (targetUuid.equals(player.getUUID())) {
                    player.sendSystemMessage(Component.literal("§c❌ Bạn là chủ nhà rồi, không cần tự trust chính mình!"));
                    return 0;
                  }

                  if (myClaim.trustPlayer(targetUuid)) {
                    state.setDirty();
                    player.sendSystemMessage(Component.literal("§a✅ Đã cấp quyền sử dụng Căn Cứ cho §e" + targetProfile.name() + "§a."));
                  } else {
                    player.sendSystemMessage(Component.literal("§c❌ Người chơi này đã có quyền trong nhà bạn rồi."));
                  }
                  return 1;
                })
            )
        );
      }

      // Lặp qua các từ khóa lệnh ĐUỔI BẠN (untrust, duoiban, kick)
      for (String untrustAlias : config.cmdUntrust) {
        mainCommand.then(Commands.literal(untrustAlias)
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(context -> {
                  ServerPlayer player = context.getSource().getPlayerOrException();
                  MinecraftServer server = context.getSource().getServer();
                  KyoClaimState state = KyoClaimState.getServerState(server);

                  ClaimData myClaim = state.getClaim(player.getUUID());
                  if (myClaim == null) {
                    player.sendSystemMessage(Component.literal("§c❌ Bạn chưa có Căn Cứ!"));
                    return 0;
                  }

                  var profiles = GameProfileArgument.getGameProfiles(context, "player");
                  if (profiles.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§c❌ Không tìm thấy người chơi này!"));
                    return 0;
                  }

                  var targetProfile = profiles.iterator().next();
                  UUID targetUuid = targetProfile.id();

                  if (myClaim.untrustPlayer(targetUuid)) {
                    state.setDirty();
                    player.sendSystemMessage(Component.literal("§c⚠️ Đã thu hồi quyền sử dụng Căn Cứ của §e" + targetProfile.name() + "§c."));
                  } else {
                    player.sendSystemMessage(Component.literal("§c❌ Người chơi này không có trong danh sách bạn bè của bạn."));
                  }
                  return 1;
                })
            )
        );
      }

      // Đăng ký toàn bộ cụm lệnh này vào bộ máy Server
      dispatcher.register(mainCommand);
    }
  }
}
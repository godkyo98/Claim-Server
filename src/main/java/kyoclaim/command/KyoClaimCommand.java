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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

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
      LiteralArgumentBuilder<CommandSourceStack> mainCommand = Commands.literal(mainAlias).executes(context -> {
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

      // ==========================================
      // Sub-command: MUA ĐẤT (buy) - LƯU THẲNG VÀO Ổ CỨNG + HIỆN RANH GIỚI
      // ==========================================
      for (String buyAlias : config.cmdBuy) {
        mainCommand.then(Commands.literal(buyAlias).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          UUID playerUuid = player.getUUID();
          BlockPos pos = player.blockPosition();

          // Gọi State lưu trữ từ ổ cứng
          KyoClaimState state = server.overworld().getDataStorage().computeIfAbsent(KyoClaimState.TYPE);

          if (state.claims.containsKey(playerUuid)) {
            player.sendSystemMessage(Component.literal("§c❌ Sếp đã sở hữu căn cứ rồi! Không thể mua thêm cái thứ hai."));
            return 0;
          }

          // Kiểm tra xem khu vực này đã có ai claim chưa (Quét từ ổ cứng)
          for (ClaimData existingClaim : state.claims.values()) {
            if (existingClaim.contains(pos)) {
              player.sendSystemMessage(Component.literal("§c❌ Vị trí này đã thuộc chủ quyền của " + existingClaim.getOwnerName() + "!"));
              return 0;
            }
          }

          double balance = KyoEconomyAPI.getBalanceInXu(server, playerUuid);
          int price = config.basePrice;

          if (balance < price) {
            player.sendSystemMessage(Component.literal("§c❌ Không đủ tiền! Mua căn cứ cần §e" + price + " Xu§c."));
            return 0;
          }

          KyoEconomyAPI.removeMoneyInXu(server, playerUuid, price);

          // Tạo dữ liệu nhà mới và đẩy thẳng vào ổ cứng
          ClaimData newClaim = new ClaimData(playerUuid, player.getName().getString(), pos, config.baseRadius);
          state.addClaim(playerUuid, newClaim); // Hàm này đã có sẵn setDirty() để tự động lưu vĩnh viễn

          // 🔥 HỒI SINH HIỆU ỨNG: Kích hoạt máy chiếu hiển thị ranh giới ngay khi mua xong thành công!
          KyoClaimVisualizer.showBorder(player, newClaim);

          player.sendSystemMessage(Component.literal("§a🎉 Chúc mừng sếp đã mua đất thành công! Vị trí lõi: §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
          return 1;
        }));
      }

      // ==========================================
      // Sub-command: NÂNG CẤP ĐẤT (upgrade)
      // ==========================================
      for (String upgradeAlias : config.cmdUpgrade) {
        mainCommand.then(Commands.literal(upgradeAlias).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          UUID playerUuid = player.getUUID();

          // 1. FIX CHÍ MẠNG: Phải gọi Dữ liệu từ Ổ CỨNG (KyoClaimState) thay vì RAM
          KyoClaimState state = server.overworld().getDataStorage().computeIfAbsent(KyoClaimState.TYPE);

          if (!state.claims.containsKey(playerUuid)) {
            player.sendSystemMessage(Component.literal("§c❌ Sếp chưa có nhà! Mua đất trước khi nâng cấp nhé."));
            return 0;
          }

          ClaimData claim = state.claims.get(playerUuid);

          // 2. Chống Hack: Bắt buộc sếp phải đứng bên trong nhà mới cho nâng cấp
          if (!claim.contains(player.blockPosition())) {
            player.sendSystemMessage(Component.literal("§c❌ Sếp phải chạy về đứng bên trong Lãnh thổ của mình thì mới được nâng cấp!"));
            return 0;
          }

          int upgradePrice = getUpgradePrice(claim.getRadius());
          double balance = KyoEconomyAPI.getBalanceInXu(server, playerUuid);

          if (balance < upgradePrice) {
            player.sendSystemMessage(Component.literal("§c❌ Không đủ tiền! Sếp cần §e" + upgradePrice + " Xu§c để nâng cấp lãnh thổ."));
            return 0;
          }

          // 3. Trừ tiền và Mở rộng đất
          KyoEconomyAPI.removeMoneyInXu(server, playerUuid, upgradePrice);
          claim.addRadius(config.upgradeRadiusBoost); // Cộng thêm bán kính

          // 4. Lưu trực tiếp vào ổ cứng để không bị mất khi Restart
          state.setDirty();

          player.sendSystemMessage(Component.literal("§a🎉 Nâng cấp thành công! Lãnh thổ đã được mở rộng."));
          player.sendSystemMessage(Component.literal("§7(Bán kính mới: §b" + claim.getRadius() + " blocks§7)"));

          // Bắn hạt Particle để hiển thị ranh giới mới
          KyoClaimVisualizer.showBorder(player, claim);

          return 1;
        }));
      }

      // Lặp qua các từ khóa lệnh BÁN (sell, ban)
      for (String sellAlias : config.cmdSell) {
        mainCommand.then(Commands.literal(sellAlias).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          KyoClaimState state = server.overworld().getDataStorage().computeIfAbsent(KyoClaimState.TYPE);

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
        mainCommand.then(Commands.literal(trustAlias).then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          KyoClaimState state = server.overworld().getDataStorage().computeIfAbsent(KyoClaimState.TYPE);

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
        })));
      }

      // Lặp qua các từ khóa lệnh ĐUỔI BẠN (untrust, duoiban, kick)
      for (String untrustAlias : config.cmdUntrust) {
        mainCommand.then(Commands.literal(untrustAlias).then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          KyoClaimState state = server.overworld().getDataStorage().computeIfAbsent(KyoClaimState.TYPE);

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
        })));
      }

      // ==========================================
      // Sub-command: XEM TOẠ ĐỘ & LÀM GPS CHỈ ĐƯỜNG (info)
      // ==========================================
      for (String infoAlias : config.cmdInfo) {
        mainCommand.then(Commands.literal(infoAlias).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          UUID playerUuid = player.getUUID();

          // Đọc dữ liệu đồng bộ từ ổ cứng
          KyoClaimState state = server.overworld().getDataStorage().computeIfAbsent(KyoClaimState.TYPE);

          if (!state.claims.containsKey(playerUuid)) {
            player.sendSystemMessage(Component.literal("§c❌ Sếp làm gì đã có sổ đỏ mà đòi xem! Gõ lệnh mua đất trước đi!"));
            return 0;
          }

          ClaimData claim = state.claims.get(playerUuid);
          BlockPos center = claim.getCenter();
          BlockPos playerPos = player.blockPosition();

          // Tính toán khoảng cách thực tế 3D (Pythagoras)
          int distance = (int) Math.sqrt(playerPos.distSqr(center));

          player.sendSystemMessage(Component.literal("§e📐 THÔNG TIN ĐẠI BẢN DOANH:"));
          player.sendSystemMessage(Component.literal("§7- Chủ sở hữu: §f" + claim.getOwnerName()));
          player.sendSystemMessage(Component.literal("§7- Tọa độ lõi: §aX: " + center.getX() + " | Y: " + center.getY() + " | Z: " + center.getZ()));
          player.sendSystemMessage(Component.literal("§7- Bán kính lãnh thổ: §b" + claim.getRadius() + " blocks"));
          player.sendSystemMessage(Component.literal("§7- 📏 Cách vị trí hiện tại: §c" + distance + " blocks"));

          ServerLevel level = (ServerLevel) player.level();

          // XỬ LÝ HIỆU ỨNG THÔNG MINH THEO KHOẢNG CÁCH
          if (distance <= 32) {
            // Trường hợp 1: Ở gần -> Bật máy chiếu ranh giới Vòm Sắt xung quanh nhà
            KyoClaimVisualizer.showBorder(player, claim);
            player.sendSystemMessage(Component.literal("§a✨ Đã bật máy chiếu hiển thị ranh giới Lãnh thổ trong 15 giây sếp nhé!"));
          } else {
            // Trường hợp 2: Ở xa -> Tạo dải hạt "La Bàn Chỉ Đường" (GPS Trail) hướng về nhà
            double pX = player.getX();
            double pY = player.getY() + 1.0; // Ngang tầm mắt/ngực
            double pZ = player.getZ();

            // Tính Vector chỉ hướng từ Player -> Lõi căn cứ
            double dirX = center.getX() + 0.5 - pX;
            double dirY = center.getY() + 0.5 - pY;
            double dirZ = center.getZ() + 0.5 - pZ;

            double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (len > 0) {
              dirX /= len;
              dirY /= len;
              dirZ /= len;
            }

            // Bắn 10 hạt nối đuôi nhau tạo thành một mũi tên chỉ đường trước mặt player bằng hiệu ứng Sao chổi (END_ROD)
            for (int i = 1; i <= 10; i++) {
              double spawnX = pX + (dirX * i * 0.8);
              double spawnY = pY + (dirY * i * 0.8);
              double spawnZ = pZ + (dirZ * i * 0.8);
              level.sendParticles(ParticleTypes.END_ROD, spawnX, spawnY, spawnZ, 1, 0, 0, 0, 0.0);
            }

            // FIX: Bắn packet âm thanh trực tiếp cho Player theo chuẩn 1.21.4
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    1.0f, 1.5f,
                    player.getRandom().nextLong()
            ));

            player.sendSystemMessage(Component.literal("§e🧭 Sếp đang ở xa! Đã bắn một dải hạt sáng chỉ hướng Đại Bản Doanh ngay trước mặt sếp!"));
            player.sendSystemMessage(Component.literal("§e🚕 Hoặc gõ §a/" + config.commandAliases.get(0) + " " + config.cmdHome.get(0) + " §eđể đi xe ôm về tận giường luôn sếp ơi!"));
          }

          return 1;
        }));
      }

      // ==========================================
      // Sub-command: VỀ NHÀ (home) - ĐỒNG BỘ HOÀN HẢO & XUYÊN DIMENSION
      // ==========================================
      for (String homeAlias : config.cmdHome) {
        mainCommand.then(Commands.literal(homeAlias).executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          MinecraftServer server = context.getSource().getServer();
          UUID playerUuid = player.getUUID();

          // Đọc ổ cứng ở Overworld (Nơi lưu dữ liệu gốc)
          KyoClaimState state = server.overworld().getDataStorage().computeIfAbsent(KyoClaimState.TYPE);

          if (!state.claims.containsKey(playerUuid)) {
            player.sendSystemMessage(Component.literal("§c❌ Tôi không có nhà để về! Gõ lệnh mua đất trước đi sếp ơi!"));
            return 0;
          }

          ClaimData claim = state.claims.get(playerUuid);
          double balance = KyoEconomyAPI.getBalanceInXu(server, playerUuid);
          int taxiPrice = config.homeTeleportPrice;

          if (balance < taxiPrice) {
            player.sendSystemMessage(Component.literal("§c❌ Nghèo quá! Sếp cần " + taxiPrice + " Xu để gọi Taxi về nhà!"));
            return 0;
          }

          KyoEconomyAPI.removeMoneyInXu(server, playerUuid, taxiPrice);

          double homeX = claim.getCenter().getX() + 0.5;
          double homeY = claim.getCenter().getY() + 1.0;
          double homeZ = claim.getCenter().getZ() + 0.5;

          // Bắn khói ở vị trí cũ (Có thể là Nether hoặc The End)
          ServerLevel currentLevel = (ServerLevel) player.level();
          currentLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);

          // 🔥 FIX CHÍ MẠNG: CHỐT CỨNG ĐIỂM ĐẾN LÀ OVERWORLD
          ServerLevel overworld = server.overworld();

          // Teleport xuyên không gian sang Overworld
          player.teleportTo(overworld, homeX, homeY, homeZ, java.util.Set.of(), player.getYRot(), player.getXRot(), false);

          // Bắn hạt chào mừng tại Overworld
          overworld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, homeX, homeY + 1, homeZ, 30, 0.5, 0.5, 0.5, 0.5);

          player.sendSystemMessage(Component.literal("§a🚕 Đã trừ §e" + taxiPrice + " Xu§a. Xe ôm đã đưa sếp từ xa xăm về tới Đại Bản Doanh an toàn!"));
          return 1;
        }));
      }
      // Đăng ký toàn bộ cụm lệnh này vào bộ máy Server
      dispatcher.register(mainCommand);
    }
  }
}
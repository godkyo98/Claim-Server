package kyoclaim.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClaimData {
  private final UUID owner;
  private final String ownerName;
  private final BlockPos center;
  private int radius;
  // Thêm danh sách những người được cấp quyền
  private final List<UUID> trustedPlayers;

  // BỘ MÃ HÓA CODEC (Đã cập nhật để lưu thêm List<UUID>)
  public static final Codec<ClaimData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      UUIDUtil.CODEC.fieldOf("owner").forGetter(ClaimData::getOwner),
      Codec.STRING.fieldOf("ownerName").forGetter(ClaimData::getOwnerName),
      BlockPos.CODEC.fieldOf("center").forGetter(ClaimData::getCenter),
      Codec.INT.fieldOf("radius").forGetter(ClaimData::getRadius),
      // Codec đọc/ghi danh sách, mặc định là list rỗng nếu chưa có ai
      Codec.list(UUIDUtil.CODEC).optionalFieldOf("trustedPlayers", new ArrayList<>()).forGetter(ClaimData::getTrustedPlayers)
  ).apply(instance, ClaimData::new));

  public ClaimData(UUID owner, String ownerName, BlockPos center, int radius, List<UUID> trustedPlayers) {
    this.owner = owner;
    this.ownerName = ownerName;
    this.center = center;
    this.radius = radius;
    this.trustedPlayers = trustedPlayers != null ? trustedPlayers : new ArrayList<>();
  }

  // Constructor phụ cho lúc mới tạo nhà
  public ClaimData(UUID owner, String ownerName, BlockPos center, int radius) {
    this(owner, ownerName, center, radius, new ArrayList<>());
  }

  public UUID getOwner() { return owner; }
  public String getOwnerName() { return ownerName; }
  public BlockPos getCenter() { return center; }
  public int getRadius() { return radius; }
  public List<UUID> getTrustedPlayers() { return trustedPlayers; }

  public void addRadius(int amount) { this.radius += amount; }

  // Kiểm tra một người có quyền tương tác với nhà không (Là chủ HOẶC nằm trong list tin tưởng)
  public boolean hasPermission(UUID playerUuid) {
    return owner.equals(playerUuid) || trustedPlayers.contains(playerUuid);
  }

  // Thêm bạn bè
  public boolean trustPlayer(UUID playerUuid) {
    if (!trustedPlayers.contains(playerUuid)) {
      trustedPlayers.add(playerUuid);
      return true;
    }
    return false;
  }

  // Đuổi bạn bè
  public boolean untrustPlayer(UUID playerUuid) {
    return trustedPlayers.remove(playerUuid);
  }

  public boolean contains(BlockPos pos) {
    return Math.abs(pos.getX() - center.getX()) <= radius &&
        Math.abs(pos.getZ() - center.getZ()) <= radius;
  }

  public boolean overlapsWith(BlockPos otherCenter, int otherRadius) {
    return Math.abs(this.center.getX() - otherCenter.getX()) <= (this.radius + otherRadius) &&
        Math.abs(this.center.getZ() - otherCenter.getZ()) <= (this.radius + otherRadius);
  }
}
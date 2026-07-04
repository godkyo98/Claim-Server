package kyoclaim;

import kyoclaim.command.KyoClaimCommand;
import kyoclaim.config.KyoClaimConfig;
import kyoclaim.event.KyoClaimEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KyoClaim implements ModInitializer {
	public static final String MOD_ID = "kyoclaim";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Kyo Claim] Đang khởi động hệ thống Bất Động Sản...");

		// 1. NẠP CONFIG TỪ Ổ CỨNG LÊN RAM TRƯỚC TIÊN
		KyoClaimConfig.load();
		LOGGER.info("[Kyo Claim] Đã nạp xong file kyoclaim.json!");

		// 2. Kích hoạt Máy chiếu 3D
		KyoClaimVisualizer.register();

		// 3. Kích hoạt chặn phá hoại
		KyoClaimEvents.register();

		// 4. Kích hoạt Lệnh (Bây giờ lệnh sẽ linh động theo Config)
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			KyoClaimCommand.register(dispatcher);
		});
	}
}
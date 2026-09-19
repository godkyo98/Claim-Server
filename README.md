# 🏰 KYO CLAIM - BẢO VỆ LÃNH THỔ TỐI THƯỢNG

![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-2ea44f?style=for-the-badge&logo=minecraft)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.5-dbd087?style=for-the-badge)
![Mod Version](https://img.shields.io/badge/Version-1.0.5-blue?style=for-the-badge)
![Platform](https://img.shields.io/badge/Fabric-Server--Side-E3C95A?style=for-the-badge)

Dự án Server-side độc quyền thuộc hệ sinh thái **TEA Server**, mang đến giải pháp quản lý và bảo hộ đất đai toàn diện, an toàn và tối ưu tài nguyên tuyệt đối mà **không yêu cầu cài đặt bất kỳ mod nào ở phía Client**.

---

## 🌟 TÍNH NĂNG ĐỘT PHÁ

### 1. Mua Đất & Thiết Lập Thánh Địa (Base-Building)
* **Khởi tạo theo bán kính tự do:** Không gò bó theo các "Chunk" vuông vức nhàm chán. Kyo Claim thiết lập một **Lõi Trung Tâm** ngay tại tọa độ bạn đang đứng.
* **Bảo vệ toàn diện:** Sử dụng lệnh `/nha mua` (hoặc `/claim buy`) với chi phí cơ bản **5,000 Xu** để kích hoạt lõi bảo vệ. Mọi khối (block), rương đồ và thực thể trong phạm vi thánh địa đều được bảo vệ nghiêm ngặt trước hành vi đập phá hoặc trộm cắp từ người lạ.

### 2. Mở Rộng Quy Mô Lãnh Thổ
* Mở rộng ranh giới dễ dàng bằng lệnh `/nha nangcap` (hoặc `/claim upgrade`) với chi phí **20,000 Xu**.
* Bán kính bảo hộ lập tức mở rộng tức thì, bao trọn nông trại, kho báu và các công trình phụ trợ của bạn.

### 3. Vòm Hiệu Ứng & Lưới Laser 3D
* **Hiển thị trực quan:** Khi mua đất, nâng cấp, hoặc gõ `/nha info` trong phạm vi lãnh thổ, máy chiếu ranh giới 3D sẽ tự động dựng khung trong **15 giây**.
* **Đường biên sắc nét:** Hàng rào lửa (`FLAME`) bám sát bề mặt địa hình kết hợp cùng 4 cột laser ma thuật (`END_ROD`) dựng đứng tại 4 góc, giúp bạn và đồng đội xác định ranh giới một cách chuẩn xác.

### 4. Định Vị La Bàn GPS (Dẫn Đường Tức Thì)
* Khi lạc đường ở khoảng cách xa, gõ `/nha info` để kích hoạt tia sao chổi rực rỡ bắn thẳng về hướng lãnh thổ kèm âm thanh ping định vị. Người chơi chỉ cần bám theo vệt sáng để tìm đường trở về căn cứ.

### 5. Dịch Chuyển An Toàn Xuyên Chiều Không Gian ("Taxi Xe Ôm")
* **Hồi quy Overworld an toàn:** Lệnh `/nha ve` (hoặc `/claim home`) với mức phí taxi **1,000 Xu** cho phép bạn dịch chuyển tức thì về thẳng đại bản doanh.
* **Bảo vệ tuyệt đối:** Hệ thống luôn chủ động đọc trạng thái khu đất từ Thế giới thực (**Overworld**) và ép luồng dịch chuyển về Overworld an toàn, loại bỏ triệt để nguy cơ kẹt nhân vật hay lỗi vị trí khi kích hoạt từ Nether hoặc The End.
* **Hiệu ứng kép:** Phát hiệu ứng hạt bốc hơi tại chiều không gian xuất phát và hiệu ứng pháo hoa đón chào rực rỡ tại điểm đến.

### 6. Quản Lý Quyền Hạn Đất Đai
* `/nha trust <người chơi>`: Cấp quyền xây dựng, canh tác và mở rương cho bạn bè cùng sinh sống.
* `/nha untrust <người chơi>`: Thu hồi quyền sử dụng đất ngay lập tức.
* `/nha sell`: Thanh lý khu đất để thu hồi một phần vốn khi chuyển địa điểm sinh sống.

---

## 💻 GÓC NHÌN DEVELOPER (Kiến Trúc Kỹ Thuật)

* **100% Server-Side:** Toàn bộ hiệu ứng hạt 3D (Particle Engine), gói tin âm thanh (SoundPackets), và phép tính vector dẫn đường (Vector Math) được tính toán hoàn toàn trên CPU máy chủ và đồng bộ trực tiếp xuống Client vanilla.
* **Lưu trữ Cố định Chuẩn Mới (`SavedDataType`):**
  * Dữ liệu trạng thái đất đai được gắn trực tiếp vào hệ thống `server.overworld().getDataStorage()` qua `KyoClaimState.TYPE`.
  * Tích hợp **Hybrid UUID Codec** (`STRING` $\leftrightarrow$ numeric UUID), đảm bảo khả năng tương thích ngược mượt mà với dữ liệu cũ trên đĩa mà không lo lỗi hỏng file lưu trữ.
* **Tích Hợp KyoEconomy:** Kết nối chặt chẽ với hệ sinh thái **KyoEconomy (Modrinth Maven)**, cân bằng dòng tiền thông qua cơ chế đốt coin (sink) từ phí mua đất, nâng cấp và dịch chuyển.

---

## ⚙️ CẤU HÌNH MẪU (`config/kyoclaim.json`)

```json
{
  "basePrice": 5000.0,
  "upgradePrice": 20000.0,
  "homeTeleportPrice": 1000.0,
  "defaultRadius": 16,
  "upgradeRadius": 32,
  "cmdClaim": ["claim", "nha"],
  "cmdInfo": ["info", "thongtin"],
  "cmdHome": ["home", "ve"]
}
```
---
## Phát triển bởi Kyo — Dành riêng cho Kỷ nguyên TEA Server.

# BTLAndroid - Ứng Dụng Bán Thực Phẩm Hữu Cơ

![Logo App](app/src/main/res/drawable/logohealthyapp.png)

Ứng dụng di động Android giúp người dùng mua sắm thực phẩm hữu cơ, rau củ quả tươi với giao diện dễ sử dụng và trải nghiệm mua sắm mượt mà.

## Tính Năng

### Dành Cho Người Dùng
- **Đăng nhập/Đăng ký**: Đăng ký tài khoản mới hoặc đăng nhập bằng tài khoản hiện có và Google
- **Duyệt Sản Phẩm**: Xem danh sách các loại sản phẩm theo danh mục
- **Tìm Kiếm**: Tìm kiếm sản phẩm theo tên hoặc mô tả
- **Giỏ Hàng**: Thêm sản phẩm vào giỏ hàng và thanh toán
- **Yêu Thích**: Đánh dấu sản phẩm yêu thích để mua sau
- **Đơn Hàng**: Theo dõi trạng thái đơn hàng và xem lịch sử mua hàng
- **Hồ Sơ Cá Nhân**: Quản lý thông tin cá nhân, ảnh đại diện
- **Trò Chuyện**: Chat với người bán/admin để được hỗ trợ

### Dành Cho Admin
- **Quản Lý Sản Phẩm**: Thêm, sửa, xóa sản phẩm
- **Quản Lý Đơn Hàng**: Xem và cập nhật trạng thái đơn hàng
- **Quản Lý Người Dùng**: Xem danh sách người dùng và quản lý quyền
- **Thống Kê Thu Nhập**: Xem biểu đồ doanh thu và các sản phẩm bán chạy
- **Chat Hỗ Trợ**: Trả lời tin nhắn của khách hàng

## Công Nghệ Sử Dụng
- **Firebase Authentication**: Xác thực người dùng
- **Firebase Realtime Database**: Lưu trữ dữ liệu sản phẩm, đơn hàng, người dùng
- **Firebase Storage**: Lưu trữ hình ảnh sản phẩm và ảnh đại diện người dùng
- **Glide & Picasso**: Hiển thị hình ảnh
- **MPAndroidChart**: Tạo biểu đồ thống kê doanh thu
- **CircleImageView**: Hiển thị ảnh đại diện dạng tròn

## Cấu Trúc Dự Án
- **Activities**: Chứa các màn hình chính của ứng dụng
- **Adapters**: Chứa các adapter để hiển thị dữ liệu trong RecyclerView
- **Domain/Model**: Chứa các lớp mô tả dữ liệu (User, Product, Order, etc.)
- **Utils**: Các tiện ích và hàm hỗ trợ

## Cài Đặt & Chạy Dự Án

### Yêu Cầu
- Android Studio Hedgehog hoặc mới hơn
- JDK 17 trở lên
- Thiết bị hoặc máy ảo chạy Android API level 24 (Android 7.0) trở lên

### Các Bước Cài Đặt
1. Clone dự án từ GitHub:
   ```
   git clone https://github.com/username/BTLAndroid.git
   ```

2. Mở dự án trong Android Studio

3. Sync Gradle và cài đặt các dependencies

4. Chạy ứng dụng trên thiết bị hoặc máy ảo

## Cấu Hình Firebase
Dự án đã được cấu hình với Firebase. Nếu bạn muốn sử dụng Firebase của riêng mình, hãy thay thế file `google-services.json` trong thư mục `app/` và cập nhật các thông tin liên quan.

## Cấu Trúc Database
Dự án sử dụng Firebase Realtime Database với các node chính:
- **Users**: Thông tin người dùng
- **Admins**: Danh sách admin
- **Items/Products**: Thông tin sản phẩm
- **Category**: Các danh mục sản phẩm
- **Orders**: Thông tin đơn hàng
- **Cart**: Giỏ hàng của người dùng
- **Favorites**: Sản phẩm yêu thích
- **Messages**: Tin nhắn giữa người dùng và admin

## Đóng Góp
Nếu bạn muốn đóng góp cho dự án, vui lòng fork repository, tạo branch mới, thêm tính năng hoặc sửa lỗi, và tạo Pull Request.

## Giấy Phép
Dự án được phát hành theo giấy phép MIT. Xem file LICENSE để biết thêm chi tiết.

---
© 2025 BTLAndroid. Tất cả quyền được bảo lưu.

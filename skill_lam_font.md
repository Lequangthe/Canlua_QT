# Skill: Tạo Font "Downloadable Google Fonts" trong Jetpack Compose

Ghi lại kinh nghiệm kỹ thuật khi làm tính năng chọn nhiều kiểu chữ bằng **Google Fonts downloadable** (tải font về qua mạng, không nhúng `.ttf` vào APK). Áp dụng cho app Compose Material3.

---

## 1. Nguyên lý

- Font không được đóng gói trong APK mà được tải lúc chạy từ provider `com.google.android.gms.fonts` (Google Play Services) qua **Downloadable Fonts**.
- Compose dùng `Font(googleFont = GoogleFont("TênFont"), fontProvider = provider, weight = ...)` để khai báo font sẽ tải.
- Build thành `FontFamily`, nhúng vào `Typography` của `MaterialTheme` → toàn app đổi font theo setting.
- Cần `LocalFontFamilyResolver` để Compose resolve font bất đồng bộ (tải về rồi render lại).

---

## 2. Các bước triển khai chuẩn

1. **Dependency** (trong `gradle/libs.versions.toml` + `app/build.gradle.kts`):
   ```kotlin
   // libs.versions.toml
   androidx-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }
   // build.gradle.kts
   implementation(libs.androidx.google.fonts)
   ```
   Version do Compose BOM quản lý.

2. **`res/values/font_certs.xml`** — cert xác thực provider (BẮT BUỘC, xem mục Lỗi #2):
   ```xml
   <array name="com_google_android_gms_fonts_certs">
       <item>@array/com_google_android_gms_fonts_certs_dev</item>
       <item>@array/com_google_android_gms_fonts_certs_prod</item>
   </array>
   <string-array name="com_google_android_gms_fonts_certs_dev">...</string-array>
   <string-array name="com_google_android_gms_fonts_certs_prod">...</string-array>
   ```

3. **Manifest** — thêm quyền INTERNET (BẮT BUỘC, xem Lỗi #1):
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

4. **`ui/theme/Type.kt`**:
   ```kotlin
   val provider = GoogleFont.Provider(
       providerAuthority = "com.google.android.gms.fonts",
       providerPackage = "com.google.android.gms",
       certificates = R.array.com_google_android_gms_fonts_certs
   )

   fun getFontFamily(fontName: String): FontFamily {
       if (fontName == "Default") return FontFamily.Default
       val font = GoogleFont(fontName)
       return FontFamily(
           Font(googleFont = font, fontProvider = provider, weight = FontWeight.Normal),
           Font(googleFont = font, fontProvider = provider, weight = FontWeight.Bold)
       )
   }

   fun getTypography(scale: Float, fontName: String): Typography { ... } // build toàn bộ TextStyle với fontFamily
   ```

5. **`ui/theme/Theme.kt`** — truyền typography vào MaterialTheme:
   ```kotlin
   MaterialTheme(colorScheme = ..., typography = getTypography(fontScale, fontFamilyName), content = ...)
   ```

6. **`MainActivity.kt`** — cung cấp resolver + đọc setting font:
   ```kotlin
   CompositionLocalProvider(
       LocalFontFamilyResolver provides createFontFamilyResolver(LocalContext.current, handler)
   ) {
       CANLUAV3Theme(fontScale = appSettings.globalFontScale, fontFamilyName = appSettings.fontFamilyName) { ... }
   }
   ```

7. **`AppSettings`** thêm `fontFamilyName: String = "Default"` + migration Room (hoặc `fallbackToDestructiveMigration`).

8. **`SettingsScreen`** — radio list các font: `"Default", "Roboto", "Montserrat", "Oswald", "Smooch Sans", "Alfa Slab One", "Lobster", "Quicksand", "Roboto Mono"`; onClick → `updateAppSettings(settings.copy(fontFamilyName = id))`.

---

## 3. NHỮNG LỖI ĐÃ MẮC PHẢI (CẢNH BÁO / CẦN TRÁNH)

### LỖI #1 — Thiếu quyền INTERNET → font không bao giờ hiện, fallback mặc định (CRITICAL)
- **Triệu chứng**: Chọn font mới nhưng toàn app vẫn font mặc định; log chỉ thấy `Building FontFamily for: X`, **không có lỗi**.
- **Nguyên nhân**: Downloadable fonts phải tải file font qua mạng từ provider. Không có `android.permission.INTERNET` → tải thất bại âm thầm → Compose fallback về Roboto/mặc định.
- **Cách tránh**: Luôn khai báo `<uses-permission android:name="android.permission.INTERNET" />` trong `AndroidManifest.xml`.

### LỖI #2 — `font_certs.xml` sai → `bad base-64` crash (CRITICAL)
- **Triệu chứng**: Logcat:
  ```
  FontDebug E  Error checking provider availability
  java.lang.IllegalArgumentException: bad base-64
      at androidx.core.content.res.FontResourcesParserCompat.toByteArrayList
  ```
- **Nguyên nhân**:
  - Chuỗi base64 cert bị hỏng (mất ký tự / sai padding) — thường do copy tay.
  - Chỉ có 1 cert (thiếu cert `prod`), hoặc cấu trúc `array` không tham chiếu 2 string-array.
- **Cách tránh**:
  - **KHÔNG tự gõ cert**. Lấy nguyên file từ sample chính thức:
    `github.com/android/user-interface-samples/DownloadableFonts/app/src/main/res/values/font_certs.xml`.
  - Cấu trúc đúng gồm `array` tham chiếu 2 `string-array` `_dev` + `_prod`.
  - Kiểm tra nhanh bằng PowerShell:
    ```powershell
    [Convert]::FromBase64String($certString.Trim())  # không throw = hợp lệ
    ```
  - Sau khi sửa phải **build lại và cài lại** (không phải hot reload).

### LỖI #3 — Yêu cầu weight không tồn tại (WARNING)
- Nhiều font Google chỉ có 1 weight (vd `Alfa Slab One`, `Lobster` chỉ có Regular 400). Nếu khai báo `FontWeight.Medium/Bold` cho font đó có thể resolve thất bại → fallback.
- **Cách tránh**: chỉ khai báo các weight chắc chắn tồn tại (thường Normal + Bold), Compose tự chọn weight gần nhất (`bestEffort` mặc định).

### LỖI #4 — Tạo lại FontFamily/Resolver liên tục khi recompose (OPTIMIZE)
- Gọi `createFontFamilyResolver(context, handler)` trong composition mỗi recompose → tạo resolver mới lặp lại. Các instance share cùng typeface cache nên không vỡ, nhưng nên tạo **một lần** (`remember {}`) để tránh tải lại không cần thiết.

### LỖI #5 — Tên font không khớp catalog Google Fonts (WARNING)
- Tên trong `GoogleFont("...")` phải khớp chính xác tên font trên fonts.google.com. Tên sai → provider trả "font not found" → fallback Roboto âm thầm.

### Lưu ý vận hành
- Lần chọn font đầu tiên **cần có mạng**; sau đó hệ thống cache font nên lần sau nhanh.
- Test trên máy thật có Google Play Services (emulator không GMS → provider không available → fallback mặc định).
- Device báo `WindowOnBackInvokedCallback is not enabled` là cảnh báo vô hại, không liên quan font.

---

## 4. Checklist kiểm tra khi làm font downloadable

- [ ] `AndroidManifest.xml` có `android.permission.INTERNET`
- [ ] `font_certs.xml` lấy từ sample chính thức, base64 VALID, đủ `_dev` + `_prod`
- [ ] Dependency `ui-text-google-fonts` có trong build.gradle
- [ ] Provider + `getFontFamily` + `getTypography` đúng
- [ ] `MainActivity` cung cấp `LocalFontFamilyResolver` + đọc `fontFamilyName` từ AppSettings
- [ ] Setting `fontFamilyName` có migration Room (hoặc fallback destructive khi chưa production)
- [ ] Tên font khớp fonts.google.com
- [ ] Build sạch + cài lại app trước khi test

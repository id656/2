# SuperProxy

Android-приложение: направляет весь трафик телефона через HTTP-прокси (VpnService + tun2proxy).
Профиль из `superproxy-proxylist-*.txt` загружается при первом запуске (assets/default_profiles.txt);
другие списки того же формата можно импортировать кнопкой в приложении.

## Сборка

1. Нативная библиотека (один раз):
   ```
   rustup target add aarch64-linux-android armv7-linux-androideabi
   cargo install cargo-ndk
   git clone https://github.com/tun2proxy/tun2proxy && cd tun2proxy
   cargo ndk -t arm64-v8a -t armeabi-v7a -o /путь/к/SuperProxy/app/src/main/jniLibs build --release
   ```
   Должны появиться `jniLibs/arm64-v8a/libtun2proxy.so` и `jniLibs/armeabi-v7a/libtun2proxy.so`.
   (Нужны Android NDK и переменная ANDROID_NDK_HOME.)
2. Открыть папку в Android Studio -> Build -> Build APK(s).

## Заметки
- Если в пароле есть спецсимволы (@ : / # %), закодируйте их в процентной форме.
- HTTP-прокси передаёт только TCP; UDP (например QUIC) не пройдёт, браузеры откатятся на TCP.
- Само приложение исключено из VPN (addDisallowedApplication), чтобы не было петли.

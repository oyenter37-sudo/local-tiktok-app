<div align="center">

# 🌝 Local TikTok

**Смотри тт без интернета** — вертикальная лента в стиле TikTok из твоих собственных видео и фото. Полностью офлайн.

[![Build APK](https://github.com/oyenter37-sudo/local-tiktok-app/actions/workflows/build.yml/badge.svg)](https://github.com/oyenter37-sudo/local-tiktok-app/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/oyenter37-sudo/local-tiktok-app?label=APK&color=FE2C55)](https://github.com/oyenter37-sudo/local-tiktok-app/releases/latest)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)
![Offline](https://img.shields.io/badge/internet-не%20нужен-25F4EE)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

### [⬇️ Скачать APK](https://github.com/oyenter37-sudo/local-tiktok-app/releases/latest/download/LocalTikTok.apk)

</div>

---

## ✨ Возможности

| | |
|---|---|
| 📱 **Лента «Для вас»** | Вертикальное листание, видео и посты с картинками вперемешку |
| 🖼 **Карусели** | 1–3 картинки в посте, точки и свайп вбок |
| 🌝 **Вместо лайков** | Двойной тап — вылетает 🌝 с анимацией |
| 👁 **Честные просмотры** | Считаются только твои реальные просмотры |
| 🔁 **Без повторов** | Сначала всё непросмотренное, потом сброс и снова полный рандом |
| 🎲 **Шансы** | Слайдер шанса картинок и вес каждого альбома |
| ⏩ **Жесты** | Удержание у края — x2, удержание в центре — меню, свайп влево — профиль |
| 👤 **Альбомы = авторы** | У каждой папки свой профиль с сеткой и статистикой |
| 📊 **Мои итоги** | Время просмотра, топ альбомов, пересматриваемое, серия дней 🔥 |
| 🔍 **Поиск** | По именам файлов и альбомов |
| ⭐ **Сохранённое и история** | Всё хранится локально на устройстве |

## 🚀 Как начать

1. Скачай [`LocalTikTok.apk`](https://github.com/oyenter37-sudo/local-tiktok-app/releases/latest/download/LocalTikTok.apk) и установи.
2. Разреши доступ к фото и видео.
3. **Профиль → ⚙ → Альбомы** — добавь папки (например `/storage/emulated/0/DCIM/Camera`) или выбери из подсказок.
4. Листай 🌝

> По умолчанию альбомов нет — приложение показывает только то, что ты сам добавишь.

## 🛠 Сборка

APK собирается автоматически в **GitHub Actions** при каждом пуше в `main` и публикуется в [Releases](https://github.com/oyenter37-sudo/local-tiktok-app/releases).

Локально (нужны JDK 17 и Gradle 8.9):

```bash
gradle :app:assembleRelease
```

**Стек:** Kotlin 2.0 · Jetpack Compose (Material 3) · Media3 ExoPlayer · Coil (GIF + превью видео) · minSdk 26 / target 34

## 🔒 Приватность

Никакого интернета, аналитики и рекламы. Приложение даже не запрашивает разрешение на сеть — все данные остаются на телефоне.

## ⚠️ Дисклеймер

Неофициальный фанатский проект, не связан с TikTok / ByteDance. Иконка и логотип — оригинальные.

## 📄 Лицензия

[MIT](LICENSE)

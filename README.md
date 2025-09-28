# 🎨 Artify - Android Art Explorer App

**Artify** is a beautifully designed Android app that lets users explore artists, view artworks, and save their favorites. Built using **Jetpack Compose** and connected to a backend (like Artsy API or GCP), Artify brings the world of art into your pocket.

## 📱 Screenshots

![App Screenshot](screenshots/app-screenshot.png)

## 🛠️ Tech Stack

* **Kotlin + Jetpack Compose** – Modern Android UI toolkit
* **Retrofit** – API communication
* **Coil** – Image loading
* **PersistentCookieJar** – Session persistence
* **Material 3** – Theming and design system
* **GCP / Artsy API** – Backend integration

## 🚀 Features

* 🔍 **Search Artists** by name or keyword
* 🧑‍🎨 **View Artist Details** including bio, image, and active years
* 🖼️ **See Artworks** by each artist
* ⭐ **Add/Remove Favorites** (persisted across sessions)
* 🌓 **Dark/Light Mode** toggle support
* 🔐 **Login/Register** system (JWT-based)
* 🧭 **Bottom Navigation** with Home, Search, and Favorites

## 📦 How to Run the App

1. **Clone the repository:**
   ```bash
   git clone https://github.com/shettyrohit0810/Android-Art-Explorer-App.git
   cd Android-Art-Explorer-App
   ```

2. **Open in Android Studio:**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned repository folder
   - Select the `ArstyApp` folder

3. **Build and Run:**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press `Shift + F10`

## 📁 Project Structure

```
ArstyApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/arstyapp/
│   │   │   ├── data/          # Data layer (API, models)
│   │   │   ├── ui/            # UI components and screens
│   │   │   └── MainActivity.kt
│   │   └── res/               # Resources (layouts, drawables, etc.)
│   └── build.gradle.kts       # App-level dependencies
└── build.gradle.kts           # Project-level configuration
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Sahil Shetty**
- GitHub: [@shettyrohit0810](https://github.com/shettyrohit0810)

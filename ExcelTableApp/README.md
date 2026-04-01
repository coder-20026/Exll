# WhtsTable Master - Premium Android App

Premium WhatsApp data entry tool with floating button and Excel-compatible output.

## Features

- **Premium UI Design**: Modern Material Design with gradient buttons, cards, and smooth animations
- **Floating Quick Entry**: Add data from any app without switching
- **Auto-Parse WhatsApp Text**: Automatically extracts Bank Name, Applicant Name, and Reason
- **Excel-Compatible Copy**: Tab-separated format for direct paste into Excel
- **Row Management**: Edit, Delete, Move Up/Down functionality

## GitHub Actions Auto-Build

Yeh repository GitHub Actions ke saath configured hai. Jab aap code push karte ho:

### Automatic Downloads:
1. **Gradle Wrapper** - `gradlew`, `gradlew.bat` automatically generate hote hain
2. **gradle-wrapper.jar** - Automatically download hota hai
3. **gradle-wrapper.properties** - Automatically create hota hai
4. **Drawable PNG Files** - Premium app icons aur floating button icons generate hote hain

### Build Artifacts:
- **app-debug.apk** - Debug build for testing
- **app-release-unsigned.apk** - Release build

## How to Use

### Step 1: Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit - Premium WhtsTable Master"
git remote add origin https://github.com/YOUR_USERNAME/WhtsTableMaster.git
git push -u origin main
```

### Step 2: Check Actions
1. GitHub pe jaao
2. **Actions** tab pe click karo
3. **Android Build & Release** workflow automatically run hoga
4. Build complete hone pe **Artifacts** section me APK milega

### Step 3: Download APK
1. Actions tab me latest workflow run pe click karo
2. Page ke bottom me **Artifacts** section dekho
3. **app-debug** ya **app-release** download karo

## Project Structure

```
ExcelTableApp/
├── .github/
│   └── workflows/
│       └── android-build.yml    # Auto-build workflow
├── app/
│   ├── build.gradle             # App-level Gradle config
│   ├── proguard-rules.pro       # ProGuard optimization rules
│   └── src/main/
│       ├── AndroidManifest.xml  # App manifest
│       ├── java/com/whtstable/master/
│       │   ├── MainActivity.java
│       │   ├── FloatingButtonService.java
│       │   └── DataManager.java
│       └── res/
│           ├── drawable/        # Premium button backgrounds
│           ├── layout/          # Premium XML layouts
│           └── values/          # Colors, strings, styles
├── build.gradle                 # Root Gradle config
├── settings.gradle              # Gradle settings
└── gradle.properties            # Gradle properties
```

## Premium Design Elements

### Color Palette
- **Primary**: Deep Indigo (#1A237E)
- **Accent**: Purple (#7C4DFF)
- **Success**: Green (#4CAF50)
- **Warning**: Orange (#FF9800)
- **Danger**: Red (#FF5252)
- **Secondary**: Cyan (#00BCD4)

### UI Components
- Gradient buttons with press effects
- Rounded card containers with shadows
- Premium input fields with focus states
- Modern floating action button
- Elegant popup overlays

## Requirements

- Android 5.0+ (API 21+)
- Overlay permission for floating button

## Version History

- **v2.0.0**: Premium UI redesign with Material Design
- **v1.0.0**: Initial release with basic functionality

## License

MIT License - Free for personal and commercial use.

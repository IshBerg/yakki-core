# Yakki Core

**Pure Kotlin business logic module for Yakki Edu platform.**

## Overview

Yakki Core contains all platform-independent business logic that can be shared across:
- Android (via Gradle dependency)
- iOS (via Kotlin Multiplatform - future)
- Flutter (via Platform Channels)
- Web (via Kotlin/JS - future)

## Module Structure

```
yakki-core/
├── src/commonMain/kotlin/com/yakki/core/
│   ├── models/           # Data models
│   │   └── ClozeModels.kt
│   ├── logic/            # Business logic
│   │   └── ClozeReducer.kt
│   ├── economy/          # XP & Level system
│   │   ├── XPCalculator.kt
│   │   └── LevelCalculator.kt
│   └── utils/            # Utilities
│       └── SentenceUtils.kt
└── build.gradle.kts
```

## Key Components

### Models
- **ClozeModels** - Data classes for Cloze Drills game mode
- Serializable with kotlinx.serialization

### Logic
- **ClozeReducer** - Pure MVI reducer: `(State, Event) -> State`
- **ClozeValidator** - Answer validation with similarity scoring

### Economy
- **XPCalculator** - Unified XP calculation for all game modes
- **LevelCalculator** - CEFR-based level progression (A1-C2)

### Utils
- **SentenceUtils** - Smart tokenizer for sentence processing

## Usage

### In Android (Gradle)
```kotlin
// settings.gradle.kts
include(":yakki-core")

// app/build.gradle.kts
dependencies {
    implementation(project(":yakki-core"))
}

// Code
import com.yakki.core.economy.XPCalculator
import com.yakki.core.economy.CEFRLevel

val xp = XPCalculator.calculateClozeXP(
    level = CEFRLevel.B1,
    correctCount = 8,
    accuracy = 0.8f,
    wasOvertime = false
)
```

### In Flutter (Platform Channels)
```dart
// Dart code calls native Kotlin via MethodChannel
final result = await methodChannel.invokeMethod('calculateClozeXP', {
    'cefrLevel': 'B1',
    'correctCount': 8,
    'accuracy': 0.8,
    'wasOvertime': false,
});
```

## Build

```bash
# Build module
./gradlew :yakki-core:build

# Run tests
./gradlew :yakki-core:test
```

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-08 | Initial release with Cloze models |

## Dependencies

- Kotlin 2.0.21
- kotlinx-coroutines-core 1.9.0
- kotlinx-serialization-json 1.7.3

**No Android dependencies - pure Kotlin JVM!**

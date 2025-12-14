# Architecture Documentation

## Overview

Skywriter is an Android application built with modern Android architecture components, following MVVM (Model-View-ViewModel) pattern.

## Architecture Layers

```
┌─────────────────────────────────────┐
│           UI Layer                  │
│  (Fragments, Activities, Dialogs)   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         ViewModel Layer             │
│    (State Management, LiveData)     │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Repository Layer              │
│    (Data Access, Business Logic)      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Data Layer                 │
│  (Models, Database, JSON Parsing)    │
└─────────────────────────────────────┘
```

## Components

### UI Layer

**MainActivity**
- Host activity with navigation setup
- NFC intent handling
- Lifecycle management

**CharacterListFragment**
- Displays character grid
- Search functionality
- Character selection

**CharacterDetailDialog**
- Bottom sheet dialog
- Character information display
- Write action trigger

**WriteNFCFragment**
- NFC writing interface
- Progress indicators
- Result feedback

### ViewModel Layer

**CharacterViewModel**
- Manages character list state
- Handles search operations
- Provides LiveData for UI observation

### Repository Layer

**CharacterRepository**
- Abstracts data access
- Provides coroutine-based API
- Handles data loading

### Data Layer

**CharacterModel**
- Parcelable data class
- Represents character with NFC data
- Includes metadata

**CharacterMetadata**
- Character information
- Display name generation
- Game series categorization

**NFCDatabase**
- JSON file parsing
- Character loading from assets
- Search and filtering

### NFC Layer

**MifareClassicWriter**
- Tag authentication
- Block writing operations
- Error handling

**NFCManager**
- NFC adapter management
- Tag detection
- Foreground dispatch

**WriteResult**
- Sealed class for operation results
- Success and error states

## Data Flow

### Character Loading Flow

```
User opens app
    ↓
MainActivity created
    ↓
CharacterListFragment displayed
    ↓
CharacterViewModel.loadCharacters()
    ↓
CharacterRepository.getAllCharacters()
    ↓
NFCDatabase.loadCharacters()
    ↓
Parse JSON files from assets
    ↓
Return List<CharacterModel>
    ↓
Update LiveData
    ↓
UI updates via observer
```

### NFC Write Flow

```
User selects character
    ↓
CharacterDetailDialog shown
    ↓
User taps "Write to Tag"
    ↓
Navigate to WriteNFCFragment
    ↓
Fragment enables NFC foreground dispatch
    ↓
User taps phone to tag
    ↓
NFC intent received
    ↓
MifareClassicWriter.writeCharacter()
    ↓
Authenticate with tag
    ↓
Write blocks sequentially
    ↓
Return WriteResult
    ↓
Update UI with result
```

## Dependencies

### Core Android
- AndroidX Core KTX
- AppCompat
- Material Design Components
- Constraint Layout

### Architecture Components
- Lifecycle (ViewModel, LiveData)
- Navigation Component
- Coroutines

### Testing
- JUnit 4
- Mockito
- AndroidX Test
- Espresso (for UI tests)

## Design Patterns

### MVVM (Model-View-ViewModel)
- Separation of concerns
- Testable business logic
- Reactive UI updates

### Repository Pattern
- Single source of truth
- Abstraction of data sources
- Easy to mock for testing

### Sealed Classes
- Type-safe result handling
- Exhaustive when expressions
- Clear error states

## Testing Strategy

### Unit Tests
- Data models
- Business logic
- Utility functions

### Instrumented Tests
- Database operations
- NFC operations (mocked)
- Integration tests

### E2E Tests
- Simulated NFC write flow
- Data validation
- Error handling

## Future Enhancements

- Room database for caching
- WorkManager for background operations
- Hilt for dependency injection
- Compose UI migration
- Offline support


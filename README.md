# ChoreApp Android

A chore management Android application built with Kotlin, featuring JWT authentication and REST API integration.

## Features

### ✅ Implemented (9 points)

1. **Login Page (3 points)** - `LoginActivity.kt`
   - Username and password input fields
   - JWT token authentication
   - Token storage using SharedPreferences
   - Error handling for failed login attempts
   - Auto-navigation to chore list on successful login

2. **List Page (3 points)** - `ChoreListActivity.kt`
   - Master view with RecyclerView displaying all chores
   - Pull-to-refresh functionality
   - Display chore details: title, description, status, priority, due date, points
   - Color-coded priority and status badges
   - Floating action button to add new chores
   - Click on any chore to view/edit details
   - Logout functionality

3. **Edit/Detail Page (3 points)** - `ChoreDetailActivity.kt`
   - View and edit chore details
   - Create new chores
   - Update existing chores
   - Delete chores with confirmation dialog
   - Date picker for due date selection
   - Dropdown menus for status and priority
   - Input validation (title required)
   - Master-detail architecture

## Technical Implementation

### Architecture
- **Master-Detail UI Pattern**: List view shows all chores, detail view shows/edits individual chore
- **REST API Integration**: Full CRUD operations via Retrofit
- **JWT Authentication**: Secure authentication with token stored in SharedPreferences

### Technologies Used
- **Language**: Kotlin
- **UI**: Material Design Components, ViewBinding
- **Networking**: Retrofit 2.9.0, OkHttp 4.11.0
- **Async**: Kotlin Coroutines
- **Architecture**: Android Activity-based with lifecycle-aware components

### API Endpoints
- `POST /api/auth/login` - User authentication
- `GET /api/chores` - Fetch all chores
- `GET /api/chores/{id}` - Get single chore
- `POST /api/chores` - Create new chore
- `PUT /api/chores/{id}` - Update chore
- `DELETE /api/chores/{id}` - Delete chore

## Setup Instructions

### Prerequisites
- Android Studio (Arctic Fox or newer)
- Android SDK API 24+
- Backend server running on `http://localhost:3000`

### Configuration

1. **API Base URL Configuration**

   The app is configured to connect to your backend server. Two options:

   **For Android Emulator** (default):
   ```kotlin
   // In ApiConfig.kt
   const val BASE_URL = "http://10.0.2.2:3000/api/"
   ```
   Note: `10.0.2.2` is the special IP that maps to `localhost` on your host machine

   **For Physical Device**:
   ```kotlin
   // In ApiConfig.kt - Replace YOUR_MAC_IP with your actual IP
   const val BASE_URL = "http://YOUR_MAC_IP:3000/api/"
   ```

   To find your Mac's IP:
   ```bash
   ifconfig | grep "inet " | grep -v 127.0.0.1
   ```

2. **Start Backend Server**
   ```bash
   cd ../chore-app/backend
   npm run dev
   ```
   Ensure it's running on `http://localhost:3000`

3. **Open in Android Studio**
   - Open the `chore-app-android` folder in Android Studio
   - Wait for Gradle sync to complete
   - Click "Sync Project with Gradle Files" if needed

4. **Run the App**
   - Select an emulator or connect a physical device
   - Click the "Run" button or press Shift+F10
   - The app will install and launch

### First Time Setup

1. Create a user account in your backend (or use existing credentials)
2. Launch the app
3. Login with your credentials
4. Start managing your chores!

## Project Structure

```
app/src/main/java/com/choreapp/android/
├── LoginActivity.kt              # Login screen with JWT auth
├── ChoreListActivity.kt          # List view with RecyclerView
├── ChoreDetailActivity.kt        # Detail/Edit view for chores
├── api/
│   ├── ApiConfig.kt              # API base URL configuration
│   ├── ApiService.kt             # Retrofit API interface
│   ├── AuthInterceptor.kt        # JWT token interceptor
│   └── RetrofitClient.kt         # Retrofit client setup
├── models/
│   ├── Chore.kt                  # Chore data model
│   ├── User.kt                   # User data model
│   ├── LoginRequest.kt           # Login request model
│   ├── LoginResponse.kt          # Login response model
│   └── ChoreResponse.kt          # Chore response model
└── adapters/
    └── ChoreAdapter.kt           # RecyclerView adapter for chore list

app/src/main/res/
├── layout/
│   ├── activity_login.xml        # Login screen layout
│   ├── activity_chore_list.xml   # List screen layout
│   ├── activity_chore_detail.xml # Detail screen layout
│   └── item_chore.xml            # Chore item card layout
├── values/
│   ├── colors.xml                # App colors including priority/status colors
│   ├── strings.xml               # All string resources
│   └── themes.xml                # App themes
└── menu/
    └── menu_chore_list.xml       # Menu for list screen (logout, refresh)
```

## Features Detail

### Login Page
- Clean Material Design interface
- Input validation
- Loading indicator during authentication
- Error messages for failed login
- Persistent login (token saved in SharedPreferences)

### List Page
- Scrollable list of all chores
- Color-coded priority badges (High: Red, Medium: Orange, Low: Green)
- Color-coded status badges (Pending: Blue, In Progress: Orange, Completed: Green)
- Swipe-to-refresh for latest data
- FAB (Floating Action Button) to add new chores
- Empty state message when no chores exist
- Menu options: Logout and Refresh

### Detail/Edit Page
- View mode by default for existing chores
- Edit mode for creating new chores
- All fields editable: title, description, status, priority, due date, points
- Date picker dialog for due date selection
- Dropdown menus for status and priority
- Save button to persist changes
- Delete button with confirmation dialog
- Input validation
- Loading indicators during API calls

## Dependencies

Key dependencies (see `app/build.gradle` for full list):
- Retrofit 2.9.0 - REST API client
- OkHttp 4.11.0 - HTTP client with interceptors
- Gson - JSON serialization
- Material Design Components 1.11.0
- Kotlin Coroutines 1.7.3
- AndroidX Lifecycle components
- SwipeRefreshLayout for pull-to-refresh

## Testing

### Manual Testing Checklist

**Login Flow:**
- [ ] Enter invalid credentials - should show error
- [ ] Enter valid credentials - should navigate to list
- [ ] Close and reopen app - should stay logged in

**List Page:**
- [ ] View all chores with correct colors
- [ ] Pull down to refresh
- [ ] Click FAB to create new chore
- [ ] Click chore item to view details
- [ ] Logout from menu

**Detail Page:**
- [ ] Create new chore with all fields
- [ ] Edit existing chore
- [ ] Delete chore (with confirmation)
- [ ] Validate that title is required
- [ ] Use date picker for due date

## Troubleshooting

### Connection Issues

**"Unable to resolve host" or "Failed to connect"**
- Verify backend server is running: `curl http://localhost:3000/api/chores`
- Check `BASE_URL` in `ApiConfig.kt` matches your setup
- For emulator: use `10.0.2.2:3000`
- For device: use your Mac's IP address

### Build Issues

**"Manifest merger failed"**
- Sync Gradle files: File → Sync Project with Gradle Files
- Clean build: Build → Clean Project, then Build → Rebuild Project

**"SDK not found"**
- Set SDK location in Android Studio: File → Project Structure → SDK Location

### Runtime Issues

**App crashes on launch**
- Check Logcat for error messages
- Verify all dependencies are installed (Gradle sync)
- Ensure minimum SDK version is met (API 24+)

## API Data Models

### Chore Model
```kotlin
data class Chore(
    val id: Int?,
    val title: String,
    val description: String?,
    val status: String,           // "pending", "in_progress", "completed"
    val priority: String,         // "low", "medium", "high"
    val due_date: String?,        // Format: "YYYY-MM-DD"
    val points: Int,
    val created_at: String?,
    val updated_at: String?,
    val user_id: Int?
)
```

### Login Request/Response
```kotlin
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val user: User,
    val token: String             // JWT token
)
```

## Security

- JWT tokens stored securely in SharedPreferences
- All API requests include `Authorization: Bearer <token>` header
- `usesCleartextTraffic=true` enabled for localhost development (should be disabled in production)
- Input validation on all forms

## Future Enhancements

Potential improvements:
- [ ] Search and filter chores
- [ ] Sort by priority, due date, status
- [ ] Push notifications for due dates
- [ ] Offline mode with local database
- [ ] User registration flow
- [ ] Profile management
- [ ] Statistics and progress tracking

## Support

For issues or questions:
1. Check the Troubleshooting section
2. Review Logcat output in Android Studio
3. Verify backend server is running and accessible
4. Check API endpoint responses using Postman or curl

## License

Educational project for PDM course.
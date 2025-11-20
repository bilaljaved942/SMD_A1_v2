# 🚀 QUICK START GUIDE - Get Running in 30 Minutes

## Step-by-Step Setup

### ⏱️ 5 Minutes: Setup Backend

1. **Install Xampp**

   - Download: https://www.apachefriends.org/
   - Install and start Apache + MySQL

2. **Copy Server Files**

   ```bash
   # Copy the 'server' folder to:
   C:\xampp\htdocs\socially\
   ```

3. **Create Database**

   - Open: http://localhost/phpmyadmin
   - Create new database: `socially_db`
   - Import: `server/database_setup.sql`
   - Or run the SQL manually from MIGRATION_GUIDE.md

4. **Test API**
   - Open browser: http://localhost/socially/api/auth/session
   - You should see JSON response

---

### ⏱️ 10 Minutes: Configure Android App

1. **Open Project in Android Studio**

   ```bash
   File > Open > Select "d:\7th Sem\SMD_A1_v2"
   ```

2. **Update API Base URL**

   Edit: `app/src/main/java/com/example/firstapp/data/remote/RetrofitClient.kt`

   ```kotlin
   // Line 13: Change to your setup
   private const val BASE_URL = "http://10.0.2.2:8000/api/"

   // For emulator: Use 10.0.2.2
   // For real device: Use your PC's IP (e.g., http://192.168.1.100:8000/api/)
   ```

3. **Sync Gradle**
   ```
   File > Sync Project with Gradle Files
   Wait for dependencies to download...
   ```

---

### ⏱️ 5 Minutes: Test Basic Flow

1. **Run the App**

   ```
   Run > Run 'app'
   Select emulator or device
   ```

2. **Test Splash Screen**

   - App should show splash for 5 seconds
   - Then navigate to login/signup screen

3. **Test Signup** (if API is running)
   - Click "Sign Up"
   - Enter: Name, Email, Password
   - Click "Create Account"
   - If successful, you'll see home screen

---

### ⏱️ 10 Minutes: Verify Everything Works

1. **Check Database**

   - Open phpMyAdmin
   - Check `socially_db` > `users` table
   - Your test user should be there

2. **Check App Data**

   - In Android Studio: View > Tool Windows > App Inspection
   - Select "Database Inspector"
   - Check `socially_database` > `users` table
   - Same user should be cached locally

3. **Test Offline**
   - Turn off WiFi/Data
   - Close and reopen app
   - Should still show splash → home (from cached session)

---

## 🐛 Quick Fixes for Common Issues

### Issue 1: "Connection refused"

```
✓ Check Xampp Apache is running
✓ Check MySQL is running
✓ Test http://localhost/socially/api/auth/session in browser
✓ For emulator, use 10.0.2.2 instead of localhost
✓ For device, use PC's local IP
```

### Issue 2: "Database not found"

```
✓ Create database 'socially_db' in phpMyAdmin
✓ Import server/database_setup.sql
✓ Check database.php has correct credentials
```

### Issue 3: "Unresolved reference"

```
✓ File > Invalidate Caches > Invalidate and Restart
✓ Build > Clean Project
✓ Build > Rebuild Project
```

### Issue 4: "Room schema error"

```
✓ Uninstall app from device/emulator
✓ Rebuild and reinstall
```

---

## 📱 Quick Test Scenarios

### Scenario 1: New User Signup

```
1. Open app
2. Wait 5 seconds (splash)
3. Tap "Sign Up"
4. Enter details
5. Tap "Create Account"
6. ✓ Should navigate to home
7. ✓ Check database for new user
```

### Scenario 2: Returning User

```
1. Close app completely
2. Reopen app
3. Wait 5 seconds (splash)
4. ✓ Should auto-login to home
```

### Scenario 3: Offline Access

```
1. Login once (online)
2. Close app
3. Turn off internet
4. Reopen app
5. ✓ Should still show home (cached)
```

---

## 📋 What to Do Next

### Immediate Tasks (Choose One)

- [ ] Complete login activity (copy signup pattern)
- [ ] Update MainActivity5 to load posts from API
- [ ] Create a simple chat activity
- [ ] Add ability to create a post

### Before Submission

- [ ] Test all implemented features
- [ ] Record demo video
- [ ] Write test report
- [ ] Update README with your contributions
- [ ] Make meaningful git commits

---

## 📞 Need Help?

1. **Read Documentation First**

   - README.md - Overview
   - MIGRATION_GUIDE.md - Technical details
   - IMPLEMENTATION_CHECKLIST.md - Task list

2. **Check Code Examples**

   - SignupActivity.kt - Reference for API calls
   - DAOs - Reference for database operations
   - Server PHP files - Reference for API endpoints

3. **Debug Tips**
   - Use Logcat (Android Studio)
   - Check Apache error log (Xampp)
   - Check MySQL query log
   - Use Postman to test APIs directly

---

## 🎯 Success Criteria

You're ready to continue development when:

- ✓ Server runs at http://localhost/socially/
- ✓ Database has all 9 tables
- ✓ App builds without errors
- ✓ Signup creates user in both databases (MySQL + SQLite)
- ✓ App remembers logged-in user

---

## 🏁 You're All Set!

Your project is now configured and ready for feature development. Refer to IMPLEMENTATION_CHECKLIST.md for the complete task breakdown.

**Remember**: The foundation is built. Now focus on connecting UI to APIs!

---

**Time to Complete Setup**: ~30 minutes  
**Time to First Working Feature**: ~1 hour  
**Time to Complete Project**: ~2-3 weeks

Good luck! 🚀

# 📱 StreamChat – Real-Time Android Chat App (Firebase + AES Encryption)

StreamChat is a modern real-time chat application built in **Android (Java)** using **Firebase Authentication, Firestore, FCM Push Notifications, and AES message encryption**.  
It features one-to-one messaging, user profiles, typing indicators, image avatars, and a clean Material UI.

---

## 🚀 Features

### 🔐 Authentication
- Firebase Email/Password Login  
- Signup with name, email, phone, gender & profile picture  
- Reset Password via email  
- Auto-login using shared preferences  

### 💬 Real-Time Chat
- Firestore-backed real-time messaging  
- AES encrypted message storage (secure chat)  
- Message timestamps, ordering & formatting  
- Sender/Receiver avatar images  
- Recent conversation list with last message & time  

### 🔔 Notifications
- Firebase Cloud Messaging (FCM)  
- Background & foreground notifications  
- Tapping notification opens chat  
- Notification shows sender name + preview  

### 🧩 Architecture & Tech
- Firebase Auth  
- Firebase Firestore  
- Firebase Cloud Messaging  
- Firebase Storage  
- AES Encryption Utility  
- ViewBinding  
- Material UI Components  
- Picasso & Glide for images  
- Rounded ImageView  

---

## 🛠 Tech Stack

| Component | Library |
|----------|---------|
| Authentication | Firebase Auth |
| Database | Firebase Firestore |
| Storage | Firebase Storage |
| Notifications | FCM |
| Encryption | AES + SecretKey |
| UI | Material Components + ConstraintLayout |
| Images | Glide / Picasso |
| Build Tools | Gradle + ViewBinding |

---

## 📂 Project Structure


🧠 Encryption System

StreamChat uses AES encryption for securing messages:

SecretKey key = EncryptionUtil.generateKey("your_secure_password");
String encrypted = EncryptionUtil.encrypt(message, key);
String decrypted = EncryptionUtil.decrypt(encrypted, key);
🔥 Messages saved in Firestore are encrypted — only decrypted on receiver’s device.

🔥 Firebase Integration
Firestore Collections Used:
Users
users → {
    id,
    name,
    email,
    image,
    fcmToken,
    status
}

Messages
chat → {
    senderId,
    receiverId,
    message (encrypted),
    timestamp
}

Conversations

Used for showing latest chats.

▶️ How to Run the Project

Clone the repo:

git clone https://github.com/apoorv077569/StreamChat.git


Open Android Studio → Open Project

Add your Firebase Project:

app/google-services.json


Sync Gradle

Run the app on emulator or device

🛠 Requirements

Android Studio Ladybug or newer

Java 8

Gradle plugin 8.x

Firebase account

🆕 Latest Update (Release v1.0.0)
✔ Added Reset Password Screen
✔ Added Password Toggle Button
✔ Fixed Sender/Receiver Image in Recent Chats
✔ Added Notification Tap → Correct Chat Opening
✔ Improved Material UI layouts
✔ Integrated ViewBinding in all screens
✔ Cleaned Gradle & Updated Dependencies
📸 Screenshots (Add later)
📌 Login Screen  
📌 Signup Screen  
📌 Chat Screen  
📌 Recent Conversations  
📌 Reset Password UI  


(You can upload screenshots to GitHub and reference them here.)

🤝 Contributing

Pull requests are welcome.
For major changes, please open an issue first to discuss what you'd like to improve.

📜 License

MIT License – Free to use and modify.

⭐ If you like this project, give it a star on GitHub!

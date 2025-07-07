# **TOTP Authenticator App**

A secure TOTP (Time-based One-Time Password) Authenticator app that generates OTPs and includes features like import/export, backup and encrypted storage.


---

## **Features**

- Encrypted OTP Storage  
  - OTP data is encrypted using **AES/GCM/NoPadding**
  - Encryption key is generated using a random **KeyGenerator** and stored in **Android Keystore**

- **Import / Export & Backup**  
  - Supports **TXT**, **HTML**, and **JSON** file formats  
  - JSON can be encrypted/decrypted using **AES/GCM/NoPadding** with a password-derived key using **PBKDF2**
  - Backup **Encrypted**

- **Biometric & Password Authentication**  
  - Biometric login
  - Password authentication with passwords hashed using **BCrypt**

- **Multi-OTP QR Transfer**  
  - Transfer multiple OTPs in a single QR using **Protobuf**  
  - Encodes data into the `otpauth-migration://` URI format
 

## Languages & Technologies Used

- Java (Android Studio)
- AES/GCM/NoPadding (Encryption)
- PBKDF2 (Key Derivation)
- Bcrypt (Password Hashing)
- Protobuf (Multi-OTP QR Transfer)
- XML (UI Layouts)
- SQLite (Database)


## Demo

[Application Demo] [https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrSt/view?usp=sharing](https://drive.google.com/file/d/1A6Qs_pjDTWHTs3PLHnnfNTaf4ZslGvB2/view?usp=sharing)


## Sample Files

- [JSON File Format (Encrypted)] [https://drive.google.com/file/d/FILE_ID/view?usp=sharing](https://drive.google.com/file/d/1Wd0Y61bDT9QlSyUhbBteB2ph5TKVenqc/view?usp=sharing)
- [HTML File Format] [https://drive.google.com/file/d/FILE_ID/view?usp=sharing](https://drive.google.com/file/d/1498808mle7NzAIZWQvaRZqlK4Jug-Sfp/view?usp=sharing)
- [TEXT File Format] [https://drive.google.com/file/d/FILE_ID/view?usp=sharing](https://drive.google.com/file/d/1Y13XEwEhtgNkPQsFMxjJ1miikV3HNKtU/view?usp=sharing)
- [Backup File (Encrypted)] [https://drive.google.com/file/d/FILE_ID/view?usp=sharing](https://drive.google.com/file/d/1ebVeqdWWrVsfn9i7lXV6bD4XEXxajSL2/view?usp=sharing)


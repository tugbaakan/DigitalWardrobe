## 📄 Project Requirements Document: Virtual Outfit Creator (VOC) Android App

---

### **1. Introduction**

#### **1.1. Project Goal**
The primary goal of the Virtual Outfit Creator (VOC) Android application is to provide users with a tool to **digitize their wardrobe** and virtually **try on** different clothing combinations using their own body photo as the canvas. The application will leverage Computer Vision and AI to accurately segment clothing items and render them realistically onto the user's image.

#### **1.2. Target Audience**
Individuals who want to organize their closets, plan outfits, and visualize new combinations without physically trying them on.

---

### **2. Functional Requirements**

The application must perform the following functions:

#### **2.1. User Management and Authentication**
* **FR-A1: User Account:** Allow users to create and log into a secure account using **Email/Password (Firebase Authentication)**.
* **FR-A2: Profile Management:** Allow users to update their personal details.

#### **2.2. Image Upload and Wardrobe Digitization**
* **FR-B1: Body Photo Capture/Upload:** Users must be able to upload a single, full-body photograph (front-facing, neutral pose) which will serve as the **base canvas** for virtual try-on.
* **FR-B2: Garment Upload:** Users must be able to upload individual photos of their clothing items.
* **FR-B3: Metadata Tagging:** For each garment, the app must prompt the user to input and save the following metadata tags:
    * **Type:** (e.g., Shirt, Trousers, Skirt, Jacket, Dress, Shoes)
    * **Color:** (e.g., Black, Navy, Red, Patterned)
    * **Formality:** (e.g., Casual, Business, Formal, Athletic)
    * **Fit/Cut:** (e.g., Slim, Regular, Oversized)

#### **2.3. AI and Computer Vision Core**
* **FR-C1: Garment Segmentation:** The application must use a local **TensorFlow Lite** model to automatically detect and create a precise **silhouette mask** of the uploaded clothing item, isolating it from the background.
* **FR-C2: Body Landmark Detection:** The app must analyze the base body photo to identify **key anatomical landmarks** (e.g., shoulders, waist, hips) to assist in accurate scaling and placement of the virtual garments.
* **FR-C3: Warping and Rendering:** The core engine must apply **image warping techniques** (e.g., Thin Plate Spline) to distort and overlay the segmented clothing image onto the body photo, simulating a realistic fit and minimizing visual distortion.

#### **2.4. Outfit Creation and Visualization**
* **FR-D1: Automatic Combination Generation:** The system must automatically generate a list of **compatible outfit combinations** by cross-referencing the metadata of all available garments (e.g., Shirt + Trousers).
    * *Constraint:* Outfits must adhere to a defined **Compatibility Matrix** (e.g., Formal items do not combine with Athletic items).
* **FR-D2: Interactive Selection:** Users must be able to tap on any generated combination to immediately view the virtual try-on visualization.
* **FR-D3: Save and Share:** Users must be able to save their favorite outfits and share the resulting image.

---

### **3. Technical Requirements**

#### **3.1. Development Stack**
* **Mobile Platform:** Android (API 26+)
* **Language:** **Kotlin (Native Android)** to maximize performance for image processing.
* **Backend & Data Storage:** **Google Firebase** (Authentication, Firestore for metadata, Cloud Storage for user photos).

#### **3.2. Performance and Data**
* **TP-1: Processing Time:** Virtual try-on rendering must be completed within **3 seconds** of combination selection.
* **TP-2: Image Quality:** The segmented and warped clothing overlay must maintain sufficient resolution and visual quality.
* **TP-3: Model Deployment:** All machine learning models (Segmentation, Pose Detection) must be deployed **on-device** using **TensorFlow Lite** for quick processing and reduced server load.

---

### **4. Design Requirements (UI/UX)**

#### **4.1. Interface Flow**
* The user flow must be intuitive: **Upload Garments $\rightarrow$ Upload Body Photo $\rightarrow$ Generate Outfits $\rightarrow$ View Visualization**.

#### **4.2. Visual Design**
* The interface must be clean, modern, and utilize **Material Design** principles for a native Android feel.
* The **Visualization Screen** must prominently display the rendered outfit.

---

### **5. Publishing Requirements**

#### **5.1. App Store Submission**
* The final application must be packaged as an **Android App Bundle (AAB)** and submitted to the **Google Play Store**.
* The submission must include a high-quality icon, descriptive text, and promotional screenshots.

---

### **6. Project Roadmap (Phases)**

| Phase | Description | Key Deliverables |
| :--- | :--- | :--- |
| **Phase 1** | Setup & Authentication | Firebase Setup, Login/Signup Screens |
| **Phase 2** | Image Capture & Storage | Garment/Body Upload UI, Photo Storage in Firebase, Metadata Tagging |
| **Phase 3** | AI Core Integration | ML Model Integration (Segmentation, Pose), Warping Logic Implementation |
| **Phase 4** | Visualization & Logic | Outfit Combination Algorithm, Final Rendering View, Saved Outfits Feature |
| **Phase 5** | Testing & Launch | Bug Fixing, Performance Optimization, Google Play Store Submission |
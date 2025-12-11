# 🚀 Supabase Setup Guide for Digital Wardrobe Android App

This guide walks you through setting up Supabase as the backend for the Virtual Outfit Creator app.

---

## 1. Create a Supabase Project

### Step 1.1: Sign Up / Log In
1. Go to [https://supabase.com](https://supabase.com)
2. Click **"Start your project"**
3. Sign up with GitHub, GitLab, or email

### Step 1.2: Create New Project
1. Click **"New Project"**
2. Fill in the details:
   - **Name:** `digital-wardrobe`
   - **Database Password:** (save this securely!)
   - **Region:** Choose closest to your users
   - **Pricing Plan:** Free tier
3. Click **"Create new project"**
4. Wait for the project to initialize (~2 minutes)

### Step 1.3: Get Your Credentials
After project creation, go to **Settings → API** and note down:
- **Project URL:** `https://xxxxx.supabase.co`
- **Anon/Public Key:** `eyJhbGciOiJIUzI1NiIsInR5cCI6...`

---

## 2. Database Schema Setup

Go to **SQL Editor** in Supabase Dashboard and run these scripts:

### Step 2.1: Create Tables

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users Profile Table (extends auth.users)
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    full_name TEXT,
    avatar_url TEXT,
    body_photo_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Garments Table
CREATE TABLE public.garments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    image_url TEXT NOT NULL,
    mask_url TEXT,
    name TEXT,
    type TEXT NOT NULL CHECK (type IN ('shirt', 'trousers', 'skirt', 'jacket', 'dress', 'shoes', 'accessory', 'other')),
    color TEXT NOT NULL,
    formality TEXT NOT NULL CHECK (formality IN ('casual', 'business', 'formal', 'athletic')),
    fit TEXT CHECK (fit IN ('slim', 'regular', 'oversized')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Outfits Table
CREATE TABLE public.outfits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    name TEXT,
    preview_url TEXT,
    is_favorite BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Outfit Items Junction Table
CREATE TABLE public.outfit_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    outfit_id UUID REFERENCES public.outfits(id) ON DELETE CASCADE NOT NULL,
    garment_id UUID REFERENCES public.garments(id) ON DELETE CASCADE NOT NULL,
    layer_order INTEGER DEFAULT 0,
    UNIQUE(outfit_id, garment_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_garments_user_id ON public.garments(user_id);
CREATE INDEX idx_garments_type ON public.garments(type);
CREATE INDEX idx_outfits_user_id ON public.outfits(user_id);
CREATE INDEX idx_outfit_items_outfit_id ON public.outfit_items(outfit_id);
```

### Step 2.2: Enable Row Level Security (RLS)

```sql
-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.garments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.outfits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.outfit_items ENABLE ROW LEVEL SECURITY;

-- Profiles policies
CREATE POLICY "Users can view own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

-- Garments policies
CREATE POLICY "Users can view own garments"
    ON public.garments FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own garments"
    ON public.garments FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own garments"
    ON public.garments FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own garments"
    ON public.garments FOR DELETE
    USING (auth.uid() = user_id);

-- Outfits policies
CREATE POLICY "Users can view own outfits"
    ON public.outfits FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own outfits"
    ON public.outfits FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own outfits"
    ON public.outfits FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own outfits"
    ON public.outfits FOR DELETE
    USING (auth.uid() = user_id);

-- Outfit items policies
CREATE POLICY "Users can view own outfit items"
    ON public.outfit_items FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.outfits
            WHERE outfits.id = outfit_items.outfit_id
            AND outfits.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert own outfit items"
    ON public.outfit_items FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.outfits
            WHERE outfits.id = outfit_items.outfit_id
            AND outfits.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete own outfit items"
    ON public.outfit_items FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM public.outfits
            WHERE outfits.id = outfit_items.outfit_id
            AND outfits.user_id = auth.uid()
        )
    );
```

### Step 2.3: Create Profile Trigger (Auto-create profile on signup)

```sql
-- Function to handle new user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, full_name)
    VALUES (
        NEW.id,
        NEW.email,
        NEW.raw_user_meta_data->>'full_name'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to auto-create profile
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
```

---

## 3. Storage Setup

### Step 3.1: Create Storage Buckets

Go to **Storage** in Supabase Dashboard:

1. Click **"New bucket"**
2. Create these buckets:

| Bucket Name | Public | Description |
|-------------|--------|-------------|
| `avatars` | Yes | User profile photos |
| `body-photos` | No | Full body reference photos |
| `garments` | No | Clothing item images |
| `garment-masks` | No | Segmented clothing masks |
| `outfit-previews` | No | Generated outfit images |

### Step 3.2: Storage Policies

Go to **Storage → Policies** and add these policies:

```sql
-- Avatars bucket (public read, authenticated write)
CREATE POLICY "Avatar images are publicly accessible"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'avatars');

CREATE POLICY "Users can upload own avatar"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'avatars'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can update own avatar"
    ON storage.objects FOR UPDATE
    USING (
        bucket_id = 'avatars'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

-- Body photos bucket (private)
CREATE POLICY "Users can view own body photos"
    ON storage.objects FOR SELECT
    USING (
        bucket_id = 'body-photos'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can upload own body photos"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'body-photos'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

-- Garments bucket (private)
CREATE POLICY "Users can view own garments"
    ON storage.objects FOR SELECT
    USING (
        bucket_id = 'garments'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can upload own garments"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'garments'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete own garments"
    ON storage.objects FOR DELETE
    USING (
        bucket_id = 'garments'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

-- Garment masks bucket (private)
CREATE POLICY "Users can view own garment masks"
    ON storage.objects FOR SELECT
    USING (
        bucket_id = 'garment-masks'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can upload own garment masks"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'garment-masks'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

-- Outfit previews bucket (private)
CREATE POLICY "Users can view own outfit previews"
    ON storage.objects FOR SELECT
    USING (
        bucket_id = 'outfit-previews'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can upload own outfit previews"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'outfit-previews'
        AND auth.uid()::text = (storage.foldername(name))[1]
    );
```

---

## 4. Authentication Setup

### Step 4.1: Enable Email Auth
1. Go to **Authentication → Providers**
2. Ensure **Email** is enabled
3. Configure settings:
   - ✅ Enable email confirmations (optional for dev)
   - Set **Site URL:** `com.yourapp.voc://login-callback`

### Step 4.2: Configure Redirect URLs
1. Go to **Authentication → URL Configuration**
2. Add to **Redirect URLs:**
   ```
   com.yourapp.voc://login-callback
   ```

---

## 5. Android Integration

### Step 5.1: Add Dependencies

In your `build.gradle.kts` (app level):

```kotlin
plugins {
    // ... existing plugins
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.0.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    
    // Ktor client (required for Supabase)
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-utils:2.3.7")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### Step 5.2: Create Supabase Client

Create `SupabaseClient.kt`:

```kotlin
package com.yourapp.voc.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    
    val client = createSupabaseClient(
        supabaseUrl = "https://YOUR_PROJECT_ID.supabase.co",
        supabaseKey = "YOUR_ANON_KEY"
    ) {
        install(Auth) {
            scheme = "com.yourapp.voc"
            host = "login-callback"
        }
        install(Postgrest)
        install(Storage)
    }
}
```

### Step 5.3: Create Data Models

Create `Models.kt`:

```kotlin
package com.yourapp.voc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val email: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("body_photo_url") val bodyPhotoUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class Garment(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("mask_url") val maskUrl: String? = null,
    val name: String? = null,
    val type: String, // shirt, trousers, skirt, jacket, dress, shoes, accessory, other
    val color: String,
    val formality: String, // casual, business, formal, athletic
    val fit: String? = null, // slim, regular, oversized
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Outfit(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val name: String? = null,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class OutfitItem(
    val id: String? = null,
    @SerialName("outfit_id") val outfitId: String,
    @SerialName("garment_id") val garmentId: String,
    @SerialName("layer_order") val layerOrder: Int = 0
)
```

### Step 5.4: Create Auth Repository

Create `AuthRepository.kt`:

```kotlin
package com.yourapp.voc.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import com.yourapp.voc.data.remote.SupabaseClient

class AuthRepository {
    
    private val auth = SupabaseClient.client.auth
    
    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("full_name", fullName)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
    
    fun isLoggedIn(): Boolean {
        return auth.currentUserOrNull() != null
    }
}
```

### Step 5.5: Create Garment Repository

Create `GarmentRepository.kt`:

```kotlin
package com.yourapp.voc.data.repository

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import com.yourapp.voc.data.model.Garment
import com.yourapp.voc.data.remote.SupabaseClient
import java.io.File

class GarmentRepository {
    
    private val postgrest = SupabaseClient.client.postgrest
    private val storage = SupabaseClient.client.storage
    
    suspend fun uploadGarmentImage(userId: String, imageFile: File): String {
        val fileName = "${System.currentTimeMillis()}_${imageFile.name}"
        val path = "$userId/$fileName"
        
        storage.from("garments").upload(path, imageFile.readBytes())
        
        return storage.from("garments").publicUrl(path)
    }
    
    suspend fun createGarment(garment: Garment): Garment {
        return postgrest.from("garments")
            .insert(garment)
            .decodeSingle<Garment>()
    }
    
    suspend fun getGarments(userId: String): List<Garment> {
        return postgrest.from("garments")
            .select()
            .eq("user_id", userId)
            .decodeList<Garment>()
    }
    
    suspend fun getGarmentsByType(userId: String, type: String): List<Garment> {
        return postgrest.from("garments")
            .select()
            .eq("user_id", userId)
            .eq("type", type)
            .decodeList<Garment>()
    }
    
    suspend fun updateGarment(garment: Garment): Garment {
        return postgrest.from("garments")
            .update(garment)
            .eq("id", garment.id!!)
            .decodeSingle<Garment>()
    }
    
    suspend fun deleteGarment(garmentId: String) {
        postgrest.from("garments")
            .delete()
            .eq("id", garmentId)
    }
}
```

---

## 6. Quick Reference

### Supabase URLs
| Resource | URL |
|----------|-----|
| Dashboard | https://supabase.com/dashboard |
| API Docs | https://supabase.com/docs |
| Kotlin SDK | https://github.com/supabase-community/supabase-kt |

### Free Tier Limits
| Resource | Limit |
|----------|-------|
| Database | 500 MB |
| Storage | 1 GB |
| Bandwidth | 2 GB/month |
| Auth Users | 50,000 MAU |
| API Requests | Unlimited |

### Environment Variables (for production)
Instead of hardcoding, use:

```kotlin
// In local.properties (don't commit to git!)
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6...
```

```kotlin
// In build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${properties["SUPABASE_URL"]}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties["SUPABASE_ANON_KEY"]}\"")
    }
}
```

---

## 7. Next Steps

After completing this setup:

1. ✅ Supabase project created
2. ✅ Database tables created
3. ✅ RLS policies applied
4. ✅ Storage buckets configured
5. ✅ Android SDK integrated

You can now proceed with:
- **Task 1.6:** Implement authentication logic
- **Task 2.8:** Implement image uploads
- **Task 2.9:** Implement metadata storage

---

## Troubleshooting

### Common Issues

**1. "Invalid API key"**
- Double-check you're using the `anon` key, not the `service_role` key

**2. "Row level security policy violation"**
- Ensure user is authenticated before making requests
- Check RLS policies are correctly applied

**3. "Storage bucket not found"**
- Verify bucket names match exactly (case-sensitive)
- Check bucket is created in the dashboard

**4. "Network error on Android"**
- Add internet permission to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```


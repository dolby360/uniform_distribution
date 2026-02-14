# Step 8: Frontend-Backend Integration

## Goal

Integrate React Native frontend with Cloud Functions backend, implementing camera flow, match confirmation UI, and item display.

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                React Native Frontend Integration                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User Flow:                                                     │
│                                                                  │
│  1. HomeScreen                                                  │
│  ┌──────────────────────────────────────┐                      │
│  │  [Take Photo Button]                 │                      │
│  └──────────────────────────────────────┘                      │
│              │                                                   │
│              ▼ (Camera launched)                                │
│  ┌──────────────────────────────────────┐                      │
│  │  Photo captured (base64)             │                      │
│  │  → POST /process-outfit               │                      │
│  │  [Loading indicator]                 │                      │
│  └──────────────────────────────────────┘                      │
│              │                                                   │
│              ▼                                                   │
│  2. MatchConfirmationScreen                                     │
│  ┌──────────────────────────────────────┐                      │
│  │  Shirt:                              │                      │
│  │  ┌────────────┐  ┌────────────┐     │                      │
│  │  │ Matched!   │  │ New: 👕    │     │                      │
│  │  │ 92% match  │  │ [Preview]  │     │                      │
│  │  │ [Confirm]  │  │ [Add New]  │     │                      │
│  │  └────────────┘  └────────────┘     │                      │
│  │                                      │                      │
│  │  Pants:                              │                      │
│  │  ┌────────────┐                      │                      │
│  │  │ Matched!   │                      │                      │
│  │  │ 88% match  │                      │                      │
│  │  │ [Confirm]  │                      │                      │
│  │  └────────────┘                      │                      │
│  └──────────────────────────────────────┘                      │
│              │                                                   │
│              ▼ (Confirm/Add)                                    │
│  ┌──────────────────────────────────────┐                      │
│  │  → POST /confirm-match (matched)     │                      │
│  │  → POST /add-new-item (new)          │                      │
│  └──────────────────────────────────────┘                      │
│              │                                                   │
│              ▼                                                   │
│  3. Back to HomeScreen (updated)                                │
│  ┌──────────────────────────────────────┐                      │
│  │  Shirts: 5 items                     │                      │
│  │  Pants: 3 items                      │                      │
│  │  [View Statistics]                   │                      │
│  └──────────────────────────────────────┘                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Schemas

See Step 6 response types (ProcessOutfitResponse, ItemMatchResult)

## API Endpoint Signatures

Uses endpoints from Steps 6-7:
- POST /process-outfit
- POST /confirm-match
- POST /add-new-item

## File Structure

```
app/
  api/
    cloudFunctions.ts              # API client for Cloud Functions
    types.ts                       # TypeScript interfaces
    config.ts                      # API configuration
  screens/
    HomeScreen.tsx                 # Updated with item list
    HomeScreen.logic.ts            # Updated with API calls
    MatchConfirmationScreen.tsx    # New screen for confirming matches
    MatchConfirmationScreen.logic.ts
    MatchConfirmationScreen.styles.ts
  components/
    ItemCard.tsx                   # Display clothing item
    LoadingOverlay.tsx             # Processing indicator
  utils/
    imageUtils.ts                  # Base64 conversion
```

## Key Code Snippets

### app/api/config.ts

```typescript
export const API_CONFIG = {
  BASE_URL: 'https://us-central1-uniform-dist-XXXXX.cloudfunctions.net',
  ENDPOINTS: {
    PROCESS_OUTFIT: '/process-outfit',
    CONFIRM_MATCH: '/confirm-match',
    ADD_NEW_ITEM: '/add-new-item',
  },
  TIMEOUT: 60000, // 60 seconds
};
```

### app/api/types.ts

```typescript
export interface ProcessOutfitRequest {
  image: string; // base64
}

export interface ItemMatchResult {
  matched: boolean;
  item_id?: string;
  similarity?: number;
  image_url?: string;
  cropped_url: string;
  embedding?: number[];
}

export interface ProcessOutfitResponse {
  success: boolean;
  shirt: ItemMatchResult | null;
  pants: ItemMatchResult | null;
  original_photo_url: string;
  error?: string;
}

export interface ConfirmMatchRequest {
  item_id: string;
  item_type: 'shirt' | 'pants';
  original_photo_url: string;
  similarity_score?: number;
}

export interface AddNewItemRequest {
  item_type: 'shirt' | 'pants';
  cropped_image_url: string;
  embedding: number[];
  original_photo_url: string;
  log_wear: boolean;
}
```

### app/api/cloudFunctions.ts

```typescript
import { API_CONFIG } from './config';
import {
  ProcessOutfitRequest,
  ProcessOutfitResponse,
  ConfirmMatchRequest,
  AddNewItemRequest,
} from './types';

export async function processOutfit(imageBase64: string): Promise<ProcessOutfitResponse> {
  const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.PROCESS_OUTFIT}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ image: imageBase64 }),
    signal: AbortSignal.timeout(API_CONFIG.TIMEOUT),
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }

  return response.json();
}

export async function confirmMatch(
  itemId: string,
  itemType: 'shirt' | 'pants',
  originalPhotoUrl: string,
  similarityScore?: number
): Promise<{ success: boolean; item_id: string; wear_count: number }> {
  const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CONFIRM_MATCH}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      item_id: itemId,
      item_type: itemType,
      original_photo_url: originalPhotoUrl,
      similarity_score: similarityScore,
    }),
  });

  return response.json();
}

export async function addNewItem(
  itemType: 'shirt' | 'pants',
  croppedImageUrl: string,
  embedding: number[],
  originalPhotoUrl: string,
  logWear: boolean
): Promise<{ success: boolean; item_id: string }> {
  const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.ADD_NEW_ITEM}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      item_type: itemType,
      cropped_image_url: croppedImageUrl,
      embedding: embedding,
      original_photo_url: originalPhotoUrl,
      log_wear: logWear,
    }),
  });

  return response.json();
}
```

### app/utils/imageUtils.ts

```typescript
import * as FileSystem from 'expo-file-system';

export async function convertImageToBase64(uri: string): Promise<string> {
  /**
   * Convert image URI to base64 string
   */
  const base64 = await FileSystem.readAsStringAsync(uri, {
    encoding: FileSystem.EncodingType.Base64,
  });

  return base64;
}
```

### app/screens/HomeScreen.logic.ts (updated)

```typescript
import { useState } from 'react';
import { Alert } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { useNavigation } from '@react-navigation/native';
import { processOutfit } from '../api/cloudFunctions';
import { convertImageToBase64 } from '../utils/imageUtils';

export const useHomeScreen = () => {
  const [shirts, setShirts] = useState<any[]>([]);
  const [pants, setPants] = useState<any[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const navigation = useNavigation();

  const requestCameraPermission = async (): Promise<boolean> => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission Denied', 'Camera permission is required to take photos.');
      return false;
    }
    return true;
  };

  const launchCamera = async () => {
    return await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [3, 4],
      quality: 0.8,
    });
  };

  const handleCameraPress = async () => {
    const hasPermission = await requestCameraPermission();
    if (!hasPermission) return;

    const result = await launchCamera();

    if (result && !result.canceled) {
      const imageUri = result.assets[0].uri;

      setIsProcessing(true);

      try {
        // Convert to base64
        const base64 = await convertImageToBase64(imageUri);

        // Call backend
        const response = await processOutfit(base64);

        setIsProcessing(false);

        // Navigate to confirmation screen
        navigation.navigate('MatchConfirmation', {
          matchResults: response,
        });
      } catch (error) {
        setIsProcessing(false);
        Alert.alert('Error', 'Failed to process outfit photo. Please try again.');
        console.error(error);
      }
    }
  };

  return {
    shirts,
    pants,
    handleCameraPress,
    isProcessing,
  };
};
```

### app/components/LoadingOverlay.tsx

```typescript
import React from 'react';
import { View, ActivityIndicator, Text, StyleSheet } from 'react-native';

interface LoadingOverlayProps {
  message?: string;
}

export default function LoadingOverlay({ message = 'Processing...' }: LoadingOverlayProps) {
  return (
    <View style={styles.container}>
      <View style={styles.overlay}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.message}>{message}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    zIndex: 999,
  },
  overlay: {
    backgroundColor: 'white',
    padding: 30,
    borderRadius: 10,
    alignItems: 'center',
  },
  message: {
    marginTop: 15,
    fontSize: 16,
    color: '#333',
  },
});
```

### app/screens/MatchConfirmationScreen.tsx

```typescript
import React from 'react';
import { View, Text, Image, TouchableOpacity, ScrollView } from 'react-native';
import { useMatchConfirmation } from './MatchConfirmationScreen.logic';
import { styles } from './MatchConfirmationScreen.styles';

export default function MatchConfirmationScreen({ route }) {
  const { matchResults } = route.params;
  const { handleConfirm, handleAddNew, isLoading } = useMatchConfirmation();

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>Review Your Outfit</Text>

      {/* Shirt Section */}
      {matchResults.shirt && (
        <View style={styles.itemSection}>
          <Text style={styles.itemTitle}>Shirt</Text>

          {matchResults.shirt.matched ? (
            <View style={styles.matchCard}>
              <Image
                source={{ uri: matchResults.shirt.image_url }}
                style={styles.itemImage}
              />
              <Text style={styles.matchText}>
                Match: {Math.round((matchResults.shirt.similarity || 0) * 100)}%
              </Text>
              <TouchableOpacity
                style={styles.confirmButton}
                onPress={() => handleConfirm('shirt', matchResults.shirt, matchResults.original_photo_url)}
                disabled={isLoading}
              >
                <Text style={styles.buttonText}>Confirm Match</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.newCard}>
              <Image
                source={{ uri: matchResults.shirt.cropped_url }}
                style={styles.itemImage}
              />
              <Text style={styles.newText}>New Item Detected</Text>
              <TouchableOpacity
                style={styles.addButton}
                onPress={() => handleAddNew('shirt', matchResults.shirt, matchResults.original_photo_url)}
                disabled={isLoading}
              >
                <Text style={styles.buttonText}>Add to Wardrobe</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      )}

      {/* Pants Section */}
      {matchResults.pants && (
        <View style={styles.itemSection}>
          <Text style={styles.itemTitle}>Pants</Text>

          {matchResults.pants.matched ? (
            <View style={styles.matchCard}>
              <Image
                source={{ uri: matchResults.pants.image_url }}
                style={styles.itemImage}
              />
              <Text style={styles.matchText}>
                Match: {Math.round((matchResults.pants.similarity || 0) * 100)}%
              </Text>
              <TouchableOpacity
                style={styles.confirmButton}
                onPress={() => handleConfirm('pants', matchResults.pants, matchResults.original_photo_url)}
                disabled={isLoading}
              >
                <Text style={styles.buttonText}>Confirm Match</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.newCard}>
              <Image
                source={{ uri: matchResults.pants.cropped_url }}
                style={styles.itemImage}
              />
              <Text style={styles.newText}>New Item Detected</Text>
              <TouchableOpacity
                style={styles.addButton}
                onPress={() => handleAddNew('pants', matchResults.pants, matchResults.original_photo_url)}
                disabled={isLoading}
              >
                <Text style={styles.buttonText}>Add to Wardrobe</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      )}
    </ScrollView>
  );
}
```

### app/screens/MatchConfirmationScreen.logic.ts

```typescript
import { useState } from 'react';
import { Alert } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { confirmMatch, addNewItem } from '../api/cloudFunctions';
import { ItemMatchResult } from '../api/types';

export const useMatchConfirmation = () => {
  const [isLoading, setIsLoading] = useState(false);
  const navigation = useNavigation();

  const handleConfirm = async (
    itemType: 'shirt' | 'pants',
    item: ItemMatchResult,
    originalPhotoUrl: string
  ) => {
    setIsLoading(true);

    try {
      await confirmMatch(
        item.item_id!,
        itemType,
        originalPhotoUrl,
        item.similarity
      );

      Alert.alert('Success', 'Wear logged successfully!');
      navigation.navigate('Home');
    } catch (error) {
      Alert.alert('Error', 'Failed to confirm match');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddNew = async (
    itemType: 'shirt' | 'pants',
    item: ItemMatchResult,
    originalPhotoUrl: string
  ) => {
    setIsLoading(true);

    try {
      // Extract gs:// URL from signed URL
      const gsUrl = item.cropped_url.includes('gs://')
        ? item.cropped_url
        : `gs://uniform-dist-XXXXX.appspot.com/cropped-items/${itemType}s/...`;

      await addNewItem(
        itemType,
        gsUrl,
        item.embedding!,
        originalPhotoUrl,
        true // Log wear event
      );

      Alert.alert('Success', 'New item added to your wardrobe!');
      navigation.navigate('Home');
    } catch (error) {
      Alert.alert('Error', 'Failed to add new item');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  return {
    handleConfirm,
    handleAddNew,
    isLoading,
  };
};
```

### app/screens/MatchConfirmationScreen.styles.ts

```typescript
import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    padding: 16,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  itemSection: {
    marginBottom: 24,
  },
  itemTitle: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 12,
  },
  matchCard: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  newCard: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  itemImage: {
    width: 200,
    height: 200,
    borderRadius: 8,
    marginBottom: 12,
  },
  matchText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#4CAF50',
    marginBottom: 12,
  },
  newText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#FF9800',
    marginBottom: 12,
  },
  confirmButton: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 32,
    paddingVertical: 12,
    borderRadius: 8,
  },
  addButton: {
    backgroundColor: '#2196F3',
    paddingHorizontal: 32,
    paddingVertical: 12,
    borderRadius: 8,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
});
```

### app/App.tsx (update navigation)

```typescript
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import HomeScreen from './screens/HomeScreen';
import MatchConfirmationScreen from './screens/MatchConfirmationScreen';

const Stack = createStackNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen
          name="Home"
          component={HomeScreen}
          options={{ title: 'Uniform Distribution' }}
        />
        <Stack.Screen
          name="MatchConfirmation"
          component={MatchConfirmationScreen}
          options={{ title: 'Confirm Matches' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
```

### app/package.json (add dependencies)

```json
{
  "dependencies": {
    "expo-file-system": "~17.0.1",
    "existing dependencies...": "..."
  }
}
```

## Acceptance Criteria

- [ ] Camera flow captures photo and sends to backend
- [ ] Loading indicator shows during API call
- [ ] Match confirmation screen displays results correctly
- [ ] Matched items show similarity percentage
- [ ] New items show cropped preview
- [ ] Confirming match updates backend and returns to home
- [ ] Adding new item creates entry and returns to home
- [ ] Error handling for network failures, timeout
- [ ] Images load from signed URLs
- [ ] Navigation flow works smoothly
- [ ] UI is responsive and user-friendly

## Setup Instructions

1. **Install Dependencies**:
   ```bash
   cd app
   npm install expo-file-system
   ```

2. **Update API Configuration**:
   Edit `app/api/config.ts` with your Cloud Function URLs

3. **Run App**:
   ```bash
   npx expo start
   ```

## Verification

### E2E Test Flow

1. Launch app
2. Press "Take Photo" button
3. Capture outfit photo
4. Wait for processing (~10-20 seconds)
5. Review results on MatchConfirmationScreen
6. Confirm or add items
7. Verify navigation back to HomeScreen
8. Check Firestore for logged data

### Test Checklist

- [ ] Camera permission requested
- [ ] Photo captured successfully
- [ ] API call completes without errors
- [ ] Loading overlay displays during processing
- [ ] Match results displayed correctly
- [ ] Images render from signed URLs
- [ ] Buttons respond to taps
- [ ] Success alert shown after confirmation
- [ ] Navigation works correctly
- [ ] No memory leaks or crashes

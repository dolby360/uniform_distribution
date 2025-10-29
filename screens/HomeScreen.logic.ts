import { useState } from 'react';
import { Alert } from 'react-native';
import * as ImagePicker from 'expo-image-picker';

export const useHomeScreen = () => {
  const [shirts, setShirts] = useState<any[]>([]);
  const [pants, setPants] = useState<any[]>([]);

  const requestCameraPermission = async (): Promise<boolean> => {
    const permissionResult = await ImagePicker.requestCameraPermissionsAsync();
    
    if (!permissionResult.granted) {
      Alert.alert(
        'Permission Denied',
        'You need to grant camera permissions to use this feature.'
      );
      return false;
    }
    
    return true;
  };

  const launchCamera = async (): Promise<ImagePicker.ImagePickerResult | null> => {
    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      aspect: [4, 3],
      quality: 1,
    });

    return result;
  };

  const handleCameraPress = async () => {
    const hasPermission = await requestCameraPermission();
    if (!hasPermission) return;

    const result = await launchCamera();
    
    if (result && !result.canceled) {
      console.log('Image captured:', result.assets[0].uri);
    }
  };

  return {
    shirts,
    pants,
    handleCameraPress,
  };
};

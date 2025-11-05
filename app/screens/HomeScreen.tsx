import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import { useHomeScreen } from './HomeScreen.logic';
import { styles } from './HomeScreen.styles';

const ClothingSection = ({ title, items }: { title: string; items: any[] }) => (
  <View style={styles.section}>
    <Text style={styles.sectionTitle}>{title}</Text>
    <Text style={styles.placeholder}>
      {items.length === 0 ? `No ${title.toLowerCase()} added yet` : `${items.length} items`}
    </Text>
  </View>
);

const CameraButton = ({ onPress }: { onPress: () => void }) => (
  <TouchableOpacity style={styles.cameraButton} onPress={onPress}>
    <Text style={styles.cameraButtonText}>📷</Text>
  </TouchableOpacity>
);

export default function HomeScreen() {
  const { shirts, pants, handleCameraPress } = useHomeScreen();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>👕 Uniform Distribution 📱</Text>
      <ClothingSection title="Shirts" items={shirts} />
      <ClothingSection title="Pants" items={pants} />
      <CameraButton onPress={handleCameraPress} />
    </View>
  );
}

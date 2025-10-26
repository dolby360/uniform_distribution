import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';

export default function HomeScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>👕 Uniform Distribution 📱</Text>
      
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Shirts</Text>
        <Text style={styles.placeholder}>No shirts added yet</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Pants</Text>
        <Text style={styles.placeholder}>No pants added yet</Text>
      </View>

      <TouchableOpacity style={styles.cameraButton}>
        <Text style={styles.cameraButtonText}>📷</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    padding: 20,
    paddingTop: 60,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 30,
  },
  section: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 20,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 10,
  },
  placeholder: {
    color: '#999',
    fontStyle: 'italic',
  },
  cameraButton: {
    position: 'absolute',
    bottom: 30,
    left: '50%',
    marginLeft: -35,
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: '#007AFF',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 5,
  },
  cameraButtonText: {
    fontSize: 32,
    textAlign: 'center',
    lineHeight: 70,
  },
});

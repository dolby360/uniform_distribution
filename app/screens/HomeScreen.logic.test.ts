import { renderHook, act } from '@testing-library/react-native';
import { Alert } from 'react-native';

jest.mock('expo-image-picker', () => ({
  requestCameraPermissionsAsync: jest.fn(),
  launchCameraAsync: jest.fn(),
}));

const ImagePicker = require('expo-image-picker');

jest.spyOn(Alert, 'alert');

import { useHomeScreen } from './HomeScreen.logic';

describe('useHomeScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should initialize with empty arrays for shirts and pants', () => {
    const { result } = renderHook(() => useHomeScreen());

    expect(result.current.shirts).toEqual([]);
    expect(result.current.pants).toEqual([]);
  });

  it('should request camera permission and launch camera successfully', async () => {
    const mockImageUri = 'file://test-image.jpg';
    
    (ImagePicker.requestCameraPermissionsAsync as jest.Mock).mockResolvedValue({
      granted: true,
    });

    (ImagePicker.launchCameraAsync as jest.Mock).mockResolvedValue({
      canceled: false,
      assets: [{ uri: mockImageUri }],
    });

    const consoleLogSpy = jest.spyOn(console, 'log');

    const { result } = renderHook(() => useHomeScreen());

    await act(async () => {
      await result.current.handleCameraPress();
    });

    expect(ImagePicker.requestCameraPermissionsAsync).toHaveBeenCalledTimes(1);
    expect(ImagePicker.launchCameraAsync).toHaveBeenCalledWith({
      allowsEditing: true,
      aspect: [4, 3],
      quality: 1,
    });
    expect(consoleLogSpy).toHaveBeenCalledWith('Image captured:', mockImageUri);

    consoleLogSpy.mockRestore();
  });

  it('should show alert when camera permission is denied', async () => {
    (ImagePicker.requestCameraPermissionsAsync as jest.Mock).mockResolvedValue({
      granted: false,
    });

    const { result } = renderHook(() => useHomeScreen());

    await act(async () => {
      await result.current.handleCameraPress();
    });

    expect(Alert.alert).toHaveBeenCalledWith(
      'Permission Denied',
      'You need to grant camera permissions to use this feature.'
    );
    expect(ImagePicker.launchCameraAsync).not.toHaveBeenCalled();
  });

  it('should handle cancelled camera action', async () => {
    (ImagePicker.requestCameraPermissionsAsync as jest.Mock).mockResolvedValue({
      granted: true,
    });

    (ImagePicker.launchCameraAsync as jest.Mock).mockResolvedValue({
      canceled: true,
    });

    const consoleLogSpy = jest.spyOn(console, 'log');

    const { result } = renderHook(() => useHomeScreen());

    await act(async () => {
      await result.current.handleCameraPress();
    });

    expect(ImagePicker.launchCameraAsync).toHaveBeenCalledTimes(1);
    expect(consoleLogSpy).not.toHaveBeenCalled();

    consoleLogSpy.mockRestore();
  });
});

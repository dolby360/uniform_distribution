# Uniform Distribution 👕📱

**Uniform Distribution** is a minimal React Native app that helps users wear all their clothes evenly.  
The app tracks daily outfit photos and suggests which clothes to wear next based on usage frequency.

---

## 🎯 Goal

Take a daily picture of yourself wearing your outfit.  
The app keeps track of when each item (shirt, pants, etc.) was last worn.  
Next time, it suggests the items you rarely wear so your wardrobe usage stays uniformly distributed.

---

## emulator

# 1. Start the emulator
```bash
"/c/Users/Dolevb/AppData/Local/Android/Sdk/emulator/emulator" -avd Medium_Phone_API_35
```

## setup
```bash
adb kill-server
adb start-server
```

## start

```bash
node node_modules/expo/bin/cli start
```

## get logs
```
adb -s emulator-5554 logcat -d | grep -i "error\|exception\|crash\|fatal" | tail -n 50
```
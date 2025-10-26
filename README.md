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
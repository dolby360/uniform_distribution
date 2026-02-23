.PHONY: reload upload emulator build

reload:
	bash scripts/reload.sh

upload:
	bash scripts/upload_to_android.sh

build:
	cd android && ./gradlew assembleDebug

emulator:
	/c/Users/dolev/Android/Sdk/emulator/emulator.exe @uniform_dist_test &

.PHONY: reload upload emulator

reload:
	bash scripts/reload.sh

upload:
	bash scripts/upload_to_android.sh

emulator:
	/c/Users/dolev/Android/Sdk/emulator/emulator.exe @uniform_dist_test &

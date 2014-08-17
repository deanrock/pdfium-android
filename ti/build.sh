./ti/gyp_android
cp GypAndroid.mk Android.mk
cp ./ti/Application.mk ./Application.mk
sed 's/include \$(BUILD_SYSTEM)\/base_rules.mk/#include \$(BUILD_SYSTEM)\/base_rules.mk/g' safemath.target.mk > x.safemath.target.mk
mv x.safemath.target.mk safemath.target.mk

LOCAL_CFLAGS += -DNDEBUG
NDK_PROJECT_PATH=PdfiumAndroid NDK_DEBUG=0 ndk-build

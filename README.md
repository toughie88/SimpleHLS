This is an Android app that simply plays and pauses an HTTP Live Stream for audio.  It's made using the (rather impressive) superpowered SDK. A lot of the functionality comes from the HLS Example provided by superpowered. 

To install:
1.) Download the Android NDK Make sure your local.properties file has 

ndk.dir=/path/to/ndk

in it.

2.) Download the superpowered SDK (superpowered.com) and make sure src/main/jni/Android.mk has

SUPERPOWERED_PATH = := /path/to/superpoweredsdk

This app is for purely educational purposes. 

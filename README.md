### *There is only one person you have no secrets for*

This Android app is an experiment to test the privacy implications of true love. Two loved ones install the app and activate it. The app shows a compass that always points towards the other app. This is implemented by using [Google Cloud Messaging](https://developer.android.com/google/gcm/index.html) to trigger the other app to send its GPS location to an app server. The client can then request the location and render the arrow.

This repository contains both the client (`app`) and server (`api`) code. It has been cleared of email addresses and passwords. A working version of the app is available in `Connect-Forever.apk` and in the Play store.

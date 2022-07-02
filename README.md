# VoiceTag

## 1 Introduction

> This is the result product for the Summer Bootcamp of Future Interaction for Smart Glasses hosted in Zhejiang University.

VoiceTag is an Android application based on Huawei Smart Eyewear. It allows users to leave messages asynchronously by the time, location, or even activity.

## 2 Abstraction
People may want to leave a message to somebody, expecting them to hear it at specific location, time or during specific activities. 

However, the existing communication platforms, such as QQ and telegram, are too complicated to send such messages, and also can’t afford automatic message receiving.

With SmartEyeWear, we designed an app, “VoiceTag”, to enable users to receive voice messages at specific location, time, or during certain activities, which are created and shared by fellow users.

The messages are stored with specific information tagged in the app’s cloud, waiting to be triggered.

In this way, we have implemented our unique asynchronous communication pattern: through sensors of SmartEyeWear and GPS, pre-uploaded voice messages will be triggered automatically under certain conditions, pleasantly surprising users during their daily monotony.

After several tests, our app now supports message triggering during time interval of 5 mins, space interval of 30m radius and during a walk.

## 3 Section & Contribution
- Liu Xiaokang:
  - 3200105838
  - Implement the front end of UI
  - Voice record function
  - Recording files storage management
  - Implement the location listener
- Jianjun Zhou：
- Ganhao Chen：
  - 3200102534
  - MediaPlayer management
  - Audio playing according to location
  - Audio playing according to time
  - Filename management & parsing

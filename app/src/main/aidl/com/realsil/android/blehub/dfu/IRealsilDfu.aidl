/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.realsil.android.blehub.dfu;

import com.realsil.android.blehub.dfu.IRealsilDfuCallback;

/**
 * APIs for Realsil Dfu service
 *
 */
interface IRealsilDfu {
    // Public API
    boolean start(String packageName, String addr, String path);
    boolean isWorking();
    boolean setSecretKey(in byte[] key);
    boolean setVersionCheck(boolean enable);
    int getCurrentOtaState();
    boolean setWorkMode(int mode);
    boolean setSpeedControl(boolean en, int speed);
    int getWorkMode();
    void registerCallback(String packageName, IRealsilDfuCallback cb); 
    void unregisterCallback(String packageName, IRealsilDfuCallback cb); 
}

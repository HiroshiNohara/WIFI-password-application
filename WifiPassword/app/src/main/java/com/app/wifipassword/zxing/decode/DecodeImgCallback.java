package com.app.wifipassword.zxing.decode;

import com.google.zxing.Result;

public interface DecodeImgCallback {
    void onImageDecodeSuccess(Result result);

    void onImageDecodeFailed();
}

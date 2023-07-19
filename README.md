

#### 项目说明

极速识别中国二代身份证、驾驶证、护照 (无需联网，离线秒扫，极速识别)身份证所有信息, 包含姓名、性别、出生年月、详细地址，正反面。不需要联网即可离线识别身份证所有信息包括新疆少数民族身份证，全网仅此一个，识别速度快，识别率高可保存识别图片。 

<img src='https://github.com/XieZhiFa/IdCardOCR/blob/master/image/device-demo.png?raw=true' width='800' alt='扫描示例'/>


#### Application中初始化
```java
OcrDecodeFactory.initOCR(context);
```


#### 调用扫描界面
```java
OcrDecodeFactory.newBuilder(context)
  .requestCode(REQUEST_CODE)
  .ocrType(binding.type.getSelectedItemPosition())    //0身份证, 1驾驶证, 2护照
  .broadcastAction(broadcastAction)                   //扫描结果发送到这个广播上
  .startOcrActivity();                                //使用内置的扫描界面
```


#### 返回结果
<img src='https://github.com/XieZhiFa/IdCardOCR/blob/master/image/device-result.png?raw=true' width='375' alt='识别结果'/>

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
        String result = data.getStringExtra("OCRResult");
        try {
            JSONObject jo = new JSONObject(result);
            StringBuffer sb = new StringBuffer();
            sb.append(String.format("正面 = %s\n", jo.opt("type")));
            sb.append(String.format("姓名 = %s\n", jo.opt("name")));
            sb.append(String.format("性别 = %s\n", jo.opt("sex")));
            sb.append(String.format("民族 = %s\n", jo.opt("folk")));
            sb.append(String.format("日期 = %s\n", jo.opt("birt")));
            sb.append(String.format("号码 = %s\n", jo.opt("num")));
            sb.append(String.format("住址 = %s\n", jo.opt("addr")));
            sb.append(String.format("签发机关 = %s\n", jo.opt("issue")));
            sb.append(String.format("有效期限 = %s\n", jo.opt("valid")));
            sb.append(String.format("整体照片 = %s\n", jo.opt("imgPath")));
            sb.append(String.format("头像路径 = %s\n", jo.opt("headPath")));
            sb.append("\n驾照专属字段\n");
            sb.append(String.format("国家 = %s\n", jo.opt("nation")));
            sb.append(String.format("初始领证 = %s\n", jo.opt("startTime")));
            sb.append(String.format("准驾车型 = %s\n", jo.opt("drivingType")));
            sb.append(String.format("有效期限 = %s\n", jo.opt("registerDate")));
            binding.textview.setText(sb.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}


//也可以通过广播接收扫描数据
private class ResultReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if(broadcastAction.equals(intent.getAction())){
      String result = intent.getStringExtra("OCRResult");
      Toast.makeText(context, "从广播中接收到扫描数据: " + result, Toast.LENGTH_LONG).show();
    }
  }
}
```




#### aar集成方法
将文件aar文件复制到 libs目录下, 然后在build.gradle中增加:

```groovy
android{
    repositories {
        flatDir {
            dirs 'libs'
        }
    }

    defaultConfig {
        manifestPlaceholders = [
            //debug.keystore生成, 正式包需要重新生成.
            //**注意: 一个KEY只绑定一个applicationId 多渠道打包需要注意**
            "OCR_API_KEY" : "26f1f6a0d4d7cb0dd0e9b28f4cedef83"    
        ]

        ndk {
            //abiFilters 'armeabi', 'x86', 'armeabi-v7a', 'arm64-v8a'

            //armeabi x86 基本上已经是淘汰了, Android11 以上版本请使用这两个架构的ABI
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }
}

dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation (name: 'library-ocr-2.0-SNAPSHOT', ext: 'aar')
    //Gradle7 修改了引用方式: implementation(files("./libs/library-ocr-2.0-SNAPSHOT.aar"))
}
```


#### 自定义识别框方式集成
如果当前扫描界面无法满足,您可以自己开发相机预览界面,使用以下API进行识别. 

```java
//1. Application中初始化
OcrDecodeFactory.initOCR(context);
```

```java
//2. 初始化解码器
OcrDecode ocrDecode = OcrDecodeFactory.newBuilder(context)
  .saveImage(saveImage)       //是否保存图片, 仅身份证模式有效, 表示自动裁剪身份证头像
  .ocrType(ocrType)           //0身份证, 1驾驶证, 2护照
  .margin(getResources().getDimension(R.dimen.public_40_dp)) //预览框边距, 身份证默认48dp, 护照默认20dp
  .build();
```

```java
/* 
	3. 开始解码
	3.1 这是一个异步耗时操作, 解析成功后会回调到onSuccess中, 自己解析json
	3.2 decode 方法, 为了提高识别效率及准确度, 建议对数据进行裁剪后再传入, 具体代码考虑SimpleCameraActivity
*/
ocrDecode.decode(byte[] jpeg, callback);
ocrDecode.decode(String path, callback);

//回调是异步的, 请不要直接更新UI
private OcrDecodeCallback callback = new OcrDecodeCallback() {
  @Override
  public void onSuccess(final String json) {

  }

  @Override
  public void onFail(final int cause) {
		/*
			LibraryInitOCR.DECODE_FAIL 
			LibraryInitOCR.DECODE_UNAUTHORIZED
			LibraryInitOCR.DECODE_AUTO_FOCUS
		*/
  }
};
```


```java
//4.在Activity onDestroy 释放资源
ocrDecode.close();
```




#### 混淆排除
```groovy
参考 app/proguard-rules.pro
```



#### 更新日志
```apl
2.0
1. 由于相机扫描识别容易出错, 所以重新优化放弃用了相机扫描方式, 改为拍照识别了
2. 更新Android 12 exported 问题
3. 更新识别结果广播出去防止嗅探问题
```



#### 技术支持 QQ:2227421573

    如果只用到身份证识别，可以将驾驶证的so库删除掉，并只使用 armeabi-v7a arm64-v8a 两个架构
    授权密钥请扫描码(image/pay.png) 不白嫖.
    并将applicationId及正式包sha1发给我，我给你KEY.
    sha1 查看方式: 
    命令行进入签名文件所在的目录执行:
    keytool -list  -v -keystore 签名文件.keystore -storepass 签名文件密码



>  还有军官证 护照离线识别有需要的小伙伴可联系
>
>  请不要重复问, 有没有使用限制, 没有安装数量限制, 没有调用次数限制, 无无无..., 后续更新直接接取新版本替代aar即可.
>
>  AAR 包含了身份证、驾驶证、护照的引擎因此包比较大, 如果没有用到可以做裁剪



#### 证示例图 (国内网络问题可能无法预览图片)

对着电脑扫描识别率会比较低, 建议使用身份证原件做测试.

![身份证示例图](https://github.com/XieZhiFa/IdCardOCR/blob/master/image/%E7%A4%BA%E4%BE%8B%E8%BA%AB%E4%BB%BD%E8%AF%81.png?raw=true)

![身份证示例图](https://github.com/XieZhiFa/IdCardOCR/blob/master/image/%e7%a4%ba%e4%be%8b%e9%a9%be%e7%85%a7.jpg?raw=true)


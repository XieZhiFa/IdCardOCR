#### 项目说明
极速识别中国二代身份证(无需联网，离线秒扫，极速识别)身份证所有信息, 包含
姓名、性别、出生年月、详细地址，正反面。
本应用使用相机进行识别中国二代身份证信息正反面,也可以选择相册中的图片
不需要联网即可离线识别身份证所有信息包括新疆少数民族身份证，全网仅此一个，识别速度快，识别率高,
可保存识别图片。 </br>

<img src='https://github.com/XieZhiFa/IdCardOCR/blob/master/image/device-demo.png?raw=true' width='800' alt='扫描示例'/>
 
 
#### Application中初始化
    LibraryInitOCR.initOCR(context);


#### 调用扫描界面
    Bundle bundle = new Bundle();
    bundle.putBoolean("saveImage", binding.saveImage.getSelectedItemPosition() == 0 ? true : false); // 是否保存识别图片
    bundle.putBoolean("showSelect", true);                          // 是否显示选择图片
    bundle.putBoolean("showCamera", true);                          // 显示图片界面是否显示拍照(驾照选择图片识别率比扫描高)
    bundle.putInt("requestCode", REQUEST_CODE);                     // requestCode
    bundle.putInt("type", binding.type.getSelectedItemPosition());  // 0身份证, 1驾驶证

    //broadcastAction 将扫描结果广播出去, 注意增加 intent.addCategory(context.getPackageName());
    //如果不需要广播,就不会传这个参数
    bundle.putString("broadcastAction", broadcastAction);
    LibraryInitOCR.startScan(context, bundle);


    //如果您不想集成aar, 那么可以通过隐式意图拉起示例中的扫描界面

    /*
    //身份证:com.msd.ocr.idcard.ICVideo, 驾驶证: com.msd.ocr.idcard.id.DIVideoActivity
    Intent intent = new Intent("com.msd.ocr.idcard.ICVideo");
    intent.putExtra("bundle", bundle);                         //具体参数如上
    intent.addCategory(getPackageName());                      //调用demo中的扫描界面使用: com.tomcat.ocr.idcard
    startActivityForResult(intent, REQUEST_CODE);
     */
	
	//返回的结果都是一样的. 但是选择图片的时候头像暂时不能提取.


#### 返回结果
<img src='https://github.com/XieZhiFa/IdCardOCR/blob/master/image/device-result.png?raw=true' width='375' alt='识别结果'/>

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




#### aar集成方法
将文件aar文件复制到 libs目录下, 然后在build.gradle中增加:

    android{
        repositories {
            flatDir {
                dirs 'libs'
            }
        }

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

    dependencies {
        implementation fileTree(include: ['*.jar'], dir: 'libs')
        implementation (name: 'library-ocr-1.0.7-SNAPSHOT', ext: 'aar')
        
        //使用OCR aar包 图片选择需要依赖另外一个库
        implementation 'com.squareup.picasso:picasso:2.4.0'
        implementation(name: 'library-multi-image-selector-1.0.5-SNAPSHOT', ext: 'aar')
    }


#### 自定义识别框方式集成
如果当前扫描界面无法满足,您可以自己开发相机预览界面,使用以下API进行识别. 

    //1. Application中初始化
    LibraryInitOCR.initOCR(context); 
    
    //2. 初始化解码器
    /**
     * 解码器初始化, 如果需要保存图片, 需要在调用向系统审核SDCard读写权限.
     * @param context   Activity
     * @param handler   接收解码消息
     * @param isSaveImage   是否保存图片
     */
    public static void initDecode(Context context, Handler handler, boolean isSaveImage)



    //3. 开始解码
    /**
     * 开始解码, 将相机预览数据传递到这里来, onPreviewFrame(byte[] data, Camera camera)
     * @param rect  预览框对象
     * @param previewWidth  界面预览宽
     * @param previewHeight 界面预览高
     * @param data  相机预览数据
     */
    public static void decode(Rect rect, int previewWidth, int previewHeight, byte[] data)

    
    /**
     * 识别选择的身份证图片(注意提前申请读写权限)
     * @param filePath  文件路径
     */
    public static void decode(String filePath)


    //4.在Activity onDestroy 释放资源
    /**
     * 释放资源
     */
    public static void closeDecode()
    
    
    
    //解码结果通过handler 接收
    switch (msg.what){
        //解码成功
        case LibraryInitOCR.DECODE_SUCCESS: {
            Intent intent = (Intent) msg.obj;
            String result = intent.getStringExtra("OCRResult");
            String headImg = intent.getStringExtra("headImg");
            String fullImg = intent.getStringExtra("fullImg");
            break;
        }

        //解码失败
        case LibraryInitOCR.DECODE_FAIL:{
            break;
        }

        //未授权
        case LibraryInitOCR.DECODE_UNAUTHORIZED:{
            break;
        }

        //提示重新聚焦
        case LibraryInitOCR.DECODE_AUTO_FOCUS:{
            break;
        }
    }
    
    

#### 混淆排除
    已经自动管理混淆,不需要再单独设置混淆排除了



#### 更新日志
    1.0.1
    初始版本提交, 相机直接扫描身份证识别.
    
    
    1.0.2
    1. 增加选择图片识别.
    2. 增加用户自定义扫描框识别.
    
    
    1.0.3
    1. 增加驾驶证识别.
    2. 选择图片界面增加相机拍照.
    3. 暂时不支持用户自定义扫描框识别驾驶证.
    
    
    1.0.4 
    1. 增加自定义扫描框调用示例.
    2. 自定义扫描Handler 不回调等优化.

    1.0.5
    1. 增加通过广播来返回数据.
    2. 统一用LibraryInitOCR.startScan(context, bundle);启动扫描界面.
    3. 代理混淆自动管理.

    1.0.6
    1. 增加 arm64-v8a so库。

    1.0.7
    1. 修复Android 11 版本手机, 初始化卡死问题.
    


#### 技术支持 QQ:2227421573
    如果只用到身份证识别，可以将驾驶证的so库删除掉，并只使用 armeabi-v7a arm64-v8a 两个架构
    授权密钥请扫描码(image/pay.png)
    并将applicationId及正式包sha1发给我，我给你KEY.
    sha1 查看方式: 
    命令行进入签名文件所在的目录执行:
    keytool -list  -v -keystore 签名文件.keystore -storepass 签名文件密码



#### 证示例图 (国内网络问题可能无法预览图片)
对着电脑扫描识别率会比较低, 建议使用身份证原件做测试.<br/><br/>
![身份证示例图](https://github.com/XieZhiFa/IdCardOCR/blob/master/image/%E7%A4%BA%E4%BE%8B%E8%BA%AB%E4%BB%BD%E8%AF%81.png?raw=true)
![身份证示例图](https://github.com/XieZhiFa/IdCardOCR/blob/master/image/%e7%a4%ba%e4%be%8b%e9%a9%be%e7%85%a7.jpg?raw=true)



    # Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#淆时忽略所以的警告
-ignorewarning
-dontoptimize


-keepattributes InnerClasses
-keepattributes EnclosingMethod


#排除身份证识别库本地方法
-keep class com.ym.idcard.reg.** {*;}
-keep class com.ym.ocr.img.** {*;}
-keep class hotcard.doc.reader.** {*;}
-keep class com.msd.ocr.idcard.LibraryInitOCR {*;}
-keepclassmembers class * {
    native <methods>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
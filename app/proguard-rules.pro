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

-dontskipnonpubliclibraryclassmembers

-keepattributes *Annotation*

-dontwarn module-info
-dontwarn org.apache.**
-dontwarn com.ianhanniballake.**
-dontwarn com.google.android.gms.**
-dontwarn com.ipaulpro.afilechooser.**
-dontwarn org.bouncycastle.**
-dontwarn okhttp3.**
-dontwarn com.aa.nmirror.navdy.tmapdata.**
-dontwarn org.yaml.snakeyaml.**

-keep class ai.comma.**  { *; }
-keep interface ai.comma.**  { *; }

-keep class org.capnproto.**  { *; }
-keep interface org.capnproto.**  { *; }

-keep class org.apache.**  { *; }
-keep interface org.apache.**  { *; }

-keep class okhttp3.**  { *; }
-keep interface okhttp3.**  { *; }

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}


-keep public class com.google.android.gms.** {
   public *;
}

-keep public class com.google.android.vending.** {
   public *;
}

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}


-keep class org.apache.http.** { *; }
-keep interface org.apache.http.** { *; }

-keep class android.net.** { *; }
-keep interface android.net.** { *; }

-dontwarn android.webkit.WebView
-dontwarn android.webkit.WebViewClient


-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep class jcifs.** { *; }
-dontwarn jcifs.**

-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }

-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-keep class it.sephiroth.android.library.tooltip.TooltipOverlayDrawable {*;}

-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }

-keep class com.google.android.** { *; }
-keep interface com.google.android.** { *; }

-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.services.drive.** { *; }

-dontwarn com.google.common.**

-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }

-keep class com.crossbowffs.remotepreferences.** { *; }
-keep interface com.crossbowffs.remotepreferences.** { *; }

-keep class org.sufficientlysecure.** { *; }
-keep interface org.sufficientlysecure.** { *; }

-keep class org.sufficientlysecure.** { *; }
-keep interface org.sufficientlysecure.** { *; }

-keep class de.robv.android.xposed.**{*;}
-keepnames class de.robv.android.xposed.**

#-keep class com.aa.nmirror.hotspot.** { *; }
#-keep interface com.aa.nmirror.hotspot.** { *; }

-keep class com.android.dx.** { *; }
-keep interface com.android.dx.** { *; }
#-dontwarn com.android.dx.**

-keep class org.mockito.** { *; }
-keep interface org.mockito.** { *; }
-dontwarn org.mockito.**

-keep class org.objenesis.** { *; }
-keep interface org.objenesis.** { *; }
-dontwarn org.objenesis.**

-keep class com.esafirm.imagepicker.** { *; }
-keep interface com.esafirm.imagepicker.** { *; }
-dontwarn com.esafirm.imagepicker.**

-keep class com.bumptech.glide.** { *; }
-keep interface com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**



-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keep class com.aa.nmirror.util.NotificationCenter$NotificationHandler {*;}
-keep interface com.aa.nmirror.util.NotificationCenter$NotificationHandler {*;}

-keep @com.aa.nmirror.util.NotificationCenter$NotificationHandler class *

-keepclassmembers class * {
	@com.aa.nmirror.util.NotificationCenter$NotificationHandler <fields>;
	@com.aa.nmirror.util.NotificationCenter$NotificationHandler <methods>;
}
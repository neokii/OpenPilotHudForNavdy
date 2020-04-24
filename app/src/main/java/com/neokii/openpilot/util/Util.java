package com.neokii.openpilot.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatTextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;


public class Util
{
    public static final boolean LOG_WRITE_FILE = false;

    private static final String TAG = "Util";

    public static Context context;

    public static int DP2PX(Context context, int DP)
    {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)((float)DP * scale);
    }

    public static int PX2DP(Context context, int PX)
    {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)((float)PX / scale);
    }


    public static void toast(Context context, String text)
    {
        try
        {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
        catch(Exception e){}
    }

    private static final AtomicInteger _nextGeneratedId = new AtomicInteger(1);

    public static void generateViewId(View v)
    {
        for(;;)
        {
            final int result = _nextGeneratedId.get();
            int newValue = result + 1;
            if(newValue > 0x00FF0000) newValue = 1;
            if(_nextGeneratedId.compareAndSet(result, newValue))
            {
                v.setId(result);
                return;
            }
        }
    }

    public static boolean isInstalled(Context context, String packageName)
    {
        try
        {
            context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return true;
        }
        catch(Exception e)
        {}
        return false;
    }

    public static boolean enabled(Context context, String packageName)
    {
        try
        {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return ai.enabled;
        }
        catch(Exception e)
        {}
        return false;
    }

    public static boolean openApp(Context context, String packageName, boolean multi_window)
    {
        if(isInstalled(context, packageName))
        {
            try
            {
                Intent i = context.getPackageManager().getLaunchIntentForPackage(packageName);

                /*if(!multi_window)
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                else
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);*/

                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(i);

                return true;
            }
            catch(Exception e)
            {
            }
        }

        return false;
    }

    public static boolean isComponentEnabled(PackageManager pm, String pkgName, String clsName)
    {
        ComponentName componentName = new ComponentName(pkgName, clsName);
        int componentEnabledSetting = pm.getComponentEnabledSetting(componentName);

        switch (componentEnabledSetting) {
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                return false;
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                return true;
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
            default:
                // We need to get the application info to get the component's default state
                try {
                    PackageInfo packageInfo = pm.getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES
                            | PackageManager.GET_RECEIVERS
                            | PackageManager.GET_SERVICES
                            | PackageManager.GET_PROVIDERS
                            | PackageManager.GET_DISABLED_COMPONENTS);

                    List<ComponentInfo> components = new ArrayList<>();
                    if (packageInfo.activities != null) Collections.addAll(components, packageInfo.activities);
                    if (packageInfo.services != null) Collections.addAll(components, packageInfo.services);
                    if (packageInfo.providers != null) Collections.addAll(components, packageInfo.providers);

                    for (ComponentInfo componentInfo : components) {
                        if (componentInfo.name.equals(clsName)) {
                            return componentInfo.isEnabled();
                        }
                    }

                    // the component is not declared in the AndroidManifest
                    return false;
                } catch (PackageManager.NameNotFoundException e) {
                    // the package isn't installed on the device
                    return false;
                }
        }
    }

    public static void printSubView(View v, int depth, String tag)
    {
        try
        {
            String str = "";
            for(int i = 0; i < depth; i++)
                str += "-";

            depth++;

            //v.setBackgroundColor(0xFFFFFF00);

            Log.e(tag, str + v.toString());

            if(!(v instanceof ViewGroup))
                return;

            ViewGroup vg = (ViewGroup)v;
            for(int i = 0; i < vg.getChildCount(); i++)
            {
                printSubView(vg.getChildAt(i), depth, tag);
            }
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
    }


    public static String getLauncher(Context context)
    {
        try
        {
            Intent intent= new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo defaultLauncher= context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return defaultLauncher.activityInfo.packageName;
        }
        catch (Throwable e){}

        return null;
    }

    public static void resetWallpaper(Context context)
    {
        try
        {
            /*Intent intent= new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo defaultLauncher= _appContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            String nameOfLauncherPkg= defaultLauncher.activityInfo.packageName;

            killProcess(nameOfLauncherPkg);*/

            //killProcess("com.android.systemui.ImageWallpaper");

            //WallpaperManager wallpaperManager =

        }
        catch (Exception e){}
    }



    public static String getHomeApp(Context context)
    {
        try
        {
            PackageManager localPackageManager = context.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            return localPackageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName;
        }
        catch(Exception e){}
        return null;
    }

    public static void launchHome(Context context)
    {
        try
        {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        catch(Exception e){}
    }

    public static void log(String tag, String msg)
    {
        Log.d(tag, msg);

        try
        {
            if(LOG_WRITE_FILE)
            {
                File file = new File(Environment.getExternalStorageDirectory(), "nmirror.log");

                FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), true);

                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                String formattedDate = df.format(c.getTime());

                fileWriter.append("\n[" + formattedDate + "] " + msg);
                fileWriter.flush();
                fileWriter.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public interface OnResultListener
    {
        void onResult(boolean success);
    }


    public static String md5(final String s)
    {
        final String MD5 = "MD5";
        try
        {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        }
        catch (NoSuchAlgorithmException e)
        {
        }
        return "";
    }

    public static boolean writeObjectToFile(Context context, Object obj, String fileName)
    {
        FileOutputStream fos = null;
        ObjectOutputStream os = null;
        try
        {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(obj);
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                os.close();
            }
            catch(Exception e){}

            try
            {
                fos.close();
            }
            catch(Exception e){}
        }

        return false;
    }

    public static Object readObjectFromFile(Context context, String fileName)
    {
        FileInputStream fis = null;
        ObjectInputStream is = null;

        try
        {
            fis = context.openFileInput(fileName);
            is = new ObjectInputStream(fis);
            return is.readObject();
        }
        catch(Exception e)
        {
        }
        finally
        {
            try
            {
                is.close();
            }
            catch(Exception e){}

            try
            {
                fis.close();
            }
            catch(Exception e){}
        }

        return null;
    }

    public static Point getLastSetDisplaySize(Context context)
    {
        Point size = new Point();

        /*int w = SettingUtil.getInt(_appContext, "__last_set_width__", 0);
        int h = SettingUtil.getInt(_appContext, "__last_set_height__", 0);

        if(w <= 0 || h <= 0)
        {
            WindowManager wm = (WindowManager)_appContext.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealSize(size);
        }
        else
        {
            size.x = w;
            size.y = h;
        }*/

        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealSize(size);

        return size;
    }

    public static void setLastSetDisplaySize(Context context, int width, int height)
    {
        SettingUtil.setInt(context, "__last_set_width__", width);
        SettingUtil.setInt(context, "__last_set_height__", height);
    }

    public static void removeFromParent(View v)
    {
        try
        {
            ((ViewGroup)v.getParent()).removeView(v);
        }
        catch(Exception e){}
    }


    public static Bitmap getBitmapFromAsset(Context context, String filePath)
    {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }

    public static void startForegroundService(Context context, Intent intent)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            context.startForegroundService(intent);
        }
        else
        {
            context.startService(intent);
        }
    }

    public static byte[] toBytes(Bitmap bitmap)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static Bitmap toBitmap(byte[] bytes)
    {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static boolean setMarqueeSpeed(TextView tv, float speed)
    {
        if (tv != null)
        {
            try
            {
                Field f = null;
                if (tv instanceof AppCompatTextView)
                {
                    f = tv.getClass().getSuperclass().getDeclaredField("mMarquee");
                }
                else
                {
                    f = tv.getClass().getDeclaredField("mMarquee");
                }

                f.setAccessible(true);
                Object marquee = f.get(tv);
                if (marquee != null)
                {
                    String scrollSpeedFieldName = "mPixelsPerSecond";
                    Field mf = marquee.getClass().getDeclaredField(scrollSpeedFieldName);
                    mf.setAccessible(true);

                    if((int)mf.getFloat(marquee) != (int)speed)
                        mf.setFloat(marquee, speed);

                    return true;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static void setText(TextView textView, String text)
    {
        String old = textView.getText() != null ? textView.getText().toString() : "";

        if(old.equals(text))
            return;

        textView.setText(text);
    }
}

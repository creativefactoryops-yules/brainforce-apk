package studio.creativefactory.della;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.util.List;

@CapacitorPlugin(name = "AppLauncher")
public class AppLauncherPlugin extends Plugin {

    @PluginMethod
    public void getApps(PluginCall call) {
        PackageManager pm = getContext().getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        } else {
            apps = pm.queryIntentActivities(intent, 0);
        }

        JSArray appList = new JSArray();
        for (ResolveInfo app : apps) {
            try {
                JSObject obj = new JSObject();
                obj.put("name", app.loadLabel(pm).toString());
                obj.put("package", app.activityInfo.packageName);
                obj.put("activity", app.activityInfo.name);

                Drawable icon = app.loadIcon(pm);
                if (icon != null) {
                    Bitmap bitmap = drawableToBitmap(icon);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                    String base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
                    obj.put("icon", "data:image/png;base64," + base64);
                }
                appList.put(obj);
            } catch (Exception e) { /* skip bad apps */ }
        }

        JSObject ret = new JSObject();
        ret.put("apps", appList);
        call.resolve(ret);
    }

    @PluginMethod
    public void launchApp(PluginCall call) {
        String pkg = call.getString("package");
        String act = call.getString("activity");
        if (pkg == null || act == null) {
            call.reject("Need package + activity");
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(pkg, act);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        call.resolve();
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int w = Math.max(drawable.getIntrinsicWidth(), 1);
        int h = Math.max(drawable.getIntrinsicHeight(), 1);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }
}

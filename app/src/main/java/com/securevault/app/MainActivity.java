package com.securevault.app;

import android.app.*;
import android.hardware.biometrics.BiometricPrompt;
import android.os.CancellationSignal;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.*;
import androidx.webkit.WebViewAssetLoader;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int PICK_FILES = 1001;
    private String pendingCategory = "other";
    private File vaultRoot;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        vaultRoot = new File(getFilesDir(), "vault");
        if (!vaultRoot.exists()) vaultRoot.mkdirs();
        webView = new WebView(this);
        setContentView(webView);
        WebView.setWebContentsDebuggingEnabled(false);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(false); s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder().addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r){
                String u=r.getUrl().toString();
                if(u.startsWith("https://vault.local/file/")) return serveVaultUrl(u);
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,String u){
                if(u.startsWith("https://vault.local/file/")) return serveVaultUrl(u);
                return loader.shouldInterceptRequest(Uri.parse(u));
            }
        });
        webView.addJavascriptInterface(new VaultBridge(), "AndroidVault");
        webView.loadUrl("https://appassets.androidplatform.net/assets/animations.html");
    }

    private WebResourceResponse serveVaultUrl(String url){
        try {
            String part=url.substring("https://vault.local/file/".length());
            String[] p=part.split("/",2); if(p.length<2) return null;
            String cat=URLDecoder.decode(p[0],"UTF-8"); String name=URLDecoder.decode(p[1],"UTF-8");
            File f=safeFile(cat,name); if(f==null||!f.exists()) return null;
            String mime=getMime(name); return new WebResourceResponse(mime,"UTF-8",new FileInputStream(f));
        }catch(Exception e){return null;}
    }

    private File categoryDir(String category){ File d=new File(vaultRoot,sanitize(category)); if(!d.exists()) d.mkdirs(); return d; }
    private String sanitize(String s){ return s==null?"other":s.replaceAll("[^a-zA-Z0-9_-]","_"); }
    private File safeFile(String category,String name){ if(name==null||name.contains("..")||name.contains("/")) return null; return new File(categoryDir(category),name); }
    private String getMime(String n){
        String x=n.toLowerCase(Locale.ROOT);
        if(x.endsWith(".jpg")||x.endsWith(".jpeg"))return"image/jpeg"; if(x.endsWith(".png"))return"image/png"; if(x.endsWith(".gif"))return"image/gif"; if(x.endsWith(".webp"))return"image/webp";
        if(x.endsWith(".mp4"))return"video/mp4"; if(x.endsWith(".webm"))return"video/webm"; if(x.endsWith(".mkv"))return"video/x-matroska"; if(x.endsWith(".3gp"))return"video/3gpp"; if(x.endsWith(".mov"))return"video/quicktime";
        if(x.endsWith(".mp3"))return"audio/mpeg"; if(x.endsWith(".wav"))return"audio/wav"; if(x.endsWith(".m4a"))return"audio/mp4"; if(x.endsWith(".ogg"))return"audio/ogg"; if(x.endsWith(".aac"))return"audio/aac";
        if(x.endsWith(".pdf"))return"application/pdf"; if(x.endsWith(".txt"))return"text/plain"; if(x.endsWith(".html"))return"text/html"; if(x.endsWith(".json"))return"application/json"; return"application/octet-stream";
    }
    private String jsonList(String category){
        JSONArray a=new JSONArray(); File[] fs=categoryDir(category).listFiles(); if(fs==null)return a.toString();
        Arrays.sort(fs,(x,y)->Long.compare(y.lastModified(),x.lastModified()));
        for(File f:fs)try{JSONObject o=new JSONObject();o.put("name",f.getName());o.put("size",f.length());o.put("time",f.lastModified());o.put("mime",getMime(f.getName()));o.put("url","https://vault.local/file/"+URLEncoder.encode(category,"UTF-8")+"/"+URLEncoder.encode(f.getName(),"UTF-8"));a.put(o);}catch(Exception ignored){}
        return a.toString();
    }
    private String categoryFromMime(String mime,String name){
        String m=mime==null?getMime(name):mime; if(m.startsWith("image/"))return"images"; if(m.startsWith("video/"))return"videos"; if(m.startsWith("audio/"))return"music"; if(m.equals("application/pdf")||m.startsWith("text/")||m.contains("word")||m.contains("excel")||m.contains("powerpoint"))return"documents"; return"other";
    }
    private String uniqueName(File dir,String name){String base=name,ext="";int d=name.lastIndexOf('.');if(d>0){base=name.substring(0,d);ext=name.substring(d);}String out=name;int i=1;while(new File(dir,out).exists())out=base+"_"+(i++)+ext;return out;}

    private class VaultBridge {
        @JavascriptInterface public String listFiles(String category){return jsonList(category);}
        @JavascriptInterface public void pickFiles(String category){ pendingCategory=category==null?"other":category; runOnUiThread(()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(acceptType(pendingCategory));i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,PICK_FILES);
        }); }
        @JavascriptInterface public boolean deleteFile(String category,String name){File f=safeFile(category,name);return f!=null&&f.delete();}
        @JavascriptInterface public boolean renameFile(String category,String oldName,String newName){File a=safeFile(category,oldName),b=safeFile(category,newName);return a!=null&&b!=null&&a.exists()&&!b.exists()&&a.renameTo(b);}
        @JavascriptInterface public void toast(String message){runOnUiThread(()->Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show());}
        @JavascriptInterface public void authenticateBiometric(){ runOnUiThread(()->{ if(android.os.Build.VERSION.SDK_INT<28){ Toast.makeText(MainActivity.this,"Biometric control is not available on this Android version",Toast.LENGTH_SHORT).show(); return; } Executor ex=MainActivity.this.getMainExecutor(); BiometricPrompt prompt=new BiometricPrompt.Builder(MainActivity.this).setTitle("Folder Locker").setSubtitle("Unlock your private vault").setDescription("Use your phone biometric control to unlock Folder Locker.").setNegativeButton("Use password",ex,(d,w)->{}).build(); prompt.authenticate(new CancellationSignal(),ex,new BiometricPrompt.AuthenticationCallback(){ @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){ webView.post(()->webView.evaluateJavascript("window.onNativeBiometric && window.onNativeBiometric(true)",null)); } @Override public void onAuthenticationFailed(){ webView.post(()->webView.evaluateJavascript("window.onNativeBiometric && window.onNativeBiometric(false)",null)); } @Override public void onAuthenticationError(int e,CharSequence m){ webView.post(()->webView.evaluateJavascript("window.onNativeBiometric && window.onNativeBiometric(false)",null)); }}); }); }
    }
    private String acceptType(String c){ if("images".equals(c))return"image/*";if("videos".equals(c))return"video/*";if("music".equals(c))return"audio/*";if("documents".equals(c))return"application/pdf,text/*,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation";return"*/*"; }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=PICK_FILES||resultCode!=RESULT_OK||data==null)return;ArrayList<Uri> uris=new ArrayList<>();if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());else if(data.getData()!=null)uris.add(data.getData());int added=0;for(Uri u:uris)if(copyUri(u,pendingCategory))added++;final int n=added;webView.postDelayed(()->webView.evaluateJavascript("window.onNativeFilesImported && window.onNativeFilesImported("+JSONObject.quote(pendingCategory)+","+n+")",null),100);}
    private boolean copyUri(Uri uri,String requested){try{String name=getName(uri);String mime=getContentResolver().getType(uri);String actual=categoryFromMime(mime,name);if(!"other".equals(requested)&&!requested.equals(actual))return false;File dir=categoryDir(actual);String out=uniqueName(dir,name);File f=new File(dir,out);try(InputStream in=getContentResolver().openInputStream(uri);OutputStream os=new FileOutputStream(f)){if(in==null)return false;byte[] b=new byte[65536];int r;while((r=in.read(b))!=-1)os.write(b,0,r);}return true;}catch(Exception e){return false;}}
    private String getName(Uri uri){try(Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)return c.getString(i);}}catch(Exception ignored){}String p=uri.getPath();return p==null?"file_"+System.currentTimeMillis():p.substring(p.lastIndexOf('/')+1);}
}

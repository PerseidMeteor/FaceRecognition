package com.example.facerecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{

    private static final int PERMISSIONS_REQUESTCODE = 1001;

    private static final int ALBUM_REQUEST = 0;

    private static final int CAMERA_REQUEST = 1;

    private static String TAG = "FaceRecognititon";

    private static String imageFilePath = "Android/data/com.example.package.name/files/Pictures";

    private Button title_back;

    private ImageButton title_more;

    private static ProgressDialog dialog;

    private AlertDialog alertDialog;

    private static final String[] PERMISSIONS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,
            Manifest.permission.CAMERA};


    private static final int IMG_COUNT = 8;//最大添加图片数

    private static final String IMG_ADD_TAG = "The button for add picture";//判断是不是添加按钮

    private GridView gridView;

    private MyAdapter adapter;

    private List<String> list;     //存储bitmap的数组

    Button button;                //匹配按钮

    List<String> permissionsList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();//动态获取权限



        gridView = findViewById(R.id.GridPicture);
        button = findViewById(R.id.btMatch);
        adapter = new MyAdapter();
        list = new ArrayList<>();

        list.add(IMG_ADD_TAG);

        gridView.setAdapter(adapter);
        refreshAdapter();//刷新


        //状态栏颜色设置为深色
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //修改为深色，因为我们把状态栏的背景色修改为主题色白色，默认的文字及图标颜色为白色，导致看不到了。
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        //gridViewItem监听事件
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (list.get(position).equals(IMG_ADD_TAG)) {
                    new DialogForPhoto(MainActivity.this){
                        @Override
                        public void btnPickByTake(){
                            openCameraIntent();
                        }

                        @Override
                        public void btnPickBySelect() {
                            //判断图片的数目是否超出限制
                          if (list.size() < IMG_COUNT) {
                              //打开相册，选择图片，这里存在小问题，一次只能选择一张图片，日后有时间再进行修改
                             Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                             startActivityForResult(intent, ALBUM_REQUEST);
                          }else
                            Toast.makeText(MainActivity.this, "图片数目超过上限", Toast.LENGTH_SHORT).show();
                        }

                    }.show();
                }
            }
        });

        //匹配按钮监听事件
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(list.size()<3)
                    Toast.makeText(MainActivity.this, "请至少选择两张图片", Toast.LENGTH_SHORT).show();
                else{

                    Handler handler=new Handler();
                    //在run里面写了跳转activity
                    Runnable runnable=new Runnable() {
                        @Override
                        public void run() {
                          Intent intent = new Intent(MainActivity.this,MatchActivity.class) ;
                          intent.putExtra("Path_List", (Serializable)list);
                          startActivity(intent);
                        }
                    };
                    handler.post(runnable);

                    dialog= ProgressDialog.show(MainActivity.this,"请稍等","正在进行人脸分析匹配",
                            true,true);
                }
            }
        });


        //隐藏actionbar
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.hide();
        }

        //退出按钮设置
        title_back = findViewById(R.id.title_back);
        title_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //退出APP
                finish();
            }
        });

        alertDialog = new AlertDialog.Builder(this)
                .setMessage("人脸五官匹配数值处于[0,1]之间，越接近1则越相似，考虑到图片清晰度和截取位置的偏差，大约在0.6以上" +
                        "可认定为相似，匹配值为0即未检测到某个部位或相似度极小。\n更多问题请联系QQ：3223324058（西北大学软工于强）")
                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();

        //更多按钮设置
        title_more = findViewById(R.id.title_more);
        title_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.show();
            }
        });
    }

    //在onCreate方法外面定义静态方法
    public static void closeProgressDialog() {
        dialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case ALBUM_REQUEST:{
                if(resultCode == RESULT_OK){
                    if(list.size() < IMG_COUNT){
                        final Uri uri = data.getData();
                        String path = getImageAbsolutePath(this, uri);
                        removeItem();
                        list.add(path);
                        list.add(IMG_ADD_TAG);
                        refreshAdapter();
                    }else {
                        Toast.makeText(this, "图片数目已达上限", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case CAMERA_REQUEST:{
                if(resultCode == RESULT_OK){
                    if(list.size() < IMG_COUNT){
                        final Uri uri = Uri.parse(imageFilePath);
                        removeItem();
                        list.add(uri.toString());
                        list.add(IMG_ADD_TAG);
                        refreshAdapter();
                    }else{
                        Toast.makeText(this, "图片数目已达上限",Toast.LENGTH_SHORT).show();
                    }
                }
            }
            default:break;
        }
    }

    private void removeItem() {
        if(list.size() < IMG_COUNT && list.size() > 0)
            list.remove(list.size()-1);
    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        //有些问题，稍后修改
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplication()).inflate(R.layout.gv_photo_item, parent, false);
                holder = new ViewHolder();
                holder.imageView =  convertView.findViewById(R.id.main_gridView_item_photo);
                holder.checkBox =  convertView.findViewById(R.id.main_gridView_item_cb);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String str = list.get(position);

            if (!str.equals(IMG_ADD_TAG)) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.imageView.setImageBitmap(createImageThumbnail(str));
            } else {
                holder.checkBox.setVisibility(View.GONE);
                holder.imageView.setImageResource(R.mipmap.add);
            }

            //点击删除图标
            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    list.remove(position);//移除选择的图标
                    refreshAdapter();//刷新
                }
            });
            return convertView;
        }

        /*
        * 私有内部类，一个是图片，一个是右上角的选择框
        */
        private class ViewHolder {
            ImageView imageView;
            CheckBox checkBox;
        }
    }

    private void refreshAdapter() {
        //先判断adapter和list是否为空
        if (list == null) {
            list = new ArrayList<>();
        }
        if (adapter == null) {
            adapter = new MyAdapter();
        }
        //刷新
        adapter.notifyDataSetChanged();
    }


    /*
    * 动态获取权限
    * */
    private void getPermission(){
        //            0表示授权，-1表示未授权
        permissionsList.clear();

        if (Build.VERSION.SDK_INT >= 23) {
            //验证是否许可权限
            for (String str : PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(this,str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    permissionsList.add(str);
                }
            }
        }
        //请求权限
        if(!permissionsList.isEmpty()){
            String[] permissions = permissionsList.toArray(new String[permissionsList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permissions,PERMISSIONS_REQUESTCODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_REQUESTCODE:
                break;
            default:
                break;
        }
    }


/*
* 以下为图片处理函数
* */

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imageFilePath = image.getAbsolutePath();
        return image;
    }

    private void openCameraIntent() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(pictureIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            if (photoFile != null) {
                //Uri photoURI = Uri.fromFile(photoFile);
                Uri photoURI = FileProvider.getUriForFile(this,getPackageName() +".provider",photoFile);
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                startActivityForResult(pictureIntent,CAMERA_REQUEST);
            }
        }
    }

    public Bitmap createImageThumbnail(String filePath) {
        getPermission();//动态获取权限

        Bitmap bitmap = null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;


        opts.inSampleSize = computeSampleSize(opts, -1, 128 * 128);
        opts.inJustDecodeBounds = false;
        try {
            bitmap = BitmapFactory.decodeFile(filePath, opts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public String getImageAbsolutePath(Activity context, Uri imageUri) {

        getPermission();//动态获取权限

        if (context == null || imageUri == null)
            return null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, imageUri)) {
            if (isExternalStorageDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(imageUri)) {
                String id = DocumentsContract.getDocumentId(imageUri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = null;//不获取视频
                } else if ("audio".equals(type)) {
                    contentUri = null;//不获取音频
                }
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
            if (isGooglePhotosUri(imageUri))
                return imageUri.getLastPathSegment();
            return getDataColumn(context, imageUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return imageUri.getPath();
        }
        return null;
    }

    public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        getPermission();//动态获取权限

        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        getPermission();//动态获取权限

        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    public int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        getPermission();//动态获取权限

        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
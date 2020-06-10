package com.example.facerecognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import com.example.facerecognition.ui.main.SectionsPagerAdapter;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatchActivity extends AppCompatActivity {

    private List<String> pictureList;     //存储图像地址的数组

    private List<List<Bitmap>> tabLists;  //存储tab图像地址的数组

    private ObjectDetector mFaceDetector; //脸部detector

    private ObjectDetector mEyesDetector; //眼睛detector

    private ObjectDetector mNoseDetector; //鼻子detector

    private ObjectDetector mMouthDetector; //嘴巴detector

    private static String strLibraryName = "opencv_java4"; // 不需要添加前缀 libopencv_java4

    private static String TAG = "MatchActivity";

    private static final int AEG_CLOSE_PROGRESSBAR = 100;

    private ImageButton title_back;

    private List<double[]> similarities = new ArrayList<>();

    static {
        try {
            Log.e("loadLibrary", strLibraryName);
            System.loadLibrary(strLibraryName);
            //System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // couldn't find "libopencv_java320.so"
        } catch (UnsatisfiedLinkError e) {
            Log.e("loadLibrary", "Native code library failed to load.\n" + e);
        } catch (Exception e) {
            Log.e("loadLibrary", "Exception: " + e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        //状态栏颜色设置为深色
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //修改为深色，因为我们把状态栏的背景色修改为主题色白色，默认的文字及图标颜色为白色，导致看不到了。
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        //接受intent传来的数组
        pictureList = (List<String>)getIntent().getSerializableExtra("Path_List");


        //生成脸部级联分辨器
        mFaceDetector = new ObjectDetector(this, R.raw.haarcascade_frontalface_alt,
                6, 0.2F, 0.2F, new Scalar(255, 0, 0, 255));

        mEyesDetector = new ObjectDetector(this, R.raw.haarcascade_eye,
                6, 0.2F, 0.2F, new Scalar(255, 0, 0, 255));

        mNoseDetector = new ObjectDetector(this, R.raw.haarcascade_mcs_nose,
                6, 0.2F, 0.2F, new Scalar(255, 0, 0, 255));

        mMouthDetector = new ObjectDetector(this, R.raw.haarcascade_mcs_mouth,
                6, 0.2F, 0.2F, new Scalar(255, 0, 0, 255));


        //生成新的tab数组
        tabLists = CreateList(pictureList);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager(),
                tabLists,similarities);

        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        //退出按钮设置
        title_back = findViewById(R.id.title_back);
        title_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //退出APP
                finish();
            }
        });


        if(pictureList.size() > 4){
            tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            Message message = mHandler.obtainMessage(AEG_CLOSE_PROGRESSBAR);
            mHandler.sendMessage(message);
        }

    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what){
                case AEG_CLOSE_PROGRESSBAR:
                    MainActivity.closeProgressDialog();
                    break;
            }
        }};

    private List<List<Bitmap>> CreateList(List<String> pircturelist){
        List<List<Bitmap>> lists = new ArrayList<>();

        List<Bitmap> bitmapList = new ArrayList<>();

        List<Mat[]> mats = new ArrayList<>();

        Mat matFace;
        Mat matEyes;
        Mat matNose;
        Mat matMouth;

        //存储图像地址的最后一个元素时TAG，不是地址，故排除
        for(int j = 0;j < pictureList.size()-1;++j){
            Bitmap bitmap = BitmapFactory.decodeFile(pictureList.get(j));
            matFace = detect(mFaceDetector,bitmap,true);
            matEyes = detect(mEyesDetector, bitmap,true);
            matNose = detect(mNoseDetector, bitmap,false);
            matMouth = detect(mMouthDetector, bitmap,false);
            mats.add(new Mat[]{matFace,matEyes,matNose,matMouth});
            bitmapList.add(bitmap);
        }

        for(int i = 0;i< bitmapList.size();++i){
            for(int j = i+1;j < bitmapList.size();++j){
                lists.add(Arrays.asList(bitmapList.get(i),bitmapList.get(j)));
                similarities.add(compare(mats.get(i), mats.get(j)));
            }
        }
        return lists;
    }

    private Mat detect(ObjectDetector objectDetector,Bitmap bitmap,boolean flag) {
        Mat rectMat = null;
        try {
            // bitmapToMat
            Mat toMat = new Mat();
            Utils.bitmapToMat(bitmap, toMat);
            Mat copyMat = new Mat();
            toMat.copyTo(copyMat); // 复制

            // togray
            Mat gray = new Mat();
            Imgproc.cvtColor(toMat, gray, Imgproc.COLOR_RGBA2GRAY);

            MatOfRect mRect = new MatOfRect();
            Rect[] object = objectDetector.detectObjectImage(gray, mRect);

            if(!flag) {
                Rect rectTmp = null;
                if (object != null) {
                    rectTmp = object[0];
                    Imgproc.rectangle(
                            toMat,
                            new Point(rectTmp.x, rectTmp.y),
                            new Point(rectTmp.x + rectTmp.width, rectTmp.y + rectTmp.height),
                            new Scalar(255, 0, 0), 2);
                }

                if (rectTmp != null) {
                    // 剪切最大的头像
                    Rect rect = new Rect(rectTmp.x, rectTmp.y, rectTmp.width, rectTmp.height);
                    rectMat = new Mat(copyMat, rect);  // 从原始图像拿
                }
            }
            else {

            int maxRectArea = 0 * 0;
            Rect maxRect = null;

            int facenum = 0;
            // Draw a bounding box around each face.
            for (Rect rect : object) {
                Imgproc.rectangle(
                        toMat,
                        new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(255, 0, 0), 2);
                ++facenum;
                // 找出最大的面积
                int tmp = rect.width * rect.height;
                if (tmp >= maxRectArea) {
                    maxRectArea = tmp;
                    maxRect = rect;
                }
            }

            if (facenum != 0) {
                // 剪切最大的头像
                Rect rect = new Rect(maxRect.x, maxRect.y, maxRect.width, maxRect.height);
                rectMat = new Mat(copyMat, rect);  // 从原始图像拿
            }

            }
            Utils.matToBitmap(toMat, bitmap);

            return rectMat;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rectMat;
    }
    private double[] compare(Mat[] mat1,Mat[] mat2){
        double[] similarity = new double[4];
        for (int i = 0;i<4;++i){
            similarity[i] = 0;
            if(mat1[i] == null || mat2[i] == null){
                continue;
            }
            Mat hist_1 = new Mat();
            Mat hist_2 = new Mat();

            Imgproc.resize(mat2[i],mat2[i],mat1[i].size());

            mat1[i].convertTo(mat1[i], CvType.CV_32F);
            mat2[i].convertTo(mat2[i], CvType.CV_32F);

            MatOfFloat ranges = new MatOfFloat(0f, 256f);
            MatOfInt histSize = new MatOfInt(1000);

            Imgproc.calcHist(Arrays.asList(mat1[i]), new MatOfInt(0),
                    new Mat(), hist_1, histSize, ranges);
            Imgproc.calcHist(Arrays.asList(mat2[i]), new MatOfInt(0),
                    new Mat(), hist_2, histSize, ranges);

            similarity[i] = Imgproc.compareHist(hist_1, hist_2, Imgproc.CV_COMP_CORREL);
        }
        return similarity;
    }
}

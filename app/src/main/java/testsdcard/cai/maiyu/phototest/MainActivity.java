package testsdcard.cai.maiyu.phototest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    //拍照按钮
    @ViewInject(R.id.take_photo)
    private Button mBtnPicture;

    //从相册中选择按钮
    @ViewInject(R.id.select_photo)
    private Button mBtnSelectPic;

    //显示图片按钮
    @ViewInject(R.id.picture)
    private ImageView imgPicture;

    //拍照，选择图片
    private static  final  int TAKE_PHOTO = 1;
    private static  final  int SELECT_PHOTO = 2;

    private static final String TAG = "MainActivity";
    //Uri对象
    private Uri imageUri ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置布局
        setContentView(R.layout.activity_main);
        //初始化inject
        ViewUtils.inject(this);


    }

    /**
     * 拍照监听
     * @param view
     */
    @OnClick(R.id.take_photo)
    public void takePhone(View view){

        //从缓存中读取
        File outputImage = new File(getExternalCacheDir() , "output_image.jpg");

        try{
            //若本来存在则删除
            if(outputImage.exists()){
                outputImage.delete();
            }
            outputImage.createNewFile();

        }catch (Exception e){
            e.printStackTrace();
        }

        //判断版本权限，从24开始，是需要运行时权限的
        if(Build.VERSION.SDK_INT  >= 24){
            imageUri = FileProvider.getUriForFile(this , "testsdcard.cai.maiyu.phototest.fileprovider" , outputImage);
        }else {
            imageUri = Uri.fromFile(outputImage);
        }

        //创建intent，设置图片,启动
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT , imageUri);
        startActivityForResult(intent , TAKE_PHOTO);

    }


    /**
     * 从相册中选择图片
     * @param view
     */
    @OnClick(R.id.select_photo)
    public void selectPhoto(View view){
        Log.d(TAG , "selectPhoto");

        //检查权限
        if(ContextCompat.checkSelfPermission(MainActivity.this , Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            //若没有权限，则调用方法去访问权限
            ActivityCompat.requestPermissions(MainActivity.this , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE} , 1);

        }else {
            //打开相册
            openAlbum();
        }

    }

    /**
     * 打开相册
     */
    private void openAlbum() {

        Log.d(TAG , "openAlbum");

        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent , SELECT_PHOTO);
    }

    /**
     * 访问权限回调方法
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(TAG , "onRequestPermissionsResult");
        switch (requestCode){
            case  1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //打开相册
                    openAlbum();
                }else {
                    Toast.makeText(this , "你禁止了权限" , Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }


    /**
     * 回调方法
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case TAKE_PHOTO :
                if(resultCode == RESULT_OK){
                    try {
                        //获取图片，显示
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver()
                                .openInputStream(imageUri));
                        imgPicture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case  SELECT_PHOTO :
                Log.d(TAG , "activity result select");
                if(resultCode == RESULT_OK){
                    //Android4.4
                    if(Build.VERSION.SDK_INT  >= 19){
                        handleImageOnKitKat(data);
                    }else{
                        handleImageBeforeKitKat(data);
                    }

                }
                break;
            default:
                break;

        }

    }

    /**
     * 在4.4以下时调用
     * @param data
     */
    private void handleImageBeforeKitKat(Intent data) {
        Log.d(TAG , "handleImage below 4.4");
        Uri uri = data.getData();
        String imagePath = getImagePath(uri ,null);
        displayImage(imagePath);
    }

    /**
     * 在4.4或以上调用
     * @param data
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        //如果是document类型的uri,则通过document id处理
        if(DocumentsContract.isDocumentUri(this , uri)){
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI , selection);
            }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content:" +
                        "//downloads/public_downloads") , Long.valueOf(docId));
                imagePath = getImagePath(contentUri , null);
            }
        }else if("content".equalsIgnoreCase(uri.getScheme())){
            //若果是content类型的uri,则使用普通方式处理
            imagePath = getImagePath(uri , null);
        }else  if("file".equalsIgnoreCase(uri.getScheme())){
            //如果是file类型的uri,直接获取图片路径即可
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    /**
     * 获取根据imagePath显示图片
     * @param imagePath
     */
    private void displayImage(String imagePath) {
        Log.d(TAG , "displayImage");
        if(imagePath != null){
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            imgPicture.setImageBitmap(bitmap);
        }else {
            Toast.makeText(this , "获取图片失败" ,Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 根据uri ,String获取图片
     * @param uri
     * @param selection
     * @return
     */
    private String getImagePath(Uri uri, String selection) {
        Log.d(TAG , "getImagePath");

        //创建路径
        String path = null;
        //查询
        Cursor cursor = getContentResolver().query(uri , null , selection , null ,null);
        if(cursor != null){
            if(cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return  path;
    }
}

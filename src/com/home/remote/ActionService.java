package com.home.remote;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ActionService extends Service {
    public static final String TAG = "ActionService";
    private static final String DEVIEDOMIN = "homedevice.iask.in";
    private static final int PORT = 19900;

   /*activity��service��ʱ�����*/
    public IBinder onBind(Intent intent) {
    	Log.d(TAG, "onBind");
        return new MyBinder();
    }

    /*��һ�δ���ʱ�Ż���ã�֮���ڷ���û�����ٶ�������� ������*/
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    /*��һ�δ���ʱ���Ժ�ÿ������������ã��ؽ�*/
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

   /*��onStartCommand()����һ��ÿ�ζ�����ã�����*/
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, "onStart");
    }

    /*����ʱ����*/
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public class MyBinder extends Binder {
        ActionService getService() {
            return ActionService.this;
        }
    }
    
    private void connectServerWithTCPSocket() {   
        Socket socket;  
        try {// ����һ��Socket���󣬲�ָ������˵�IP���˿ں�  
            socket = new Socket(DEVIEDOMIN, PORT);  
            // ����һ��InputStream�û���ȡҪ���͵��ļ���  
            InputStream inputStream = new FileInputStream("/sdcard/");  
            // ��ȡSocket��OutputStream�������ڷ������ݡ�  
            OutputStream outputStream = socket.getOutputStream();  
            // ����һ��byte���͵�buffer�ֽ����飬���ڴ�Ŷ�ȡ�ı����ļ�  
            byte buffer[] = new byte[4 * 1024];  
            int temp = 0;  
            // ѭ����ȡ�ļ�  
            while ((temp = inputStream.read(buffer)) != -1) {  
                // ������д�뵽OuputStream������  
                outputStream.write(buffer, 0, temp);  
            }  
            // ���Ͷ�ȡ�����ݵ������  
            outputStream.flush();  
  
            /** �򴴽�һ�����ģ�ʹ��BufferedWriterд��,��������� **/  
//          String socketData = "[2143213;21343fjks;213]";  
//          BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(  
//                  socket.getOutputStream()));  
//          writer.write(socketData.replace("\n", " ") + "\n");  
//          writer.flush();  
            /************************************************/  
        } catch (UnknownHostException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
  
    }
}
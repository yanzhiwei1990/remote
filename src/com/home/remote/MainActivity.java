package com.home.remote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.PowerManager;


public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	//private static final String DEVIEDOMIN = "homedevice.iask.in";
	private static final String DEVIEDOMIN = "192.168.0.106";
    //private static final int PORT = 39627;
	private static final int PORT = 8080;
    private boolean isConnect = false;
    private Socket mSocket;
    //private PrintWriter mSend;
    private BufferedWriter mSend;
    private BufferedReader mRead;
    private boolean isRun = true;
    private boolean isonline = false;
    private long onlinetimeoutrecord = -1;
    private long onlinetimecount = 0;
    private long onlinejudgetimeoutrecord = -1;
    private long onlinejudgetimecount = 0;
    
    private Button Button_connect;
    private Button Button_ring;
    private Button Button_manual_answer;
    private Button Button_auto_answer;
    private EditText EditText_ip;
    private EditText EditText_port;
    private TextView TextView_status;
    private Button Button_tv_power;
    private Button Button_magic_power;
    private Button Button_left;
    private Button Button_reset;
    private Button Button_right;
    
    private final int STATUS = 1;
    private final int START_SOCKET = 2;
    private final int CLOSE_SOCKET = 3;
    private final int SERIESACTION = 4;
    
    private PowerManager mPowerManager = null;
    private WakeLock mWakeLock = null; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPowerManager = (PowerManager)MainActivity.this.getSystemService(Context.POWER_SERVICE);
        init_button();
        mHandler.sendEmptyMessage(STATUS);
        //mHandler.sendEmptyMessage(START_SOCKET);
        Log.i(TAG, "onCreate finished");
    }

    private Handler mHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		switch (msg.what){
    			case STATUS:
    				if (isConnect) {
    					TextView_status.setText("连接到服务器！");
    				} else {
    					TextView_status.setText("未连接到服务器！");
    				}
    				Button_connect.setText(isConnect ? "已连接" : "连接");
    				if (mSocket != null) {
    					isConnect = mSocket.isConnected() && !mSocket.isClosed();
    				} else {
    					isConnect = false;
    				}
    				onlinejudgetimecount++;
    				/*if (onlinetimeoutrecord < 0) {
    					onlinetimeoutrecord = onlinetimecount;
    				}
    				if (onlinejudgetimeoutrecord < 0) {
    					onlinejudgetimeoutrecord = onlinejudgetimecount;
    				}
    				long onlineperiod = onlinetimecount - onlinetimeoutrecord;
    				long onlinejudgeperiod = onlinejudgetimecount - onlinejudgetimeoutrecord;
    				if (onlinejudgeperiod >= onlineperiod + 5) {
    					onlinetimeoutrecord = onlinetimecount;
    					onlinejudgetimeoutrecord = onlinejudgetimecount;
    					isonline = false;
    				}*/
    				if (onlinejudgetimecount - onlinetimecount > 5) {
    					isonline = false;
    					onlinetimecount = 0;
    					onlinejudgetimecount = 0;
    				}
    				if (isonline) {
    					Button_magic_power.setTextColor(getResources().getColor(R.color.green));
                    } else {
                    	Button_magic_power.setTextColor(getResources().getColor(R.color.red));
                    }
    				//Log.i(TAG, "onlineperiod = " + onlineperiod + ", onlinejudgeperiod = " + onlinejudgeperiod + ", isonline = " + isonline + ", isConnect = " + isConnect);
    				//Log.i(TAG, "onlinejudgetimecount = " + onlinejudgetimecount + ", onlinetimecount = " + onlinetimecount + "isonline = " + isonline + ", isConnect = " + isConnect);
    				mHandler.sendEmptyMessageDelayed(STATUS, 1000);
    				sendSocketMessage("client online");
    				break;
    			case START_SOCKET:
    				connectServerWithTCPSocket();
    				break;
    			case CLOSE_SOCKET:
                	closeSocket();
    				break;
    			case SERIESACTION:
    				if (msg.obj != null) {
    					sendSocketMessage((String)msg.obj);
    				}
    				break;
    			default:
    				break;
    		}
    	}
    };
    
    private void init_button() {
    	EditText_ip = (EditText) findViewById(R.id.editText_ip);
    	EditText_port = (EditText) findViewById(R.id.EditText_port);
    	TextView_status = (TextView) findViewById(R.id.TextView_status);
    	Button_connect = (Button) findViewById(R.id.button_connect);
    	Button_connect.requestFocus();
    	Button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isConnect) {
                	mHandler.sendEmptyMessage(START_SOCKET);
                	Log.d(TAG, "connect......");
                } else {
                	mHandler.sendEmptyMessage(CLOSE_SOCKET);
                	Log.d(TAG, "disconnect......");
                }
            }
        });
    	Button_ring = (Button) findViewById(R.id.Button_ring);
    	Button_ring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	sendSocketMessage("speaker action");
            	sendToHandle(SERIESACTION, "magic m", 0);
            	sendToHandle(SERIESACTION, "magic ok", 500);
            }
        });
    	Button_manual_answer = (Button) findViewById(R.id.Button_manual_answer);
    	Button_manual_answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	sendSocketMessage("magic ok");
            }
        });
    	Button_auto_answer = (Button) findViewById(R.id.Button_auto_answer);
    	Button_auto_answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	/*Button_connect.setEnabled(false);
            	Button_ring.setEnabled(false);
            	Button_manual_answer.setEnabled(false);
            	Button_auto_answer.setEnabled(false);*/
            	sendToHandle(SERIESACTION, "magic m", 0);
            	for (int i = 0; i < 5; i++) {
            		sendToHandle(SERIESACTION, "magic left", 500 + i * 500);//2500
            	}
            	sendToHandle(SERIESACTION, "magic right", 3000);
            	sendToHandle(SERIESACTION, "magic ok", 2000);//打开小鹰直播
            }
        });
    	Button_tv_power = (Button) findViewById(R.id.button_tv_power);
    	Button_tv_power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	sendSocketMessage("tv power");
            }
        });
    	Button_magic_power = (Button) findViewById(R.id.button_magic_power);
    	Button_magic_power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	sendSocketMessage("magic power");
            }
        });
    	Button_left = (Button) findViewById(R.id.Button_left);
    	Button_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	sendSocketMessage("camera left");
            }
        });
    	Button_reset = (Button) findViewById(R.id.Button_reset);
    	Button_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	sendSocketMessage("camera reset");
            }
        });
    	Button_right = (Button) findViewById(R.id.Button_right);
    	Button_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	sendSocketMessage("camera right");
            }
        });
    }
    
    private void sendToHandle(int type, String mess, int delay) {
    	Message message = new Message();
    	message.what = type;
    	message.obj = mess;
    	mHandler.sendMessageDelayed(message, delay);
    }
    
    private void connectServerWithTCPSocket() {   
    	Log.i(TAG, "connect");
    	String host = EditText_ip.getText().toString();
    	int port = Integer.valueOf(EditText_port.getText().toString());
    	//host = "192.168.1.88";
    	//port = 19900;
    	Log.i(TAG, "new socket host = " + host + ", port = " + port);
    	initClientSocket();
    }
    
    private void initClientSocket()
    {
       //开启子线程
        new Thread(new Runnable() {
          
          @Override
          public void run() {
              try {
            	  //closeSocket();
            	  String host = EditText_ip.getText().toString();
              	  int port = Integer.valueOf(EditText_port.getText().toString());
              	  mSocket = new Socket();
              	  mSocket.connect(new InetSocketAddress(host, port), 5000);
              	  isConnect = mSocket.isConnected() && !mSocket.isClosed();
              	  isRun = true;
              	  mRead = new BufferedReader(new InputStreamReader(
              			mSocket.getInputStream()));
              	  mSend = new BufferedWriter(new OutputStreamWriter(  
              			mSocket.getOutputStream()));  
              			/*new PrintWriter(new BufferedWriter(new OutputStreamWriter(
              			mSocket.getOutputStream())), true);*/
              	  Log.i(TAG, "new socket creat success isConnect = " + isConnect);
            	  sendSocketMessage("client online");
              	  String line = "";
              	  char[] temp = new char[4096];
              	  int len = 0;
              	  while (isRun) {
              		  try {   
              			while ((len = mRead.read(temp)) > 0) {
              				line = new String(temp,0,len);
                            Log.i(TAG, "getdata = " + line + ", len = " + len);
                            if (line.startsWith("magic online") || line.startsWith("result magic online")) {
                            	isonline = true;//design to update per 2 seconds
                            	onlinetimecount++;
                            }
                        }
              			//Log.i(TAG, "read socket....");
              		  } catch (Exception e) {
                        Log.i(TAG, "receive erro = " + e.getMessage());
                        e.printStackTrace();
              		  }
              	  }
              	Log.i(TAG, "read socket end");
              } catch (UnknownHostException e) {
                    Log.i(TAG, "connect UnknownHostException: " + e.getMessage());
              } catch (IOException e) {
            	  Log.i(TAG, "connect IOException: " + e.getMessage());
              }
          }
      }).start();
    }

    
    private void sendSocketMessage(final String mess) {
    	new Thread(new Runnable() {
            @Override
            public void run() {
            	if (mSocket != null && isConnect && !TextUtils.isEmpty(mess)) {
                	try {   
                    	/*BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(  
                    			mSocket.getOutputStream()));*/
                    	mSend.write(mess);  
                    	mSend.flush();  
                    } catch (UnknownHostException e) {  
                        e.printStackTrace();  
                        Log.i(TAG, "sendSocketMessage UnknownHostException = " + e.getMessage());
                    } catch (IOException e) {  
                        e.printStackTrace();  
                        Log.i(TAG, "sendSocketMessage UnknownHostException = " + e.getMessage());
                    }
            	} else {
            		//initClientSocket();
            	}
            }
        }).start();
    }
    
    private void closeSocket() {
    	new Thread(new Runnable() {
            
            @Override
            public void run() {
            	if (mSocket != null) {
            		Log.i(TAG, "closeSocket");
            		sendSocketMessage("client close");
            		isRun = false;
            		try {
        				mSocket.close();
        				mRead.close();
            			mSend.close();
        				mSocket = null;
        				isConnect = false;
        				Log.i(TAG, "closeSocket end");
        			} catch (IOException e) {
        				e.printStackTrace();
        				Log.i(TAG, "closeSocket IOException = " + e.getMessage());
        			}
            	}
            }
        }).start();
    }
    
    private void acquireWakeLock() {
    	if (mWakeLock == null && mPowerManager != null) {
    		mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "remote");
    	}
    	if (mWakeLock != null) {
    		mWakeLock.acquire();
    	}
    }
    
    private void releaseWakeLock() {
    	if (mWakeLock != null) {
    		mWakeLock.release();
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    protected void onStart(){ 
        super.onStart(); 
        Log.i(TAG, "onStart"); 
      } 
      protected void onRestart(){ 
        super.onRestart(); 
        Log.i(TAG, "onReatart"); 
      } 
      protected void onResume(){ 
        super.onResume();
        acquireWakeLock();
        //mHandler.sendEmptyMessage(START_SOCKET);
        Log.i(TAG, "onResume"); 
      } 
      protected void onPause(){ 
        super.onPause();
        releaseWakeLock();
        Log.i(TAG, "onPause"); 
        if(isFinishing()){ 
          Log.w(TAG, "will be destroyed!"); 
        }else{ 
          Log.w(TAG, "just pausing!"); 
        } 
      } 
      protected void onStop(){ 
        super.onStop(); 
        Log.i(TAG, "onStop"); 
      } 
      protected void onDestroy(){ 
        super.onDestroy();
        mHandler.sendEmptyMessage(CLOSE_SOCKET);
        mHandler.removeCallbacksAndMessages(null);
        Log.i(TAG, "onDestroy"); 
      } 
}


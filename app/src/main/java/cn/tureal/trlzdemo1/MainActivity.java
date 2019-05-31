package cn.tureal.trlzdemo1;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int LOCATION_PERMISSION_CODE = 3;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;


    private BroadcastReceiver mBtBleUartReceiver;
    public int mBtBleConnectionState = STATE_DISCONNECTED;
    private BtBleUartService mBtBleUartService = null;
    private BluetoothDevice mBtBleDevice = null;
    private BluetoothAdapter mBtBleAdapter = null;
    private ServiceConnection mBtBleUartServiceConnection = null;

    private Menu mMenuMain;
    private TextView mTextViewLog;
    private TextView mTextViewStatus;
    private TRZTMatchView mTRZTMatchView;

    private List<TRPenObj> mPenListInput;
    public int[] mPenInputData;
    private Bitmap mBGBitmap;
    private TRPenObj mNewPenObj;
    private Rect mPaperRect = new Rect(2500, 600, 26500, 29600);

    public byte[] m_ZiTieStream;
    public int[] m_nStrokeScore;

    public int m_nTurnCenterX = 0;
    public int m_nTurnCenterY = 0;
    public int m_nOffsetX = 0;
    public int m_nOffsetY = 0;
    public float m_fScale = 0.001f;
    public float m_fTurnAngle = 0.0f;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private void updateConnectStateView() {
        String strState = getString(R.string.state_disconnected);
        switch (mBtBleConnectionState) {
            case STATE_CONNECTED:
                strState = getString(R.string.state_connected);
                break;
            case STATE_CONNECTING:
                strState = getString(R.string.state_connecting);
                break;
            case STATE_DISCONNECTED:
                strState = getString(R.string.state_disconnected);
                break;
        }
        mTextViewStatus.setText(strState);
    }

    private void AppendLogText(String strLog) {
        if (mTextViewLog != null && strLog != null) {
            mTextViewLog.append(strLog);
            mTextViewLog.append("\n");
            int offset = mTextViewLog.getLineCount() * mTextViewLog.getLineHeight();
            if (offset > mTextViewLog.getHeight()) {
                mTextViewLog.scrollTo(0, offset - mTextViewLog.getHeight());
            }
        }
    }

    private static int mBtBleRecvDataCount = 0;
    private static byte[] mBtBleRecvData = new byte[8];

    private void ProcessBtBleData(byte[] data) {
        Log.d(TAG,"ProcessBtBleData: " + Arrays.toString(data));
        for (int i = 0; i < data.length; i++) {
            mBtBleRecvData[mBtBleRecvDataCount] = data[i];
            switch (mBtBleRecvDataCount) {
                case 0:
                    if (mBtBleRecvData[mBtBleRecvDataCount] != 0x00) {
                        mBtBleRecvDataCount = 0;
                    } else {
                        mBtBleRecvDataCount++;
                    }
                    break;
                case 1:
                    if ((mBtBleRecvData[mBtBleRecvDataCount] & 0xff) != 0x5a) {
                        mBtBleRecvDataCount = 0;
                    } else {
                        mBtBleRecvDataCount++;
                    }
                    break;
                case 7:
                    if ((mBtBleRecvData[mBtBleRecvDataCount] & 0xff) != 0xa5) {
                        mBtBleRecvDataCount = 0;
                    } else {
                        mBtBleRecvDataCount++;

                        final int x = (mBtBleRecvData[3] & 0xff) + 256 * (mBtBleRecvData[4] & 0xff);
                        final int y = (mBtBleRecvData[5] & 0xff) + 256 * (mBtBleRecvData[6] & 0xff);
                        if (mBtBleRecvData[2] == 0) { //up
                            AddPenData(0, x, y);
                        } else {
                            AddPenData(1, x, y);
                        }

                        mBtBleRecvDataCount = 0;
                    }
                    break;
                default:
                    mBtBleRecvDataCount++;
                    break;
            }
        }
    }

    private int mMatchStrokeCount = 0;

    private void AddPenData(int type, int posX, int posY) {
        if (type > 0) { // pen tip down
            if (mNewPenObj == null) {
                mNewPenObj = new TRPenObj(getResources().getColor(R.color.colorWBFG), 8);
            }
            if (mNewPenObj != null) {
                if (posX >= mPaperRect.left && posX < mPaperRect.right && posY >= mPaperRect.top && posY < mPaperRect.bottom) {
                    mNewPenObj.addPenPoint(posX - mPaperRect.left, posY - mPaperRect.right);
                } else {
                    if (mNewPenObj.mPenPointList.size() > 1) {
                        mPenListInput.add(mNewPenObj);
                        mNewPenObj = null;
                    }
                }
            }
        } else {
            if (mNewPenObj != null && mNewPenObj.mPenPointList.size() > 1) {
                mPenListInput.add(mNewPenObj);
                mNewPenObj = null;

                SavePenInput();

                if (mPenInputData != null && m_ZiTieStream != null && mPenInputData.length > 0 && m_ZiTieStream.length > 0) {
                    m_nStrokeScore = new int[mPenListInput.size() + 1];
                    int code = Test2(m_ZiTieStream, mPenInputData, m_nStrokeScore);
                    Log.e(TAG, "AddPenData:code= " + code);
                    if (code == 1) {
                        mMatchStrokeCount = mPenListInput.size();
                        CalcPenStroke();
                        PrintScore();
                    } else {
                        CalcPenStroke();
                        AppendLogText("字没写好，请重新书写\r\n");
                    }
                }
            }
        }
    }

    private void SavePenInput() {
        mPenInputData = null;
        if (mPenListInput != null && mPenListInput.size() > 0) {
            int nLen = 1;
            for (TRPenObj pen : mPenListInput) {
                nLen += 1;
                nLen += 2 * pen.mPenPointList.size();
            }

            mPenInputData = new int[nLen];
            if (mPenInputData != null) {
                int nIndex = 0;
                mPenInputData[nIndex++] = mPenListInput.size();
                for (TRPenObj pen : mPenListInput) {
                    mPenInputData[nIndex++] = pen.mPenPointList.size();
                    for (Point pt : pen.mPenPointList) {
                        mPenInputData[nIndex++] = pt.x;
                        mPenInputData[nIndex++] = pt.y;
                    }
                }
            }
        }
    }

    private void CalcPenStroke() {
        mTRZTMatchView.ClearWB();
        double fCosA = Math.cos((double) m_fTurnAngle);
        double fSinA = Math.sin((double) m_fTurnAngle);
        int nIndex = 0;
        for (TRPenObj pen1 : mPenListInput) {
            int nColor = getResources().getColor(R.color.colorWBFG);
            if (nIndex < mMatchStrokeCount) {
                nColor = getResources().getColor(R.color.colorMatched);
            } else if (nIndex == mMatchStrokeCount) {
                nColor = getResources().getColor(R.color.colorError);
            } else {
                nColor = getResources().getColor(R.color.colorWrite);
            }
            nIndex++;

            TRPenObj pen2 = new TRPenObj(nColor, 5);
            if (pen2 != null) {
                for (Point pt : pen1.mPenPointList) {
                    double x = (pt.x - m_nTurnCenterX) * fCosA - (pt.y - m_nTurnCenterY) * fSinA + m_nTurnCenterX;
                    double y = (pt.x - m_nTurnCenterX) * fSinA + (pt.y - m_nTurnCenterY) * fCosA + m_nTurnCenterY;

                    int xnew = (int) ((x * m_fScale + m_nOffsetX) * 225.0 / 416.0);
                    int ynew = (int) ((y * m_fScale + m_nOffsetY) * 225.0 / 416.0);

                    pen2.addPenPoint(xnew, ynew);
                }
                mTRZTMatchView.AddPenObj(pen2);
            }
        }
    }

    protected void PrintScore() {
        if (m_nStrokeScore == null || m_nStrokeScore.length < 2) {
            return;
        }

        mTextViewLog.setText("");

        String strText = new String();
        for (int i = 0; i < m_nStrokeScore.length - 1; i++) {
            AppendLogText(String.format("第%d笔得分=%d分", i + 1, m_nStrokeScore[i]));
        }

        AppendLogText(String.format("整字得分=%d分\r\n", m_nStrokeScore[m_nStrokeScore.length - 1]));
    }

    private void initBTUart() {
        mBtBleUartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(BtBleUartService.ACTION_DATA_AVAILABLE)) {
                    final byte[] txValue = intent.getByteArrayExtra(BtBleUartService.EXTRA_DATA);
                    if (txValue.length > 0) {
                        ProcessBtBleData(txValue);
                    }
                } else if (action.equals(BtBleUartService.ACTION_GATT_CONNECTED)) {
                    mBtBleConnectionState = STATE_CONNECTED;
                    updateConnectStateView();
                } else if (action.equals(BtBleUartService.ACTION_GATT_DISCONNECTED)) {
                    mBtBleConnectionState = STATE_DISCONNECTED;
                    updateConnectStateView();
                } else if (action.equals(BtBleUartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                    mBtBleUartService.enableTXNotification();
                } else if (action.equals(BtBleUartService.ACTION_LOG_AVAILABLE)) {
                    String msg = intent.getStringExtra(BtBleUartService.EXTRA_DATA);
                    AppendLogText(msg);
                }
            }
        };

        IntentFilter filterBtBleUart = new IntentFilter();
        filterBtBleUart.addAction(BtBleUartService.ACTION_GATT_CONNECTED);
        filterBtBleUart.addAction(BtBleUartService.ACTION_GATT_DISCONNECTED);
        filterBtBleUart.addAction(BtBleUartService.ACTION_GATT_SERVICES_DISCOVERED);
        filterBtBleUart.addAction(BtBleUartService.ACTION_DATA_AVAILABLE);
        filterBtBleUart.addAction(BtBleUartService.ACTION_LOG_AVAILABLE);
        filterBtBleUart.addAction(BtBleUartService.DEVICE_DOES_NOT_SUPPORT_UART);
        registerReceiver(mBtBleUartReceiver, filterBtBleUart);

        mBtBleUartServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder rawBinder) {
                mBtBleUartService = ((BtBleUartService.LocalBinder) rawBinder).getService();
                AppendLogText("onServiceConnected mService= " + mBtBleUartService);
                if (!mBtBleUartService.initialize()) {
                    AppendLogText("Unable to initialize Uart Bluetooth");
                    mBtBleUartService = null;
                    return;
                }
            }

            public void onServiceDisconnected(ComponentName classname) {
                mBtBleUartService = null;
            }
        };

        Intent serviceUart = new Intent(this, BtBleUartService.class);
        bindService(serviceUart, mBtBleUartServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void initZTMatch() {
        mPenListInput = new ArrayList<TRPenObj>();
        mBGBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.zt0001);
        mTRZTMatchView.SetBG(mBGBitmap);

        try {
            InputStream in = getResources().openRawResource(R.raw.zt0001);
            int length = in.available();
            m_ZiTieStream = new byte[length];
            in.read(m_ZiTieStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.title_clear, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_clear, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mTextViewLog.setText("");
                                mTRZTMatchView.ClearWB();
                                mNewPenObj = null;
                                mPenListInput.clear();
                                mPenInputData = null;
                                mMatchStrokeCount = 0;
                            }
                        }).show();
            }
        });

        mTextViewStatus = (TextView) findViewById(R.id.textViewStatus);
        mTextViewLog = (TextView) findViewById(R.id.textViewLog);
        mTextViewLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTRZTMatchView = (TRZTMatchView) findViewById(R.id.canvasWB);

        initZTMatch();
        initBTUart();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenuMain = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_connect) {
            OnBtnConnectClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu == mMenuMain) {
            MenuItem item = menu.findItem(R.id.action_connect);
            if (item != null) {
                if (mBtBleConnectionState == STATE_CONNECTED) {
                    item.setTitle(R.string.action_disconnect);
                } else {
                    item.setTitle(R.string.action_connect);
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    private void OnBtnConnectClick() {
        mBtBleAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtBleAdapter == null) {
            AppendLogText("Bluetooth is not available");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                AppendLogText("request location permission");
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
                return;
            }
        }

        if (!mBtBleAdapter.isEnabled()) {
            Log.i(TAG, "onClick - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mBtBleConnectionState == STATE_DISCONNECTED) {
                Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
            } else {
                if (mBtBleUartService != null) {
                    mBtBleUartService.disconnect();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean grantedLocation = true;
        if (requestCode == LOCATION_PERMISSION_CODE) {
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    grantedLocation = false;
                }
            }
        }

        if (!grantedLocation) {
            Toast.makeText(this, "Permission error !!!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mBtBleDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    if (mBtBleDevice != null) {
                        if (mBtBleDevice.getName().toUpperCase().contains("UART")) {
                            if (mBtBleUartService != null) {
                                AppendLogText("... onActivityResultdevice.address==" + mBtBleDevice + "mserviceValue" + mBtBleUartService);
                                AppendLogText(mBtBleDevice.getName() + " - connecting");
                                mBtBleUartService.connect(mBtBleDevice.getAddress());
                            }
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(mBtBleUartReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

        if (mBtBleUartServiceConnection != null) {
            unbindService(mBtBleUartServiceConnection);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native int Test2(byte[] ZiTieStream, int[] UserStrokeStream, int[] StrokeScore);
}

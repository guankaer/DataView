package ecgthread.dataview;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;





public class Bluetooth {

    private static Handler handler = new Handler();
    private static Context btContext;
    private static ImageView btState;

    private static final UUID btUUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static BluetoothAdapter btAdapter;
    private static BluetoothSocket btSocket;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static boolean Connecting;

    private static long receiveData;
    public static ArrayList<BluetoothDevice> listDevice = new ArrayList<BluetoothDevice>();

    public static boolean isConn;
    public static byte[] byteData;
    public static int flag = 0;
    public static void Init(Context context, ImageView imageView) {
        btContext = context;
        btState = imageView;
//		btState.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				if(flag==0){
//					btSearch();
//				}else if(flag==1){
//					btConnAbort();
//				}
//			}
//		});
        isConn = false;
        Connecting = false;
        btSearch();
    }

    public static boolean state(){
        if(!btAdapter.isEnabled())
            return false;
        return true;
    }


    public static void btSearch() {
        try {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!btAdapter.isEnabled()) {
                Intent mIntentOpenBT = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                btContext.startActivity(mIntentOpenBT);
            }
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            btContext.registerReceiver(btReceiver, filter);
            btAdapter.startDiscovery();

//			Toast.makeText(btContext.getApplicationContext(), R.string.bt_search, Toast.LENGTH_SHORT).show();
            //??撠?銝?
            handler.postDelayed(btNoConn, 20000);
        } catch (Exception e) {
        }
    }

    private static BroadcastReceiver btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // 搜索到的不是已经绑定的蓝牙设备
                    if (device.getName().contains("Dual-SPP")) {
                        // 显示在TextView上
                        listDevice.add(device);
                    }
                    System.out.println("Bluetooth：" + listDevice.size());
//					if (device != null) {
//						String deviceName = device.getName();
//						if (deviceName != null && deviceName.length() > 0) {
//							if (deviceName.indexOf("Dual-SPP") >= 0)
//								btConnect(device);
//						}
//					}
                }
            } catch (Exception e) {
            }
        }
    };

    public static void btConnect(final BluetoothDevice device) {
        if (Connecting)
            return;
        Connecting = true;
        handler.removeCallbacks(btNoConn);
        btState.setImageResource(R.drawable.blooth_out);
//		Toast.makeText(btContext.getApplicationContext(), R.string.bt_connect, Toast.LENGTH_SHORT).show();
        //??蝺?銝?
        new Thread() {
            String Error = "";

            public void run() {
                try {
                    btContext.unregisterReceiver(btReceiver);
                    btAdapter.cancelDiscovery();
                    btSocket = device
                            .createInsecureRfcommSocketToServiceRecord(btUUID);
                    btSocket.connect();
                    inputStream = btSocket.getInputStream();
                    outputStream = btSocket.getOutputStream();
                } catch (IOException e) {
                    Error = e.toString();
                }
                handler.post(new Runnable() {
                    public void run() {
                        if (Error.length() > 0)
                            btConnFail();
                        else
                            btConnSuccess(device);
//						Toast.makeText(btContext, device.getAddress(),
//								Toast.LENGTH_SHORT).show();
                        Connecting = false;
                    }
                });
            }
        }.start();
    }


    private static void btConnSuccess(BluetoothDevice device) {
        isConn = true;
        btState.setImageResource(R.drawable.blooth_on);
        //???賡??蝺?
        Toast.makeText(btContext.getApplicationContext(), R.string.bt_success, Toast.LENGTH_SHORT).show();
        flag = 1;
//		btState.setClickable(false);
        receiveData = System.currentTimeMillis();
        // btCount = 0;
        handler.postDelayed(btConnCheck, 1000);
    }


    private static void btConnFail() {
        isConn = false;
        btState.setImageResource(R.drawable.blooth_out);
        Toast.makeText(btContext.getApplicationContext(), R.string.bt_fail, Toast.LENGTH_SHORT).show();
        //???賡??蝺?憭望??
        btReConn();
    }


    private static Runnable btConnCheck = new Runnable() {
        public void run() {
            Bluetooth.getData();
            long idleTime = (System.currentTimeMillis() - receiveData) / 1000;
            if (idleTime > 0.1)
                btConnAbort();
            if (isConn)
                handler.postDelayed(this, 1000);
        }
    };


    private static Runnable btNoConn = new Runnable() {
        public void run() {
            try {
                if (isConn || Connecting)
                    return;
                btState.setImageResource(R.drawable.blooth_out);
                Toast.makeText(btContext.getApplicationContext(), R.string.bt_nodevice, Toast.LENGTH_SHORT).show();
                //?曆??啗?蝵?

                btReConn();
                btContext.unregisterReceiver(btReceiver);
                btAdapter.cancelDiscovery();
            } catch (Exception e) {
            }
        }
    };


    public static void btConnAbort() {
        isConn = false;
        btState.setImageResource(R.drawable.blooth_out);
        Toast.makeText(btContext.getApplicationContext(), R.string.bt_abort, Toast.LENGTH_SHORT).show();
        btReConn();
        try {
            inputStream.close();
            outputStream.close();
            btSocket.close();
            inputStream = null;
            outputStream = null;
            btSocket = null;
            byteData = null;
        } catch (Exception e) {
        }
    }

    private static void btReConn() {
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {

                btState.setImageResource(R.drawable.blooth_out);

                btState.setClickable(true);
                flag = 0;
            }
        }, 3000);
    }

    public static int getData() {
        int bytes = 0;
        try {
            if (inputStream != null) {
                if (inputStream.available() > 0) {
                    byteData = new byte[inputStream.available()];
//					System.out.println(byteData.length);
                    bytes = inputStream.read(byteData);

                    receiveData = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
        }
        return bytes;
    }

    public static void sendData(Context context, int data) {
        try {
            if (outputStream != null) {
                outputStream.write(data);
                outputStream.flush();
            }
        } catch (Exception e) {
        }
    }

    public static void onDestroy() {
        handler.removeCallbacks(btNoConn);
        try {
            inputStream.close();
            outputStream.close();
            btSocket.close();
            inputStream = null;
            outputStream = null;
            btSocket = null;
            byteData = null;
        } catch (Exception e) {
        }
    }

    // From 2's complement Decimal to Unsigned Decimal
    public static int From2ComplementtoUnsigned(int data) {

        String binary = Integer.toBinaryString(data); // Decimal to Binary
        if (binary.length() > 8)
            binary = binary.substring(binary.length() - 8); // Binary to 8
        // digit.
        return Integer.parseInt(binary, 2);// Binary to Decimal.
    }

}

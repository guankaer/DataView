package ecgthread.dataview;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import static ecgthread.dataview.Bluetooth.btConnAbort;
import static ecgthread.dataview.Bluetooth.btConnect;
import static ecgthread.dataview.Bluetooth.btSearch;
public class MainActivity extends AppCompatActivity {

    private Handler handler = new Handler();

    private int sequence = 0;
    private static byte header = -91;
    private int sum_1 = 0;
    private int sum_2 = 0;
    private int sum_3 = 0;
    private int sum_4 = 0;
    private int sum_5 = 0;
    private int sum_6 = 0;
    private int count = 0;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
    private String currentDateandTime;

    private CharSequence adapteritems[] = {
            "Dual-SPP:8C:DE:52:C9:3F:86",
            "Dual-SPP:34:81:F4:11:0D:A7",
//            "Dual-SPP:34:81:F4:11:0D:74 ",
    };

    private ImageView openBluetooth;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        content=(LinearLayout)findViewById(R.id.content);
        openBluetooth=(ImageView)findViewById(R.id.bluetooth);
        setOnClick(openBluetooth, adapteritems);
        Bluetooth.Init(this, (ImageView) findViewById(R.id.bluetooth));
        handler.post(btTimer);


    }

    byte[] BluetoothTemp;
    private Runnable btTimer = new Runnable() {
        public void run() {
            try {


                if (Bluetooth.byteData != null) {
                    BluetoothTemp = Bluetooth.byteData;

                    if (BluetoothTemp.length % 18 == 0 && BluetoothTemp.length != 0) {
                        for (int j = 0; j < BluetoothTemp.length / 18; j++) {
                            int tempSequence = Bluetooth.From2ComplementtoUnsigned(BluetoothTemp[j * 18 + 2]) +
                                    Bluetooth.From2ComplementtoUnsigned(BluetoothTemp[j * 18 + 3]) * 256;
                            if (BluetoothTemp[j * 18] == header && BluetoothTemp[j * 18 + 1] == header &&
                                    (tempSequence > sequence || sequence - tempSequence == 999)) {

                                for (int k = 0; k < 2; k++) {
                                    combineLowHigh(BluetoothTemp, j, k);
                                    if (++count >= 2) {

                                        currentDateandTime = sdf.format(new Date());
                                        TextView textView=new TextView(MainActivity.this);
                                        textView.setText(currentDateandTime + " \t " + (sequence) + " \t " + sum_1 + " \t " + sum_2 + " \t " + sum_3 + " \t " + sum_4 + " \t " + sum_5 + " \t " + sum_6);
                                        textView.setTextColor(Color.BLACK);
                                        content.addView(textView);

                                        count = 0;
                                        sum_1 = 0;
                                        sum_2 = 0;
                                        sum_3 = 0;
                                        sum_4 = 0;
                                        sum_5 = 0;
                                        sum_6 = 0;
                                    }
                                }
                                sequence = tempSequence;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, e.toString(),
                        Toast.LENGTH_LONG).show();
            }

            handler.post(this);
        }
    };

    int value_1 = 0;
    int value_2 = 0;
    int value_3 = 0;
    int value_4 = 0;
    int value_5 = 0;
    int value_6 = 0;
    private void combineLowHigh(byte[] temp, int j, int k) {

        value_1 = Bluetooth
                    .From2ComplementtoUnsigned(temp[j * 18 + k + 4]);
        value_2 = Bluetooth
                    .From2ComplementtoUnsigned(temp[j * 18 + k + 6]);
        value_3 = Bluetooth
                    .From2ComplementtoUnsigned(temp[j * 18 + k + 8]);

        value_4 = (temp[j * 18 + k + 10]);
        value_5 = (temp[j * 18 + k + 12]);
        value_6 = (temp[j * 18 + k + 14]);


        sum_1 = sum_1 + (value_1 << ((count % 2) * 8));
        sum_2 = sum_2 + (value_2 << ((count % 2) * 8));
        sum_3 = sum_3 + (value_3 << ((count % 2) * 8));
        sum_4 = sum_4 + (value_1 << ((count % 2) * 8));
        sum_5 = sum_5 + (value_2 << ((count % 2) * 8));
        sum_6 = sum_6 + (value_3 << ((count % 2) * 8));
    }

    private void setOnClick(final ImageView btn, final CharSequence[] str) {
        btn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Bluetooth.flag == 0) {
                    btSearch();
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    AlertDialog alertDialog = builder.setTitle(R.string.bluetoothChoose).
                            setIcon(R.drawable.bluetooth_mini).create();
                    Window window = alertDialog.getWindow();
                    window.setGravity(Gravity.CENTER);

                    builder.setItems(str, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            Toast.makeText(getApplicationContext(), str[item], Toast.LENGTH_SHORT).show();

                            for (BluetoothDevice btDevice : Bluetooth.listDevice) {
                                String address = btDevice.getAddress();
                                if (str[item].toString().contains(address)) {
                                    btConnect(btDevice);

                                }
                            }
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else if (Bluetooth.flag == 1) {
                    btConnAbort();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        try {
            handler.removeCallbacks(btTimer);
            Bluetooth.onDestroy();
        } catch (Exception e) {
        }
        super.onDestroy();
    }

}

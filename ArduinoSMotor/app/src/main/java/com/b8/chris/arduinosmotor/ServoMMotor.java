package com.b8.chris.arduinosmotor;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
public class ServoMMotor extends AppCompatActivity
{
    Button izq,drc,adelante,atras, desconectar;
    CheckBox cAutomatica;
    TextView conectividad;
    Handler bluetoothIn;
    final int handlerstate=0;
    private BluetoothAdapter btAdapter=null;
    private BluetoothSocket btSocket=null;
    private StringBuilder dataStringIN=new StringBuilder();
    private ConnectedThread myConexionBT;
    private static final UUID BTMODULEUUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address=null;
    @SuppressLint({"HandlerLeak", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servo_mmotor);
        izq=(Button) findViewById(R.id.izq);
        drc=(Button) findViewById(R.id.drc);
        adelante=(Button) findViewById(R.id.adelante);
        atras=(Button) findViewById(R.id.atras);
        desconectar=(Button) findViewById(R.id.desconectar);
        conectividad=(TextView) findViewById(R.id.conectividad);
        cAutomatica=(CheckBox) findViewById(R.id.cAutomatica);

        bluetoothIn=new Handler() {
            public void handleMessage(android.os.Message msg) {
                if(msg.what==handlerstate)
                {
                    String readMessage = (String) msg.obj;
                    dataStringIN.append(readMessage);
                    int endOflineIndex=dataStringIN.indexOf("#");
                    if(endOflineIndex>0)
                    {
                        String dataInPrint=dataStringIN.substring(0,endOflineIndex);
                        conectividad.setText("CONECTIVIDAD : "+dataInPrint);
                        dataStringIN.delete(0, dataStringIN.length());
                    }
                }
            }
        };
        btAdapter=BluetoothAdapter.getDefaultAdapter();
        VerificarEstadoBT();

        izq.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        myConexionBT.write("1");
                        break;
                    case MotionEvent.ACTION_UP:
                        myConexionBT.write("0");
                    default:
                        break;
                }
                return false;
            }
        });

        drc.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        myConexionBT.write("2");
                        break;
                    case MotionEvent.ACTION_UP:
                        myConexionBT.write("0");
                    default:
                        break;
                }
                return false;
            }
        });

        adelante.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        myConexionBT.write("e");
                        break;
                    case MotionEvent.ACTION_UP:
                        myConexionBT.write("d");
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        atras.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        myConexionBT.write("a");
                        break;
                    case MotionEvent.ACTION_UP:
                        myConexionBT.write("d");
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        desconectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if(btSocket!=null)
                {
                    try{
                        btSocket.close();
                    }catch (IOException e){
                        Toast.makeText(getBaseContext(),"ERROR",Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        });

        cAutomatica.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if(cAutomatica.isSelected()){
                    myConexionBT.write("4");
                }
                else{
                    myConexionBT.write("5");
                }
            }
        });
    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException{
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private void VerificarEstadoBT()
    {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter==null)
            Toast.makeText(getBaseContext(),"El dispositivo no soporta BT",Toast.LENGTH_SHORT).show();
        else
        if (btAdapter.isEnabled()) {
        }
        else
        {
            Intent enableBtIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,1);
        }
    }
    public void onResume()
    {
        super.onResume();
        Intent i=getIntent();
        address=i.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device =btAdapter.getRemoteDevice(address);
        try
        {
            btSocket=createBluetoothSocket(device);

        }catch (IOException e)
        {
            Toast.makeText(getBaseContext(),"CREACION DEL SOCKET FALLO", Toast.LENGTH_SHORT).show();
        }
        try
        {
            btSocket.connect();
        }catch (IOException e)
        {
            try
            {
                btSocket.close();
            }catch (IOException e1){}
        }
        myConexionBT=new ConnectedThread(btSocket);
        myConexionBT.start();


    }
    public void onPause()
    {
        super.onPause();
        try
        {
            btSocket.close();
        }catch (IOException e){}
    }

    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn=null;
            OutputStream tmpOut=null;
            try
            {
                tmpIn=socket.getInputStream();
                tmpOut=socket.getOutputStream();
            }catch (IOException e){}
            mmInStream=tmpIn;
            mmOutStream=tmpOut;
        }
        public void run()
        {
            byte[] buffer=new byte[256];
            int bytes;
            while(true)
            {
                try
                {
                    bytes =mmInStream.read(buffer);
                    String readMessage = new String(buffer,0,bytes);
                    bluetoothIn.obtainMessage(handlerstate,bytes,-1,readMessage);
                }catch (IOException e){break;}
            }
        }
        public void write(String input)
        {
            try
            {
                mmOutStream.write(input.getBytes());
            }catch (IOException e){
                Toast.makeText(getBaseContext(),"La Conexion fallo",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}

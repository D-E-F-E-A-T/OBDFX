package OBDFX;

import com.jfoenix.controls.JFXRippler;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

public class dashController implements Initializable {
    @FXML
    public VBox bluetoothDevicesVBox;

    ArrayList<String> bDevicesMAC = new ArrayList<>();
    HashMap<String,String> bDevicesName = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startBluetooth();
    }

    Process proc;
    BufferedWriter stdOutput;
    boolean isStop = false;


    void log(String log)
    {
        System.out.println(log);
    }

    public void startBluetooth()
    {
        log("Init Bluetooth Thread ...");
        Task<Void> blueTask = new Task<Void>() {
            @Override
            protected Void call(){
                try
                {
                    System.out.println("XASDAS");
                    bDevicesMAC.clear();
                    Platform.runLater(()->{
                        bluetoothDevicesVBox.getChildren().clear();
                    });
                    Runtime rt = Runtime.getRuntime();
                    String[] commands = {"bluetoothctl"};
                    proc = rt.exec(commands);

                    BufferedReader stdInput = new BufferedReader(new
                            InputStreamReader(proc.getInputStream()));

                    stdOutput = new BufferedWriter(new
                            OutputStreamWriter(proc.getOutputStream()));

                    BufferedReader stdError = new BufferedReader(new
                            InputStreamReader(proc.getErrorStream()));

                    System.out.println("Here is the standard output of the command:\n");
                    String s = "";
                    while ((s = stdInput.readLine()) != null) {
                        if(s.contains("Agent registered"))
                        {
                            s = "";
                            System.out.println("XXX");
                            stdOutput.write("scan on\n");
                            stdOutput.flush();
                        }

                        if(s.contains("\u001B[0;94m[bluetooth]\u001B[0m#                         "))
                        {
                            s=s.replace("\u001B[0;94m[bluetooth]\u001B[0m#                         ","");
                        }

                        System.out.println("LINE : '"+s+"'");

                        if(s.contains("Device"))
                        {
                            String[] pass1 = s.split(" ");
                            if(pass1[1].equals("Device"))
                            {
                                if(!bDevicesMAC.contains(pass1[2]))
                                {
                                    bDevicesMAC.add(pass1[2]);
                                }

                                if(!pass1[3].equals("RSSI:") && !pass1[3].equals("TxPower:") && !pass1[3].equals("ManufacturerData"))
                                {
                                    String deviceName = "";
                                    for(int i = 3;i<pass1.length;i++)
                                    {
                                        deviceName+=pass1[i]+" ";
                                    }
                                    bDevicesName.put(pass1[2],deviceName);
                                }

                                refreshBluetoothDeviceList();
                            }
                            System.out.println("\n");
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                System.out.println("END!");
                Platform.runLater(()->{
                    bluetoothDevicesVBox.getChildren().clear();
                });
                return null;
            }
        };

        new Thread(blueTask).start();
    }

    @FXML
    public void stopBluetooth()
    {
        log("Stopping ...");
        try
        {
            stdOutput.write("exit\n");
            stdOutput.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    int j;
    public void refreshBluetoothDeviceList() throws Exception
    {
        for(j = 0;j<bDevicesMAC.size();j++)
        {
            boolean isFound = false;
            Thread.sleep(500);
            for(Node eachNode : bluetoothDevicesVBox.getChildren())
            {
                JFXRippler p = (JFXRippler) eachNode;
                HBox eachPane = (HBox) p.getChildren().get(0);
                if(eachPane.getId().equals(bDevicesMAC.get(j)))
                {
                    if(bDevicesName.get(bDevicesMAC.get(j)) != null)
                    {
                        ObservableList<Node> labels = eachPane.getChildren();
                        Label nameLabel = (Label) labels.get(1);
                        if(nameLabel.getText().equals(""))
                        {
                            String dName = bDevicesName.get(bDevicesMAC.get(j));
                            System.out.println("dName : '"+dName+"'");
                            Platform.runLater(()->nameLabel.setText(dName));
                        }
                    }
                    isFound = true;
                    break;
                }
            }

            if(!isFound)
            {
                Label macAddressLabel = new Label(bDevicesMAC.get(j));
                macAddressLabel.setFont(Font.font("Roboto Regular",18));
                macAddressLabel.setTextFill(Paint.valueOf("#ffffff"));
                Label deviceNameLabel = new Label("");
                deviceNameLabel.setFont(Font.font("Roboto Regular",18));
                deviceNameLabel.setTextFill(Paint.valueOf("#ffffff"));
                if(bDevicesName.get(bDevicesMAC.get(j)) != null)
                    deviceNameLabel.setText(bDevicesName.get(bDevicesMAC.get(j)));
                HBox newHbox = new HBox(macAddressLabel,deviceNameLabel);
                newHbox.setId(bDevicesMAC.get(j));
                newHbox.setPadding(new Insets(15,15,15,15));
                newHbox.setSpacing(25);
                JFXRippler rippler = new JFXRippler(newHbox);
                rippler.setCache(true);
                rippler.setCacheHint(CacheHint.SPEED);
                Platform.runLater(()-> {
                    bluetoothDevicesVBox.getChildren().add(rippler);
                });
            }
        }
    }
}

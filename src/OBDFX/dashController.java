package OBDFX;

import com.jfoenix.controls.*;
import com.jfoenix.controls.events.JFXDialogEvent;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.FocusModel;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
    @FXML
    public StackPane alertStackPane;
    @FXML
    public StackPane progressStackPane;

    ArrayList<String> bDevicesMAC = new ArrayList<>();
    HashMap<String,String> bDevicesName = new HashMap<>();
    private final Paint WHITE_PAINT = Paint.valueOf("#ffffff");

    Process proc;
    BufferedReader stdInput;
    BufferedWriter stdOutput;
    boolean isStop = false;
    boolean isBluetoothON = false;
    boolean goAheadBind = false;

    String macAddressToBeConnected = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startBluetooth();
    }

    public void connectToOB2Device(String macAddress)
    {
        new Thread(new Task<Void>() {
            @Override
            protected Void call(){
                try
                {
                    if(isBluetoothON)
                    {
                        Platform.runLater(()->{
                            bluetoothDevicesVBox.setDisable(true);
                            showProgress("Connecting to "+macAddress+" ...");
                        });

                        writeLineToStream("pair "+macAddress);
                        goAheadBind = true;
                        macAddressToBeConnected = macAddress;
                        writeLineToStream("trust "+macAddress);
                        writeLineToStream("scan off");
                        writeLineToStream("exit");

                        Platform.runLater(()->{
                            bluetoothDevicesVBox.setDisable(false);
                            hideProgress();
                        });
                    }
                    else
                    {
                        showErrorAlert("Uh Oh!","Bluetooth isnt ON!");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    showErrorAlert("Error!","Couldnt connect to "+macAddress+" successfully. Please check Stacktrace");
                }
                return null;
            }
        }).start();
    }

    void writeLineToStream(String line) throws Exception
    {
        stdOutput.write(line+"\n");
        stdOutput.flush();
        Thread.sleep(500);
    }

    public void showProgress(String text)
    {
        JFXDialogLayout l = new JFXDialogLayout();
        l.getStyleClass().add("dialog_style");
        Label textLabel = new Label(text);
        textLabel.setFont(Font.font("Roboto Regular",15));
        textLabel.setTextFill(WHITE_PAINT);
        textLabel.setWrapText(true);

        //TODO : Add Transparent Indeterminate Progress loading gif
        JFXRippler x = new JFXRippler();
        x.setCache(true);
        HBox content = new HBox(textLabel,x);
        l.setBody(content);

        progressDialog = new JFXDialog(progressStackPane,l, JFXDialog.DialogTransition.CENTER);
        progressDialog.setOverlayClose(false);
        progressDialog.getStyleClass().add("dialog_box");

        progressStackPane.toFront();
        progressDialog.show();
    }

    JFXDialog progressDialog;

    public void hideProgress()
    {
        progressDialog.close();
        progressDialog.setOnDialogClosed(new EventHandler<JFXDialogEvent>() {
            @Override
            public void handle(JFXDialogEvent event) {
                progressStackPane.toBack();
            }
        });
    }

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

                    stdInput = new BufferedReader(new
                            InputStreamReader(proc.getInputStream()));

                    stdOutput = new BufferedWriter(new
                            OutputStreamWriter(proc.getOutputStream()));

                    BufferedReader stdError = new BufferedReader(new
                            InputStreamReader(proc.getErrorStream()));

                    isBluetoothON = true;
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

                    stdOutput.close();
                    stdInput.close();

                    if(goAheadBind)
                    {
                        Runtime rt2 = Runtime.getRuntime();
                        String[] commands2 = {"sudo rfcomm bind rfcomm0 "+macAddressToBeConnected};
                        proc = rt2.exec(commands2);

                        Thread.sleep(1000);
                        Runtime rt3 = Runtime.getRuntime();
                        String[] commands3 = {"screen /dev/rfcomm0"};
                        proc = rt3.exec(commands3);

                        stdInput = new BufferedReader(new
                                InputStreamReader(proc.getInputStream()));

                        stdOutput = new BufferedWriter(new
                                OutputStreamWriter(proc.getOutputStream()));

                        while ((s = stdInput.readLine()) != null) {
                            
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    showErrorAlert("Uh Oh!","Unable to Start Bluetooth. Check StackTrace!");
                    goAheadBind = false;
                }
                System.out.println("END!");
                isBluetoothON = false;
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
                JFXRippler r = (JFXRippler) eachNode;
                HBox eachPane = (HBox) r.getChildren().get(0);
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
                JFXRippler r = new JFXRippler(newHbox);
                Platform.runLater(()-> {
                    bluetoothDevicesVBox.getChildren().add(r);
                });

                r.setOnMouseClicked(event -> {
                    HBox inside = (HBox) r.getChildren().get(0);
                    connectToOB2Device(inside.getId());
                });

                r.setOnTouchReleased(event -> {
                    HBox inside = (HBox) r.getChildren().get(0);
                    connectToOB2Device(inside.getId());
                });
            }
        }
    }

    public void showErrorAlert(String heading, String content)
    {
        JFXDialogLayout l = new JFXDialogLayout();
        l.getStyleClass().add("dialog_style");
        Label headingLabel = new Label(heading);
        headingLabel.setTextFill(WHITE_PAINT);
        headingLabel.setFont(Font.font("Roboto Regular",25));
        l.setHeading(headingLabel);
        Label contentLabel = new Label(content);
        contentLabel.setFont(Font.font("Roboto Regular",15));
        contentLabel.setTextFill(WHITE_PAINT);
        contentLabel.setWrapText(true);
        l.setBody(contentLabel);
        JFXButton okButton = new JFXButton("OK");
        okButton.setTextFill(WHITE_PAINT);
        l.setActions(okButton);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                alertStackPane.getChildren().clear();
            }
        });
        JFXDialog alertDialog = new JFXDialog(alertStackPane,l, JFXDialog.DialogTransition.CENTER);
        alertDialog.setOverlayClose(false);
        alertDialog.getStyleClass().add("dialog_box");
        okButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                alertDialog.close();
                alertDialog.setOnDialogClosed(new EventHandler<JFXDialogEvent>() {
                    @Override
                    public void handle(JFXDialogEvent event) {
                        alertStackPane.toBack();
                    }
                });
            }
        });


        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                alertStackPane.toFront();
                alertDialog.show();
            }
        });

    }
}

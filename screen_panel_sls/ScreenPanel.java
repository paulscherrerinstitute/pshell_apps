import ch.psi.pshell.bs.StreamCamera;
import ch.psi.pshell.camserver.PipelineSource;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.epics.ChannelInteger;
import ch.psi.pshell.epics.DiscretePositioner;
import ch.psi.pshell.epics.BinaryPositioner;
import ch.psi.pshell.epics.ChannelDouble;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.imaging.Overlay;
import ch.psi.pshell.imaging.Overlays;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.ui.CamServerViewer;
import static ch.psi.pshell.ui.CamServerViewer.ARG_CAMERA;
import static ch.psi.pshell.ui.CamServerViewer.ARG_CAMERA_SERVER;
import static ch.psi.pshell.ui.CamServerViewer.ARG_CONSOLE;
import static ch.psi.pshell.ui.CamServerViewer.ARG_PIPELINE_SERVER;
import static ch.psi.pshell.ui.CamServerViewer.ARG_STREAM_LIST;
import static ch.psi.pshell.ui.CamServerViewer.ARG_TYPE;
import static ch.psi.pshell.ui.CamServerViewer.ARG_BUFFER_SIZE;
import ch.psi.pshell.ui.Panel;
import ch.psi.utils.Elog;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 *
 */
public class ScreenPanel extends Panel implements CamServerViewer.CamServerViewerListener{
    
    public static final String BTR_TYPE = "BTR";
    public static final String BOOSTER_TYPE = "Booster";
    public static final String FRONTEND_TYPE = "Frontend";
    public static final String LTB_TYPE = "LTB";
    public static final String LINAC_TYPE = "Linac";
    public static final String RF_TYPE = "RF";
    public static final String RING_TYPE = "Ring";
    public static final String SIMULATION_TYPE = "Simulation";
    
    DiscretePositioner screen;
    ChannelDouble exposure;
    ChannelDouble flStep;
    //DiscretePositioner mirror;
    BinaryPositioner mirror;
    BinaryPositioner ledPower;

    final Logger logger;
    
    

    public ScreenPanel() {
        logger = Logger.getLogger(getClass().getName());
        initComponents();        
        panelScreen.setVisible(false);
        panelControls.setVisible(false);
        camServerViewer.setListener(this);
        this.remove(customPanel);
        camServerViewer.getCustomPanel().add(customPanel);
        camServerViewer.setSidePanelVisible(true);
        
        camServerViewer.setPersistenceFile(Paths.get(getContext().getSetup().getContextPath(), "ScreenPanel.bin"));

        if (App.hasArgument(ARG_BUFFER_SIZE)) {
            try {
                camServerViewer.setBufferLength(Integer.valueOf(App.getArgumentValue(ARG_BUFFER_SIZE)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        camServerViewer.setTypeList(App.hasArgument(ARG_TYPE) ? List.of(App.getArgumentValue(ARG_TYPE).split(",")) : null);
        camServerViewer.setStreamList(App.hasArgument(ARG_STREAM_LIST) ? Arrays.asList(App.getArgumentValue(ARG_STREAM_LIST).split("\\|")) : null);
        camServerViewer.setConsoleEnabled(App.getBoolArgumentValue(ARG_CONSOLE));               


        if (App.hasArgument("user_overlays")) {
            try {
                camServerViewer.setUserOverlaysConfigFile(App.getArgumentValue("user_overlays"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (App.getBoolArgumentValue("local_fit")) {
            camServerViewer.setLocalFit(true);
        }

        if (App.getBoolArgumentValue("persist_camera")) {
            camServerViewer.setPersistCameraState(true);
        }

        if (App.hasArgument("pipeline_format")) {
            camServerViewer.setPipelineNameFormat(App.getArgumentValue("pipeline_format"));
        }

        if (App.hasArgument("instance_format")) {
            camServerViewer.setInstanceNameFormat(App.getArgumentValue("instance_format"));
        } 
        camServerViewer.setShowFit(true);
        camServerViewer.setShowProfile(true);
        camServerViewer.setShowReticle(true);
    }

    @Override
    public void onStart() {
        super.onStart(); 
        try {            
            camServerViewer.setCameraServerUrl(App.getArgumentValue(ARG_CAMERA_SERVER));
            camServerViewer.setPipelineServerUrl(App.getArgumentValue(ARG_PIPELINE_SERVER));
            camServerViewer.setStartupStream(App.getArgumentValue(ARG_CAMERA));        
            SwingUtilities.invokeLater(()->{
                try {            
                    camServerViewer.initialize(CamServerViewer.SourceSelecionMode.Cameras);
                } catch (Exception ex) {
                    Logger.getLogger(ScreenPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
		updateDialogTitle();
            });
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }
      
    
    
    @Override
    public void onStateChange(State state, State former) {

    }

    @Override
    public void onExecutedFile(String fileName, Object result) {
    }

    //Callback to perform update - in event thread
    @Override
    protected void doUpdate() {
    }

    Thread devicesInitTask;

    
    State state;
    void checkAppState(){
        if (App.isDetached()){
            State state = App.getInstance().getState();
            StreamCamera camera = camServerViewer.getCamera();
            if (state.isInitialized()){
                state = (camera == null) ? state : camera.getState();
            }
            if (state!=this.state){
                App.getInstance().getPropertyChangeSupport().firePropertyChange("appstate", this.state, state);
                this.state = state;
            }
        }
    }
    
    
    @Override
    protected void onTimer() {
        //for (Device dev : new Device[]{screen, exposure}) {
        //    if (dev != null) {
        //        dev.request();
        //    }
        //}
        
        checkAppState();
                
        if (App.hasArgument("s")) {
            try {
                ((Source) getDevice("image")).initialize();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }   

    @Override
    public void onOpeningStream(String name) throws Exception {
        System.out.println("Initializing stream " + name);        
        if ((devicesInitTask != null) && (devicesInitTask.isAlive())) {
            devicesInitTask.interrupt();
        }
        for (Device dev : new Device[]{screen, exposure, mirror, ledPower, flStep}) {
            if (dev != null) {
                dev.close();
            }
        }        
        screen = null;
        exposure = null;
        mirror=null;
        ledPower=null;
        flStep=null;
        
        comboScreen.setModel(new DefaultComboBoxModel());
        comboScreen.setEnabled(false);
        valueScreen.setEnabled(false);
        panelFlStep.setEnabled(false);
        buttonFLDown.setEnabled(false);
        buttonFLUp.setEnabled(false);  
        selLedPower.setEnabled(false);
        panelExposure.setEnabled(false);
        selMirror.setEnabled(false);
        if ((name==null)|| name.isBlank()){
            panelScreen.setVisible(false);
            panelControls.setVisible(false);        
        }        
	updateDialogTitle();
    }    
    

    @Override
    public void onOpenedStream(String name, String instance) throws Exception {        
        System.out.println("Initialized instance: " + instance);        
        String cameraName = camServerViewer.getCameraName();
        List types = camServerViewer.getCameraTypes(cameraName);
        boolean btr = (types!=null) && types.contains(BTR_TYPE);
        boolean booster = (types!=null) && types.contains(BOOSTER_TYPE);
        boolean frontend = (types!=null) && types.contains(FRONTEND_TYPE);
        boolean ltb = (types!=null) && types.contains(LTB_TYPE);
        boolean linac = (types!=null) && types.contains(LINAC_TYPE);
        boolean rf = (types!=null) && types.contains(RF_TYPE);
        boolean ring = (types!=null) && types.contains(RING_TYPE);
        
        boolean cameraControls = linac || ltb || booster || btr || ring;
        boolean flipMirror = linac || ltb || booster;

        panelScreen.setVisible(cameraControls);
        panelControls.setVisible(cameraControls);
        if (cameraName!=null){            
            if (cameraControls) {
                //Parallelizing initialization
                devicesInitTask = new Thread(() -> {
                    
                    try {
                        exposure = new ChannelDouble("Exposure Time", cameraName + ":EXPOSURE");
                        exposure.setMonitored(true);
                        exposure.initialize();

                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                        exposure = null;
                    }
                    panelExposure.setEnabled(exposure != null);
                    panelExposure.setDevice(exposure);
 
                    if (flipMirror){
                        try {
                            mirror = new BinaryPositioner("Flip Mirror", cameraName + ":FLIP-MIRROR");
                            mirror.setMonitored(true);
                            mirror.initialize();

                        } catch (Exception ex) {
                            System.err.println(ex.getMessage());
                            mirror = null;
                        }
                    } else {
                        mirror = null;
                    }
                    selMirror.setEnabled(mirror != null);
                    selMirror.setDevice(mirror);
                    
 
                    try {
                        ledPower = new BinaryPositioner("Led Power", cameraName + ":LED-POWER");
                        ledPower.setMonitored(true);
                        ledPower.initialize();

                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                        ledPower = null;
                    }
                    selLedPower.setEnabled(ledPower != null);
                    selLedPower.setDevice(ledPower);         
                    
                    try {
                        flStep = new ChannelDouble("Lens FL Step", cameraName + "-LENS:FL_STEP");
                        flStep.setMonitored(true);
                        flStep.initialize();

                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                        flStep = null;
                    }
                    panelFlStep.setEnabled(flStep != null);
                    buttonFLDown.setEnabled(flStep != null);
                    buttonFLUp.setEnabled(flStep != null);
                    panelFlStep.setDevice(flStep);                    
                    
                    try{                                                
                        screen = new DiscretePositioner("CurrentScreen", cameraName + ":SET_SCREEN1_POS", cameraName + ":GET_SCREEN1_POS");
                        screen.setMonitored(true);
                        screen.initialize();
                        DefaultComboBoxModel model = new DefaultComboBoxModel();
                        for (String pos : screen.getPositions()) {
                            model.addElement(pos);
                        }
                        comboScreen.setModel(model);
                        comboScreen.setSelectedItem(screen.read());

                    } catch (Exception ex) {
                        comboScreen.setModel(new DefaultComboBoxModel());
                        System.err.println(ex.getMessage());
                        screen = null;
                    }
                    comboScreen.setEnabled(screen != null);
                    valueScreen.setEnabled(screen != null);
                    valueScreen.setDevice(screen);

                    
                });
                devicesInitTask.start();
            }
        }
        updateDialogTitle();
    }    

    public void onSavedSnapshot(String name, String instancee, String snapshotFile) throws Exception {
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[]{0, 180};   //Minimum width
        layout.rowHeights = new int[]{30, 30, 30};   //Minimum height
        panel.setLayout(layout);
        JComboBox comboLogbook = new JComboBox(new String[]{"SLS", "SLS Development", "SLS Measurement Data"});
        JTextField textComment = new JTextField();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Data file:"), c);
        c.gridy = 1;
        panel.add(new JLabel("Logbook:"), c);
        c.gridy = 2;
        panel.add(new JLabel("Comment:"), c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        panel.add(textComment, c);
        c.gridy = 1;
        panel.add(comboLogbook, c);
        c.gridy = 0;
        panel.add(new JLabel(getContext().getExecutionPars().getPath()), c);

        if (SwingUtils.showOption(getTopLevel(), "Success", panel, OptionType.OkCancel) == OptionResult.Yes) {
            StringBuilder message = new StringBuilder();
            message.append("Camera: ").append(name).append("\n");
            message.append("Screen: ").append(String.valueOf(valueScreen.getLabel().getText())).append("\n");
            message.append("Data file: ").append(getContext().getExecutionPars().getPath()).append("\n");
            message.append("Comment: ").append(textComment.getText()).append("\n");
            //Add slicing message
            Overlay[] fitOv = camServerViewer.getFitOverlays();
            if ((fitOv != null) && (fitOv.length > 5) && (fitOv[fitOv.length - 1] instanceof Overlays.Text)) {
                Overlays.Text text = (Overlays.Text) fitOv[fitOv.length - 1];
                message.append(text.getText()).append("\n");
            }
            Elog.log((String) comboLogbook.getSelectedItem(), "ScreenPanel Snapshot", message.toString(), new String[]{snapshotFile});
        }
    }   
    
    public void onSavingImages(String name, String instance, DataManager dm, String pathRoot) throws IOException{
        if (valueScreen.isVisible() && valueScreen.isEnabled()) {
            dm.setAttribute(pathRoot, "Screen", String.valueOf(valueScreen.getLabel().getText()));
        }
        if (panelExposure.isVisible() && panelExposure.isEnabled()) {
            dm.setAttribute(pathRoot, "Exposure", String.valueOf(panelExposure.getValue()));
        }
        if (selMirror.isVisible() && selMirror.isEnabled()) {
            dm.setAttribute(pathRoot, "Mirror", String.valueOf(selMirror.getComboBox().getSelectedItem()));
        }
        if (selLedPower.isVisible() && selLedPower.isEnabled()) {
            dm.setAttribute(pathRoot, "LedPower", String.valueOf(selLedPower.getComboBox().getSelectedItem()));
        }
    }
    
    void updateDialogTitle() {
        if (App.isDetached()) {
            getTopLevel().setTitle(camServerViewer.getInstanceName() == null ? "ScreenPanel" : camServerViewer.getInstanceName());
        }
    }

    

    ////////
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        jProgressBar1 = new javax.swing.JProgressBar();
        customPanel = new javax.swing.JPanel();
        panelScreen = new javax.swing.JPanel();
        valueScreen = new ch.psi.pshell.swing.DeviceValuePanel();
        comboScreen = new javax.swing.JComboBox();
        panelControls = new javax.swing.JPanel();
        panelExposure = new ch.psi.pshell.swing.RegisterPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        selMirror = new ch.psi.pshell.swing.DiscretePositionerSelector();
        jLabel3 = new javax.swing.JLabel();
        selLedPower = new ch.psi.pshell.swing.DiscretePositionerSelector();
        jLabel4 = new javax.swing.JLabel();
        panelFlStep = new ch.psi.pshell.swing.RegisterPanel();
        buttonFLDown = new javax.swing.JButton();
        buttonFLUp = new javax.swing.JButton();
        camServerViewer = new ch.psi.pshell.ui.CamServerViewer();

        setPreferredSize(new java.awt.Dimension(873, 600));
        setLayout(new java.awt.BorderLayout());

        panelScreen.setBorder(javax.swing.BorderFactory.createTitledBorder("Screen"));

        comboScreen.setEnabled(false);
        comboScreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboScreenActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelScreenLayout = new javax.swing.GroupLayout(panelScreen);
        panelScreen.setLayout(panelScreenLayout);
        panelScreenLayout.setHorizontalGroup(
            panelScreenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelScreenLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelScreenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueScreen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboScreen, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelScreenLayout.setVerticalGroup(
            panelScreenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelScreenLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(comboScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        panelControls.setBorder(javax.swing.BorderFactory.createTitledBorder("Camera Control"));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Exposure:");

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Flip Mirror:");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Led Power:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Lens FL:");

        buttonFLDown.setText("<");
        buttonFLDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFLDownActionPerformed(evt);
            }
        });

        buttonFLUp.setText(">");
        buttonFLUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFLUpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelControlsLayout = new javax.swing.GroupLayout(panelControls);
        panelControls.setLayout(panelControlsLayout);
        panelControlsLayout.setHorizontalGroup(
            panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelControlsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelExposure, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(selMirror, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(selLedPower, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(panelControlsLayout.createSequentialGroup()
                        .addComponent(buttonFLDown)
                        .addGap(0, 0, 0)
                        .addComponent(panelFlStep, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGap(0, 0, 0)
                        .addComponent(buttonFLUp)))
                .addContainerGap())
        );

        panelControlsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2});

        panelControlsLayout.setVerticalGroup(
            panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlsLayout.createSequentialGroup()
                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(panelExposure, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(selMirror, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(selLedPower, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(buttonFLDown))
                    .addComponent(panelFlStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonFLUp))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout customPanelLayout = new javax.swing.GroupLayout(customPanel);
        customPanel.setLayout(customPanelLayout);
        customPanelLayout.setHorizontalGroup(
            customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelScreen, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelControls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        customPanelLayout.setVerticalGroup(
            customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(panelScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelControls, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(342, Short.MAX_VALUE))
        );

        add(customPanel, java.awt.BorderLayout.WEST);
        add(camServerViewer, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void comboScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboScreenActionPerformed

        comboScreen.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int index = comboScreen.getSelectedIndex();
                    if (index >= 0) {
                        String cameraName = camServerViewer.getCameraName();
                        String channel = cameraName + ":SET_SCREEN1_POS";
                        Integer readback = Epics.get(channel, Integer.class);
                        if ((readback == null) || (readback != index)) {                            
                            System.out.println("Writing " + index + " to " + channel);
                            Epics.put(channel, Integer.valueOf(index));
                        }
                        screen.read();
                    }
                } catch (Exception ex) {
                    showException(ex);
                } finally {
                    comboScreen.setEnabled(true);
                }
            }
        }).start();
    }//GEN-LAST:event_comboScreenActionPerformed

    private void buttonFLDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFLDownActionPerformed
        try {
            String cameraName = camServerViewer.getCameraName();
            if ((flStep!=null) && (cameraName!=null)){                                        
                String channel = cameraName + "-LENS:DEC_FL.PROC";
                Epics.putq(channel, Integer.valueOf(1));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonFLDownActionPerformed

    private void buttonFLUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFLUpActionPerformed
        try {
            String cameraName = camServerViewer.getCameraName();
            if ((flStep!=null) && (cameraName!=null)){
                String channel = cameraName + "-LENS:INC_FL.PROC";
                Epics.putq(channel, Integer.valueOf(1));
           }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonFLUpActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonFLDown;
    private javax.swing.JButton buttonFLUp;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private ch.psi.pshell.ui.CamServerViewer camServerViewer;
    private javax.swing.JComboBox comboScreen;
    private javax.swing.JPanel customPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JPanel panelControls;
    private ch.psi.pshell.swing.RegisterPanel panelExposure;
    private ch.psi.pshell.swing.RegisterPanel panelFlStep;
    private javax.swing.JPanel panelScreen;
    private ch.psi.pshell.swing.DiscretePositionerSelector selLedPower;
    private ch.psi.pshell.swing.DiscretePositionerSelector selMirror;
    private ch.psi.pshell.swing.DeviceValuePanel valueScreen;
    // End of variables declaration//GEN-END:variables

}


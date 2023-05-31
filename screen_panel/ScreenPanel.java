import ch.psi.pshell.bs.StreamCamera;
import ch.psi.pshell.camserver.PipelineSource;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.epics.ChannelInteger;
import ch.psi.pshell.epics.DiscretePositioner;
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
    
    public static final String LASER_TYPE = "Laser";
    public static final String ELECTRONS_TYPE = "Electrons";
    public static final String PHOTONICS_TYPE = "Photonics";
    public static final String TWO_PULSES_TYPE = "2Pulses";
    
    DiscretePositioner screen;
    DiscretePositioner filter;
    final Logger logger;
    
    

    public ScreenPanel() {
        logger = Logger.getLogger(getClass().getName());
        initComponents();        
        panelPulse.setVisible(false);
        panelScreen.setVisible(false);
        panelFilter.setVisible(false);
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
        for (Device dev : new Device[]{screen, filter}) {
            if (dev != null) {
                dev.request();
            }
        }
        
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
        try {            
            if (panelPulse.isVisible()){
                CamServerViewer.Frame frame = camServerViewer.getCurrentFrame();
                Object pulse = null;
                try{
                    pulse = frame.cache.getValue("pulse");
                } catch (Exception ex) {                            
                }
                textPulse.setText((pulse==null) ? "" : Str.toString(pulse));
            }                                
        } catch (Exception ex) {
            ex.printStackTrace();
        }        
    }   

    @Override
    public void onOpeningStream(String name) throws Exception {
        System.out.println("Initializing stream " + name);        
        if ((devicesInitTask != null) && (devicesInitTask.isAlive())) {
            devicesInitTask.interrupt();
        }
        if (screen != null) {
            screen.close();
            screen = null;
        }
        if (filter != null) {
            filter.close();
            filter = null;
        }        
	updateDialogTitle();
    }    
    

    @Override
    public void onOpenedStream(String name, String instance) throws Exception {
        
        System.out.println("Initialized instance: " + instance);        
        String cameraName = camServerViewer.getStream();
        boolean electrons = (cameraName!=null) && camServerViewer.getCameraTypes(cameraName).contains(ELECTRONS_TYPE);
        boolean twoPulses = (cameraName!=null) && camServerViewer.getCameraTypes(cameraName).contains(TWO_PULSES_TYPE);
        comboScreen.setModel(new DefaultComboBoxModel());
        comboScreen.setEnabled(false);
        comboFilter.setModel(new DefaultComboBoxModel());
        comboFilter.setEnabled(false);
        panelFilter.setVisible(electrons);
        panelScreen.setVisible(electrons);
        panelPulse.setVisible(twoPulses);
        textPulse.setText("");
        if (cameraName!=null){
            if (electrons) {
                //Parallelizing initialization
                devicesInitTask = new Thread(() -> {
                    try {
                        if (cameraName.contains("DSRM")) {
                            screen = new DiscretePositioner("CurrentScreen", cameraName + ":POSITION_SP", cameraName + ":POSITION");
                        } else {
                            screen = new DiscretePositioner("CurrentScreen", cameraName + ":SET_SCREEN1_POS", cameraName + ":GET_SCREEN1_POS");
                        }
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
                    valueScreen.setDevice(screen);

                    try {
                        filter = new DiscretePositioner("CurrentFilter", cameraName + ":SET_FILTER", cameraName + ":GET_FILTER");
                        filter.setMonitored(true);
                        filter.initialize();
                        DefaultComboBoxModel model = new DefaultComboBoxModel();
                        for (String pos : filter.getPositions()) {
                            model.addElement(pos);
                        }
                        comboFilter.setModel(model);
                        comboFilter.setSelectedItem(filter.read());
                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                        filter = null;
                    }
                    comboFilter.setEnabled(filter != null);
                    valueFilter.setDevice(filter);
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
        JComboBox comboLogbook = new JComboBox(new String[]{"SwissFEL commissioning data", "SwissFEL commissioning"});
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
            message.append("Filter: ").append(String.valueOf(valueFilter.getLabel().getText())).append("\n");
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
        if (camServerViewer.getTypes().contains(ELECTRONS_TYPE)) {
            dm.setAttribute(pathRoot, "Screen", String.valueOf(valueScreen.getLabel().getText()));
            dm.setAttribute(pathRoot, "Filter", String.valueOf(valueFilter.getLabel().getText()));
        }
    }
    
    void updateDialogTitle() {
        if (App.isDetached()) {
            getTopLevel().setTitle(camServerViewer.getStream() == null ? "ScreenPanel" : camServerViewer.getStream());
        }
    }

    void setLaserState(int bunch, boolean value) throws Exception {
        System.out.println("Setting laser state: " + value  + " - bunch" + bunch);
        //Epics.putq("SIN-TIMAST-TMA:Beam-Las-Delay-Sel", value ? 0 : 1);
        if ((bunch<=0) || (bunch==1)){
            Epics.putq("SIN-TIMAST-TMA:Bunch-1-OnDelay-Sel", value ? 0 : 1);            
        }
        if ((bunch<=0) || (bunch==2)){
            Epics.putq("SIN-TIMAST-TMA:Bunch-2-OnDelay-Sel", value ? 0 : 1);            
        }
        
        Epics.putq("SIN-TIMAST-TMA:Beam-Apply-Cmd.PROC", 1);
        Thread.sleep(3000);
    }

    boolean getLaserState(int bunch) throws Exception {
        //return (Epics.get("SIN-TIMAST-TMA:Beam-Las-Delay-Sel", Integer.class) == 0);
        try{
            if (bunch<=0){
                return getLaserState(1) && getLaserState(2);
            }
            if (bunch==2){
                return (Epics.get("SWISSFEL-STATUS:Bunch-2-OnDelay-Sel", Integer.class) == 0);
            }
            return (Epics.get("SWISSFEL-STATUS:Bunch-1-OnDelay-Sel", Integer.class) == 0);
        } catch (Exception ex){
            return false;
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
        panelFilter = new javax.swing.JPanel();
        valueFilter = new ch.psi.pshell.swing.DeviceValuePanel();
        comboFilter = new javax.swing.JComboBox();
        panelPulse = new javax.swing.JPanel();
        buttonPulse1 = new javax.swing.JButton();
        buttonPulse2 = new javax.swing.JButton();
        textPulse = new javax.swing.JTextField();
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

        panelFilter.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter"));

        comboFilter.setEnabled(false);
        comboFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboFilterActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelFilterLayout = new javax.swing.GroupLayout(panelFilter);
        panelFilter.setLayout(panelFilterLayout);
        panelFilterLayout.setHorizontalGroup(
            panelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFilterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboFilter, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelFilterLayout.setVerticalGroup(
            panelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFilterLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(comboFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelPulse.setBorder(javax.swing.BorderFactory.createTitledBorder("Pulse"));

        buttonPulse1.setText("Pulse 1");
        buttonPulse1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPulse1ActionPerformed(evt);
            }
        });

        buttonPulse2.setText("Pulse 2");
        buttonPulse2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPulse2ActionPerformed(evt);
            }
        });

        textPulse.setEditable(false);
        textPulse.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout panelPulseLayout = new javax.swing.GroupLayout(panelPulse);
        panelPulse.setLayout(panelPulseLayout);
        panelPulseLayout.setHorizontalGroup(
            panelPulseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPulseLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonPulse1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPulse2)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(textPulse, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        panelPulseLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonPulse1, buttonPulse2});

        panelPulseLayout.setVerticalGroup(
            panelPulseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPulseLayout.createSequentialGroup()
                .addGroup(panelPulseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonPulse1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonPulse2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textPulse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout customPanelLayout = new javax.swing.GroupLayout(customPanel);
        customPanel.setLayout(customPanelLayout);
        customPanelLayout.setHorizontalGroup(
            customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelScreen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelPulse, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        customPanelLayout.setVerticalGroup(
            customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(panelScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelPulse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        add(customPanel, java.awt.BorderLayout.WEST);
        add(camServerViewer, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void comboScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboScreenActionPerformed

        comboScreen.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChannelInteger setpoint = null;
                try {
                    int index = comboScreen.getSelectedIndex();
                    if (index >= 0) {
                        String cameraName = camServerViewer.getStream();
                        if (cameraName.contains("DSRM")) {
                            setpoint = new ChannelInteger(null, cameraName + ":POSITION_SP");
                        } else {
                            setpoint = new ChannelInteger(null, cameraName + ":SET_SCREEN1_POS");
                        }
                        setpoint.initialize();
                        Integer readback = setpoint.read();
                        if ((readback == null) || (setpoint.read() != index)) {
                            setpoint.write(index);
                        }
                        screen.read();
                    }
                } catch (Exception ex) {
                    showException(ex);
                } finally {
                    comboScreen.setEnabled(true);
                    if (setpoint != null) {
                        setpoint.close();
                    }
                }
            }
        }).start();
    }//GEN-LAST:event_comboScreenActionPerformed

    private void comboFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboFilterActionPerformed
        try {
            String setpoint = (String) comboFilter.getSelectedItem();
            if (setpoint != null) {
                if (!setpoint.equals(filter.read())) {
                    filter.write(setpoint);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboFilterActionPerformed

    private void buttonPulse1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPulse1ActionPerformed
        try {
            PipelineSource server = camServerViewer.getServer();
            if ((server != null) && (server.isStarted())) {
                server.setInstanceConfigValue("pulse",1);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonPulse1ActionPerformed

    private void buttonPulse2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPulse2ActionPerformed
        try {
            PipelineSource server = camServerViewer.getServer();
            if ((server != null) && (server.isStarted())) {
                server.setInstanceConfigValue("pulse",2);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonPulse2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.JButton buttonPulse1;
    private javax.swing.JButton buttonPulse2;
    private ch.psi.pshell.ui.CamServerViewer camServerViewer;
    private javax.swing.JComboBox comboFilter;
    private javax.swing.JComboBox comboScreen;
    private javax.swing.JPanel customPanel;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JPanel panelFilter;
    private javax.swing.JPanel panelPulse;
    private javax.swing.JPanel panelScreen;
    private javax.swing.JTextField textPulse;
    private ch.psi.pshell.swing.DeviceValuePanel valueFilter;
    private ch.psi.pshell.swing.DeviceValuePanel valueScreen;
    // End of variables declaration//GEN-END:variables

}


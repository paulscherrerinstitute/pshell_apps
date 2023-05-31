import ch.psi.pshell.bs.PipelineServer;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.epics.ChannelDouble;
import ch.psi.pshell.imaging.RendererMode;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.LinePlotJFree;
import ch.psi.pshell.ui.Panel;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 */
public class PSSS extends Panel {

    final String CAMERA_NAME = "SARFE10-PSSS059";
    PipelineServer pipelineServer;
    volatile boolean updatingPlot;
    volatile boolean updatingImage;    
            
    public PSSS() {
        initComponents();
        plot.getAxis(Plot.AxisId.X).setLabel(null);
        plot.getAxis(Plot.AxisId.Y).setLabel(null);
        renderer.setMode(RendererMode.Stretch);
    }

    //Overridable callbacks
    @Override
    public void onInitialize(int runCount) {
        startTimer(1000);
        
        try {
            setGlobalVar("PSSS_PLOT", plot);
            setGlobalVar("HISTORY_PLOT", history);
            setGlobalVar("PSSS_RENDERER", renderer);                        
            
            pipelineServer = (PipelineServer) getDevice("cam_server");
            ((LinePlotJFree)histogramGeneratorPanelCenter.getPlot()).setLegendVisible(true);
            ((LinePlotJFree)histogramGeneratorFwhm.getPlot()).setLegendVisible(true);
            histogramGeneratorPanelCenter.getPlot().getAxis(Plot.AxisId.Y).setRange(0, 100);
            histogramGeneratorFwhm.getPlot().getAxis(Plot.AxisId.Y).setRange(0, 100);
            //setImageEnabled(true);
            tabStateChanged(null);
                                                
            spinnerAverage.setValue(( (Number) eval("get_psss_averaging()", true)).intValue());
            
            try{
                Double energy = (((ChannelDouble)getDevice("energy_machine")).take(-1));
                energy=Convert.roundDouble(energy, 0);
                spFromEn.setValue(energy-150);
                spToEn.setValue(energy+150);
            } catch (Exception ex) {
                getLogger().warning("Error reading energy_machine");
            } 
            
            
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }        
    }

    @Override
    public void onStateChange(State state, State former) {
        this.btStartCr.setEnabled(state ==  State.Ready);
        this.btStartEn.setEnabled(state ==  State.Ready);
        this.btStartCam.setEnabled(state ==  State.Ready);
        radioEnergyScan.setEnabled(state ==  State.Ready);
        radioCrystalScan.setEnabled(state ==  State.Ready);
        radioCameraScan.setEnabled(state ==  State.Ready);
        this.btAbort.setEnabled(state.isRunning());
    }

    @Override
    public void onExecutedFile(String fileName, Object result) {
    }
    
    @Override
    public void onTimer() {
        try {
            if (!updatingPlot){
                updatingPlot = true;
                //evalAsync("plot_psss(PSSS_PLOT, HISTORY_PLOT, " + spinnerAverage.getValue() + ")", true).handle((ret,ex)->{                
                evalAsync("plot_psss(PSSS_PLOT, HISTORY_PLOT)", true).handle((ret,ex)->{                
                    updatingPlot = false;
                    return ret;
                });
            }
            if (isImageEnabled()){
                if (!updatingImage){
                    updatingImage = true;
                    evalAsync("update_psss_image(PSSS_RENDERER)", true).handle((ret,ex)->{                
                        updatingImage = false;
                        return ret;
                    });
                }
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }
    
    //Callback to perform update - in event thread
    @Override
    protected void doUpdate() {
    }    
    
    void setImageEnabled(boolean enabled){
        try{    
            imageEnabled = enabled;
            evalAsync("enable_psss_image(" + Str.capitalizeFirst(String.valueOf(enabled)) + ", PSSS_RENDERER)", true);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }          
    }
    volatile boolean imageEnabled;
    
    boolean isImageEnabled(){
        return imageEnabled;
    }
    
    
    void runScan(String name, Map args){
        try {
            args.put("PLOT", plotScan);    
            this.runAsync(name, args).handle((ret,ex)->{
                if (ex!=null){
                    if (!getContext().isAborted()){
                        showException((Exception)ex);
                    }
                }
                return ret;
            });
        } catch (Context.ContextStateException ex) {
            showException(ex);
        }    
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        tab = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        plot = new ch.psi.pshell.plot.LinePlotJFree();
        history = new ch.psi.pshell.plot.TimePlotJFree();
        jLabel1 = new javax.swing.JLabel();
        spinnerAverage = new javax.swing.JSpinner();
        histogramGeneratorPanelCenter = new ch.psi.pshell.swing.HistogramGeneratorPanel();
        histogramGeneratorFwhm = new ch.psi.pshell.swing.HistogramGeneratorPanel();
        jPanel4 = new javax.swing.JPanel();
        renderer = new ch.psi.pshell.imaging.Renderer();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        btAbort = new javax.swing.JButton();
        radioEnergyScan = new javax.swing.JRadioButton();
        radioCameraScan = new javax.swing.JRadioButton();
        radioCrystalScan = new javax.swing.JRadioButton();
        jPanel5 = new javax.swing.JPanel();
        panelScan = new javax.swing.JPanel();
        panelEnergyScan = new javax.swing.JPanel();
        spFromEn = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        spToEn = new javax.swing.JSpinner();
        spStepsEn = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        spShotsEn = new javax.swing.JSpinner();
        btStartEn = new javax.swing.JButton();
        panelCameraScan = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        spFromCam = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();
        spToCam = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        spStepsCam = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        spShotsCam = new javax.swing.JSpinner();
        btStartCam = new javax.swing.JButton();
        panelCrystalScan = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        spFromCr = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        spToCr = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        spStepsCr = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        spShotsCr = new javax.swing.JSpinner();
        btStartCr = new javax.swing.JButton();
        plotScan = new ch.psi.pshell.plot.LinePlotJFree();

        tab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabStateChanged(evt);
            }
        });

        plot.setTitle("");

        jLabel1.setText("Average:");

        spinnerAverage.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100, 1));
        spinnerAverage.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerAverageStateChanged(evt);
            }
        });

        histogramGeneratorPanelCenter.setDeviceName("histo_center");

        histogramGeneratorFwhm.setDeviceName("histo_fwhm");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerAverage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(history, javax.swing.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
                    .addComponent(plot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(histogramGeneratorFwhm, javax.swing.GroupLayout.PREFERRED_SIZE, 372, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(histogramGeneratorPanelCenter, javax.swing.GroupLayout.PREFERRED_SIZE, 372, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(spinnerAverage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(histogramGeneratorPanelCenter, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
                    .addComponent(plot, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(history, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
                    .addComponent(histogramGeneratorFwhm, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );

        tab.addTab("Spectrum", jPanel1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 843, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(renderer, javax.swing.GroupLayout.DEFAULT_SIZE, 843, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 432, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(renderer, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE))
        );

        tab.addTab("Camera", jPanel4);

        btAbort.setText("Abort");
        btAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAbortActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioEnergyScan);
        radioEnergyScan.setSelected(true);
        radioEnergyScan.setText("Energy Scan");
        radioEnergyScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioEnergyScanActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioCameraScan);
        radioCameraScan.setText("Camera Scan");
        radioCameraScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioCameraScanActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioCrystalScan);
        radioCrystalScan.setText("Crystal Height Scan");
        radioCrystalScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioCrystalScanActionPerformed(evt);
            }
        });

        panelScan.setLayout(new java.awt.CardLayout());

        spFromEn.setModel(new javax.swing.SpinnerNumberModel(7200.0d, 1.0d, 20000.0d, 10.0d));

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Range From:");

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Range To:");

        spToEn.setModel(new javax.swing.SpinnerNumberModel(7340.0d, 1.0d, 20000.0d, 10.0d));

        spStepsEn.setModel(new javax.swing.SpinnerNumberModel(20, 1, 1000, 1));

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Steps:");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Num Shots:");

        spShotsEn.setModel(new javax.swing.SpinnerNumberModel(100, 1, 1000, 1));

        btStartEn.setText("Start Energy Scan");
        btStartEn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStartEnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelEnergyScanLayout = new javax.swing.GroupLayout(panelEnergyScan);
        panelEnergyScan.setLayout(panelEnergyScanLayout);
        panelEnergyScanLayout.setHorizontalGroup(
            panelEnergyScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelEnergyScanLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelEnergyScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelEnergyScanLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spToEn, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelEnergyScanLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spFromEn, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelEnergyScanLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spStepsEn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelEnergyScanLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spShotsEn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addComponent(btStartEn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        panelEnergyScanLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel6, jLabel7, jLabel8, jLabel9});

        panelEnergyScanLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spFromEn, spShotsEn, spStepsEn, spToEn});

        panelEnergyScanLayout.setVerticalGroup(
            panelEnergyScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelEnergyScanLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelEnergyScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(spFromEn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelEnergyScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spToEn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelEnergyScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(spStepsEn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelEnergyScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(spShotsEn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btStartEn)
                .addContainerGap())
        );

        panelScan.add(panelEnergyScan, "energy");

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText("Range From:");

        spFromCam.setModel(new javax.swing.SpinnerNumberModel(-17.0d, -30.0d, 30.0d, 1.0d));

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Range To:");

        spToCam.setModel(new javax.swing.SpinnerNumberModel(-11.0d, -30.0d, 30.0d, 1.0d));

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel12.setText("Steps:");

        spStepsCam.setModel(new javax.swing.SpinnerNumberModel(20, 1, 1000, 1));

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel13.setText("Num Shots:");

        spShotsCam.setModel(new javax.swing.SpinnerNumberModel(100, 1, 1000, 1));

        btStartCam.setText("Start Camera Scan");
        btStartCam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStartCamActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelCameraScanLayout = new javax.swing.GroupLayout(panelCameraScan);
        panelCameraScan.setLayout(panelCameraScanLayout);
        panelCameraScanLayout.setHorizontalGroup(
            panelCameraScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCameraScanLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelCameraScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelCameraScanLayout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spToCam, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelCameraScanLayout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spFromCam, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelCameraScanLayout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spStepsCam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelCameraScanLayout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spShotsCam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(btStartCam, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        panelCameraScanLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel11, jLabel12, jLabel13});

        panelCameraScanLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spFromCam, spShotsCam, spStepsCam, spToCam});

        panelCameraScanLayout.setVerticalGroup(
            panelCameraScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCameraScanLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelCameraScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(spFromCam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCameraScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(spToCam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCameraScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(spStepsCam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCameraScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(spShotsCam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btStartCam)
                .addContainerGap())
        );

        panelScan.add(panelCameraScan, "camera");

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Range From:");

        spFromCr.setModel(new javax.swing.SpinnerNumberModel(-0.8d, -10.0d, 10.0d, 0.1d));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Range To:");

        spToCr.setModel(new javax.swing.SpinnerNumberModel(-1.7d, -10.0d, 10.0d, 0.1d));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Steps:");

        spStepsCr.setModel(new javax.swing.SpinnerNumberModel(20, 1, 1000, 1));

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Num Shots:");

        spShotsCr.setModel(new javax.swing.SpinnerNumberModel(100, 1, 1000, 1));

        btStartCr.setText("Start Crystal Height Scan");
        btStartCr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStartCrActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelCrystalScanLayout = new javax.swing.GroupLayout(panelCrystalScan);
        panelCrystalScan.setLayout(panelCrystalScanLayout);
        panelCrystalScanLayout.setHorizontalGroup(
            panelCrystalScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCrystalScanLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelCrystalScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelCrystalScanLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spToCr, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelCrystalScanLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spFromCr, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelCrystalScanLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spStepsCr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelCrystalScanLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spShotsCr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(btStartCr, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        panelCrystalScanLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel2, jLabel3, jLabel4, jLabel5});

        panelCrystalScanLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spFromCr, spShotsCr, spStepsCr, spToCr});

        panelCrystalScanLayout.setVerticalGroup(
            panelCrystalScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCrystalScanLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelCrystalScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(spFromCr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCrystalScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(spToCr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCrystalScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(spStepsCr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCrystalScanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(spShotsCr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btStartCr)
                .addContainerGap())
        );

        panelScan.add(panelCrystalScan, "crystal");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel5Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(panelScan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, 0)))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel5Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(panelScan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(radioCrystalScan)
                    .addComponent(radioCameraScan)
                    .addComponent(radioEnergyScan)
                    .addComponent(btAbort, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(radioEnergyScan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(radioCameraScan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(radioCrystalScan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btAbort)
                .addContainerGap())
        );

        plotScan.setTitle("");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(plotScan, javax.swing.GroupLayout.DEFAULT_SIZE, 614, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(plotScan, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
        );

        tab.addTab("Alignment", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tab)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tab, javax.swing.GroupLayout.Alignment.TRAILING)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabStateChanged
        setImageEnabled(tab.getSelectedIndex()==1);
    }//GEN-LAST:event_tabStateChanged

    private void btAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btAbortActionPerformed
        try {
            abort();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_btAbortActionPerformed

    private void btStartCrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStartCrActionPerformed
        Map args = new HashMap();
        args.put("RANGE_FROM", spFromCr.getValue());
        args.put("RANGE_TO", spToCr.getValue());
        args.put("STEPS", spStepsCr.getValue());
        args.put("NUM_SHOTS", spShotsCr.getValue());        
        runScan("psss/CrystalHeightScan",args);
    }//GEN-LAST:event_btStartCrActionPerformed

    private void btStartCamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStartCamActionPerformed
        Map args = new HashMap();
        args.put("RANGE_FROM", spFromCam.getValue());
        args.put("RANGE_TO", spToCam.getValue());
        args.put("STEPS", spStepsCam.getValue());
        args.put("NUM_SHOTS", spShotsCam.getValue());  
        runScan("psss/CameraScan",args);
    }//GEN-LAST:event_btStartCamActionPerformed

    private void btStartEnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStartEnActionPerformed
        Map args = new HashMap();
        args.put("RANGE_OFF", null);
        args.put("RANGE_FROM", spFromEn.getValue());
        args.put("RANGE_TO", spToEn.getValue());
        args.put("STEPS", spStepsEn.getValue());
        args.put("NUM_SHOTS", spShotsEn.getValue());
        runScan("psss/EnergyScan",args);
    }//GEN-LAST:event_btStartEnActionPerformed

    private void radioEnergyScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioEnergyScanActionPerformed
        ((CardLayout)panelScan.getLayout()).show(panelScan, "energy");
    }//GEN-LAST:event_radioEnergyScanActionPerformed

    private void radioCameraScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioCameraScanActionPerformed
        ((CardLayout)panelScan.getLayout()).show(panelScan, "camera");
    }//GEN-LAST:event_radioCameraScanActionPerformed

    private void radioCrystalScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioCrystalScanActionPerformed
        ((CardLayout)panelScan.getLayout()).show(panelScan, "crystal");
    }//GEN-LAST:event_radioCrystalScanActionPerformed

    private void spinnerAverageStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerAverageStateChanged
        try {
            eval("set_psss_averaging(" + spinnerAverage.getValue() + ")", true); 
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_spinnerAverageStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btAbort;
    private javax.swing.JButton btStartCam;
    private javax.swing.JButton btStartCr;
    private javax.swing.JButton btStartEn;
    private javax.swing.ButtonGroup buttonGroup1;
    private ch.psi.pshell.swing.HistogramGeneratorPanel histogramGeneratorFwhm;
    private ch.psi.pshell.swing.HistogramGeneratorPanel histogramGeneratorPanelCenter;
    private ch.psi.pshell.plot.TimePlotJFree history;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel panelCameraScan;
    private javax.swing.JPanel panelCrystalScan;
    private javax.swing.JPanel panelEnergyScan;
    private javax.swing.JPanel panelScan;
    private ch.psi.pshell.plot.LinePlotJFree plot;
    private ch.psi.pshell.plot.LinePlotJFree plotScan;
    private javax.swing.JRadioButton radioCameraScan;
    private javax.swing.JRadioButton radioCrystalScan;
    private javax.swing.JRadioButton radioEnergyScan;
    private ch.psi.pshell.imaging.Renderer renderer;
    private javax.swing.JSpinner spFromCam;
    private javax.swing.JSpinner spFromCr;
    private javax.swing.JSpinner spFromEn;
    private javax.swing.JSpinner spShotsCam;
    private javax.swing.JSpinner spShotsCr;
    private javax.swing.JSpinner spShotsEn;
    private javax.swing.JSpinner spStepsCam;
    private javax.swing.JSpinner spStepsCr;
    private javax.swing.JSpinner spStepsEn;
    private javax.swing.JSpinner spToCam;
    private javax.swing.JSpinner spToCr;
    private javax.swing.JSpinner spToEn;
    private javax.swing.JSpinner spinnerAverage;
    private javax.swing.JTabbedPane tab;
    // End of variables declaration//GEN-END:variables
}

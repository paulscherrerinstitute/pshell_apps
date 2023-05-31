/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */

import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.swing.ChannelSelector;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.ui.Panel;
import ch.psi.utils.State;
import ch.psi.utils.Chrono;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 */
public class Correlation extends Panel {

    public Correlation() {
        initComponents();
        this.setPersistedComponents(new Component[]{textDevX, textDevY, spinnerInterval, spinnerWindow, comboTypeX, comboTypeY});
        plot.getAxis(Plot.AxisId.X).setLabel(null);
        plot.getAxis(Plot.AxisId.Y).setLabel(null);
        try{
            if (App.hasArgument("interval")) {
                spinnerInterval.setValue(Double.valueOf(App.getArgumentValue("interval")));
            }        
        } catch (Exception ex){            
            System.err.println(ex.getMessage());
        }
        try{
            if (App.hasArgument("window")) {
                spinnerWindow.setValue(Integer.valueOf(App.getArgumentValue("window")));
            }        
        } catch (Exception ex){            
            System.err.println(ex.getMessage());
        }                
        try{
            if (App.hasArgument("linear")) {
                checkLinear.setSelected(App.getBoolArgumentValue("linear"));
            }        
        } catch (Exception ex){            
            System.err.println(ex.getMessage());
        }  
        try{
            if (App.hasArgument("quadratic")) {
                checkQuadratic.setSelected(App.getBoolArgumentValue("quadratic"));
            }        
        } catch (Exception ex){            
            System.err.println(ex.getMessage());
        }          
    }

     //Overridable callbacks
    @Override
    public void onInitialize(int runCount) {
        super.onInitialize(runCount);
        this.startTimer(100, 10);
        if (App.hasArgument("dx")) {
                textDevX.setText(App.getArgumentValue("dx"));
        }
        if (App.hasArgument("dy")) {
                textDevY.setText(App.getArgumentValue("dy"));
        }        
        try{
            if (App.hasArgument("dxtype")) {
                comboTypeX.setSelectedIndex(Integer.valueOf(App.getArgumentValue("dxtype")));
            }        
        } catch (Exception ex){            
           System.err.println(ex.getMessage());
        }
        try{
            if (App.hasArgument("dytype")) {
                comboTypeY.setSelectedIndex(Integer.valueOf(App.getArgumentValue("dytype")));
            }        
        } catch (Exception ex){            
           System.err.println(ex.getMessage());
        }
        comboTypeXActionPerformed(null);
        comboTypeYActionPerformed(null);
    }
    
    @Override
    protected void onTimer(){
        if (isRunning()){
            updateResults();
        }
    }
    
    //DecimalFormat formatter = new DecimalFormat("0.##E0");
    void updateResults(){
        try{
            textCorrelation.setText(String.format("%1.4f", Double.valueOf((Double)getContext().getInterpreterVariable("corr"))));            
        } catch (Exception ex){
            textCorrelation.setText("");
        }
        
        if (checkLinear.isSelected()){
            try{
                List pars = (List)getContext().getInterpreterVariable("pars_lin");            
                //textLinear.setText(String.format("%1.3fx%+1.3f", (Double)(pars.get(1)),  (Double)(pars.get(0))));            
                textLinear.setText(String.format("%1.6gx%+1.6g",pars.get(1),  pars.get(0)));            
            } catch (Exception ex){
                textLinear.setText("");
            }        
        }
        
        if (checkQuadratic.isSelected()){
            try{
                List pars = (List)getContext().getInterpreterVariable("pars_quad");            
                //textQuadratic.setText(String.format("%1.2fx\u00B2 %+1.2fx%+1.2f", (Double)(pars.get(0)), (Double)(pars.get(1)),  (Double)(pars.get(0))));            
                textQuadratic.setText(String.format("%1.3gx\u00B2%+1.3gx%+1.3g", pars.get(0), pars.get(1), pars.get(0)));            
                //textQuadratic.setText(formatter.format(pars.get(2))+ formatter.format(pars.get(1)) + formatter.format(pars.get(0)));            

            } catch (Exception ex){
                textQuadratic.setText("");
            }        
            try{
                String peak = (String)getContext().getInterpreterVariable("pos_peak");            
                if (peak!=null){
                    textPeak.setText(peak + " (max)");            
                } else {
                    peak = (String)getContext().getInterpreterVariable("neg_peak");            
                    if (peak!=null){
                        textPeak.setText(peak + " (min)");            
                    } else {
                        textPeak.setText("");
                    }                    
                }

            } catch (Exception ex){
                textPeak.setText("");
            }        
        }        
    }

    @Override
    public void onStateChange(State state, State former) {
        buttonStart.setEnabled((state==State.Ready) || (state==State.Busy));
        if (isRunning()){
            if (state==State.Ready){
                buttonStart.setText("Start");
            }
        } else {
            if (state==State.Busy){
                buttonStart.setText("Stop");
                buttonElog.setEnabled(true);
            }
        }
        textDevX.setEnabled(state==State.Ready);
        textDevY.setEnabled(state==State.Ready);
        spinnerInterval.setEnabled(state==State.Ready);
        spinnerWindow.setEnabled(state==State.Ready);        
        comboTypeX.setEnabled(state==State.Ready);        
        comboTypeY.setEnabled(state==State.Ready);        
        checkLinear.setEnabled(state==State.Ready);        
        checkQuadratic.setEnabled(state==State.Ready);        
        
        if ((former==State.Initializing) && (state == State.Ready)){
            if (App.hasArgument("start")) {
                buttonStartActionPerformed(null);
            }        
        }
        
    }
    
    boolean isRunning(){
        return buttonStart.getText().equals("Stop");
    }

    @Override
    public void onExecutedFile(String fileName, Object result) {
    }

    
    //Callback to perform update - in event thread
    @Override
    protected void doUpdate() {
    }
    
    void elog(String logbook, String title, String message, String[] attachments) throws Exception {
        String domain = "";
        String category = "Info";
        String entry = "";
        StringBuffer cmd = new StringBuffer();

        cmd.append("G_CS_ELOG_add -l \"").append(logbook).append("\" ");
        cmd.append("-a \"Author=ScreenPanel\" ");
        cmd.append("-a \"Type=pshell\" ");
        cmd.append("-a \"Entry=").append(entry).append("\" ");
        cmd.append("-a \"Title=").append(title).append("\" ");
        cmd.append("-a \"Category=").append(category).append("\" ");
        cmd.append("-a \"Domain=").append(domain).append("\" ");
        for (String attachment : attachments) {
            cmd.append("-f \"").append(attachment).append("\" ");
        }
        cmd.append("-n 1 ");
        cmd.append("\"").append(message).append("\" ");
        System.out.println(cmd.toString());

        final Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd.toString()});
        new Thread(() -> {
            try {
                process.waitFor();
                int bytes = process.getInputStream().available();
                byte[] arr = new byte[bytes];
                process.getInputStream().read(arr, 0, bytes);
                System.out.println(new String(arr));
                bytes = process.getErrorStream().available();
                arr = new byte[bytes];
                process.getErrorStream().read(arr, 0, bytes);
                System.err.println(new String(arr));
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }).start();
    }    
    

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        plot = new ch.psi.pshell.plot.LinePlotJFree();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        spinnerInterval = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        spinnerWindow = new javax.swing.JSpinner();
        buttonStart = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        textCorrelation = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        textLinear = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        textQuadratic = new javax.swing.JTextField();
        checkLinear = new javax.swing.JCheckBox();
        checkQuadratic = new javax.swing.JCheckBox();
        comboTypeX = new javax.swing.JComboBox();
        comboTypeY = new javax.swing.JComboBox();
        textDevX = new ch.psi.pshell.swing.ChannelSelector();
        textDevY = new ch.psi.pshell.swing.ChannelSelector();
        buttonElog = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        textPeak = new javax.swing.JTextField();
        checkAlign = new javax.swing.JCheckBox();

        plot.setTitle("");

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("X device:");

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Y device:");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Interval (s):");

        spinnerInterval.setModel(new javax.swing.SpinnerNumberModel(0.1d, 0.001d, null, 1.0d));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Window size:");

        spinnerWindow.setModel(new javax.swing.SpinnerNumberModel(50, 3, null, 1));

        buttonStart.setText("Start");
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("Correlation:");

        textCorrelation.setEditable(false);
        textCorrelation.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("Liner fit:");

        textLinear.setEditable(false);
        textLinear.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText("Quadratric fit:");

        textQuadratic.setEditable(false);
        textQuadratic.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        checkLinear.setSelected(true);
        checkLinear.setText("Linear fit");

        checkQuadratic.setText("Quadratic fit");

        comboTypeX.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Channel", "Stream", "Camera" }));
        comboTypeX.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboTypeXActionPerformed(evt);
            }
        });

        comboTypeY.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Channel", "Stream", "Camera" }));
        comboTypeY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboTypeYActionPerformed(evt);
            }
        });

        textDevX.setHistorySize(20);
        textDevX.setName("Correlation_textDevX"); // NOI18N

        textDevY.setHistorySize(20);
        textDevY.setName("Correlation_textDevY"); // NOI18N

        buttonElog.setText("ELOG");
        buttonElog.setEnabled(false);
        buttonElog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonElogActionPerformed(evt);
            }
        });

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Peak:");

        textPeak.setEditable(false);
        textPeak.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        checkAlign.setText("Align Camera Streams");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkAlign)
                            .addComponent(comboTypeY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboTypeX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textCorrelation, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                            .addComponent(spinnerInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spinnerWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textLinear, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                            .addComponent(checkLinear)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(textPeak, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(textQuadratic, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)))
                        .addGap(0, 70, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(textDevX, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(checkQuadratic)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(buttonElog))
                            .addComponent(textDevY, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonStart, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerInterval, spinnerWindow});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel5, jLabel6, jLabel7});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {textCorrelation, textLinear, textQuadratic});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(textDevX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboTypeX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2)
                    .addComponent(textDevY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addComponent(comboTypeY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(spinnerInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(spinnerWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkAlign)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkLinear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkQuadratic)
                    .addComponent(buttonElog))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(textCorrelation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel6)
                    .addComponent(textLinear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel7)
                    .addComponent(textQuadratic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel8)
                    .addComponent(textPeak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(buttonStart)
                .addGap(33, 33, 33))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(plot, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(plot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartActionPerformed
        try {
            if (isRunning()){
                //abort();
                //Stooping smootly so displayed variables get updated.
                getContext().setInterpreterVariable("stop_exec", true);
                Chrono chrono = new Chrono();
                while (!chrono.isTimeout(500)){
                    if (!Boolean.TRUE.equals(getContext().getInterpreterVariable("stop_exec"))){
                        break;
                    }
                    Thread.sleep(1);
                }    
                if (Boolean.TRUE.equals(getContext().getInterpreterVariable("stop_exec"))){
                    System.out.println("Timeout stopping script - aborting...");
                    abort();
                }
                updateResults();
                //buttonStart.setText("Start");
            } else {
                textCorrelation.setText("");
                textLinear.setText("");
                textQuadratic.setText("");
                textPeak.setText("");
                HashMap args = new HashMap();
                args.put("dx", textDevX.getText());
                args.put("dy", textDevY.getText());
                args.put("interval", spinnerInterval.getValue());
                args.put("window", spinnerWindow.getValue());
                args.put("dxtype", comboTypeX.getSelectedIndex());
                args.put("dytype", comboTypeY.getSelectedIndex());
                args.put("linear_fit", checkLinear.isSelected());
                args.put("quadratic_fit", checkQuadratic.isSelected());
                args.put("merge_camera_stream", checkAlign.isSelected());

                args.put("p", plot);
                runAsync("correlation", args).handle((ok, ex) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                    }            
                    return ok;
                }); 
                ///buttonStart.setText("Stop");
            }

        } catch (Exception ex) {
            showException(ex);
        }        
    }//GEN-LAST:event_buttonStartActionPerformed

    private void comboTypeXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboTypeXActionPerformed
        if (comboTypeX.getSelectedIndex()==0){
            //textDevX.configure(ChannelSelector.Type.Epics, "http://epics-boot-info.psi.ch", "swissfel", 5000);
            textDevX.configure(ChannelSelector.Type.IocInfo,"http://iocinfo.psi.ch/api/v2", "swissfel", 5000);
	} else if (comboTypeX.getSelectedIndex()==1){
            textDevX.configure(ChannelSelector.Type.DataAPI, "https://data-api.psi.ch/sf", "sf-databuffer", 5000);
        } else if (comboTypeX.getSelectedIndex()==2){
            textDevX.configure(ChannelSelector.Type.Camera, "sf-daqsync-01:8889", null, 5000);
        }       
    }//GEN-LAST:event_comboTypeXActionPerformed

    private void comboTypeYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboTypeYActionPerformed
        if (comboTypeY.getSelectedIndex()==0){
            textDevY.configure(ChannelSelector.Type.IocInfo,"http://iocinfo.psi.ch/api/v2", "swissfel", 5000);
        } else if (comboTypeY.getSelectedIndex()==1){
            textDevY.configure(ChannelSelector.Type.DataAPI, "https://data-api.psi.ch/sf", "sf-databuffer", 5000);
        } else if (comboTypeY.getSelectedIndex()==2){
            textDevY.configure(ChannelSelector.Type.Camera, "sf-daqsync-01:8889", null, 5000);
        }         
    }//GEN-LAST:event_comboTypeYActionPerformed

    private void buttonElogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonElogActionPerformed
        try{
            String snapshotFile =  getContext().getSetup().expandPath("{context}/correlation_plot.png");        
            plot.saveSnapshot(snapshotFile, "png");        
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
            panel.add(new JLabel("Logbook:"), c);
            c.gridy = 1;
            panel.add(new JLabel("Comment:"), c);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            panel.add(textComment, c);
            c.gridy = 0;
            panel.add(comboLogbook, c);

            if (SwingUtils.showOption(getTopLevel(), "ELOG", panel, OptionType.OkCancel) == OptionResult.Yes) {
                StringBuilder message = new StringBuilder();
                message.append("Device X: ").append(String.valueOf(textDevX.getText())).append("\n");
                message.append("Device Y: ").append(String.valueOf(textDevY.getText())).append("\n");
                message.append("Interval: ").append(String.valueOf(spinnerInterval.getValue())).append("\n");
                message.append("Samples: ").append(String.valueOf(spinnerWindow.getValue())).append("\n");
                message.append("Correlation: ").append(textCorrelation.getText()).append("\n");
                message.append("Linear fit: ").append(textLinear.getText()).append("\n");
                message.append("Quadratic fit: ").append(textQuadratic.getText()).append("\n");
                message.append("Peak: ").append(textPeak.getText()).append("\n");
                message.append("Comment: ").append(textComment.getText()).append("\n");                
                elog((String) comboLogbook.getSelectedItem(), "Correlation Panel Snapshot", message.toString(), new String[]{snapshotFile});
            }            
        }catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_buttonElogActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonElog;
    private javax.swing.JButton buttonStart;
    private javax.swing.JCheckBox checkAlign;
    private javax.swing.JCheckBox checkLinear;
    private javax.swing.JCheckBox checkQuadratic;
    private javax.swing.JComboBox comboTypeX;
    private javax.swing.JComboBox comboTypeY;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private ch.psi.pshell.plot.LinePlotJFree plot;
    private javax.swing.JSpinner spinnerInterval;
    private javax.swing.JSpinner spinnerWindow;
    private javax.swing.JTextField textCorrelation;
    private ch.psi.pshell.swing.ChannelSelector textDevX;
    private ch.psi.pshell.swing.ChannelSelector textDevY;
    private javax.swing.JTextField textLinear;
    private javax.swing.JTextField textPeak;
    private javax.swing.JTextField textQuadratic;
    // End of variables declaration//GEN-END:variables
}

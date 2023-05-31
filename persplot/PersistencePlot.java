import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceListener;
import static ch.psi.pshell.device.Record.SIZE_VALID;
import static ch.psi.pshell.device.Record.UNDEFINED_PRECISION;
import ch.psi.pshell.swing.ChannelSelector;
import ch.psi.pshell.ui.Panel;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.epics.ChannelDoubleArray;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.utils.State;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 */
public class PersistencePlot extends Panel{
    ChannelDoubleArray channel;
    LinePlotSeries series;
    LinePlotSeries seriesMax;
    LinePlotSeries seriesMin;
    boolean started;
    double[] max;
    double[] min;
    double[] last;
    boolean permanent;
    
    
    public PersistencePlot() {
        initComponents();
        plot.setTitle(null);
        plot.getAxis(Plot.AxisId.X).setLabel(null);
        plot.getAxis(Plot.AxisId.Y).setLabel(null);
        plot.setLegendVisible(true);
    }

    //Overridable callbacks
    @Override
    public void onInitialize(int runCount) {        
        channelSelector.configure(ChannelSelector.Type.Epics, "http://epics-boot-info.psi.ch", "sls", 3000);        
        if (App.hasArgument("channel")){
            channelSelector.setText(App.getArgumentValue("channel"));
        }
        if (App.hasArgument("start")){
            buttonStartActionPerformed(null);
        }
        startTimer(500);
    }

    @Override
    public void onStateChange(State state, State former) {
         updateButtons();
    }

    @Override
    public void onExecutedFile(String fileName, Object result) {
    }

    
    //Callback to perform update - in event thread
    @Override
    protected void doUpdate() {
    }
        
    final Object lock = new Object();
    volatile boolean updated;
    
    @Override
    protected void onTimer() {
        if (started){
            synchronized(lock){
                if (updated){
                    series.setData(last);  
                    seriesMax.setData(max);  
                    seriesMin.setData(min);  
                    if (permanent){
                        //LinePlotSeries series = new LinePlotSeries("", Color.RED.darker().darker());            
                        //plot.addSeries(series);            
                        //series.setPointsVisible(false);
                    }   
                    updated = false;
                }
            }
        }
    }    
    
    void updateButtons(){
        boolean initialized = getState().isInitialized();
        buttonStart.setEnabled(initialized && !started);
        buttonStop.setEnabled(initialized && started);
        channelSelector.setEnabled(initialized && !started);
    }
    
    final DeviceListener deviceListener = new DeviceAdapter() {
        @Override
        public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
            try{
                onData((double[]) value);
            } catch (Exception ex){
                Logger.getLogger(PersistencePlot.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    };
    
    void onData(double[]  data){
        if (data.length!=last.length){
            stop();                                            
           showMessage("Error", "Waveforme size changed: stopping plot");
        }
        
        synchronized(lock){
            for (int i=0; i< data.length;i++){
                double value = data[i];
                if (value>max[i]){
                    max[i]=value;
                }
                if (value<min[i]){
                    min[i]=value;
                }
            }
            last=data;         
            updated = true;
        }
    }
    
    void start(String channelName){
        stop();
        clear();
        try {            
            series = new LinePlotSeries(channelName, Color.RED.darker().darker());
            seriesMax = new LinePlotSeries(channelName + " max", Color.RED);
            seriesMin = new LinePlotSeries(channelName + " min", Color.RED.darker());
            plot.addSeries(series);
            plot.addSeries(seriesMax);
            plot.addSeries(seriesMin);
            series.setPointsVisible(false);

            channel = new ChannelDoubleArray(channelName, channelName,UNDEFINED_PRECISION, SIZE_VALID);
            channel.initialize();             
            last = channel.read();
            max = new double[last.length];
            min = new double[last.length];
            System.arraycopy(last, 0, max, 0, last.length);
            System.arraycopy(last, 0, min, 0, last.length);
            channel.setMonitored(true);
            channel.addListener(deviceListener);
            onData(last);
            
            started = true;
        } catch (Exception ex) {
            Logger.getLogger(PersistencePlot.class.getName()).warning("Error starting plot: " + ex.getMessage());
        }  finally{
            updateButtons();
        }    
    }    
    
    void stop(){
        try{
            if (started){
                if (channel != null){
                    channel.removeListener(deviceListener);
                    if (SwingUtilities.isEventDispatchThread()){
                        channel.close();
                    } else {
                        SwingUtilities.invokeLater(()->{
                            channel.close();
                        });
                    }
                }
            }
        } finally{
            started = false;
            updateButtons();
        }
    }
    
    void clear(){
        try {
            plot.clear();
        } catch (Exception ex) {
            Logger.getLogger(PersistencePlot.class.getName()).log(Level.WARNING, null, ex);
        }          
    }
    
        


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        plot = new ch.psi.pshell.plot.LinePlotJFree();
        channelSelector = new ch.psi.pshell.swing.ChannelSelector();
        buttonStart = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        channelSelector.setHistorySize(20);
        channelSelector.setName("Correlation_textDevX"); // NOI18N

        buttonStart.setText("Start");
        buttonStart.setEnabled(false);
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });

        buttonStop.setText("Stop");
        buttonStop.setEnabled(false);
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        jLabel1.setText("Channel:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(plot, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(channelSelector, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(buttonStart)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonStop)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonStart, buttonStop});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(channelSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonStart)
                    .addComponent(buttonStop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(plot, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartActionPerformed
        try{
            //setChannel( "ALIRF-A1-KLYDCP10:REF-POWER");
            start(channelSelector.getText());            
        } catch (Exception ex) {
           showException(ex);
        }
    }//GEN-LAST:event_buttonStartActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        try{
            stop();
        } catch (Exception ex) {
           showException(ex);
        } 
    }//GEN-LAST:event_buttonStopActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonStart;
    private javax.swing.JButton buttonStop;
    private ch.psi.pshell.swing.ChannelSelector channelSelector;
    private javax.swing.JLabel jLabel1;
    private ch.psi.pshell.plot.LinePlotJFree plot;
    // End of variables declaration//GEN-END:variables
}

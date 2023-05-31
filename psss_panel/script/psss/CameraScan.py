#Scan the PSSS camera position
#Purpose:
#To set or confirm the camera is positioned with the measured spectrum in the centre of the spectral integration window

#If running from editor
if get_exec_pars().source == CommandSource.ui:  
#User inputs - define travel range of camera
    RANGE_FROM = -17
    RANGE_TO = -11
    STEPS = 20
    NUM_SHOTS= 10 #100
    PLOT=None
p = plot(None, title="Data")[0] if (PLOT is None) else PLOT
p.clear()
p.removeMarker(None)
p.setLegendVisible(True)
p.addSeries(LinePlotSeries("PSSS Spectrum Average")) 


run("cpython/wrapper")

if not is_dry_run():
    cam_x=Channel("SARFE10-PSSS059:MOTOR_X5.VAL", name="cam_x")
else:
    cam_x=DummyRegister("cam_x")

av = create_averager(psss_spectrum_y, NUM_SHOTS, interval=-1, name="spectrum_average")
av_samples = av.samples
av_samples.alias = "spectrum_samples"


#Scan and take data
def after_read(record, scan):
    p.getSeries(0).setData(psss_spectrum_x.take(), record[av])
    p.setTitle("Cam X = %1.3f" %(record[cam_x]))

r = lscan(cam_x, (av, av_samples), RANGE_FROM, RANGE_TO, STEPS, latency=0.0, after_read = after_read, save=False)
average, samples, cam_range = r.getReadable(0), r.getReadable(1), r.getPositions(0) 

signal_centre, projection = get_signal_centre(samples, cam_range)

#Set max position
cam_x.write(signal_centre)
cam_x.close()



"""
plt.figure(figsize=[10,5])
plt.subplot(121)
plt.title('PSSS scan of camera position')
plt.pcolormesh(np.arange(0,Scan_spec.shape[2]), Cam_range, Scan_spec.mean(axis=1),cmap='CMRmap')
plt.xlim([0,Scan_spec.shape[2]])
plt.xlabel('Camera pixel dispersive direction')
plt.ylabel('Set PSSS cam_x _pos [mm] \n'+PSSS_cam_x_PV_name[0:-4])
plt.subplot(122)
plt.plot(projection,Cam_range,linewidth = 2, color = 'orange',label ='projected signal')
plt.title('Spectrum centred at %.1f [mm] (from signal max) \n trace should have hard edges'%signal_centre)
plt.xticks([])
plt.legend()
plt.grid(True)
"""
#PLOT.clear()
#plot_data(PLOT, projection, "Data", xdata=cam_range,  show_points = True, color=Color.BLUE)
#p,pars = plot_gauss_fit(cam_range, projection, gauss_pars=None, p=PLOT, title = "Data")

p.clear()
p.setTitle("")
plot_data(p, projection, "Projection", xdata=cam_range,  show_points = True, color=Color.BLUE)
p.addMarker(signal_centre, None, "Signal Centre=" + str(round(signal_centre,2)), Color.LIGHT_GRAY)


set_return(signal_centre)
###############################################################################
#Scan the PSSS crystal height
#Purpose:
#The PSSS signal level is very sensitive to the crystal height. This script will scan the height and set the position to the maximum signal

if get_exec_pars().source == CommandSource.ui:  
#User inputs - define travel range of camera
    RANGE_FROM = -0.8
    RANGE_TO = -1.7
    STEPS = 10 #20
    NUM_SHOTS= 10 # 100
    PLOT=None
# get current camera ROIs and then set to max for scan
roi_min = psss_roi_min.read()
roi_max = psss_roi_max.read()
psss_roi_min.write(1)
psss_roi_max.write(2000)

p = plot(None, title="Data")[0] if (PLOT is None) else PLOT
p.clear()
p.removeMarker(None)
p.setLegendVisible(True)
p.addSeries(LinePlotSeries("PSSS Spectrum Average")) 

run("cpython/wrapper")


#Setup and functions setup¶
#if not is_dry_run(): # C.arrell commented out 20.01.21
xstal_height=Channel("SARFE10-PSSS059:MOTOR_Y3.VAL", name="xstal_height")
#else:
#    xstal_height=DummyRegister("xstal_height")

av = create_averager(psss_spectrum_y, NUM_SHOTS, interval=-1, name="spectrum_average")
av_samples = av.samples
av_samples.alias = "spectrum_samples"
       
#Scan and take data
def after_read(record, scan):
    p.getSeries(0).setData(psss_spectrum_x.take(), record[av])
    p.setTitle("Xtal Height = %1.3f" %(record[xstal_height]))
    
r = lscan(xstal_height, (av, av_samples), RANGE_FROM, RANGE_TO, STEPS, latency=2.0, after_read = after_read, save=False)

#User inputs - define travel range of crystal
#It is unlikely these values need to be changed
average, samples, xstal_range =  r.getReadable(0), r.getReadable(1), r.getPositions(0)  

#return maxium position
[amp, mean_val, sigma, offset], projection = fit_crystal_height(RANGE_FROM, RANGE_TO, STEPS+1, samples)
print(mean_val)

if not (RANGE_FROM < mean_val < RANGE_TO or RANGE_TO < mean_val < RANGE_FROM):
    raise Exception ("Invalid fit mean: " + str(mean_val))  


#Set max position
#Cell below will push the maximum position to the xstal height
xstal_height.write(mean_val)
xstal_height.close()

# return ROI to inital value
psss_roi_min.write(roi_min)
psss_roi_max.write(roi_max)

#Plots

p.clear()
p.setTitle("")
plot_gauss_fit(xstal_range, projection, gauss_pars=(offset, amp, mean_val, sigma), p=p, title = "Data")


set_return(mean_val)

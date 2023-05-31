###############################################################################
#Scan the PSSS photon energy
#Purpose: To find and centre the PSSS photon energy so the measured spectrum is centred on the camera chip

#PARAMETERS
#User inputs - define energy range to scan below by running the appropiate cell
#Below is for a large scan range assuming offset from machine upto $\pm$ 300 eV

#If running from editor
if get_exec_pars().source == CommandSource.ui:   
    RANGE_OFF = None
    RANGE_FROM = 11100
    RANGE_TO = 11300
    STEPS = 5 #60
    NUM_SHOTS= 10 #100
    PLOT=None
p = plot(None, title="Data")[0] if (PLOT is None) else PLOT
p.clear()
p.removeMarker(None)
p.setLegendVisible(True)
p.addSeries(LinePlotSeries("PSSS Spectrum Average")) 

if RANGE_OFF is not None:    
    RANGE_FROM = energy_machine.read()-RANGE_OFF
    RANGE_TO = energy_machine.read()+RANGE_OFF

run("cpython/wrapper")

# get current camera ROIs and then set to max for scan
roi_min = psss_roi_min.read()
roi_max = psss_roi_max.read()
psss_roi_min.write(1)
psss_roi_max.write(2000)


#Scan and take data
class PSSS_energy(Writable):
    def write(self, value):
        #if not is_dry_run():
        psss_energy.write(value)
        exec_cpython("/ioc/modules/qt/PSSS_motion.py", args = ["-m1", "SARFE10-PSSS059"])
            # python / ioc / modules / qt / PSSS_motion.py - m1 SARFE10 - PSSS059
        time.sleep(1)
        print(value)

en = PSSS_energy()
en.alias = "energy"

av = create_averager(psss_spectrum_y, NUM_SHOTS, interval=-1, name="spectrum_average")
av_samples = av.samples
av_samples.alias = "spectrum_samples"


def after_read(record, scan):
    p.getSeries(0).setData(psss_spectrum_x.take(), record[av])
    p.setTitle("Energy = %1.3f" %(record[en]))
      
r = lscan(en, (av, av_samples), RANGE_FROM, RANGE_TO, STEPS, latency=0.0, after_read = after_read, save=False )
average, samples, energy_range =  r.getReadable(0), r.getReadable(1), r.getPositions(0)    
# return ROI to inital value
psss_roi_min.write(roi_min)
psss_roi_max.write(roi_max)

[amp, mean_val, sigma, offset],centre_line_out = fit_energy(RANGE_FROM, RANGE_TO, STEPS+1, NUM_SHOTS, samples)

if not (RANGE_FROM < mean_val < RANGE_TO or RANGE_TO < mean_val < RANGE_FROM):

    raise Exception ("Invalid fit mean: " + str(mean_val))  

    
measured_offset = energy_machine.read() - mean_val
#Set fitted energy
print "measured offset", measured_offset

en.write(mean_val)


p.clear()
p.setTitle("")
plot_gauss_fit(energy_range, centre_line_out, gauss_pars=(offset, amp, mean_val, sigma), p=PLOT, title = "Data")


set_return(mean_val)







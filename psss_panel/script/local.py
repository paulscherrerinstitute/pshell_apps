###################################################################################################
#  Deployment specific global definitions - executed after startup.py
###################################################################################################

from mathutils import estimate_peak_indexes, fit_gaussians, create_fit_point_list
from mathutils import fit_polynomial,fit_gaussian, fit_harmonic, calculate_peaks, fit_gaussian_offset
from mathutils import PolynomialFunction, Gaussian, HarmonicOscillator, GaussianOffset
from plotutils import plot_function, plot_data

import java.awt.Color as Color
run("psss/psss")

###################################################################################################
# DRY RUN
###################################################################################################

def set_dry_run(value):
    global dry_run    
    dry_run = value

def is_dry_run():
    if "dry_run" in globals():
        return True if dry_run else False
    return False
          

###################################################################################################
# Machine utilities
###################################################################################################

def is_laser_on():
    return (caget ("SIN-TIMAST-TMA:Beam-Las-Delay-Sel",'d') == 0 )

def is_timing_ok():
    return caget("SIN-TIMAST-TMA:SOS-COUNT-CHECK") == 0

def get_repetition_rate():
    return caget("SIN-TIMAST-TMA:Evt-15-Freq-I")


###################################################################################################
# Shortcut to maths utilities
###################################################################################################

def gfit(ydata, xdata = None):
    """
    Gaussian fit
    """
    if xdata is None:
        xdata = frange(0, len(ydata), 1)
    #ydata = to_list(ydata)
    #xdata = to_list(xdata)
    max_y= max(ydata)
    index_max = ydata.index(max_y)
    max_x= xdata[index_max]
    print "Max index:" + str(index_max),
    print " x:" + str(max_x),
    print " y:" + str(max_y)
    gaussians = fit_gaussians(ydata, xdata, [index_max,])
    (norm, mean, sigma) = gaussians[0]
    p = plot([ydata],["data"],[xdata], title="Fit" )[0]
    fitted_gaussian_function = Gaussian(norm, mean, sigma)
    scale_x = [float(min(xdata)), float(max(xdata)) ]
    points = max((len(xdata)+1), 100)
    resolution = (scale_x[1]-scale_x[0]) / points
    fit_y = []
    fit_x = frange(scale_x[0],scale_x[1],resolution, True)
    for x in fit_x:
        fit_y.append(fitted_gaussian_function.value(x))
    p.addSeries(LinePlotSeries("fit"))
    p.getSeries(1).setData(fit_x, fit_y)

    if abs(mean - xdata[index_max]) < ((scale_x[0] + scale_x[1])/2):
        print "Mean -> " +  str(mean)
        p.addMarker(mean, None, "Mean="+str(round(norm,2)), Color.MAGENTA.darker())
        return (norm, mean, sigma)
    else:
        p.addMarker(max_x, None, "Max="+str(round(max_x,2)), Color.GRAY)
        print "Invalid gaussian fit: " +  str(mean)
        return (None, None, None)


def hfit(ydata, xdata = None):
    """
    Harmonic fit
    """
    if xdata is None:
        xdata = frange(0, len(ydata), 1)

    max_y= max(ydata)
    index_max = ydata.index(max_y)
    max_x= xdata[index_max]

    start,end = min(xdata), max(xdata)
    (amplitude, angular_frequency, phase) = fit_harmonic(ydata, xdata)
    fitted_harmonic_function = HarmonicOscillator(amplitude, angular_frequency, phase)

    print "amplitude = ", amplitude
    print "angular frequency = ", angular_frequency
    print "phase = ", phase

    f = angular_frequency/ (2* math.pi)
    print "frequency = ", f

    resolution = 4.00 # 1.00
    fit_y = []
    for x in frange(start,end,resolution, True):
        fit_y.append(fitted_harmonic_function.value(x))
    fit_x = frange(start, end+resolution, resolution)

    p = plot(ydata,"data", xdata, title="HFit")[0]
    p.addSeries(LinePlotSeries("fit"))
    p.getSeries(1).setData(fit_x, fit_y)

    #m = (phase + math.pi)/ angular_frequency
    m = -phase / angular_frequency
    if (m<start):
        m+=(1.0/f)

    if start <=m <=end:
        print "fit = ", m
        p.addMarker(m, None, "Fit="+str(round(m ,2)), Color.MAGENTA.darker())
        return (amplitude, angular_frequency, phase, True, m, fit_x, fit_y)
    else:
        print "max = ",max_x
        p.addMarker(max_x, None, "Max="+str(round(max_x ,2)), Color.MAGENTA.darker())
        return (amplitude, angular_frequency, phase, False, max_x, fit_x, fit_y)



def plot_gauss_fit(xdata, ydata, gauss_pars=None, p=None, title = "Data"):
    if gauss_pars is None:
        gauss_pars= fit_gaussian_offset(ydata, xdata, None)
    (offset, amp, mean_value, sigma) = gauss_pars
    print "Gauss plot: ", (offset, amp, mean_value, sigma)
    fitted_gaussian_function = GaussianOffset(offset, amp, mean_value, abs(sigma))
    
    if p is None:
        p = plot(None, title=title)[0]
    p.clear()
    plot_data(p, ydata, title, xdata=xdata,  show_points = True, color=Color.BLUE)
    fit_range = frange(xdata[0],xdata[-1],float(xdata[1]-xdata[0])/100, True)
    plot_function(p, fitted_gaussian_function, "Gauss", fit_range, show_points=False, color=Color.RED)
    p.setLegendVisible(True)
    p.addMarker(mean_value, None, "Mean=" + str(round(mean_value,2)), Color.LIGHT_GRAY)
    return p,(amp, mean_value, sigma)

###################################################################################################
# Tools
###################################################################################################

def elog(title, message, attachments = [], author = None, category = "Info", domain = "", logbook = "Bernina", encoding=1):
    """
    Add entry to ELOG.
    """
    if author is None:
        author = "pshell" #get_context().user.name
    typ = "pshell"
    entry = ""

    cmd =           'G_CS_ELOG_add -l "' + logbook       + '" '
    cmd =     cmd + '-a "Author='        + author        + '" '
    cmd =     cmd + '-a "Type='          + typ           + '" '
    cmd =     cmd + '-a "Entry='         + entry         + '" '
    cmd =     cmd + '-a "Title='         + title         + '" '
    cmd =     cmd + '-a "Category='      + category      + '" '
    cmd =     cmd + '-a "Domain='        + domain        + '" '
    for attachment in attachments:
        cmd = cmd + '-f "'               + attachment    + '" '
    cmd =     cmd + '-n '                + str(encoding)
    cmd =     cmd + ' "'                 + message       + '"'
    #print cmd
    #os.system (cmd)
    #print os.popen(cmd).read()
    import subprocess
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
    (out, err) = proc.communicate()
    if (err is not None) and err!="":
        raise Exception(err)
    print out
    try:
        return int(out[out.find("ID=") +3 : ])
    except:
        print out
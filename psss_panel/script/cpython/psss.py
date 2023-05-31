import numpy as np
from scipy.optimize import curve_fit
import sys


def gaus(x, a, x0, sigma, offset):
    return offset + a * np.exp(-(x - x0) ** 2 / (2 * sigma ** 2))

#Return [amp, mean_val, sigma, offset]
def fit_energy(e_from, e_to, steps, num_shots, data):
    energy_range = np.linspace(e_from, e_to, steps)
    energy_range_fit = np.linspace(energy_range[0], energy_range[-1], len(energy_range)*10)
    centre_line_out = data[:,:,int(data.shape[2]/2)].mean(axis=1)
    try:
        popt,pcov = curve_fit(gaus,energy_range,centre_line_out,p0=[1,energy_range[np.argmax(centre_line_out)],energy_range.mean()*1e-3,1e3*num_shots])
    except:
        raise Exception('Fit failed: spectrum might not be near scan range center \n' + str(sys.exc_info()[1]))
        #print('Fit failed: spectrum might not be near scan range center')
        #return None
    max_ind = np.argmax(centre_line_out)
    max_photon_energy=energy_range[max_ind]
    print(max_photon_energy)
    return popt, centre_line_out


#Return [amp, mean_val, sigma, offset]
def fit_crystal_height(xstal_from, xstal_to, steps, data):
    xstal_range = np.linspace(xstal_from, xstal_to, steps)
    projection = data.mean(axis=1).mean(axis=1)
    offset = np.mean(projection[0:100])
    signal_centre = xstal_range[np.argmax(projection)]
    xstal_range_fit = np.linspace(xstal_range[0], xstal_range[-1], len(xstal_range)*10)
    try:
        popt,pcov = curve_fit(gaus,xstal_range,projection,p0=[100,signal_centre,-0.2,offset])
    except:
        raise Exception('Fit failed: spectrum might not be near scan range center \n' + str(sys.exc_info()[1]))
        #print('Fit failed: spectrum might not be near scan range center')
        #return None
    return popt, projection


def get_signal_centre(data, data_range):
    projection = data.mean(axis=1).mean(axis=1)
    signal_centre = data_range[np.argmax(projection)]
    return signal_centre, projection

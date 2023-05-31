import numpy as np
import matplotlib.pyplot as plt


def gaus(x, a, x0, sigma, offset):
    return offset + a * np.exp(-(x - x0) ** 2 / (2 * sigma ** 2))
    
def plot_energy(E_from, E_to, steps, Scan_spec, popt, measured_offset):
    Energy_range = np.linspace(E_from, E_to, steps)
    centre_line_out = Scan_spec[:,:,int(Scan_spec.shape[2]/2)].mean(axis=1)
    Energy_range_fit = np.linspace(Energy_range[0], Energy_range[-1], len(Energy_range)*10)

    
    plt.figure(figsize=[10,5])
    plt.subplot(121)
    plt.title('PSSS scan of set photon energy')
    plt.pcolormesh(np.arange(0,Scan_spec.shape[2]), Energy_range, Scan_spec.mean(axis=1),cmap='CMRmap')
    plt.vlines(int(Scan_spec.shape[2]/2), Energy_range[0], Energy_range[-1],linestyles='--', colors='orange')
    plt.xlim([0,Scan_spec.shape[2]])
    plt.xlabel('Camera pixel')
    plt.ylabel('Set PSSS energy [eV] \n SARFE10-PSSS059:ENERGY')
    
    plt.subplot(122)
    plt.title('At camera centre pixel %1i \nCalibrated energy = %.1f [eV]\n Offset from machine = %.1f [eV]'%(int(Scan_spec.shape[2]/2),popt[1],measured_offset))
    plt.plot(centre_line_out,Energy_range,linewidth = 2, color = 'orange',label ='measured')
    try:
        plt.plot(gaus(Energy_range_fit,*popt),Energy_range_fit,'r:',label='fit')
    except:
        pass
    plt.xticks([])
    plt.legend()
    plt.grid(True)  
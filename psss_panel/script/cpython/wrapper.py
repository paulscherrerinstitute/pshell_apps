from jeputils import *

RELOAD_CPYTHON = not App.isDetached()

def fit_energy(e_from, e_to, steps, num_shots, data):
    data = to_array(data, 'd')    
    dims = [len(data), len(data[0]), len(data[0][0])]
    data = Convert.flatten(data)
    arr = to_npa(data, dims)
    popt, centre_line_out =  call_jep("cpython/psss", "fit_energy", [e_from, e_to, steps, num_shots, arr], reload=RELOAD_CPYTHON)
    return popt.getData(), centre_line_out.getData()

def fit_crystal_height(xstal_from, xstal_to, steps, data):
    data = to_array(data, 'd')    
    dims = [len(data), len(data[0]), len(data[0][0])]
    data = Convert.flatten(data)
    arr = to_npa(data, dims)
    popt, projection =  call_jep("cpython/psss", "fit_crystal_height", [xstal_from, xstal_to, steps, arr], reload=RELOAD_CPYTHON)
    return popt.getData(), projection.getData()

def get_signal_centre(data, data_range):
    data = to_array(data, 'd')    
    dims = [len(data), len(data[0]), len(data[0][0])]
    data = Convert.flatten(data)   
    arr = to_npa(data, dims)
    data_range = to_npa(to_array(data_range, 'd'))
    signal_centre, projection =  call_jep("cpython/psss", "get_signal_centre", [arr, data_range], reload=RELOAD_CPYTHON)
    return signal_centre, projection.getData()

def plot_energy(e_from, e_to, steps, data, popt, measured_offset):
    data = to_array(data, 'd')    
    dims = [len(data), len(data[0]), len(data[0][0])]
    data = Convert.flatten(data)
    arr = to_npa(data, dims)
    ret =  call_jep("cpython/psss_plot", "plot_energy", [e_from, e_to, steps, arr, popt, measured_offset], reload=RELOAD_CPYTHON)
    return ret

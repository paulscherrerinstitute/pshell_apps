import math
import sys, traceback          
from mathutils import fit_polynomial, PolynomialFunction, calculate_peaks 
from plotutils import plot_line, plot_function
from ch.psi.pshell.swing.Shell import getColorStdout
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation as PearsonsCorrelation
try:
    import ch.psi.pshell.bs.StreamMerger as StreamMerger
except:
    merge_camera_stream = False

TYPE_CHANNEL = 0
TYPE_STREAM = 1
TYPE_CAMERA= 2

INVOKE_PLOT_UPDATES = True

if get_exec_pars().source == CommandSource.ui:
    #dx = "SINEG01-DICT215:B1_CHARGE"            ; dxtype = TYPE_CHANNEL
    dx=get_beam_ok_channel(1)                   ; dxtype = TYPE_STREAM
    #dx = "SLG-LCAM-C042 x_rms"                  ; dxtype = TYPE_CAMERA   
    
    #dy = "SINDI01-RLLE-STA:SLAVE1-DLTIMER"      ; dytype = TYPE_CHANNEL    
    dy = "SATES31-CAMS187-RIXS1_sp1 intensity"  ; dytype = TYPE_CAMERA
    #dy=get_beam_ok_channel(2)                   ; dytype = TYPE_STREAM
    
    interval = 0.01
    window = 40
    p = plot(None)[0]    
    linear_fit = True
    quadratic_fit = True
    merge_camera_stream = True
print dx, dxtype
print dy, dytype

corr = None
pars_lin = None 
pars_quad = None
pos_peak = None
neg_peak = None
stop_exec = None


bs = TYPE_STREAM in [dxtype, dytype]

for s in p.getAllSeries():
    p.removeSeries(s)    

_stream = None
_camname = None

instances = []

def _get_device(d, type):
    global _stream, _camname
    egu = None
    if isinstance(d, basestring):    
        name = d.strip()
        d = None
        try:
            d = get_device(name) 
            if d is None: 
                d = eval(name)
            #print name
            if d is not None:
                if not isinstance(r, Device): 
                    d = None
                else:
                    try:
                        egu = d.unit
                    except:
                        pass                            
        except:
            pass
        if d is None:            
            offset = 0            
            if type==TYPE_STREAM:
                if " " in name:
                    tokens = name.split(" ")
                    name = tokens[0]
                    offset = int(tokens[1])
                if _stream == None:
                    _stream = Stream("corr_stream", dispatcher)
                    instances.append(_stream)
                #d = _stream.addScalar(name, name, int(interval*100), offset)
                _stream.addScalar(name, name, int(interval*100), offset)
                d=name
            elif type==TYPE_CHANNEL:
                d =  Channel(name)    
                d.set_monitored(True)
            elif  type==TYPE_CAMERA:
                tokens = name.split(" ")
                _camname = tokens[0]
                field = tokens[1]
                return field, ""
            else:
                raise Exception("Invalid type: " + str(type))
            if not isinstance(d, basestring):    
                instances.append(d)
            try:
                if type==TYPE_CHANNEL:
                    egu = caget(name+".EGU",'s')
            except:
                pass            
    else:
        try:
            egu = d.unit
        except:
            pass                    
    return d, egu

dx, egux = _get_device(dx, dxtype)
dy, eguy = _get_device(dy, dytype)

p.getAxis(p.AxisId.X).setLabel(egux)
p.getAxis(p.AxisId.Y).setLabel(eguy)

merge = False
try:
    if _stream != None:
        _stream.initialize()
        _stream.start(True)
    if _camname != None:
        shared = _camname.endswith("_sp1")
        print "Camera: " , _camname, " shared: ", shared
        cam_server.start(_camname, shared )    
        
        cam_server.stream.waitCacheChange(10000);
        _camera_stream = cam_server.stream

        if merge_camera_stream:
            merge =  ((dxtype==TYPE_CAMERA) and (dytype == TYPE_STREAM)) or ((dxtype==TYPE_STREAM) and (dytype == TYPE_CAMERA))
            if merge:
                print "Merging dispatcher and camera streams"
                _merger = StreamMerger("stream", _stream, cam_server.stream)
                _merger.monitored=True
                _merger.start()
                _camera_stream.setBufferCapacity(500)
                _stream.setBufferCapacity(500)
                instances.append(_merger)
                _merger.waitCacheChange(5000)
                #print _merger.readables
                _camera_stream=_merger
                _stream = _merger                
        
        if  dxtype==TYPE_CAMERA:
            dx=_camera_stream.getChild(dx)
        if  dytype==TYPE_CAMERA:
            dy=_camera_stream.getChild(dy)

    if  dxtype==TYPE_STREAM:
        dx=_stream.getChild(dx)
    if  dytype==TYPE_STREAM:
        dy=_stream.getChild(dy)                   
        
    p.addSeries(LinePlotSeries("Data")) 
    sd=p.getSeries(0)
    sd.setLinesVisible(False)        
    sd.setPointSize(4)
    
    if get_exec_pars().source == CommandSource.ui:
        if globals().has_key("marker"):
            p.removeMarker(marker)    
        if globals().has_key("peak_marker"):
            p.removeMarker(peak_marker)    
        marker=None
        peak_marker=None
                
    while(True):
        #Sample and plot data
        if bs == True:
            _stream.waitCacheChange(10000)
            #bsdata = list(_stream.take().values())
        x=dx.read() 
        y=dy.read()
            
        def update():
            global marker, peak_marker, corr, pars_lin, pars_quad, pos_peak, neg_peak, stop_exec
            sd.appendData(x, y)
            if len(sd.x) > window:
                #Remove First Element
                sd.token.remove(0)    
            ax = sd.x
            ay = sd.y
            if len(ax)>2:
                x1, x2 = min(ax), max(ax)  
                res = (x2-x1)/100     
                if x1!=x2:
                    #Display correlation
                    corr= PearsonsCorrelation().correlation(to_array(ax,'d'), to_array(ay,'d'))        
                    s = "Correlation=" + str(round(corr,4))
                    #print s
                    if get_exec_pars().source == CommandSource.ui:
                        if marker is not None: 
                            p.removeMarker(marker)
                        marker = p.addMarker(x2+res, p.AxisId.X, s,  p.getBackground())
                        marker.setLabelPaint(getColorStdout())
                    if linear_fit:
                        #Calculate, print and plot linear fit
                        pars_lin = (a0,a1) = fit_polynomial(ay, ax, 1)
                        #print "Fit lin  a1:" , a1, " a0:",a0        
                        y1 = poly(x1, pars_lin)
                        y2 = poly(x2, pars_lin)
                        plot_line(p, x1, y1, x2, y2, width = 2, color = Color.BLUE, name = "Fit Linear")
                    if quadratic_fit:
                        #Calculate, print and plot quadratic fit
                        pars_quad = (a0,a1,a2) = fit_polynomial(ay, ax, 2)
                        #print "Fit quad a2:" , a2, "a1:" , a1, " a0:",a0        
                        fitted_quad_function = PolynomialFunction(pars_quad)
                        ax = frange(x1, x2, res, True)
                        plot_function(p, fitted_quad_function, "Fit Quadratic", ax, color=Color.GREEN)
    
                        peak = None                    
                        peaks = calculate_peaks(fitted_quad_function, x1, x2, positive=True)
                        if len(peaks)>0:
                            peak = peaks[0]
                            pos_peak = str(round(peak,4))
                            peak_str = "Positive peak: " + pos_peak
                        else:
                            peaks = calculate_peaks(fitted_quad_function, x1, x2, positive=False)
                            if len(peaks)>0:
                                peak = peaks[0]
                                neg_peak = str(round(peak,4))
                                peak_str = "Negative peak: " + neg_peak
                            else:
                                pos_peak = neg_peak = None
                        if get_exec_pars().source == CommandSource.ui:
                            if peak_marker is not None: 
                                p.removeMarker(peak_marker)
                            if peak is not None: 
                                peak_marker = p.addMarker(peak, p.AxisId.X, peak_str,  Color(0,128,0))
        if INVOKE_PLOT_UPDATES:
            invoke(update, False)
        else:
            update()
        
        if stop_exec == True:
            stop_exec = False
            break
        if bs != True:
            time.sleep(interval)    
except KeyboardInterrupt:
    pass                   
finally:
    for dev in instances:
        try:
            dev.close()
        except:
            pass
    if _camname != None:
        cam_server.stop()    
            

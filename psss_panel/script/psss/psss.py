import org.jfree.ui.RectangleAnchor as RectangleAnchor
import org.jfree.ui.TextAnchor as TextAnchor
import ch.psi.pshell.imaging.Overlay as Overlay
import ch.psi.pshell.plot.RangeSelectionPlot as RangeSelectionPlot
from collections import deque

PSSS_CAMERA_NAME = "SARFE10-PSSS059";

def integrate_arrays(arrays):
    if arrays is None or (len(arrays)==0):
        return None
    ret = arrays[0]
    for a in arrays[1:]:
        ret=arradd(ret, a)
    return ret

def average_arrays(arrays):
    ret = integrate_arrays(arrays)
    if ret is not None:
        s=len(arrays)        
        ret = [x/s for x in ret]      
    return ret 


def get_psss_data(average=1):
    ax,ay,ac,af=[],[],[],[]
    x = psss_spectrum_x.take()
    for i in range(average):
        y =  psss_spectrum_y.take()
        center,fwhm = psss_center.take(), psss_fwhm.take()  
        if average==1: 
            return x,y,center,fwhm 
        #ax.append(x)  
        ay.append(y)
        ac.append(center)
        af.append(fwhm)        
        if i < (average-1):                        
            psss_spectrum_y.waitCacheChange(2000)
            #psss_center.waitCacheChange(1)
            #psss_fwhm.waitCacheChange(1)                    
#x=average_arrays(ax)
    y=average_arrays(ay)
    center=mean(ac)
    fwhm=mean(af)
    return x,y,center,fwhm 

def plot_psss(p, h=None, average = None):
    """
    if len(p.getMarkers())==0:
        m1=p.addMarker(0,None,"",Color.WHITE)
        m2=p.addMarker(0,None,"",Color.WHITE)
        m2.setLabelAnchor(RectangleAnchor.TOP) 
    else:
        m1,m2 = p.getMarkers()
    """

    #Manipulate axis (use PSSS_PLOT for the global object):
    #p.getAxis(LinePlot.AxisId.X).

     # Setup queues
    if p.getNumberOfSeries()==0: 
        center_queue = deque(maxlen=100)
        fwhm_queue = deque(maxlen=100)
    
    # Setup figures
    
    if p.getNumberOfSeries()==0:
        p.addSeries(LinePlotSeries("spectrum"))
        p.addSeries(LinePlotSeries("average"))
        p.setLegendVisible(True)
        p.getAxis(LinePlot.AxisId.X)
        p.getAxis(LinePlot.AxisId.X).setLabel("Energy [eV]")
        p.getAxis(LinePlot.AxisId.Y).setLabel("Sum counts")
    if len(p.getMarkers())==0:
        paint = RangeSelectionPlot().getSelectionColor() #p.chart.getBackgroundPaint()
        m=p.addIntervalMarker(0,0, None,"", paint)    
        m.setLabelAnchor(RectangleAnchor.BOTTOM) 
        m.alpha=0.2
        m.setLabelPaint(Color.WHITE)
    else:
        m = p.getMarkers()[0]
   

    x,y, = psss_spectrum_x.take(), psss_spectrum_y.take()
    # update spectral plot
    if (x is None) or (y is None):
        p.getSeries(0).clear()
    else:
        p.getSeries(0).setData(x,y) 
    if (x is None) or (y is None):
        p.getSeries(0).clear()
    else:
        p.getSeries(0).setData(x,y)           

    if average is not None:
        print "Average: ", average
        x,y, center,fwhm  = get_psss_data(average)
    else:
        y = psss_spectrum_y_average.take()
        center = psss_center_average.take()            
        fwhm  = psss_fwhm_average.take()
    
    if (x is None) or (y is None):
        p.getSeries(1).clear()
    else:
        p.getSeries(1).setData(x,y)  
        
           
    if (center!= None) and (fwhm!=None):
        center=center.doubleValue()
        fwhm=fwhm.doubleValue()
        m.startValue, m.endValue =  center - fwhm/2, center + fwhm/2
        m.label = str(center)     
        
        if h:
            if h.getNumberOfSeries()==0:
                    h.addSeries(TimePlotSeries("centre"))
                    h.addSeries(TimePlotSeries("Energy spread SS",2))
                    h.addSeries(TimePlotSeries("Energy spread cum avg",2))
                    h.setLegendVisible(True)
                    h.setTimeAxisLabel("")
                    h.getAxis(Timeplot.AxisId.Y1).setLabel("Central energy [eV]")              
            per_mil = (fwhm/center)*1e3
            per_mil_avg = psss_fwhm_avg.take()
            h.getSeries(0).appendData(center)
            h.getSeries(1).appendData(per_mil)
            h.getSeries(2).appendData(per_mil_avg)
    return center,fwhm

ovmin, ovmax, ovavg = None, None, None

def update_psss_image(renderer):
    global ovmin, ovmax
    #if ovmin: ovmin.update(Point(0,psss_roi_min.take()))
    #if ovmax: ovmax.update(Point(0,psss_roi_max.take()))
    
    
    width=psss_spectrum_x.size
    if ovmin: ovmin.update(Point(0,psss_roi_min.take()), Point(width, psss_roi_min.take()))
    if ovmax: ovmax.update(Point(0,psss_roi_max.take()), Point(width, psss_roi_max.take()))
    try:
        data = renderer.data
        av = "%1.2f" %(data.integrate(False)/data.width/data.height)
    except:
        av = ""
    if ovavg: ovavg.update(av)

def enable_psss_image(enabled, renderer):
    global ovmin, ovmax, ovavg    
    try:
        if (enabled):
            #Start or connect to ScreenPanel pipeline
            renderer.setDevice(cam_server)       
            renderer.setProfile(renderer.Profile.Both)
            renderer.setShowProfileLimits(False)

            #Changing colormap
            #print Colormap.values() #Check values
            cam_server.config.colormap=Colormap.Temperature
            
            
            cam_server.start(PSSS_CAMERA_NAME + "_sp", PSSS_CAMERA_NAME + "_sp1")       
            #ovmin, ovmax= Overlays.Crosshairs(renderer.getPenMarker(), Dimension(-1,1)), \
            #              Overlays.Crosshairs(renderer.getPenMarker(), Dimension(-1,1))
            ovmin, ovmax= Overlays.Line(renderer.getPenMarker()), Overlays.Line(renderer.getPenMarker())           
             
            ovavg = Overlays.Text(Pen(java.awt.Color.GREEN.darker()), "", \
                    java.awt.Font("Verdana", java.awt.Font.PLAIN, 12), java.awt.Point(-50,20))
            ovavg.fixed=True
            ovavg.anchor=Overlay.ANCHOR_IMAGE_TOP_RIGHT
            renderer.addOverlays([ovmin, ovmax, ovavg])
            update_psss_image(renderer)
        else:
            ovmin, ovmax, ovavg = None, None, None
            renderer.setDevice(None)
            renderer.clearOverlays()
            cam_server.stop()
    except:
        log(sys.exc_info()[1])


def get_psss_averaging():
    return psss_spectrum_y_average.config.measures    

def set_psss_averaging(measures):
    psss_spectrum_y_average.config.measures=measures        
    psss_center_average.config.measures=measures        
    psss_fwhm_average.config.measures=measures        
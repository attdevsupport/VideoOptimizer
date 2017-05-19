package com.att.aro.ui.view.video;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.impl.CollectOptionsReaderImpl;
//import com.att.aro.core.peripheral.impl.CollectOptionsReaderImpl;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;
import com.att.aro.view.images.Images;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_state_t;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class VlcjPlayer implements IVideoPlayer {
	
    private EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private MediaPlayer player;
	private JFrame frame;
    private JButton playButton;	    
    private JSlider slider; 
    private JLabel playtimeLabel;
    private DiagnosticsTab diagnosticsTab;   
    private double videoOffset;
	private SharedAttributesProcesses aroView;
	private double duration;
	private String lastPlayedMRL;
	private libvlc_state_t lastLibVLCState;

	// TODO: Move these to a config file
	private static final String WIN_VLCLIBPATH = "C:\\Program Files\\VideoLAN\\VLC";
    private static final String LINUX_VLCLIBPATH = "/usr/lib/vlc";
	 /*
	  *  25 is frame rate, we multiply video duration by it to be the max 
	  *  value of the slider of the player so that we can see every frame 
	  *  when we increment the slider in the slider dialog box.
	  */
    private static final int FRAME_RATE = 25;
    
    private static ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);
    private IExternalProcessRunner extRunner = ContextAware.getAROConfigContext().getBean(IExternalProcessRunner.class);
    private IFileManager fileManager = ContextAware.getAROConfigContext().getBean(IFileManager.class);
    
    private static final String[] MEDIA_PLAYER_FACTORY_ARGS = {
    		"--video-title=vlcj video output",
    		"--no-snapshot-preview",
    		"--quiet-synchro",
    		"--sub-filter=logo:marq",
    		"--intf=dummy",
    		"--no-overlay"
    };
    
    public VlcjPlayer(SharedAttributesProcesses aroView){
    	this.aroView = aroView;
    }
    
    private void setUpPlayer(int xPosition, int yPosition, int frameWidth, int frameHeight) {

    	String title = MessageFormat.format(ResourceBundleHelper.getMessageString("aro.videoTitle"),  
				ApplicationConfig.getInstance().getAppShortName());    	
    	
        frame = new JFrame(title);
        
        if(isLandscape()) {
        	xPosition=1200;
        	frameWidth = 512;
        	frameHeight = 288;
        }
        frame.setBounds(xPosition, yPosition, frameWidth, frameHeight);
		frame.setIconImage(Images.ICON.getImage());
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            	aroView.updateVideoPlayerSelected(false);
            }
        });

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());

        if (!createMediaPlayerComponent()) {
        	return;
    	}
        
        player = mediaPlayerComponent.getMediaPlayer();
        player.setRepeat(true);
        
        contentPane.add(mediaPlayerComponent, BorderLayout.CENTER);

        JPanel controlsPane = new JPanel();
        playButton = new JButton(">");
        controlsPane.add(playButton);
        /*
         * In Linux, the slider's position value is displayed on top of the slider. 
         * This line is to disable it.
         */
        UIManager.put("Slider.paintValue", false);
        slider = new JSlider(); 
        slider.setValue(0);
        controlsPane.add(slider);
        playtimeLabel = new JLabel("00:00/00:00");
        controlsPane.add(playtimeLabel);
        contentPane.add(controlsPane, BorderLayout.SOUTH);

        player.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {  
        	
        	@Override
        	public void newMedia(MediaPlayer player) {
    			diagnosticsTab.setGraphPanelClicked(false);
        	}

        	@Override
        	public void mediaChanged(MediaPlayer player, libvlc_media_t media, String mrl) {
        		slider.setValue(0);
        		playButton.setText(">");
        		playtimeLabel.setText(getZeroPlaytimeDisplay());   
        	}       	

        	@Override
        	public void playing(MediaPlayer player) {
        		if(player.mrl() != null && !(player.mrl().isEmpty()) && player.mrl().equalsIgnoreCase(lastPlayedMRL) && lastLibVLCState == libvlc_state_t.libvlc_Ended ){
        			player.setPosition(0);
        			player.pause();
        		}
        		lastLibVLCState = player.getMediaPlayerState();
        	}
        	
        	@Override
        	public void positionChanged(MediaPlayer player, final float newPosition) {
        		// newPlaytime is in seconds
        		final float newPlaytime = Math.round(newPosition*duration);  // newPosition is % between 0 and 1     		
        		SwingUtilities.invokeLater(new Runnable() {       			
        			public void run() {
        				slider.setValue(Math.round(newPlaytime*FRAME_RATE));
        				playtimeLabel.setText(formatPlaytimeDisplay(newPlaytime));
        			}
        		});
        	}
        	
        	@Override
        	public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
        		SwingUtilities.invokeLater(new Runnable() {
        			public void run() {
        				double crosshairValue = (double) newTime/1000 + videoOffset;   
    					if (crosshairValue < 0) {
    						crosshairValue = 0.0;
    					}
    					diagnosticsTab.setTimeLineLinkedComponents(crosshairValue, true);
        			}
        		});    		
        	}
        	
        	@Override
        	public void finished(MediaPlayer player) {     
        		lastLibVLCState = player.getMediaState();
        		lastPlayedMRL = player.mrl();
        		playButton.setText(">");
        		slider.setValue(0);
        		playtimeLabel.setText(getZeroPlaytimeDisplay());
        	}
        });    

        playButton.addActionListener(new ActionListener() {
          
        	public void actionPerformed(ActionEvent e) {         
        		if (player.isPlaying()) {
        			player.pause();
            		playButton.setText(">");
            	}
        		// Only if there's a video loaded
        		else if (player.mrl() != null && !player.mrl().isEmpty()) {
        			player.play();
        			playButton.setText("||");
            	}
            }
        });    
                
        slider.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				// Do something only if there's a video loaded
        		if (player.mrl() == null || player.mrl().isEmpty()) {
        			return;
    			}
	 
    			int sliderValue = ((JSlider) e.getSource()).getValue()/FRAME_RATE;
    			// Divide by frame rate at the end (vs earlier) to get a higher precision
			    float pos = (float) (((JSlider) e.getSource()).getValue()/duration)/FRAME_RATE;
			    
			    player.setPosition(pos); 

			    playtimeLabel.setText(formatPlaytimeDisplay(sliderValue));
		    
			    if (!player.isPlaying() && !slider.getValueIsAdjusting()) {
			    	diagnosticsTab.setTimeLineLinkedComponents(sliderValue + videoOffset, true);
			    }				
			}

			@Override
			public void mouseMoved(MouseEvent arg0) {}
        	
        });

        frame.setContentPane(contentPane);
        frame.setVisible(true);  	
    }
    
    private boolean isLandscape() {

    	boolean isOrientationLandscape = false;
    	 
		CollectOptionsReaderImpl collectOptionsReaderImpl = (CollectOptionsReaderImpl) ContextAware
				.getAROConfigContext().getBean("collectOptionsReaderImpl");

		if (TraceDataConst.UserEvent.KEY_LANDSCAPE.equalsIgnoreCase(collectOptionsReaderImpl.getOrientation())) {
			isOrientationLandscape = true;
		}
		
		return isOrientationLandscape;

	}

	private boolean createMediaPlayerComponent() {
	
		try {
	    	/*
	    	 * There is an Intel X graphics driver bug, our test Linux machine 
	    	 * has Intel HD 520 hraphics card and is affected 
	    	 * (https://bugs.launchpad.net/ubuntu/+source/xserver-xorg-video-intel/+bug/1537053). 
	    	 * One solution is to disable the HW Acceleration "Overlay" option. 
	    	 */
	        if (disableOverlay()) {
	        	
	        	 mediaPlayerComponent = new EmbeddedMediaPlayerComponent() {
	             	protected String[] onGetMediaPlayerFactoryArgs() {
	             		return MEDIA_PLAYER_FACTORY_ARGS;
	             	}
	             };
	       
	        } else {
	        	
	        	mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
	        }
	        
        } catch (RuntimeException e) {
        	
			showVlcjError();        	     	
        	return false;
        }
		
		return true;
	}

    private boolean disableOverlay() {
    	
    	if (!Util.isLinuxOS()) {
    		return false;
    	}
    	
		String cmd = "lspci | grep VGA";
		String line = extRunner.executeCmd(cmd);
	
		logger.debug("GPU info:" + line);
		
		if (line.contains("Sky Lake")) {
			return true;
		}
		
		return false;
    }
    
    // if video duration is an hour or longer
    private boolean durationHourOrAbove() {
    	 return duration/3600 >= 1;
    }
    
    private String getZeroPlaytimeDisplay() {    	
    	String duration = getDurationDisplay();
    	return durationHourOrAbove()? "00:00:00/" + duration: "00:00/" + duration;
    }
    
    // Gets the video duration part (the total video duration) for the playtime label
    private String getDurationDisplay() {
    	
    	double durationHr = duration/3600;
    	double durationMin = duration/60;
    	double durationSec = duration - ((int) durationHr)*3600 - ((int) durationMin)*60;
    	
    	String durationDisplayOverHr = String.format("%d:%02d:%02d",
				(int) durationHr, (int) durationMin, (int) durationSec);
    	
    	String durationDisplayUnderHr = String.format("%02d:%02d", 
				(int) durationMin, (int) durationSec);
    			
    	return durationHourOrAbove()? durationDisplayOverHr:durationDisplayUnderHr;
    }
    
    // playtime expected to be in seconds
    private String formatPlaytimeDisplay(double playtime) {

    	double playtimeHr = playtime/3600;
    	double playtimeMin = playtime/60;
    	double playtimeSec = playtime - ((int) playtimeHr)*3600 - ((int) playtimeMin)*60;
    	
    	String durationDisplay = getDurationDisplay();
    	
    	String timeDisplayOverHr = String.format("%d:%02d:%02d/", (int) playtimeHr, 
    			(int) playtimeMin, (int) playtimeSec) + durationDisplay;
    	
    	String timeDisplayUnderHr = String.format("%02d:%02d/", (int) playtimeMin,
    			(int) playtimeSec) + durationDisplay;
    	
    	return durationHourOrAbove()? timeDisplayOverHr:timeDisplayUnderHr;
    }
    
	@Override
	public void setVisibility(boolean visible) {
		if (frame != null) {
			frame.setVisible(visible);
		}
	}

	@Override
	public void setAroAdvancedTab(DiagnosticsTab diagnosticsTab) {
		this.diagnosticsTab = diagnosticsTab;		
	}

	@Override
	public double getVideoOffset() {
		return videoOffset;
	}

	@Override
	public double getMediaTime() {	
		
		// Example: vlc player is not found on OS
		if (player == null) {
			logger.debug("null player");
			return 0;
		}
		
		long timeInMilliSeconds = player.getTime();
		return (double) timeInMilliSeconds/1000;
	}

	// hairlineTime expected to be in seconds
	@Override
	public void setMediaTime(final double hairlineTime) {
        
		// Example: vlc player is not found on OS
		if (player == null) {
			logger.debug("null player");
			return;
		}
		
		double videoTime = hairlineTime - this.videoOffset;

		if ((videoTime < 0.0)) {	
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (diagnosticsTab != null) {
						diagnosticsTab.setTimeLineLinkedComponents(hairlineTime, false);
					}
				}
			});
		}
		
		player.setTime(Math.round(videoTime * 1000));
		slider.setValue((int) (videoTime*FRAME_RATE));
	} 

	@Override
	public double getDuration() {
		
		// Example: vlc player is not found on OS
		if (player == null) {
			logger.debug("null player");
			return 0;
		}
		
		// Convert milliseconds to seconds
		return (double) player.getLength()/1000;
	}
	
	@Override
	public boolean isPlaying() {
		
		// Example: vlc player is not found on OS
		if (player == null) {
			logger.debug("null player");
			return false;
		}
		
		return player.isPlaying();
	}

	@Override
	public void launchPlayer(final int xPosition, final int yPosition, final int frameWidth, final int frameHeight) {		
		
		if (!vlcjDependenciesPresent()) {
			return;
		}
		
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setUpPlayer(xPosition, yPosition, frameWidth, frameHeight);
            }
        });     
	}

	private boolean vlcjDependenciesPresent() {
	
		try {
	
			if (Util.isWindowsOS()) {
				
				if (fileManager.directoryExist(WIN_VLCLIBPATH)) {
					
					NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), WIN_VLCLIBPATH);
					Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
			 
				} else {
					
					showVlcjError();
					return false;
				}
			
			} else if (Util.isLinuxOS()) {
				
				if (fileManager.directoryExist(LINUX_VLCLIBPATH)) {
					
					NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), LINUX_VLCLIBPATH);
					Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
	
				} else {
				
					showVlcjError();
					return false;
				}
			}
		
		} catch (UnsatisfiedLinkError e) {
		
			showVlcjError();
			return false;		
		}

		return true;
	}
	
	private void showVlcjError() {

			MessageDialogFactory.showMessageDialog(null,
				ResourceBundleHelper.getMessageString("video.error.vlcj"),
				ResourceBundleHelper.getMessageString("menu.error.title"),
				JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void clear() {
		frame.setVisible(false);
		player.stop();
		try {
			player.startMedia(null);
		} catch (Exception e) {
			//Exception expected - no other way to clear the video without disposing the player
		}
		frame.dispose();
		videoOffset = 0.0;
	}

	@Override
	public VideoPlayerType getPlayerType() {
		return VideoPlayerType.MP4_VLCJ;
	}

	@Override
	public void loadVideo(AbstractTraceResult traceResult) {
        
		// Example: vlc player is not found on OS
		if (player == null) {
			logger.debug("null player");
			return;
		}
		
		/*
		 * Without calling stop(), the player view will still 
		 * show the image from the previous video after calling 
		 * prepareMedia() on the new video.
		 */
		player.stop();
		
		String movVideoPath = traceResult.getTraceDirectory() + Util.FILE_SEPARATOR 
				+ ResourceBundleHelper.getMessageString("video.videoDisplayFile");
		String mp4VideoPath = traceResult.getTraceDirectory() + Util.FILE_SEPARATOR 
				+ ResourceBundleHelper.getMessageString("video.videoFileOnDevice");
		String videoPath = new File(mp4VideoPath).exists()? mp4VideoPath : movVideoPath;

		/* 
		 * start then pause in order for graph panel and 
		 * video player to be able to go in sync before 
		 * user starts playing the video
		 */
		player.startMedia(videoPath);
		player.pause(); 
		
		duration = getDuration();

		double videoStartTime = traceResult.getVideoStartTime();
        this.videoOffset = videoStartTime > 0.0 ? videoStartTime - ((double) traceResult.getTraceDateTime().getTime() / 1000) : 0.0;
        if (Math.abs(videoOffset) >= duration) {
        	videoOffset = 0;
        }    

		/* 
		 * Adjust slider's max for each new video for more 
		 * accurate and custom fitting to the timestamp.
		 */
		slider.setMaximum((int) (duration*FRAME_RATE)); 
		playtimeLabel.setText(getZeroPlaytimeDisplay());
        if(isLandscape()) {
        	frame.setBounds(Toolkit.getDefaultToolkit().getScreenSize().width-512, 0, 512, 288);
        } else {
        	frame.setBounds(Toolkit.getDefaultToolkit().getScreenSize().width-350, 0, 350, 600);
        }
	}

	@Override
	public void notifyLauncher(boolean enabled) {
		aroView.updateVideoPlayerSelected(enabled);
	}
}
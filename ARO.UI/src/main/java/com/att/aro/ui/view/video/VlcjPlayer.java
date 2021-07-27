/*
 *  Copyright 2018 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.ui.view.video;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;
import com.att.aro.view.images.Images;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.component.MediaPlayerComponent;
import uk.co.caprica.vlcj.player.component.MediaPlayerSpecs;

public class VlcjPlayer implements IVideoPlayer {

    private static final Logger LOGGER = LogManager.getLogger(VlcjPlayer.class);
    private static final String MEDIA_PLAYER_CARD = "MEDIA_CARD";
    private static final String DUMMY_PLAYER_CARD = "DUMMY_CARD";

    private MediaPlayerComponent mediaPlayerComponent;
    private MediaPlayer player;
    private JFrame frame;
    private JPanel contentPane;
    private JButton playButton;
    private JSlider slider;
    private JLabel playtimeLabel;
    private DiagnosticsTab diagnosticsTab;
    private double videoOffset;
    private SharedAttributesProcesses aroView;
    private double duration;
    private State mediaPlayerStatus;
    private JPanel dummyMediaComponentCard;
    private JPanel dummyMediaComponentCardPanel;
    private String currentCard;
    private String videoPath;

    private static final String[] MEDIA_PLAYER_FACTORY_ARGS = {
            "--video-title=vlcj video output",
            "--no-snapshot-preview",
            "--quiet-synchro",
            "--sub-filter=logo:marq",
            "--intf=dummy",
            "--no-drop-late-frames",
            "--no-skip-frames"
    };

    /*
     * We multiply video duration by it to be the max value of the slider of the
     * player so that we can see every frame when we increment the slider in the
     * slider dialog box.
     */
    private static final int FRAME_RATE = VideoUtil.FRAME_RATE;


    public VlcjPlayer(SharedAttributesProcesses aroView) {
        this.aroView = aroView;
    }


    private void setUpPlayer(int xPosition, int yPosition, int frameWidth, int frameHeight) {

        String title = MessageFormat.format(ResourceBundleHelper.getMessageString("aro.videoTitle"), ApplicationConfig.getInstance().getAppShortName());

        frame = new JFrame(title);
        frame.setIconImage(Images.ICON.getImage());
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                aroView.updateVideoPlayerSelected(false);
            }
        });

        // Have player in portrait orientation before a trace is loaded
        int playerContentWidth = VideoUtil.PLAYER_CONTENT_WIDTH_PORTRAIT;
        int playerContentHeight = VideoUtil.PLAYER_CONTENT_HEIGHT_PORTRAIT;

        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());

        /**
         *  Add a child panel for having 2 cards.
         *  One card is a dummy one to display when the video is finished or stopped.
         *  Another card is actual media player component which displays the video. This card is added/modified during loadVideo() method.
         */
        // Create Dummy card
        JLabel label = new JLabel("", Images.VLC_LOGO.getIcon(), JLabel.CENTER);
        dummyMediaComponentCardPanel = new JPanel(new BorderLayout());
        dummyMediaComponentCardPanel.add(label, BorderLayout.CENTER);
        dummyMediaComponentCardPanel.setBackground(Color.BLACK);
        dummyMediaComponentCardPanel.setPreferredSize(new Dimension(playerContentWidth, playerContentHeight));

        dummyMediaComponentCard = new JPanel(new GridBagLayout());
        dummyMediaComponentCard.add(dummyMediaComponentCardPanel, getGridBagConstraintsObject());
        dummyMediaComponentCard.setName(DUMMY_PLAYER_CARD);
        dummyMediaComponentCard.setVisible(true);
        // Add card to card layout panel
        JPanel childContentPaneCards = new JPanel(new CardLayout());
        childContentPaneCards.add(dummyMediaComponentCard, DUMMY_PLAYER_CARD);
        contentPane.add(childContentPaneCards, BorderLayout.CENTER);
        frame.setContentPane(contentPane);
        currentCard = DUMMY_PLAYER_CARD;


        JPanel controlsPane = new JPanel();
        playButton = new JButton(">");
        controlsPane.add(playButton);
        /*
         * In Linux, the slider's position value is displayed on top of the slider. This
         * line is to disable it.
         */
        UIManager.put("Slider.paintValue", false);
        slider = new JSlider();
        slider.setValue(0);
        slider.setMajorTickSpacing(10);
        controlsPane.add(slider);

        playtimeLabel = new JLabel("00:00/00:00");
        controlsPane.add(playtimeLabel);
        contentPane.add(controlsPane, BorderLayout.SOUTH);


        playButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (player != null) {
                    if (player.status().isPlaying()) {
                        player.controls().pause();
                        playButton.setText(">");
                    }
                    // Only if there's a video loaded
                    else if (!StringUtils.isBlank(player.media().info().mrl())) {
                        player.controls().start();
                        playButton.setText("||");
                    }
                }
            }
        });

        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sliderMouseEventProcessor(e);
            }
        });

        slider.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                sliderMouseEventProcessor(e);
            }
        });

        frame.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - playerContentWidth, 0);
        frame.pack();
        frame.setVisible(true);
    }

    private void sliderMouseEventProcessor(MouseEvent e) {
        if (player == null || player.media() == null || player.media().info() == null || StringUtils.isBlank(player.media().info().mrl()) || State.ENDED.equals(mediaPlayerStatus)) {
            slider.setValue(0);
            return;
        }

        Point p = e.getPoint();
        JSlider slider = (JSlider) (e.getSource());
        // Get percentage of the new position with respect to the total width of the slider
        double percent = p.x / ((double) slider.getWidth());
        if (percent < 0.0) {
            percent = 0.0;
        } else if (percent > 1.0) {
            percent = 1.0;
        }

        int range = slider.getMaximum() - slider.getMinimum();
        double newVal = range * percent;
        int result = (int) (slider.getMinimum() + newVal);
        slider.setValue(result);
        player.controls().setPosition((float) percent);

        double newPlaytimeInSeconds = Math.round(percent * duration);
        playtimeLabel.setText(formatPlaytimeDisplay(newPlaytimeInSeconds));

        if (!player.status().isPlaying()) {
            double crosshairValue = newPlaytimeInSeconds + videoOffset;
            diagnosticsTab.setTimeLineLinkedComponents(crosshairValue, true);
        }
    }

    private GridBagConstraints getGridBagConstraintsObject() {
        GridBagConstraints constraint = new GridBagConstraints();
        constraint.weightx = 1.0;
        constraint.weighty = 1.0;
        constraint.fill = GridBagConstraints.BOTH;

        return constraint;
    }

    private boolean createMediaPlayerComponent(int playerContentWidth, int playerContentHeight) {
        try {
            MediaPlayerFactory factory = new MediaPlayerFactory(MEDIA_PLAYER_FACTORY_ARGS);
            if (Util.isMacOS()) {
                mediaPlayerComponent = new CallbackMediaPlayerComponent(MediaPlayerSpecs.callbackMediaPlayerSpec().withFactory(factory));
                player = ((CallbackMediaPlayerComponent) mediaPlayerComponent).mediaPlayer();
            } else {
                mediaPlayerComponent = new EmbeddedMediaPlayerComponent(MediaPlayerSpecs.embeddedMediaPlayerSpec().withFactory(factory));
                player = ((EmbeddedMediaPlayerComponent) mediaPlayerComponent).mediaPlayer();
            }

            player.events().addMediaPlayerEventListener(getMediaPlayerEventListener());
            ((JPanel)mediaPlayerComponent).setPreferredSize(new Dimension(playerContentWidth, playerContentHeight));
            ((JPanel) mediaPlayerComponent).setName(MEDIA_PLAYER_CARD);
        } catch (Exception e) {
            LOGGER.error("Something went wrong while initializing media player components", e);
            showVlcjInformativeMsg();
            return false;
        }

        return true;
    }

    // if video duration is an hour or longer
    private boolean durationHourOrAbove() {
        return duration / 3600 >= 1;
    }

    private String getZeroPlaytimeDisplay() {
        String duration = getDurationDisplay();
        return durationHourOrAbove() ? "00:00:00/" + duration : "00:00/" + duration;
    }

    // Gets the video duration part (the total video duration) for the playtime label
    private String getDurationDisplay() {
        double durationHr = duration / 3600;
        double durationMin = duration / 60;
        double durationSec = duration - ((int) durationHr) * 3600 - ((int) durationMin) * 60;
        String durationDisplayOverHr = String.format("%d:%02d:%02d", (int) durationHr, (int) durationMin, (int) durationSec);
        String durationDisplayUnderHr = String.format("%02d:%02d", (int) durationMin, (int) durationSec);

        return durationHourOrAbove() ? durationDisplayOverHr : durationDisplayUnderHr;
    }

    // playtime expected to be in seconds
    private String formatPlaytimeDisplay(double playtime) {
        double playtimeHr = playtime / 3600;
        double playtimeMin = playtime / 60;
        double playtimeSec = playtime - ((int) playtimeHr) * 3600 - ((int) playtimeMin) * 60;
        String durationDisplay = getDurationDisplay();
        String timeDisplayOverHr = String.format("%d:%02d:%02d/", (int) playtimeHr, (int) playtimeMin, (int) playtimeSec) + durationDisplay;
        String timeDisplayUnderHr = String.format("%02d:%02d/", (int) playtimeMin, (int) playtimeSec) + durationDisplay;

        return durationHourOrAbove() ? timeDisplayOverHr : timeDisplayUnderHr;
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

        if (player == null) {
            LOGGER.debug("player is not available");
            return 0;
        }

        long timeInMilliSeconds = player.status().time();
        return (double) timeInMilliSeconds / 1000;
    }

    // hairlineTime expected to be in seconds
    @Override
    public void setMediaTime(final double hairlineTime) {
        double videoTime = hairlineTime - this.videoOffset;
        updateUserInterfaceElements(hairlineTime);
        if (player != null) {
	        if (!player.status().isSeekable()) {
	        	String videoOptions = "start-time=" + String.valueOf(videoTime);
	        	player.media().play(videoPath, videoOptions);
	        	player.controls().setPause(true);
	        }
	        player.controls().setTime(Math.round(videoTime * 1000));
        }
    }

    private void updateUserInterfaceElements(final double hairlineTime) {
        if (player == null) {
            LOGGER.debug("player is not available");
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
        slider.setValue((int) (videoTime * FRAME_RATE));
        playtimeLabel.setText(formatPlaytimeDisplay(hairlineTime));
    }

    @Override
    public double getDuration() {

        if (player == null) {
            LOGGER.debug("player is not available");
            return 0;
        }

        // Convert milliseconds to seconds
        return (double) player.media().info().duration() / 1000;
    }

    @Override
    public boolean isPlaying() {

        if (player == null) {
            LOGGER.debug("player is not available");
            return false;
        }

        return player.status().isPlaying();
    }

    @Override
    public void launchPlayer(final int xPosition, final int yPosition, final int frameWidth, final int frameHeight) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setUpPlayer(xPosition, yPosition, frameWidth, frameHeight);
            }
        });
    }

    private void showVlcjInformativeMsg() {
        MessageDialogFactory.showMessageDialog(null, ResourceBundleHelper.getMessageString("video.informative.vlcj"),
                ResourceBundleHelper.getMessageString("menu.info.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
	public void clear() {
		frame.setVisible(false);
		changeMediaComponentDisplayTo(DUMMY_PLAYER_CARD);
		playButton.setText(">");
		duration = 0.0;
		playtimeLabel.setText(formatPlaytimeDisplay(0.0));
		slider.setValue(0);
		if (player != null) {
			player.controls().stop();
			player.release();
			player = null;
		}

		SwingUtilities.invokeLater(new Runnable() {
	        @Override
	        public void run() {
	        	frame.dispose();
	        }
	    });

		videoOffset = 0.0;
	}

    @Override
    public VideoPlayerType getPlayerType() {
        return VideoPlayerType.MP4_VLCJ;
    }

    /**
     * Changes the current display card from the content pane of media player to target card
     * @param cardName
     *              - Currently there are two cards in the layout i.e., {@value #DUMMY_PLAYER_CARD} and {@value #MEDIA_PLAYER_CARD}
     */
    private void changeMediaComponentDisplayTo(String cardName) {
        BorderLayout contentPaneLayout = (BorderLayout) contentPane.getLayout();
        JPanel childContentPaneCards = (JPanel) contentPaneLayout.getLayoutComponent(BorderLayout.CENTER);
        CardLayout childContentPaneLayout = (CardLayout) childContentPaneCards.getLayout();
        childContentPaneLayout.show(childContentPaneCards, cardName);

        switch (cardName) {
            case MEDIA_PLAYER_CARD:
                frame.setPreferredSize(((JPanel)mediaPlayerComponent).getPreferredSize());
                break;
            case DUMMY_PLAYER_CARD:
                frame.setPreferredSize(dummyMediaComponentCardPanel.getPreferredSize());
                break;
        }
        frame.pack();
    }

    private void initMediaPlayerComponent(final int playerContentWidth, final int playerContentHeight) {
        if (player == null) {
            if (!createMediaPlayerComponent(playerContentWidth, playerContentHeight)) {
                return;
            }

            BorderLayout contentPaneLayout = (BorderLayout) contentPane.getLayout();
            JPanel childContentPaneCards = (JPanel) contentPaneLayout.getLayoutComponent(BorderLayout.CENTER);
            JPanel mediaComponentCard = new JPanel(new GridBagLayout());
            mediaComponentCard.add((JPanel) mediaPlayerComponent, getGridBagConstraintsObject());
            mediaComponentCard.setName(MEDIA_PLAYER_CARD);
            childContentPaneCards.removeAll();
            childContentPaneCards.add(dummyMediaComponentCard, DUMMY_PLAYER_CARD);
            childContentPaneCards.add(mediaComponentCard, MEDIA_PLAYER_CARD);
        } else {
            changeMediaComponentDisplayTo(DUMMY_PLAYER_CARD);
            ((JPanel) mediaPlayerComponent).setPreferredSize(new Dimension(playerContentWidth, playerContentHeight));
        }

        currentCard = DUMMY_PLAYER_CARD;
    }

    private MediaPlayerEventAdapter getMediaPlayerEventListener() {
        return
                new MediaPlayerEventAdapter() {
                    @Override
                    public void mediaPlayerReady(MediaPlayer player) {
                        mediaPlayerStatus = player.media().info().state();
                    }

                    @Override
                    public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {
                        slider.setValue(0);
                        playButton.setText(">");
                        diagnosticsTab.setTimeLineLinkedComponents(videoOffset, true);
                    }

                    @Override
                    public void positionChanged(MediaPlayer player, final float newPosition) {
                        if (!MEDIA_PLAYER_CARD.equals(currentCard)) {
                            changeMediaComponentDisplayTo(MEDIA_PLAYER_CARD);
                            currentCard = MEDIA_PLAYER_CARD;
                        }

                        // newPlaytime is in seconds
                        final float newPlaytime = Math.round(newPosition * duration); // newPosition is % between 0 and 1
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                slider.setValue(Math.round(newPlaytime * FRAME_RATE));
                                playtimeLabel.setText(formatPlaytimeDisplay(newPlaytime));
                            }
                        });
                    }

                    @Override
                    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                double crosshairValue = (double) newTime / 1000 + videoOffset;
                                if (crosshairValue < 0) {
                                    crosshairValue = 0.0;
                                }
                                diagnosticsTab.setTimeLineLinkedComponents(crosshairValue, true);
                            }
                        });
                    }

                    @Override
                    public void finished(MediaPlayer player) {
                        mediaPlayerStatus = player.media().info().state();
                        playButton.setText(">");
                        slider.setValue(0);
                        slider.repaint();
                        playtimeLabel.setText(getZeroPlaytimeDisplay());
                    }
                };
    }

    @Override
    public void loadVideo(AbstractTraceResult traceResult) {
        /*
         * Without calling stop(), the player view will still show the image from the
         * previous video after calling prepareMedia() on the new video.
         */
        String movVideoPath = traceResult.getTraceDirectory() + Util.FILE_SEPARATOR + ResourceBundleHelper.getMessageString("video.videoDisplayFile");
        String mp4VideoPath = traceResult.getTraceDirectory() + Util.FILE_SEPARATOR + ResourceBundleHelper.getMessageString("video.videoFileOnDevice");
        
        videoPath = new File(mp4VideoPath).exists() ? mp4VideoPath : movVideoPath;

        /*
         * start then pause in order for graph panel and video player to be able to go
         * in sync before user starts playing the video
         */
        String videoOptions = "";
        double beginTime = 0;
        AnalysisFilter filter = ((MainFrame) aroView).getController().getTheModel().getAnalyzerResult().getFilter();
        if (null != filter) {
            beginTime = filter.getTimeRange().getBeginTime();
            videoOptions = "start-time=" + String.valueOf(beginTime);
        }

        int playerContentWidth, playerContentHeight;
        if (VideoUtil.isVideoLandscape(traceResult)) {
            playerContentWidth = VideoUtil.PLAYER_CONTENT_WIDTH_LANDSCAPE;
            playerContentHeight = VideoUtil.PLAYER_CONTENT_HEIGHT_LANDSCAPE;
        } else {
            playerContentWidth = VideoUtil.PLAYER_CONTENT_WIDTH_PORTRAIT;
            playerContentHeight = VideoUtil.PLAYER_CONTENT_HEIGHT_PORTRAIT;
        }

        initMediaPlayerComponent(playerContentWidth, playerContentHeight);
        if (player == null) {
            LOGGER.error("player is not available!");
            return;
        }

        frame.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - playerContentWidth, 0);
        frame.pack();

        player.media().start(videoPath, videoOptions);
        // Make sure the media has been started playing
        long start = System.currentTimeMillis();
        while (player.status().position() == 0) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                LOGGER.error("The thread was interrupted from sleep", e);
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.debug("Time taken to start the media " + (end-start));
        player.controls().setPause(true);
        playButton.setText(">");

        duration = getDuration();
        double videoStartTime = traceResult.getVideoStartTime();
        this.videoOffset = videoStartTime > 0.0 ? videoStartTime - ((double) traceResult.getTraceDateTime().getTime() / 1000) : 0.0;
        if (Math.abs(videoOffset) >= duration) {
            videoOffset = 0;
        }

        updateUserInterfaceElements(beginTime);

        /*
         * Adjust slider's max for each new video for more accurate and custom fitting
         * to the timestamp.
         */
        slider.setMaximum((int) (duration * FRAME_RATE));
        diagnosticsTab.setGraphPanelClicked(false);
    }

    @Override
    public void notifyLauncher(boolean enabled) {
        aroView.updateVideoPlayerSelected(enabled);
    }
}
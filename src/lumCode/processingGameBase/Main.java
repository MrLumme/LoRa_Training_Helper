package lumCode.processingGameBase;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Properties;

import lumCode.interactables.entities.Button;
import lumCode.interactables.entities.TextBox;
import lumCode.processingGameBase.keys.Input;
import lumCode.processingGameBase.keys.InputTracker;
import lumCode.processingGameBase.lora.CaptionEntry;
import lumCode.processingGameBase.sound.SoundKeeper;
import lumCode.processingGameBase.time.TimeKeeper;
import lumCode.processingGameBase.time.Trigger;
import lumCode.utils.ExMath;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;

public class Main extends PApplet {

	// ---------
	// CONSTANTS
	// ---------

	public static final int SCREEN_WIDTH = 1200;
	public static final int SCREEN_HEIGHT = 520;
	private static PApplet instance;

	// -----------------
	// PROGRAM VARIABLES
	// -----------------

	public static boolean doTick = false;
	public static Properties prop = new Properties();
	public static PFont font;

	private static File workDir;

	public static final ArrayList<CaptionEntry> captions = new ArrayList<>();
	public static CaptionEntry currentCaption = null;
	public static int captionId = 0;

	private static Button next, prev, allKeyword;
	private static TextBox newKeyword;

	// ----
	// MAIN
	// ----

	public static void main(String[] args) {
		main("lumCode.processingGameBase.Main");
	}

	// --------
	// SETTINGS
	// --------

	@Override
	public void settings() {
		size(SCREEN_WIDTH, SCREEN_HEIGHT);
		noSmooth();
	}

	// -----
	// SETUP
	// -----

	@Override
	public void setup() {
		instance = this;
		frameRate(Settings.FRAME_RATE);

		SoundKeeper sk = SoundKeeper.getInstance();
		TimeKeeper tk = TimeKeeper.getInstance();

		loadProperties();

		font = loadFont(Settings.FONT_PATH + "default.vlw");

		selectFolder("Select LoRa image- & captionfile directory:", "loadWorkDirectory");
		next = new Button(SCREEN_WIDTH - 90, SCREEN_HEIGHT - 40, 80, 30, instance, "Next >", font) {
			@Override public void action() {
				if (captionId < (captions.size() - 1)) {
					captionId++;
					currentCaption.saveKeywords();
					currentCaption = captions.get(captionId);
				}
			}
		};
		next.setOver(Colors.D_GREY);
		newKeyword = new TextBox(next.getX() - 170, next.getY(), 160, 30, instance, true, true, font);
		prev = new Button(newKeyword.getX() - 90, next.getY(), 80, 30, instance, "< Previous", font) {
			@Override public void action() {
				previousCaption();
			}
		};
		prev.setOver(Colors.D_GREY);
		allKeyword = new Button(prev.getX() - 200, next.getY(), 160, 30, instance, "Add keyword to all", font) {
			@Override public void action() {
				nextCaption();
			}
		};
		allKeyword.setOver(Colors.D_GREY);
		allKeyword.setDisabled(true);

		sk.start();
		tk.start();
		tk.addTrigger(new Trigger() {
			@Override public boolean condition() {
				boolean allLoaded = true;
				for (CaptionEntry caption : captions) {
					if (!caption.imageLoaded()) {
						allLoaded = false;
					}
				}
				return allLoaded;
			}

			@Override public void action() {
				allKeyword.setDisabled(false);
			}
		});

		doTick = true;
	}

	// ----
	// DRAW
	// ----

	@Override
	public void draw() {
		background(128);

		if (currentCaption != null) {
			currentCaption.draw();
			next.draw();
			prev.draw();
			newKeyword.draw();
			allKeyword.draw();
		} else {

		}
	}

	// ----
	// EXIT
	// ----

	@Override
	public void exit() {
		super.exit();
		saveProperties();
		for (CaptionEntry caption : captions) {
			if (caption.isChanged() && caption.imageLoaded()) {
				caption.saveKeywords();
			}
		}
	}

	// -----
	// INPUT
	// -----

	@Override
	public void mouseClicked() {
	}

	@Override
	public void mousePressed() {
	}

	@Override
	public void mouseReleased() {
		for (Button keyword : currentCaption.keywords) {
			if (keyword.mouseClicked()) {
				break;
			}
		}
		next.mouseClicked();
		prev.mouseClicked();
		newKeyword.mouseClicked();
		allKeyword.mouseClicked();
	}

	@Override
	public void keyPressed() {
		if (keyCode == PConstants.ENTER) {
			InputTracker.state(Input.SUBMIT, true);
		} else if (keyCode == PConstants.LEFT) {
			InputTracker.state(Input.PREVIOUS, true);
		} else if (keyCode == PConstants.RIGHT) {
			InputTracker.state(Input.NEXT, true);
		}

		newKeyword.keyTyped();
		newKeyword.keyPressed();

		if (InputTracker.getState(Input.SUBMIT)) {
			if (currentCaption != null) {
				if (!newKeyword.getText().trim().equals("")) {
					currentCaption.addKeyword(newKeyword.getText().trim());
					newKeyword.setText("");
				}
			}
		} else if (InputTracker.getState(Input.PREVIOUS)) {
			previousCaption();
		} else if (InputTracker.getState(Input.NEXT)) {
			nextCaption();
		}
	}

	@Override
	public void keyReleased() {
		if (keyCode == PConstants.ENTER) {
			InputTracker.state(Input.SUBMIT, false);
		} else if (keyCode == PConstants.LEFT) {
			InputTracker.state(Input.PREVIOUS, false);
		} else if (keyCode == PConstants.RIGHT) {
			InputTracker.state(Input.NEXT, false);
		}
	}

	public void loadWorkDirectory(File file) throws IOException {
		if (file == null) {
			return;
		}

		workDir = file;

		for (File imgFile : workDir.listFiles()) {
			if (imgFile.isFile() && imgFile.getName().endsWith(".png")) {
				File kwFile = new File(imgFile.getAbsolutePath().replace(".png", ".txt"));
				captions.add(new CaptionEntry(imgFile, kwFile, this));
			}
		}

		if (!captions.isEmpty()) {
			currentCaption = captions.get(0);
		}
	}

	// ---------
	// UTILITIES
	// ---------

	public static PApplet instance() {
		return instance;
	}

	public static void loadProperties() {
		try {
			prop.load(new FileReader(Settings.PROPERTIES_PATH));

			SoundKeeper.setMasterVolume(
					ExMath.clamp(Double.parseDouble(prop.getProperty("master_vol", "" + SoundKeeper.getMasterVolume())),
							0.000, 1.000));
			SoundKeeper.setEffectVolume(ExMath.clamp(
					Double.parseDouble(prop.getProperty("effects_vol", "" + SoundKeeper.getEffectVolume())), 0.000,
					1.000));
			SoundKeeper.setMusicVolume(
					ExMath.clamp(Double.parseDouble(prop.getProperty("music_vol", "" + SoundKeeper.getMusicVolume())),
							0.000, 1.000));
			SoundKeeper.setVoiceVolume(
					ExMath.clamp(Double.parseDouble(prop.getProperty("voice_vol", "" + SoundKeeper.getVoiceVolume())),
							0.000, 1.000));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveProperties() {
		try {
			DecimalFormatSymbols dc = new DecimalFormatSymbols();
			dc.setDecimalSeparator('.');
			NumberFormat format = new DecimalFormat("0.000", dc);

			prop.setProperty("master_vol", format.format(SoundKeeper.getMasterVolume()));
			prop.setProperty("effect_vol", format.format(SoundKeeper.getEffectVolume()));
			prop.setProperty("music_vol", format.format(SoundKeeper.getMusicVolume()));
			prop.setProperty("voice_vol", format.format(SoundKeeper.getVoiceVolume()));

			prop.store(new FileWriter(Settings.PROPERTIES_PATH), "saved-" + System.currentTimeMillis());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean nextCaption() {
		if (captionId < (captions.size() - 1)) {
			captionId++;
			currentCaption.saveKeywords();
			currentCaption = captions.get(captionId);
			return true;
		}
		return false;
	}

	public boolean previousCaption() {
		if (captionId > 0) {
			captionId--;
			currentCaption.saveKeywords();
			currentCaption = captions.get(captionId);
			return true;
		}
		return false;
	}
}
